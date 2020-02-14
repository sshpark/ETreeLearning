package learning.protocols;

import learning.InstanceHolder;
import learning.interfaces.AbstractProtocol;
import learning.interfaces.Model;
import learning.interfaces.ModelHolder;
import learning.messages.ModelMessage;
import peersim.config.Configuration;
import peersim.core.Node;

/**
 * @author sshpark
 * @date 11/2/2020
 */
public class ETreeLearningProtocol extends AbstractProtocol {
    private final static String PAR_MODELHOLDERNAME = "modelHolderName";
    private final static String PAR_MODELNAME = "modelName";
    private final static String PAR_LAYERS = "layers";

    /** @hidden */
    private final String modelHolderName;
    /** @hidden */
    private final String modelName;
    /** @hidden */
    private final int layers;

    private InstanceHolder eval;

    private Model workerModel;
    private ModelHolder receivedModels;

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

    }

    @Override
    public Object clone() {
        return new ETreeLearningProtocol(prefix, modelHolderName, modelName, layers);
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
