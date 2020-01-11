package etreeLearning.messages;

import gossipLearning.messages.Message;
import peersim.core.Node;

public class EModelMessage implements Message {
    private double value = 0.0;
    private Node src;

    public EModelMessage(Node src, double value) {
        this.src = src;
        this.value = value;
    }
}
