package learning.protocols;

import learning.interfaces.*;
import learning.messages.ActiveThreadMessage;
import learning.messages.ModelMessage;
import learning.modelHolders.BoundedModelHolder;
import learning.utils.SparseVector;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.core.Protocol;
import peersim.edsim.EDSimulator;

/**
 * @author sshpark
 * @date 9/3/2020
 */
public class GroupLearningProtocol extends AbstractProtocol {
    private final static String PAR_MODELHOLDERNAME = "modelHolderName";
    private final static String PAR_MODELNAME = "modelName";

    /**
     * @hidden
     */
    private final String modelHolderName;
    private final String modelName;

    private Model workerModel;
    private ModelHolder recvs;
    private int iter;

    public GroupLearningProtocol(String prefix) {
        modelHolderName = Configuration.getString(prefix + "." + PAR_MODELHOLDERNAME);
        modelName = Configuration.getString(prefix + "." + PAR_MODELNAME);
        init(prefix);
    }

    /**
     * Copy constructor
     *
     * @param prefix
     * @param modelHolderName
     * @param modelName
     */
    private GroupLearningProtocol(String prefix, String modelHolderName, String modelName) {
        this.modelHolderName = modelHolderName;
        this.modelName = modelName;
        init(prefix);
    }

    protected void init(String prefix) {
        try {
            super.init(prefix);
            workerModel = (Model) Class.forName(modelName).getConstructor().newInstance();
            workerModel.init(prefix);

            recvs = (ModelHolder) Class.forName(modelHolderName).getConstructor().newInstance();
            recvs.init(prefix);

            iter = 0;
        } catch (Exception e) {
            throw new RuntimeException("Exception occured in initialization of " + getClass().getCanonicalName() + ": " + e);
        }
    }

    @Override
    public Object clone() {
        return new GroupLearningProtocol(prefix, modelHolderName, modelName);
    }

    @Override
    public void processEvent(Node currentNode, int currentProtocolID, Object messageObj) {
        this.currentNode = currentNode;
        this.currentProtocolID = currentProtocolID;
        // first layer
        if (messageObj instanceof ActiveThreadMessage) {
            // update
            for (int sampleID = 0; instances != null && sampleID < instances.size(); sampleID++) {
                // we use each samples for updating the currently processed model
                SparseVector x = instances.getInstance(sampleID);
                double y = instances.getLabel(sampleID);
                workerModel.update(x, y);
            }
            ModelHolder latestModelHolder = new BoundedModelHolder(1);
            latestModelHolder.add((Model) workerModel.clone());
            sendTo(new ModelMessage(currentNode, latestModelHolder), currentNode.getIndex()/10*10);
            iter++;
            // next update
            EDSimulator.add(10, ActiveThreadMessage.getInstance(), currentNode, currentProtocolID);
        } else if (messageObj instanceof ModelMessage) {
            Model model = ((ModelMessage) messageObj).getModel(0);
            recvs.add(model);

            if (recvs.size() == 10) {
                // aggregate receive model
                workerModel = ((Mergeable) workerModel).aggregateDefault(recvs);
                recvs.clear();
                int base = currentNode.getIndex()/10*10;
                // update children node
                for (int i = 0; i < 10; i++) {
                    Node node = Network.get(i+base);
                    GroupLearningProtocol node_pro = (GroupLearningProtocol) node.getProtocol(currentProtocolID);
                    node_pro.setWorkerModel((Model)workerModel.clone());
                }

                CommonState.setGlobalName("compute", CommonState.getGlobalValueBy("compute")+1);
                if (CommonState.getGlobalValueBy("compute")%10 == 0) {
                    computeLoss();
                }
            }
        }
    }

    /**
     * Output loss and accuracy
     *
     */
    public void computeLoss() {
        double errs = 0.0;
        double losses = 0.0;
        for (int i = 0; i < Network.size(); i += 10) {
            Protocol p = Network.get(i).getProtocol(currentProtocolID);
            double temp_errs = 0.0;
            double temp_losses = 0.0;
            if (p instanceof LearningProtocol) {
                Model model = ((LearningProtocol) p).getWorkerModel();
                for (int testIdx = 0; eval != null && testIdx < eval.size(); testIdx++) {
                    SparseVector testInstance = eval.getInstance(testIdx);
                    double y = eval.getLabel(testIdx);
                    double pred = model.predict(testInstance);
                    temp_errs += (y == pred) ? 0.0 : 1.0;

                    double[] y_pred = ((ProbabilityModel) model).distributionForInstance(testInstance);
                    temp_losses += crossEntropyLoss(y, y_pred);
                }
            }
            errs += temp_errs/eval.size();
            losses += temp_losses/eval.size();
        }
        errs /= 10;
        losses /= 10;
        cycle++;
        System.err.println("Time: "+ CommonState.getTime() + ", loss: " + losses + ", Accuracy: " + (1.0-errs));
    }

    private void sendTo(ModelMessage message, int dst) {
        Node node = Network.get(dst);
        getTransport().send(message.getSource(), node, message, currentProtocolID);
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
        return workerModel;
    }

    public void setWorkerModel(Model workerModel) {
        this.workerModel = workerModel;
    }
}
