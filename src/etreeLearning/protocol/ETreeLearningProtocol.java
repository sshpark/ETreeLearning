package etreeLearning.protocol;

import etreeLearning.messages.EModelMessage;
import gossipLearning.interfaces.AbstractProtocol;
import gossipLearning.interfaces.ModelHolder;
import gossipLearning.messages.ModelMessage;
import peersim.core.CommonState;
import peersim.core.Node;


public class ETreeLearningProtocol extends AbstractProtocol {
    public ETreeLearningProtocol(String prefix) {
        super.init(prefix);
    }

    @Override
    public Object clone() {
        return new ETreeLearningProtocol(prefix);
    }

    @Override
    public void activeThread() {
        Node dest = getOverlay().getNeighbor( 0 );
        double value =  CommonState.r.nextGaussian();
        System.out.print("Seding!!! "+CommonState.getNode().getID() + " -> " + dest.getID()
                + " sayed: I am actived at time of " + CommonState.getTime() +
                ", value=" + value + "then delay is ");

        sendTo(new EModelMessage(currentNode, value), dest);
    }

    @Override
    public void passiveThread(ModelMessage message) {
        System.out.println("Receiving!!! "+currentNode.getID() + ", I am receving message from " + ((EModelMessage)message).src.getID()
        + ":" + CommonState.getTime() + ", " + ((EModelMessage)message).value);
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