package learning.protocols;

import learning.interfaces.AbstractProtocol;
import learning.interfaces.Model;
import learning.interfaces.ModelHolder;
import learning.main.Main;
import learning.messages.*;
import learning.modelHolders.BoundedModelHolder;
import learning.models.MergeableLogisticRegression;
import learning.node.ETreeNode;
import learning.utils.SparseVector;
import learning.utils.Utils;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * @author sshpark
 * @date 11/2/2020
 */
public class ETreeLearningProtocol extends AbstractProtocol {
    private final static String PAR_MODELHOLDERNAME = "modelHolderName";
    private final static String PAR_MODELNAME = "modelName";
    private final static String PAR_LAYERS = "layers";
    private final static String PAR_RATIOS = "ratios";
    private static final String PAR_RECVPERCENT = "recvPercent";

    /** @hidden */
    private final String modelHolderName;
    private final String modelName;
    private final int layers;
    private final double recvPercent;

    /** useful variable */
    private MergeableLogisticRegression[] layersWorkerModel;
    private ModelHolder[] layersReceivedModels;
    private ArrayList<Integer>[] layersReceivedID;
    private int[] aggregateRatio;
    private int[] aggregateCount;
    private static ArrayList<ArrayList<Integer>> layersNodeID;
    private int currentMaxLayer;

    public ETreeLearningProtocol(String prefix) {
        modelHolderName = Configuration.getString(prefix + "." + PAR_MODELHOLDERNAME);
        modelName = Configuration.getString(prefix + "." + PAR_MODELNAME);
        layers = Configuration.getInt(prefix + "." + PAR_LAYERS);
        recvPercent = Configuration.getDouble(prefix + "." + PAR_RECVPERCENT);
        init(prefix);
    }

    /**
     * Copy constructor
     * @param prefix
     * @param modelHolderName
     * @param modelName
     * @param layers
     */
    private ETreeLearningProtocol(String prefix, String modelHolderName, String modelName, int layers, double recvPercent) {
        this.modelHolderName = modelHolderName;
        this.modelName = modelName;
        this.layers = layers;
        this.recvPercent = recvPercent;
        init(prefix);
    }

    protected void init(String prefix) {
        try {
            super.init(prefix);
            // init received models
            layersReceivedModels = new ModelHolder[layers];
            for (int i = 0; i < layers; i++) {
                layersReceivedModels[i] = (ModelHolder)Class.forName(modelHolderName).getConstructor().newInstance();
                layersReceivedModels[i].init(prefix);
            }

            // init worker model
            layersWorkerModel = new MergeableLogisticRegression[layers];
            for (int i = 0; i < layers; i++) {
                layersWorkerModel[i] = (MergeableLogisticRegression) Class.forName(modelName).getConstructor().newInstance();
                layersWorkerModel[i].init(prefix);
            }

            // init ratios
            String[] agg_ratios = Configuration.getString(prefix + "." + PAR_RATIOS).split(",");
            if (agg_ratios.length != layers)
                throw new RuntimeException("The size of ratios must be equal to layers");
            aggregateRatio = new int[layers];
            for (int i = 0; i < layers; i++)
                aggregateRatio[i] = Integer.parseInt(agg_ratios[i]);

            // init aggregate count
            aggregateCount = new int[layers];

            // init layersReceivedID
            layersReceivedID = new ArrayList[layers];
            for (int i = 0; i < layers; i++) layersReceivedID[i] = new ArrayList<>();

            currentMaxLayer = 1;
            cycle = 1;
        } catch (Exception e) {
            throw new RuntimeException("Exception occured in initialization of " + getClass().getCanonicalName() + ": " + e);
        }
    }

    @Override
    public void processEvent(Node currentNode, int currentProtocolID, Object messageObj) {
        this.currentNode = currentNode;
        this.currentProtocolID = currentProtocolID;

        // first layer
        if (messageObj instanceof ActiveThreadMessage) {
            int currentLayer = 0;
            ETreeNode node = (ETreeNode) currentNode;

            // update model
            MergeableLogisticRegression wkmodel = layersWorkerModel[currentLayer];
            wkmodel = workerUpdate(wkmodel);

            // send to next layer
            ModelHolder latestModelHolder = new BoundedModelHolder(1);
            latestModelHolder.add(wkmodel);
            sendTo(new MessageUp(node, currentLayer, latestModelHolder), node.getParentNode(currentLayer));

        } else if (messageObj instanceof MessageUp) { // receive message from child node
            int layer = ((MessageUp) messageObj).getLayer()+1;  // current layer = source layer+1
            // If node is not selected in this round, than return
            if (!isSelected(layer, ((MessageUp) messageObj).getSource().getIndex())) return;

            // current node has aggregated
            ((ETreeNode)currentNode).setLayersStatus(layer, true);

            // add model to current layer's received model
            layersReceivedModels[layer].add((MergeableLogisticRegression)((MessageUp) messageObj).getModel(0).clone());

            // current node's child node size
            int numOfChildNode = Math.max((int)(((ETreeNode)currentNode).getChildNodeList(layer).size() * recvPercent), 1);
            // judge whether to aggregate
            if (layersReceivedModels[layer].size() == numOfChildNode) {
                // current layer has aggregated
                ((ETreeNode) currentNode).setLayersStatus(layer, true);
                // update next selected workers
                layersReceivedID[layer] = Utils.randomArray(numOfChildNode, ((ETreeNode) currentNode).getChildNodeList(layer));

                // add worker model where from current layer
                MergeableLogisticRegression workerModel = layersWorkerModel[layer];
                layersReceivedModels[layer].add(workerModel);

                // aggregate receive model
                workerModel = workerModel.aggregateDefault(layersReceivedModels[layer]);
                // broadcast to its child node
                bfs((ETreeNode) currentNode, layer, workerModel);

                // after aggregate, we should update some information
                layersWorkerModel[layer] = workerModel;
                aggregateCount[layer]++;
                layersReceivedModels[layer].clear();

                // whether to send nodes to the next layer
                if (aggregateCount[layer] % aggregateRatio[layer] == 0) {
                    if (layer != layers-1) {
                        // update current max layer
                        currentMaxLayer = Math.max(currentMaxLayer, layer+1);
                        // send to next layer
                        ModelHolder latestModelHolder = new BoundedModelHolder(1);
                        latestModelHolder.add(workerModel.clone());
                        sendTo(new MessageUp(currentNode, layer, latestModelHolder),
                                ((ETreeNode) currentNode).getParentNode(layer));
                    } else {
                        // output loss and accuracy
                        computeLoss(workerModel);
                    }
                }
            } // finished aggregated
            if (canSatrtedNextEpoch()) {
                resetForNextEpoch();
                for (int i = 0; i < Network.size(); i++) {
                    Node node = Network.get(i);
                    // schedule starter alarm
                    EDSimulator.add(0, ActiveThreadMessage.getInstance(), node, currentProtocolID);
                }
            }
        }
    }

    private MergeableLogisticRegression workerUpdate(MergeableLogisticRegression model) {
        // SGD
        for (int sampleID = 0; instances != null && sampleID < instances.size(); sampleID++) {
            // we use each samples for updating the currently processed model
            SparseVector x = instances.getInstance(sampleID);
            double y = instances.getLabel(sampleID);
            model.update(x, y);
        }
        return model.clone();
    }

    /**
     * Send the model to all child nodes of root.
     * It should be noted that the simulation time is updated during the model release process.
     * @param root
     * @param layer
     * @param model
     */
    private void bfs(ETreeNode root, int layer, MergeableLogisticRegression model) {
        Queue<ETreeNode> q = new LinkedList<>();
        q.offer(root);
        while (!q.isEmpty()) {
            ETreeNode top = q.poll();
            int cnt = top.getChildNodeList(layer).size();
            for (int i = 0; i < cnt; i++) {
                for (Integer id : top.getChildNodeList(layer)) {
                    ETreeNode temp = (ETreeNode) Network.get(id);
                    // update node's model
                    ETreeLearningProtocol temp_node_pro = (ETreeLearningProtocol) temp.getProtocol(currentProtocolID);
                    temp_node_pro.setLayersWorkerModel(layer-1, model.clone());
                    q.offer(temp);
                }
            }
            layer--;
            if (layer == 0) break;
        }
    }

    public void computeLoss(MergeableLogisticRegression model) {
        // loss
        double errs = 0.0;
        for (int testIdx = 0; eval != null && testIdx < eval.size(); testIdx++) {
            SparseVector testInstance = eval.getInstance(testIdx);
            double y = eval.getLabel(testIdx);
            double[] pred = model.distributionForInstance(testInstance);
            errs += crossEntropyLoss(y, pred) + r/2*model.getWeight().square();
        }
        errs = errs / eval.size();
        cycle++;
        Main.addLoss(CommonState.getTime(), errs);
        System.err.print("Time: "+ CommonState.getTime() + ", ETree 0-1 error: " + errs);

        // accuracy
        errs = 0.0;
        for (int testIdx = 0; eval != null && testIdx < eval.size(); testIdx++) {
            SparseVector testInstance = eval.getInstance(testIdx);
            double y = eval.getLabel(testIdx);
            double pred = model.predict(testInstance);
            errs += (y == pred) ? 0.0 : 1.0;
        }
        errs = errs / eval.size();
        System.err.println(", " + (1.0-errs));
    }

    /**
     * send to specified node
     * @param message
     * @param dst node id
     */
    private void sendTo(ModelMessage message, int dst) {
        message.setSource(currentNode);
        Node node = Network.get(dst);
        getTransport().send(currentNode, node, message, currentProtocolID);
    }

    /**
     * Returns if we can start next epoch
     * @return
     */
    private boolean canSatrtedNextEpoch() {
        boolean flag = true;
        for (Integer id : layersNodeID.get(1)) {
            ETreeNode node = (ETreeNode) Network.get(id);
            if (!node.getLayersStatus(1)) {
                return false;
            }
        }
        if (flag) {
            for (Integer id : layersNodeID.get(1)) {
                ETreeNode node = (ETreeNode) Network.get(id);
                node.setLayersStatus(1, false);
            }
            return true;
        }

        for (int layer = currentMaxLayer; layer > 1; layer--) {
            for (Integer id : layersNodeID.get(layer)) {
                ETreeNode node = (ETreeNode) Network.get(id);
                if (!node.getLayersStatus(layer)) {
                    return false;
                }
            }
        }

        // else reset status
        for (int layer = currentMaxLayer; layer > 1; layer--) {
            for (Integer id : layersNodeID.get(layer)) {
                ETreeNode node = (ETreeNode) Network.get(id);
                node.setLayersStatus(layer, false);
            }
        }
        return true;
    }

    private void resetForNextEpoch() {
        int maxDelayPath = 0;
        for (Integer rootId : layersNodeID.get(currentMaxLayer)) {
            int layer = currentMaxLayer;
            ETreeNode root = (ETreeNode) Network.get(rootId);
            Queue<ETreeNode> q = new LinkedList<>();
            q.offer(root);
            int tempMaxDelayPath = 0;
            int src = root.getIndex();
            while (!q.isEmpty()) {
                ETreeNode top = q.poll();
                int cnt = top.getChildNodeList(layer).size();
                int delay = 0;
                for (int i = 0; i < cnt; i++) {
                    for (Integer id : top.getChildNodeList(layer)) {
                        ETreeNode temp = (ETreeNode) Network.get(id);
                        q.offer(temp);
                        delay = Math.max(delay, minDelayMatrix[src][id]);
                    }
                }
                tempMaxDelayPath += delay;
                layer--;
                if (layer == 0) break;
            }
            maxDelayPath = Math.max(maxDelayPath, tempMaxDelayPath);
        }
//        System.out.println("maxDelay: " + maxDelayPath);
        currentMaxLayer = 1;
        CommonState.setTime( CommonState.getTime() + maxDelayPath);
    }

    @Override
    public Object clone() {
        return new ETreeLearningProtocol(prefix, modelHolderName, modelName, layers, recvPercent);
    }

    /*-------------------------------- Neglect started ------------------------------------*/
    @Override
    public void activeThread() {

    }

    @Override
    public void passiveThread(ModelMessage message) {

    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public ModelHolder getModelHolder(int index) {
        return null;
    }

    @Override
    public void setModelHolder(int index, ModelHolder modelHolder) {

    }

    @Override
    public boolean add(ModelHolder modelHolder) {
        return false;
    }

    @Override
    public ModelHolder remove(int index) {
        return null;
    }

    @Override
    public Model getWorkerModel() {
        return null;
    }
    /*-------------------------------- Neglect finished ------------------------------------*/

    public MergeableLogisticRegression getLayersWorkerModel(int layer) {
        return layersWorkerModel[layer];
    }

    public void setLayersWorkerModel(int layer, MergeableLogisticRegression model) {
        layersWorkerModel[layer] = model;
    }

    public static void setLayersNodeID(ArrayList<ArrayList<Integer>> layersNodes) {
        layersNodeID = layersNodes;
    }

    public void setLayersReceivedID(int layer, ArrayList<Integer> layersReceivedID) {
        this.layersReceivedID[layer] = layersReceivedID;
    }
    private boolean isSelected(int layer, int id) {
        return layersReceivedID[layer].contains(id);
    }
}
