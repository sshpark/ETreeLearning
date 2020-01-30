package learning.protocols;

import learning.interfaces.AbstractProtocol;
import learning.interfaces.Model;
import learning.interfaces.ModelHolder;
import learning.messages.ModelMessage;
import learning.modelHolders.BoundedModelHolder;
import learning.models.LogisticRegression;
import learning.models.MergeableLogisticRegression;
import learning.utils.SparseVector;
import peersim.config.Configuration;
import peersim.core.CommonState;

/**
 * @author sshpark
 * @date 27/1/2020
 */
public class GossipLearningProtocol extends AbstractProtocol {
    private static final String PAR_MODELHOLDERNAME = "modelHolderName";
    private static final String PAR_MODELNAME = "modelName";
    private final String PAR_DELTA = "deltaG";

    /** @hidden */
    private final String modelHolderName;
    /** @hidden */
    private final String modelName;
    /** @hidden */
    private final long deltaG;

    private Model workerModel;
    private ModelHolder receivedModels;
    private long lastAggregatedTime;

    public GossipLearningProtocol(String prefix) {
        modelHolderName = Configuration.getString(prefix + "." + PAR_MODELHOLDERNAME);
        modelName = Configuration.getString(prefix + "." + PAR_MODELNAME);
        deltaG = Configuration.getLong(prefix + "." + PAR_DELTA);
        init(prefix);
    }

    /**
     * Copy constructor
     * @param prefix
     * @param modelHolderName
     * @param modelName
     * @param deltaG
     */
    private GossipLearningProtocol(String prefix, String modelHolderName, String modelName, long deltaG) {
        this.modelHolderName = modelHolderName;
        this.modelName = modelName;
        this.deltaG = deltaG;
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

            lastAggregatedTime = 0;
        } catch (Exception e) {
            throw new RuntimeException("Exception occured in initialization of " + getClass().getCanonicalName() + ": " + e);
        }
    }


    @Override
    public Object clone() {
        return new GossipLearningProtocol(prefix, modelHolderName, modelName, deltaG);
    }

    @Override
    public void activeThread() {
        if (workerModel != null) {
            ModelHolder latestModelHolder = new BoundedModelHolder(1);
            latestModelHolder.add(workerModel);
            sendToRandomNeighbor(new ModelMessage(currentNode, latestModelHolder));
        }
    }

    @Override
    public void passiveThread(ModelMessage message) {
        long time = CommonState.getTime();
        receivedModels.add(message.getModel(0));

        if (time - lastAggregatedTime < deltaG) return;
        lastAggregatedTime = time;

        // merge
        for (int incommingModelID = 0; receivedModels != null && incommingModelID < receivedModels.size(); incommingModelID++) {
            MergeableLogisticRegression model = (MergeableLogisticRegression) receivedModels.getModel(incommingModelID);
            workerModel = ((MergeableLogisticRegression) workerModel).merge(model);
        }
        receivedModels.clear();

        // update
        for (int sampleID = 0; instances != null && sampleID < instances.size(); sampleID++) {
            // we use each samples for updating the currently processed model
            SparseVector x = instances.getInstance(sampleID);
            double y = instances.getLabel(sampleID);
            workerModel.update(x, y);
        }
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
