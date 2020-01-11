package etreeLearning.protocol;

import gossipLearning.InstanceHolder;
import gossipLearning.interfaces.AbstractProtocol;
import gossipLearning.interfaces.ModelHolder;
import gossipLearning.messages.ModelMessage;
import peersim.cdsim.CDProtocol;
import peersim.core.Node;


public class ETreeLearningProtocol implements CDProtocol {
    private InstanceHolder instanceHolder;

    public ETreeLearningProtocol(String prefix) {

    }

    @Override
    public void nextCycle(Node node, int i) {

    }

    @Override
    public Object clone() {
        return new ETreeLearningProtocol("tets");
    }

    public void setInstanceHolder(InstanceHolder instanceHolder) {
        this.instanceHolder = instanceHolder;
    }
}
