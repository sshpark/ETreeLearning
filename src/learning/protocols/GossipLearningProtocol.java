package learning.protocols;

import learning.interfaces.AbstractProtocol;
import learning.interfaces.LearningProtocol;
import learning.interfaces.Model;
import learning.interfaces.ModelHolder;
import learning.main.Main;
import learning.messages.ModelMessage;
import learning.modelHolders.BoundedModelHolder;
import learning.models.LogisticRegression;
import learning.models.MergeableLogisticRegression;
import learning.utils.SparseVector;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.core.Protocol;

/**
 * @author sshpark
 * @date 27/1/2020
 */
public class GossipLearningProtocol extends AbstractProtocol {
    private final static String PAR_MODELHOLDERNAME = "modelHolderName";
    private final static String PAR_MODELNAME = "modelName";
    private final static String PAR_COMPRESS = "compress";
    private final static String PAR_DELTAG = "deltaG";

    /** @hidden */
    private final String modelHolderName;
    private final String modelName;
    private final int compress;
    private final long deltaG;

    private Model workerModel;
    private ModelHolder receivedModels;

    public GossipLearningProtocol(String prefix) {
        modelHolderName = Configuration.getString(prefix + "." + PAR_MODELHOLDERNAME);
        modelName = Configuration.getString(prefix + "." + PAR_MODELNAME);
        compress = Configuration.getInt(prefix + "." + PAR_COMPRESS);
        deltaG = Configuration.getLong(prefix + "." + PAR_DELTAG);
        init(prefix);
    }

    /**
     * Copy constructor
     * @param prefix
     * @param modelHolderName
     * @param modelName
     * @param deltaG
     */
    private GossipLearningProtocol(String prefix, String modelHolderName, String modelName, int compress, long deltaG) {
        this.modelHolderName = modelHolderName;
        this.modelName = modelName;
        this.compress = compress;
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
        } catch (Exception e) {
            throw new RuntimeException("Exception occured in initialization of " + getClass().getCanonicalName() + ": " + e);
        }
    }


    @Override
    public Object clone() {
        return new GossipLearningProtocol(prefix, modelHolderName, modelName, compress, deltaG);
    }

    @Override
    public void activeThread() {
        ModelHolder latestModelHolder = new BoundedModelHolder(1);
        MergeableLogisticRegression model = (MergeableLogisticRegression) workerModel.clone();
        latestModelHolder.add(model.compressSubsampling(compress));
        sendToRandomNeighbor(new ModelMessage(currentNode, latestModelHolder));

    }

    @Override
    public void passiveThread(ModelMessage message) {
        MergeableLogisticRegression model = (MergeableLogisticRegression) message.getModel(0);

        // merge
        workerModel = ((MergeableLogisticRegression) workerModel).aggregateDefault(message);

        // update
        for (int sampleID = 0; instances != null && sampleID < instances.size(); sampleID++) {
            // we use each samples for updating the currently processed model
            SparseVector x = instances.getInstance(sampleID);
            double y = instances.getLabel(sampleID);
            workerModel.update(x, y);
        }
    }

    @Override
    public void computeLoss() {
        double errs = 0.0;
        for (int i = 0; i < Network.size(); i++) {
            Protocol p = ((Node) Network.get(i)).getProtocol(currentProtocolID);
            if (p instanceof LearningProtocol) {
                LogisticRegression model = (LogisticRegression) ((LearningProtocol) p).getWorkerModel();
                for (int testIdx = 0; eval != null && testIdx < eval.size(); testIdx++) {
                    SparseVector testInstance = eval.getInstance(testIdx);
                    double y = eval.getLabel(testIdx);
                    double pred = model.predict(testInstance);
                    errs += (y == pred) ? 0.0 : 1.0;
                }
            }
        }
        errs = errs / eval.size();
        cycle++;
        Main.addLoss(cycle, errs);
        System.err.println("Cycle: "+ cycle + " Fed 0-1 error: " + errs);
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
