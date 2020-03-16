package learning.messages;

import learning.interfaces.ModelHolder;
import peersim.core.Node;

/**
 * @author sshpark
 * @date 31/1/2020
 */
public class FedModelToMasterMessage extends ModelMessage {
    private long modelSentTime;

    public FedModelToMasterMessage(Node src, ModelHolder models, long modelSentTime) {
        super(src, models);
        this.modelSentTime = modelSentTime;
    }

    public long getModelSentTime() {
        return modelSentTime;
    }
}
