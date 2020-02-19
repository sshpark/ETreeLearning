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

    /** @hidden */
    private final String modelHolderName;
    private final String modelName;
    private final int layers;

    private Model[] layersWorkerModel;
    private ModelHolder[] layersReceivedModels;
    private static ArrayList<ArrayList<Integer>> layersNodeID;
    private int[] aggregateRatio;
    private int[] aggregateCount;


    public ETreeLearningProtocol(String prefix) {
        modelHolderName = Configuration.getString(prefix + "." + PAR_MODELHOLDERNAME);
        modelName = Configuration.getString(prefix + "." + PAR_MODELNAME);
        layers = Configuration.getInt(prefix + "." + PAR_LAYERS);
        init(prefix);
    }

    /**
     * Copy constructor
     * @param prefix
     * @param modelHolderName
     * @param modelName
     * @param layers
     */
    private ETreeLearningProtocol(String prefix, String modelHolderName, String modelName, int layers) {
        this.modelHolderName = modelHolderName;
        this.modelName = modelName;
        this.layers = layers;
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
            layersWorkerModel = new Model[layers];
            for (int i = 0; i < layers; i++) {
                layersWorkerModel[i] = (Model)Class.forName(modelName).getConstructor().newInstance();
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
            Model wkmodel = layersWorkerModel[currentLayer];
            wkmodel = workerUpdate(wkmodel);
            layersWorkerModel[currentLayer] = wkmodel;

            // send to next layer
            ModelHolder latestModelHolder = new BoundedModelHolder(1);
            latestModelHolder.add(wkmodel);
            sendTo(new MessageUp(node, currentLayer, latestModelHolder), node.getParentNode(currentLayer));

        } else if (messageObj instanceof MessageUp) { // receive message from child node
            int layer = ((MessageUp) messageObj).getLayer()+1;  // current layer = source layer+1
            // tell to next update
            if (layer == 1) {
                EDSimulator.add(0, ActiveThreadMessage.getInstance(), ((MessageUp) messageObj).getSource(),
                        currentProtocolID);
            }

//            System.out.println("current time: " + CommonState.getTime() + ", current node: " + currentNode.getID() + ", src: " + ((MessageUp) messageObj).getSource().getID()
//            +", current layer: " + layer);

            // gets the received model from current layer
            ModelHolder receivedModel = layersReceivedModels[layer];

            // add model to current layer's received model
            receivedModel.add(((MessageUp) messageObj).getModel(0));
            // current node's child node size
            int numOfChildNode = ((ETreeNode)currentNode).getChildNodeList(layer).size();
            // update layersReceivedModels
            layersReceivedModels[layer] = receivedModel;

            // judge whether to aggregate
            if (receivedModel.size() == numOfChildNode) {
                // add worker model where from current layer and current node
                Model workerModel = layersWorkerModel[layer];
                receivedModel.add(workerModel);

                // aggregate receive model
                workerModel = ((MergeableLogisticRegression)workerModel).aggregateDefault(receivedModel);
                // broadcast to its child node
                bfs((ETreeNode) currentNode, layer, workerModel);

                // after aggregate, we should update some information
                layersWorkerModel[layer] = workerModel;
                aggregateCount[layer]++;
                receivedModel.clear();
                layersReceivedModels[layer] = receivedModel;

                // whether to send nodes to the next layer
                if (aggregateCount[layer] % aggregateRatio[layer] == 0) {
                    if (layer != layers-1) {
                        // send to next layer
                        ModelHolder latestModelHolder = new BoundedModelHolder(1);
                        latestModelHolder.add(workerModel);
                        sendTo(new MessageUp(currentNode, layer, latestModelHolder),
                                ((ETreeNode) currentNode).getParentNode(layer));
                    } else {
//                        System.out.println("Time: " + CommonState.getTime() + ", root aggregate");
                        computeLoss(workerModel);
                    }
                }
            }
        }
    }

    private MergeableLogisticRegression workerUpdate(Model model) {
        // SGD
        for (int sampleID = 0; instances != null && sampleID < instances.size(); sampleID++) {
            // we use each samples for updating the currently processed model
            SparseVector x = instances.getInstance(sampleID);
            double y = instances.getLabel(sampleID);
            model.update(x, y);
        }
        return (MergeableLogisticRegression) model;
    }

    private void startNextIteration() {
        for (int i = 0; i < Network.size(); i++) {
            Node node = Network.get(i);
            // schedule starter alarm
            EDSimulator.add(0, ActiveThreadMessage.getInstance(), node, currentProtocolID);
        }
    }

    /**
     * Update the model of all children of root node
     * @param root
     * @param layer
     * @param model
     */
    private void bfs(ETreeNode root, int layer, Model model) {
        Queue<ETreeNode> q = new LinkedList<>();
        q.offer(root);

        while (q.isEmpty()) {
            ETreeNode top = q.poll();
            int cnt = top.getChildNodeList(layer).size();

            for (int i = 0; i < cnt; i++) {
                for (Integer id : top.getChildNodeList(layer)) {
                    ETreeNode temp = (ETreeNode) Network.get(id);
                    // update node's model
                    ETreeLearningProtocol temp_node_pro = (ETreeLearningProtocol) temp.getProtocol(currentProtocolID);
                    temp_node_pro.setLayersWorkerModel(layer, model);
                    q.offer(temp);
                }
            }
            layer--;
        }
    }

    @Override
    public void computeLoss() {
    }

    private void computeLoss(Model workerModel) {
        double errs = 0.0;
        for (int testIdx = 0; eval != null && testIdx < eval.size(); testIdx++) {
            SparseVector testInstance = eval.getInstance(testIdx);
            double y = eval.getLabel(testIdx);
            double pred = workerModel.predict(testInstance);
            errs += (y == pred) ? 0.0 : 1.0;
        }
        errs = errs / eval.size();
        cycle++;
        Main.addLoss(cycle, errs);
        System.err.println("Cycle: "+ cycle + " ETree 0-1 error: " + errs);
    }

    @Override
    public Object clone() {
        return new ETreeLearningProtocol(prefix, modelHolderName, modelName, layers);
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

    public static void setLayersNodeID(ArrayList<ArrayList<Integer>> layersID) {
        layersNodeID = layersID;
//        for (ArrayList<Integer> node : layersNodeID) {
//            System.out.print(node.size() + ": ");
//            for (Integer id : node) {
//                System.out.print(id + " ");
//            }
//            System.out.println();
//        }
    }


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

    public Model getLayersWorkerModel(int layer) {
        return layersWorkerModel[layer];
    }

    public void setLayersWorkerModel(int layer, Model model) {
        layersWorkerModel[layer] = model;
    }
}
