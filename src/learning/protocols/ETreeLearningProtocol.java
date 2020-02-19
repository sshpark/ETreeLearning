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
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

import java.util.ArrayList;
import java.util.HashMap;

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
    private int iter;


    public ETreeLearningProtocol(String prefix) {
        modelHolderName = Configuration.getString(prefix + "." + PAR_MODELHOLDERNAME);
        modelName = Configuration.getString(prefix + "." + PAR_MODELNAME);
        layers = Configuration.getInt(prefix + "." + PAR_LAYERS);
        init(prefix);
    }

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

            // init worker model
            layersWorkerModel = new Model[layers];

            // init ratios
            String[] agg_ratios = Configuration.getString(prefix + "." + PAR_RATIOS).split(",");
            if (agg_ratios.length != layers-1)
                throw new RuntimeException("The size of ratios must be equal to (layers-1)");
            aggregateRatio = new int[layers-1];
            for (int i = 0; i < layers-1; i++)
                aggregateRatio[i] = Integer.parseInt(agg_ratios[i]);

            iter = 0;
        } catch (Exception e) {
            throw new RuntimeException("Exception occured in initialization of " + getClass().getCanonicalName() + ": " + e);
        }
    }

    @Override
    public void processEvent(Node currentNode, int currentProtocolID, Object messageObj) {
        this.currentNode = currentNode;
        this.currentProtocolID = currentProtocolID;

        if (messageObj instanceof ActiveThreadMessage) {
            iter++;
            ETreeNode node = (ETreeNode) currentNode;
            int temp = 1;

            for (int layer = 0; layer < layers-1; layer++) {
                temp *= aggregateRatio[layer];
                if (iter % temp == 0) {

                    Model wkmodel = layersWorkerModel[layer].get(node.getID());
                    if (wkmodel == null) {
                        wkmodel = new MergeableLogisticRegression();
                        wkmodel.init(prefix);
                    }
                    // update
                    workerUpdate(wkmodel);
                    // send to next layer
                    ModelHolder latestModelHolder = new BoundedModelHolder(1);
                    latestModelHolder.add(wkmodel);
                    sendTo(new MessageUp(node, layer, latestModelHolder), node.getParentNode(layer));
                } else {
                    // next time to update
                    EDSimulator.add(1, ActiveThreadMessage.getInstance(),
                            currentNode, currentProtocolID);
                }
            }
        } else if (messageObj instanceof MessageUp) { // receive message from child node
            int layer = ((MessageUp) messageObj).getLayer()+1; // current layer = source layer+1
            int nodeid = (int)currentNode.getID();          // current node id

//            System.out.println("current node: " + nodeid + ", src: " + ((MessageUp) messageObj).getSource().getID()
//            +", current layer: " + layer);

            // child node next time to update
            EDSimulator.add(1, ActiveThreadMessage.getInstance(),
                    ((MessageUp) messageObj).getSource(), currentProtocolID);

            // get received model where from current layer, current node
            ModelHolder receivedModel = layersReceivedModels[layer];

            // if key is null, init it.
            if (receivedModel == null) {
                receivedModel = new BoundedModelHolder( Network.size() );
            }
            // add model to specified layer and specified node
            receivedModel.add(((MessageUp) messageObj).getModel(0));
            // current node's child node size
            int numOfChildNode = ((ETreeNode)currentNode)
                                .getChildNodeList(layer)
                                .size();
            // it must update layersReceivedModels
            layersReceivedModels[layer].put(nodeid, receivedModel);

            // judge whether to aggregate
            if (receivedModel.size() == numOfChildNode) {

                // add worker model where from current layer and current node
                Model workerModel = layersWorkerModel[layer].get(nodeid);
                if (workerModel == null) {
                    workerModel = new MergeableLogisticRegression();
                    workerModel.init(prefix);
                }
                receivedModel.add(workerModel);

                // aggregate receive model
                workerModel = ((MergeableLogisticRegression)workerModel).aggregateDefault(receivedModel);
                receivedModel.clear();
                // send to its child node,
                // note that if layer == layers-1(root), it should send to all node
                // else send to its child node's layer
                if (layer == layers-1) {
                    // update all node in all layer
                    for (int ly = 0; ly < layers; ly++) {
                        for (Integer id : layersNodeID.get(ly)) {
                            ETreeLearningProtocol temp_node_pro = (ETreeLearningProtocol) Network
                                    .get(id)
                                    .getProtocol(currentProtocolID);
                            temp_node_pro.setLayersWorkerModel(ly, id, workerModel);
                        }
                    }
                    computeLoss(workerModel);
                } else {
//                    System.out.println("ArrayList<Integer> childNodes");
                    ArrayList<Integer> childNodes = ((ETreeNode)currentNode).getChildNodeList(layer);
                    for (Integer id : childNodes) {
                        ETreeLearningProtocol temp_node_pro = (ETreeLearningProtocol) Network
                                .get(id)
                                .getProtocol(currentProtocolID);
                        temp_node_pro.setLayersWorkerModel(layer-1, id, workerModel);
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

    /**
     * Judge node whether update
     * @param nodeid
     * @param layer
     * @return
     */
    private boolean inCurrentLayer(int nodeid, int layer) {
        for (Integer id : layersNodeID.get(layer)) {
            if (id == nodeid) {
                return true;
            }
        }
        return false;
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

    /**
     * send to specified node list
     * @param message
     */
    private void sendTo(ModelMessage message, ArrayList<Integer> dst) {
        message.setSource(currentNode);
        for (int i = 0; i < dst.size(); i++) {
            Node node = Network.get(dst.get(i));
            getTransport().send(currentNode, node, message, currentProtocolID);
        }
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

    public Model getLayersWorkerModel(int layer, int nodeid) {
        return layersWorkerModel[layer].get(nodeid);
    }

    public void setLayersWorkerModel(int layer, int nodeid, Model model) {
        layersWorkerModel[layer].put(nodeid, model);
    }
}
