package learning.protocols;

import learning.interfaces.AbstractProtocol;
import learning.interfaces.Model;
import learning.interfaces.ModelHolder;
import learning.messages.ActiveThreadMessage;
import learning.messages.ModelMessage;
import learning.utils.SparseVector;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

/**
 * @author sshpark
 * @date 17/2/2020
 */
public class SingleNodeLearningProtocol extends AbstractProtocol {
    private final static String PAR_MODELHOLDERNAME = "modelHolderName";
    private final static String PAR_MODELNAME = "modelName";

    /**
     * @hidden
     */
    private final String modelHolderName;
    private final String modelName;

    private Model workerModel;

    public SingleNodeLearningProtocol(String prefix) {
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
    private SingleNodeLearningProtocol(String prefix, String modelHolderName, String modelName) {
        this.modelHolderName = modelHolderName;
        this.modelName = modelName;
        init(prefix);
    }

    protected void init(String prefix) {
        try {
            super.init(prefix);
            workerModel = (Model) Class.forName(modelName).getConstructor().newInstance();
            workerModel.init(prefix);
        } catch (Exception e) {
            throw new RuntimeException("Exception occured in initialization of " + getClass().getCanonicalName() + ": " + e);
        }
    }

    @Override
    public Object clone() {
        return new SingleNodeLearningProtocol(prefix, modelHolderName, modelName);
    }

    private void computeLoss(Model model) {

    }

    @Override
    public void activeThread() {
        // update
        for (int sampleID = 0; instances != null && sampleID < instances.size(); sampleID++) {
            // we use each samples for updating the currently processed model
            SparseVector x = instances.getInstance(sampleID);
            double y = instances.getLabel(sampleID);
            workerModel.update(x, y);
        }
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
}
