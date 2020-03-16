package learning.messages;

import learning.interfaces.ModelHolder;
import peersim.core.Node;

/**
 * @author sshpark
 * @date 16/2/2020
 */
public class MessageDown extends ModelMessage{
    public MessageDown(Node src, ModelHolder models) {
        super(src, models);
    }
}
