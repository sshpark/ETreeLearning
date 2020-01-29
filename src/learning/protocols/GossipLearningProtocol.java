package learning.protocols;

import learning.interfaces.AbstractProtocol;
import learning.interfaces.Model;
import learning.interfaces.ModelHolder;
import learning.messages.ModelMessage;
import learning.modelHolders.BoundedModelHolder;
import learning.models.LogisticRegression;
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

        // merge
        for (int incommingModelID = 0; receivedModels != null && incommingModelID < receivedModels.size(); incommingModelID++) {
            LogisticRegression model = (LogisticRegression) receivedModels.getModel(incommingModelID);
            ((LogisticRegression) workerModel).merge(model);
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
}
