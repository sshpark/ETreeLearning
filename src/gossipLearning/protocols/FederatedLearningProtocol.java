package gossipLearning.protocols;

import gossipLearning.InstanceHolder;
import gossipLearning.interfaces.AbstractProtocol;
import gossipLearning.interfaces.LearningProtocol;
import gossipLearning.interfaces.Model;
import gossipLearning.interfaces.ModelHolder;
import gossipLearning.messages.ActiveThreadMessage;
import gossipLearning.messages.ModelMessage;
import peersim.config.Configuration;
import peersim.core.Node;
import peersim.edsim.EDProtocol;

public class FederatedLearningProtocol implements EDProtocol, LearningProtocol {

    private static final String PAR_MODELHOLDERNAME = "modelHolderName";
    private static final String PAR_MODELNAME = "modelName";
    private static final String PAR_DELTAF = "deltaF";

    /** @hidden */
    private final String modelHolderName;
    /** @hidden */
    private final String modelName;
    /** @hidden */
    private final int deltaF;

    /** @hidden */
    private ModelHolder serverModels;
    /** @hidden */
    private Model workerModel;


    // instance variable
    /** @hidden */
    protected InstanceHolder instances;

    /**
     * Constructor which parses the contents of a standard Peersim configuration file.
     *
     * @param prefix
     */
    public FederatedLearningProtocol(String prefix) {
        modelHolderName = Configuration.getString(prefix + "." + PAR_MODELHOLDERNAME);
        modelName = Configuration.getString(prefix + "." + PAR_MODELNAME);
        deltaF = Configuration.getInt(prefix + "." + PAR_DELTAF);
        init(prefix);
    }

    public FederatedLearningProtocol(String modelHolderName, String modelName, int deltaF) {
        this.modelHolderName = modelHolderName;
        this.modelName = modelName;
        this.deltaF = deltaF;
    }

    /**
     * Initializes the starting modelHolder and model structure.
     *
     * @param prefix
     */
    protected void init(String prefix) {
        try {
            Model model = (Model)Class.forName(modelName).newInstance();
            model.init(prefix);

            serverModels = (ModelHolder)Class.forName(modelHolderName).newInstance();
            serverModels.init(prefix);
            workerModel = model;
            serverModels.add(model);
        } catch (Exception e) {
            throw new RuntimeException("Exception occured in initialization of " + getClass().getCanonicalName() + ": " + e);
        }
    }

    @Override
    public void processEvent(Node node, int pid, Object event) {
        if (event instanceof ActiveThreadMessage) {

        }
    }

    private void onReceiveModel(Model model) {
        int miniBatch = 10;

    }

    @Override
    public InstanceHolder getInstanceHolder() {
        return instances;
    }

    @Override
    public void setInstenceHolder(InstanceHolder instances) {
        this.instances = instances;
    }


    @Override
    public Object clone() {
        return new FederatedLearningProtocol(modelHolderName, modelName, deltaF);
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
}
