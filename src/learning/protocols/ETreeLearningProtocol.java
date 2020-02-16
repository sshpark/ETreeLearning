package learning.protocols;

import learning.interfaces.AbstractProtocol;
import learning.interfaces.Model;
import learning.interfaces.ModelHolder;
import learning.messages.ModelMessage;
import learning.modelHolders.BoundedModelHolder;
import learning.models.MergeableLogisticRegression;
import learning.node.ETreeNode;
import learning.utils.SparseVector;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;

import java.util.ArrayList;

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

    private Model workerModel;
    private ModelHolder[] layersReceivedModels;
    private ArrayList<ArrayList<Integer>> layersNodeID;
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
            layersReceivedModels = new ModelHolder[layers];
            for (int i = 0; i < layers; i++) {
                layersReceivedModels[i] = (ModelHolder) Class.forName(modelHolderName).
                        getConstructor().newInstance();
                layersReceivedModels[i].init(prefix);
            }

            workerModel = (Model)Class.forName(modelName).getConstructor().newInstance();
            workerModel.init(prefix);

            String[] agg_ratios = Configuration.getNames(prefix + "." + PAR_RATIOS);
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
        iter ++;

        int temp = 1;
        for (int layer = 0; layer < layers-1; layer++) {
            temp *= aggregateRatio[layer];
            if (iter % temp == 0) {
                // nodes in current layer
                for (Integer node_id : layersNodeID.get(layer)) {
                    ETreeNode node = (ETreeNode) Network.get(node_id);
                    ETreeLearningProtocol pro = (ETreeLearningProtocol) node.getProtocol(currentProtocolID);
                    // update
                    workerUpdate(pro.getWorkerModel());

                    // send to next layer
                    ModelHolder latestModelHolder = new BoundedModelHolder(1);
                    latestModelHolder.add(pro.getWorkerModel());
                    ETreeNode dest = (ETreeNode) Network.get(node.getParentNode(layer));
                    sendTo(new ModelMessage(node, latestModelHolder), dest);
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

    @Override
    public void computeLoss() {

    }

    @Override
    public Object clone() {
        return new ETreeLearningProtocol(prefix, modelHolderName, modelName, layers);
    }

    /**
     *
     * @param message
     */
    private void sendTo(ModelMessage message, Node dst) {
        message.setSource(currentNode);
        getTransport().send(currentNode, dst, message, currentProtocolID);
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
}
