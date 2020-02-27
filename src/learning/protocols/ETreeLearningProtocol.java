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
    private ArrayList<Integer>[] layersSelectedID;
    private int[] aggregateRatio;
    private int[] aggregateCount;
    private static ArrayList<ArrayList<Integer>> layersNodeID;

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
            layersSelectedID = new ArrayList[layers];
            for (int i = 0; i < layers; i++) layersSelectedID[i] = new ArrayList<>();

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
            latestModelHolder.add(wkmodel.clone());
            sendTo(new MessageUp(node, currentLayer, latestModelHolder), node.getParentNode(currentLayer));
        } else if (messageObj instanceof MessageUp) { // receive message from child node
//            System.out.println("Time: " + CommonState.getTime() + ", current node: " + currentNode.getIndex() + ", recv from: "
//                    + ((MessageUp) messageObj).getSource().getIndex() +
//                    ", " + ((MessageUp) messageObj).getLayer());

            int layer = ((MessageUp) messageObj).getLayer()+1;  // current layer = source layer+1
//            System.out.println("curren layer: " + layer);
            // If node is not selected in this round, than return
            if (!isSelected(layer, ((MessageUp) messageObj).getSource().getIndex())) { return; }

            // add model to current layer's received model
            layersReceivedModels[layer].add((MergeableLogisticRegression)((MessageUp) messageObj).getModel(0).clone());

            // current node's child node size
            int numOfChildNode = Math.max((int)(((ETreeNode)currentNode).getChildNodeList(layer).size() * recvPercent), 1);
            // judge whether to aggregate
            if (layersReceivedModels[layer].size() == numOfChildNode) {
                // current layer has aggregated
                ((ETreeNode) currentNode).setLayersStatus(layer, true);

                // add worker model where from current layer
                MergeableLogisticRegression workerModel = layersWorkerModel[layer];
                layersReceivedModels[layer].add(workerModel.clone());

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
//                    System.out.println(currentNode.getIndex() + " " + layer + " " + aggregateCount[layer]);
                    if (layer != layers-1) {
                        // send to next layer
                        if (canUpMessage(layer)) {
                            // global max layer
                            CommonState.setPhase( Math.max(CommonState.getPhase(), layer+1) );
                            upMessageToNextLayer(layer);
                        }
                    } else {
                        CommonState.setPhase(layers-1);
                        // output loss and accuracy
                        computeLoss(workerModel);
                    }
                }
                if (canStartedNextEpoch()) {
                    resetForNextEpoch();
//                    System.out.println("start next epoch time: " + CommonState.getTime());
                    for (int i = 0; i < Network.size(); i++) {
                        Node node = Network.get(i);
                        // schedule starter alarm
                        EDSimulator.add(0, ActiveThreadMessage.getInstance(), node, currentProtocolID);
                    }
                }
            } // finished aggregated

        }
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
            int cnt = q.size();
            for (int i = 0; i < cnt; i++) {
                ETreeNode top = q.poll();
                for (Integer id : top.getChildNodeList(layer)) {
                    ETreeNode temp = (ETreeNode) Network.get(id);
                    // update node's model
                    ETreeLearningProtocol temp_node_pro = (ETreeLearningProtocol) temp.getProtocol(currentProtocolID);
                    temp_node_pro.setLayersWorkerModel(layer-1, model.clone());
                    q.offer(temp);
                }
            }
            layer--;
        }
    }

    /**
     * Output loss and accuracy
     * @param model
     */
    public void computeLoss(MergeableLogisticRegression model) {
        // loss
        double losses = 0.0;
        for (int testIdx = 0; eval != null && testIdx < eval.size(); testIdx++) {
            SparseVector testInstance = eval.getInstance(testIdx);
            double y = eval.getLabel(testIdx);
            double[] pred = model.distributionForInstance(testInstance);
            losses += crossEntropyLoss(y, pred) + r/2*model.getWeight().square();
        }
        losses = losses / eval.size();
        cycle++;
        System.err.print("Time: "+ CommonState.getTime() + ", ETree loss: " + losses);

        // error rate
        double errs = 0.0;
        for (int testIdx = 0; eval != null && testIdx < eval.size(); testIdx++) {
            SparseVector testInstance = eval.getInstance(testIdx);
            double y = eval.getLabel(testIdx);
            double pred = model.predict(testInstance);
            errs += (y == pred) ? 0.0 : 1.0;
        }
        errs = errs / eval.size();
        Main.addLoss(CommonState.getTime(), losses, 1-errs);
        System.err.println(", acc: " + (1.0-errs));
    }

    /**
     * send to specified node
     * @param message
     * @param dst node id
     */
    private void sendTo(ModelMessage message, int dst) {
        Node node = Network.get(dst);
        getTransport().send(message.getSource(), node, message, currentProtocolID);
    }

    /**
     * Returns if we can start next epoch
     * @return
     */
    private boolean canStartedNextEpoch() {
        final int currentMaxLayer = CommonState.getPhase();
        for (Integer id : layersNodeID.get(currentMaxLayer)) {
            ETreeNode node = (ETreeNode) Network.get(id);
            if (!node.getLayersStatus(currentMaxLayer)) {
                return false;
            }
        }
//        System.out.println("layer " + currentMaxLayer + " finished at time " + CommonState.getTime());
        // else reset status
        for (int layer = currentMaxLayer; layer > 0; layer--) {
            for (Integer id : layersNodeID.get(layer)) {
                ETreeNode node = (ETreeNode) Network.get(id);
                node.setLayersStatus(layer, false);
            }
        }
        return true;
    }

    /**
     * reset status for next epoch
     */
    private void resetForNextEpoch() {
        int currentMaxLayer = CommonState.getPhase();
        int maxDelayPath = 0;
        for (Integer rootId : layersNodeID.get(currentMaxLayer)) {
            int layer = currentMaxLayer;
            ETreeNode root = (ETreeNode) Network.get(rootId);
            Queue<ETreeNode> q = new LinkedList<>();
            q.offer(root);
            int tempMaxDelayPath = 0;
            while (!q.isEmpty()) {
                int cnt = q.size();
                int delay = 0;

                for (int i = 0; i < cnt; i++) {
                    ETreeNode top = q.poll();
                    ArrayList<Integer> listID = ((ETreeLearningProtocol) top.getProtocol(currentProtocolID)).getLayersSelectedID(layer);
                    for (Integer id : listID) {
                        ETreeNode temp = (ETreeNode) Network.get(id);
                        q.offer(temp);
                        delay = Math.max(delay, minDelayMatrix[top.getIndex()][id]);
                    }
                }
                tempMaxDelayPath += delay;
                layer--;
            }
            maxDelayPath = Math.max(maxDelayPath, tempMaxDelayPath);
        }
        // update next selected workers
        for (int layer = currentMaxLayer; layer > 0; layer--) {
            for (Integer id : layersNodeID.get(layer)) {
                ETreeNode node = (ETreeNode) Network.get(id);
                ArrayList<Integer> childList = new ArrayList<>(node.getChildNodeList(layer));
                ArrayList<Integer> selectedWorkers = Utils.randomArray(Math.max((int) (childList.size() * recvPercent), 1), childList);
                ((ETreeLearningProtocol) node.getProtocol(currentProtocolID)).setLayersSelectedID(layer, selectedWorkers);
            }
        }
        CommonState.setPhase(1);
        CommonState.setTime( CommonState.getTime() + maxDelayPath);
    }

    /**
     * The nodes in the same layer send the message to the next layer
     * after the aggregation is completed.
     * @param layer
     * @return
     */
    private boolean canUpMessage(int layer) {
        for (Integer id : layersNodeID.get(layer)) {
            ETreeNode node = (ETreeNode) Network.get(id);
            if (!node.getLayersStatus(layer)) return false;
        }
        return true;
    }

    /**
     * send model to next layer
     * @param layer
     */
    private void upMessageToNextLayer(int layer) {
        for (Integer id : layersNodeID.get(layer)) {
            // current node
            ETreeNode node = (ETreeNode) Network.get(id);
            ETreeLearningProtocol node_pro = (ETreeLearningProtocol) node.getProtocol(currentProtocolID);
            MergeableLogisticRegression workerModel = node_pro.getLayersWorkerModel(layer);
            // parent node
            ETreeNode node_parent = (ETreeNode)Network.get(node.getParentNode(layer));
            ETreeLearningProtocol node_parent_pro = (ETreeLearningProtocol) node_parent.getProtocol(currentProtocolID);

            if (node_parent_pro.isSelected(layer+1, id)) {
                // send to next layer
                ModelHolder latestModelHolder = new BoundedModelHolder(1);
                latestModelHolder.add(workerModel.clone());
                sendTo(new MessageUp(node, layer, latestModelHolder), node.getParentNode(layer));
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

    public void setLayersSelectedID(int layer, ArrayList<Integer> layersSelectedID) {
        this.layersSelectedID[layer] = layersSelectedID;
    }
    public ArrayList<Integer> getLayersSelectedID(int layer) {
        return layersSelectedID[layer];
    }
    private boolean isSelected(int layer, int id) {
        return layersSelectedID[layer].contains(id);
    }
}
