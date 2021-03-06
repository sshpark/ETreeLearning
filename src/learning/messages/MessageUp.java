package learning.messages;

import learning.interfaces.ModelHolder;
import peersim.core.Node;

/**
 * @author sshpark
 * @date 16/2/2020
 */
public class MessageUp extends ModelMessage {
    private int layer;
    public MessageUp(Node src, int layer, ModelHolder models) {
        super(src, models);
        this.layer = layer;
    }

    public int getLayer() {
        return layer;
    }
}
