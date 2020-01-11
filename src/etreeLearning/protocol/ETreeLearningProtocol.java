package etreeLearning.protocol;

import etreeLearning.messages.EModelMessage;
import gossipLearning.interfaces.AbstractProtocol;
import gossipLearning.interfaces.ModelHolder;
import gossipLearning.messages.ModelMessage;
import peersim.core.CommonState;
import peersim.core.Node;


public class ETreeLearningProtocol extends AbstractProtocol {
    public ETreeLearningProtocol(String prefix) {
    }

    @Override
    public Object clone() {
        return new ETreeLearningProtocol(prefix);
    }

    @Override
    public void activeThread() {
        System.out.println(CommonState.getNode().getID() + " sayed: I am actived at time of " + CommonState.getTime());

        sendTo(new EModelMessage(currentNode, CommonState.r.nextGaussian()),
                getOverlay().getNeighbor(0));
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


    private void sendTo(EModelMessage message, Node dst) {
        getTransport().send(currentNode, dst, message, currentProtocolID);
    }
}
