package learning.protocols;

import learning.interfaces.AbstractProtocol;
import learning.interfaces.Model;
import learning.interfaces.ModelHolder;
import learning.messages.ActiveThreadMessage;
import learning.messages.ModelMessage;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

/**
 * @author sshpark
 * @date 17/2/2020
 */
public class TestProtocol extends AbstractProtocol {
    private final static String PAR_MODELHOLDERNAME = "modelHolderName";
    private final static String PAR_MODELNAME = "modelName";

    /** @hidden */
    private final String modelHolderName;
    private final String modelName;

    private int iter = 0;


    public TestProtocol(String prefix) {
        modelHolderName = Configuration.getString(prefix + "." + PAR_MODELHOLDERNAME);
        modelName = Configuration.getString(prefix + "." + PAR_MODELNAME);
        init(prefix);
    }

    /**
     * Copy constructor
     * @param prefix
     * @param modelHolderName
     * @param modelName
     */
    private TestProtocol(String prefix, String modelHolderName, String modelName) {
        this.modelHolderName = modelHolderName;
        this.modelName = modelName;
        init(prefix);
    }

    protected void init(String prefix) {
        try {
            super.init(prefix);
        } catch (Exception e) {
            throw new RuntimeException("Exception occured in initialization of " + getClass().getCanonicalName() + ": " + e);
        }
    }

    @Override
    public void processEvent(Node currentNode, int currentProtocolID, Object messageObj) {
        iter++;
        System.out.println(CommonState.getTime() + ": " + iter);
        EDSimulator.add(1, ActiveThreadMessage.getInstance(), currentNode, currentProtocolID);
    }

    @Override
    public Object clone() {
        return new TestProtocol(prefix, modelHolderName, modelName);
    }

    @Override
    public void computeLoss() {

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
