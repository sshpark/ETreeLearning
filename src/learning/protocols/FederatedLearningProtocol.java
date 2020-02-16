package learning.protocols;

import learning.interfaces.AbstractProtocol;
import learning.interfaces.Model;
import learning.interfaces.ModelHolder;
import learning.main.Main;
import learning.messages.ActiveThreadMessage;
import learning.messages.ModelMessage;
import learning.messages.OnlineSessionFollowerActiveThreadMessage;
import learning.modelHolders.BoundedModelHolder;
import learning.models.MergeableLogisticRegression;
import learning.utils.SparseVector;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

/**
 * @author sshpark
 * @date 30/1/2020
 */
public class FederatedLearningProtocol extends AbstractProtocol {

    private final static String PAR_MODELHOLDERNAME = "modelHolderName";
    private final static String PAR_MODELNAME = "modelName";
    private final static String PAR_COMPRESS = "compress";
    private final static String PAR_DELTAF = "deltaF";

    /** @hidden */
    private final String modelHolderName;
    private final String modelName;
    private final int compress;
    private final long deltaF;

    private Model workerModel;
    private ModelHolder receivedModels;
    private int masterID;

    public FederatedLearningProtocol(String prefix) {
        modelHolderName = Configuration.getString(prefix + "." + PAR_MODELHOLDERNAME);
        modelName = Configuration.getString(prefix + "." + PAR_MODELNAME);
        compress = Configuration.getInt(prefix + "." + PAR_COMPRESS);
        deltaF = Configuration.getLong(prefix + "." + PAR_DELTAF);
        init(prefix);
    }

    /**
     * Copy constructor
     * @param prefix
     * @param modelHolderName
     * @param modelName
     * @param deltaF
     */
    private FederatedLearningProtocol(String prefix, String modelHolderName, String modelName,
                                      int compress, long deltaF) {
        this.modelHolderName = modelHolderName;
        this.modelName = modelName;
        this.compress = compress;
        this.deltaF = deltaF;
        init(prefix);
    }

    protected void init(String prefix) {
        try {
            super.init(prefix);
            receivedModels = (ModelHolder)Class.forName(modelHolderName).getConstructor().newInstance();
            receivedModels.init(prefix);

            workerModel = (Model)Class.forName(modelName).getConstructor().newInstance();
            workerModel.init(prefix);

            masterID = 0;
        } catch (Exception e) {
            throw new RuntimeException("Exception occured in initialization of " + getClass().getCanonicalName() + ": " + e);
        }
    }

    @Override
    public Object clone() {
        return new FederatedLearningProtocol(prefix, modelHolderName, modelName, compress, deltaF);
    }


    @Override
    public void processEvent(Node currentNode, int currentProtocolID, Object messageObj) {
        this.currentNode = currentNode;
        this.currentProtocolID = currentProtocolID;

        if ( messageObj instanceof ActiveThreadMessage ||
                (messageObj instanceof OnlineSessionFollowerActiveThreadMessage &&
                        ((OnlineSessionFollowerActiveThreadMessage)messageObj).sessionID == sessionID) ) {
            if (currentNode.getID() == masterID) {
                activeThread();
                // After the processing we set a new alarm with a delay
                if (!Double.isInfinite(delayMean)) {
                    int delay = (int)(delayMean + CommonState.r.nextGaussian()*delayVar);
                    delay = (delay > 0) ? delay : 1;
                    // Next time of the active thread
                    EDSimulator.add(delay, new OnlineSessionFollowerActiveThreadMessage(sessionID), currentNode, currentProtocolID);
                }
            }
        } else if (messageObj instanceof ModelMessage) {
            passiveThread((ModelMessage) messageObj);
        }
    }


    @Override
    public void activeThread() {
        MergeableLogisticRegression masterModel = new MergeableLogisticRegression();
        masterModel.init(prefix);
//        System.out.println("cur time: " + CommonState.getTime() + " rec size: " + receivedModels.size());

        // merge
        masterModel = masterModel.aggregateDefault(receivedModels);
        workerModel = masterModel;

        // print loss
        computeLoss();

        receivedModels.clear();

        // send to worker
        for (int i = 0; i < Network.size(); i++) {
            if (i != masterID) {
                ModelHolder latestModelHolder = new BoundedModelHolder(1);
                latestModelHolder.add(masterModel);
                sendTo(new ModelMessage(currentNode, latestModelHolder), Network.get(i));
            }
        }
    }
    /**
     * Worker
     * @param message The content of the incoming message.
     */
    @Override
    public void passiveThread(ModelMessage message) {
        MergeableLogisticRegression model = (MergeableLogisticRegression)message.getModel(0);

        if (currentNode.getID() != 0) {
            model = update(model);
            workerModel = model;

            // compress weight
            model = model.compressSubsampling(compress);

            // send to master node
            ModelHolder latestModelHolder = new BoundedModelHolder(1);
            latestModelHolder.add(model);
            sendTo(new ModelMessage(currentNode, latestModelHolder), Network.get(masterID));
        } else {
            receivedModels.add(model);
        }
    }


    private MergeableLogisticRegression update(MergeableLogisticRegression model) {
        // update
        for (int sampleID = 0; instances != null && sampleID < instances.size(); sampleID++) {
            // we use each samples for updating the currently processed model
            SparseVector x = instances.getInstance(sampleID);
            double y = instances.getLabel(sampleID);
            model.update(x, y);
        }
        return model;
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
    public void computeLoss() {
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
