package learning.protocols;

import learning.interfaces.AbstractProtocol;
import learning.interfaces.Mergeable;
import learning.interfaces.Model;
import learning.interfaces.ModelHolder;
import learning.messages.ActiveThreadMessage;
import learning.messages.ModelMessage;
import learning.modelHolders.BoundedModelHolder;
import learning.utils.SparseVector;
import peersim.config.Configuration;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

/**
 * @author sshpark
 * @date 27/1/2020
 */
public class GossipLearningProtocol extends AbstractProtocol {
    private final static String PAR_MODELHOLDERNAME = "modelHolderName";
    private final static String PAR_MODELNAME = "modelName";
    private final static String PAR_COMPRESS = "compress";

    /** @hidden */
    private final String modelHolderName;
    private final String modelName;
    private final int compress;

    private Model workerModel;
    private ModelHolder receivedModels;

    public GossipLearningProtocol(String prefix) {
        modelHolderName = Configuration.getString(prefix + "." + PAR_MODELHOLDERNAME);
        modelName = Configuration.getString(prefix + "." + PAR_MODELNAME);
        compress = Configuration.getInt(prefix + "." + PAR_COMPRESS);
        init(prefix);
    }

    /**
     * Copy constructor
     * @param prefix
     * @param modelHolderName
     * @param modelName
     */
    private GossipLearningProtocol(String prefix, String modelHolderName, String modelName, int compress) {
        this.modelHolderName = modelHolderName;
        this.modelName = modelName;
        this.compress = compress;
        init(prefix);
    }

    /**
     * Initializes the starting modelHolder and model structure.
     *
     * @param prefix
     */
    protected void init(String prefix) {
        try {
            super.init(prefix);
            receivedModels = (ModelHolder)Class.forName(modelHolderName).getConstructor().newInstance();
            receivedModels.init(prefix);

            workerModel = (Model)Class.forName(modelName).getConstructor().newInstance();
            workerModel.init(prefix);
        } catch (Exception e) {
            throw new RuntimeException("Exception occured in initialization of " + getClass().getCanonicalName() + ": " + e);
        }
    }

    @Override
    public void processEvent(Node currentNode, int currentProtocolID, Object messageObj) {
        this.currentNode = currentNode;
        this.currentProtocolID = currentProtocolID;

        if ( messageObj instanceof ActiveThreadMessage) {
            activeThread();
        } else if (messageObj instanceof ModelMessage) {
            passiveThread((ModelMessage) messageObj);
        }
    }

    @Override
    public Object clone() {
        return new GossipLearningProtocol(prefix, modelHolderName, modelName, compress);
    }

    @Override
    public void activeThread() {
        ModelHolder latestModelHolder = new BoundedModelHolder(1);
        Model model = (Model) workerModel.clone();
        latestModelHolder.add(model);
        sendToRandomNeighbor(new ModelMessage(currentNode, latestModelHolder, 0));
    }

    @Override
    public void passiveThread(ModelMessage message) {
        // merge
        workerModel = ((Mergeable) workerModel).aggregateDefault(message);

        // update, SGD
        for (int sampleID = 0; instances != null && sampleID < instances.size(); sampleID++) {
            // we use each samples for updating the currently processed model
            SparseVector x = instances.getInstance(sampleID);
            double y = instances.getLabel(sampleID);
            workerModel.update(x, y);
        }

        // source node next update moment
        int src = currentNode.getIndex();
        int dest = message.getSource().getIndex();
        EDSimulator.add(minDelayMatrix[src][dest], ActiveThreadMessage.getInstance(),
                message.getSource(), currentProtocolID);
    }

    private void computeLoss(Model model) {

    }

    /**
     * The size is always 0 or 1 meaning that we have only zero or one ModelHolder instance.
     *
     * @return The protocol handles only zero or one ModelHolder instance.
     */
    @Override
    public int size() {
        return (receivedModels == null) ? 0 : 1;
    }

    /**
     * It returns the only one stored ModelHolder instance if the index is 0,
     * otherwise throws an exception.
     *
     * @param index Index which always has to be 0.
     * @return The stored ModelHolder instance.
     */
    @Override
    public ModelHolder getModelHolder(int index) {
        if (index != 0) {
            throw new RuntimeException(getClass().getCanonicalName() + " can handle only one modelHolder with index 0.");
        }
        return receivedModels;
    }

    /**
     * It simply replaces the stored ModelHolder instance.
     *
     * @param index Index which has to be 0.
     * @param modelHolder The new model holder.
     */
    @Override
    public void setModelHolder(int index, ModelHolder modelHolder) {
        if (index != 0) {
            throw new RuntimeException(getClass().getCanonicalName() + " can handle only one modelHolder with index 0.");
        }
        this.receivedModels = modelHolder;
    }

    /**
     * It overwrites the stored ModelHolder with the received one.
     *
     *  @param modelHolder ModelHolder instance
     *  @return true The process is always considered successful.
     */
    @Override
    public boolean add(ModelHolder modelHolder) {
        setModelHolder(0, modelHolder);
        return true;
    }

    /**
     * It returns the stored ModelHolder and sets the current one to <i>null</i>.
     *
     * @param index has to be 0.
     * @return ModelHolder instance which was stored by the node.
     */
    @Override
    public ModelHolder remove(int index) {
        if (index != 0) {
            throw new RuntimeException(getClass().getCanonicalName() + " can handle only one modelHolder with index 0.");
        }
        ModelHolder ret = receivedModels;
        receivedModels = null;
        return ret;
    }

    @Override
    public Model getWorkerModel() {
        return workerModel;
    }
}
