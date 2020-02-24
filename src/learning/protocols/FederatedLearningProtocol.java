package learning.protocols;

import learning.interfaces.AbstractProtocol;
import learning.interfaces.Model;
import learning.interfaces.ModelHolder;
import learning.main.Main;
import learning.messages.ActiveThreadMessage;
import learning.messages.ModelMessage;
import learning.modelHolders.BoundedModelHolder;
import learning.models.LogisticRegression;
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

    /** @hidden */
    private final String modelHolderName;
    private final String modelName;
    private final int compress;

    private MergeableLogisticRegression workerModel;
    private ModelHolder receivedModels;
    private static int masterID;

    public FederatedLearningProtocol(String prefix) {
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
    private FederatedLearningProtocol(String prefix, String modelHolderName, String modelName, int compress) {
        this.modelHolderName = modelHolderName;
        this.modelName = modelName;
        this.compress = compress;
        init(prefix);
    }

    protected void init(String prefix) {
        try {
            super.init(prefix);
            receivedModels = (ModelHolder)Class.forName(modelHolderName).getConstructor().newInstance();
            receivedModels.init(prefix);

            workerModel = (MergeableLogisticRegression) Class.forName(modelName).getConstructor().newInstance();
            workerModel.init(prefix);

        } catch (Exception e) {
            throw new RuntimeException("Exception occured in initialization of " + getClass().getCanonicalName() + ": " + e);
        }
    }

    @Override
    public Object clone() {
        return new FederatedLearningProtocol(prefix, modelHolderName, modelName, compress);
    }


    @Override
    public void processEvent(Node currentNode, int currentProtocolID, Object messageObj) {
        this.currentNode = currentNode;
        this.currentProtocolID = currentProtocolID;

        if ( messageObj instanceof ActiveThreadMessage) {
            if (currentNode.getID() != masterID) {
                workerUpdate();
            }
        } else if (messageObj instanceof ModelMessage) {
            masterAggregate((ModelMessage) messageObj);
        }
    }

    private void workerUpdate() {
        update(workerModel);

        // compress weight
        workerModel = workerModel.compressSubsampling(compress);

        // send to master node
        ModelHolder latestModelHolder = new BoundedModelHolder(1);
        latestModelHolder.add(workerModel.clone());
        sendTo(new ModelMessage(currentNode, latestModelHolder), Network.get(masterID));
    }

    private void masterAggregate(ModelMessage message) {
        Model model = (MergeableLogisticRegression) message.getModel(0).clone();
        receivedModels.add(model);

        int workerNum = Network.size()-1;
        if (receivedModels.size() == workerNum) {
            receivedModels.add(workerModel.clone());
            // master node aggregate
            workerModel = workerModel.aggregateDefault(receivedModels);
            // print 0-1 error
            computeLoss();
            // clear receivedModels
            receivedModels.clear();
            // send to child node
            for (int id = 0; id < Network.size(); id++) {
                if (id != masterID) {
                    Node node = Network.get(id);
                    FederatedLearningProtocol node_pro = (FederatedLearningProtocol) node.getProtocol(currentProtocolID);
                    // update worker model
                    node_pro.setWorkerModel(workerModel.clone());

                    EDSimulator.add(minDelayMatrix[masterID][id], ActiveThreadMessage.getInstance(),
                            node, currentProtocolID);
                }
            }
        }
    }


    @Override
    public void activeThread() {
    }

    @Override
    public void passiveThread(ModelMessage message) {
    }

    /**
     * update model
     * @param model
     * @return
     */
    private Model update(Model model) {
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

    private void computeLoss() {
        double errs = 0.0;

        for (int testIdx = 0; eval != null && testIdx < eval.size(); testIdx++) {
            SparseVector testInstance = eval.getInstance(testIdx);
            double y = eval.getLabel(testIdx);
            double[] pred = workerModel.distributionForInstance(testInstance);
//            errs += (y == pred) ? 0.0 : 1.0;
            errs += crossEntropyLoss(y, pred);
        }
        errs = errs / eval.size();
        cycle++;
        Main.addLoss(CommonState.getTime(), errs);
        System.err.println("Time: "+ CommonState.getTime() + " Fed loss: " + errs);
    }

    /**
     * Set master
     * @param id
     */
    public static void setMasterID(int id) {
        masterID = id;
    }

    /**
     *
     * @return masterId
     */
    public static int getMasterID() {
        return masterID;
    }

    public void setWorkerModel(MergeableLogisticRegression workerModel) {
        this.workerModel = workerModel;
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
