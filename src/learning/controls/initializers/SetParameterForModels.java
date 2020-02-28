package learning.controls.initializers;

import learning.interfaces.LearningProtocol;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Protocol;

/**
 * @author sshpark
 * @date 28/2/2020
 */
public class SetParameterForModels implements Control {
    private static final String PAR_PROT = "protocol";
    private final int pid;

    public  SetParameterForModels(String prefix) {
        pid = Configuration.getPid(prefix + "." + PAR_PROT);
    }

    public boolean execute() {
        // for each learning protocol sets the number of classes for the initial models based on the stored instance holder
        for (int i = 0; i < Network.size(); i++) {
            Protocol learningProtocolP = Network.get(i).getProtocol(pid);
            if (learningProtocolP instanceof LearningProtocol) {
                // get the learning protocol
                LearningProtocol learningProtocol = (LearningProtocol) learningProtocolP;

                // for each model holder
                for (int j = 0; j < learningProtocol.size(); j ++) {
                    // for each model
                    for (int k = 0; learningProtocol.getModelHolder(j) != null && k < learningProtocol.getModelHolder(j).size(); k ++) {
                        if (learningProtocol.getModelHolder(j).getModel(k) != null) {
                            // sets the number of classes based on the stored instance holder
                            learningProtocol.getModelHolder(j).getModel(k).setNumberOfClasses(learningProtocol.getInstanceHolder().getNumberOfClasses());
                            learningProtocol.getModelHolder(j).getModel(k).setNumberOfFeatures(learningProtocol.getInstanceHolder().getNumberOfFeatures());
                        }
                    }
                }
            } else {
                throw new RuntimeException("The given protocol in initializer setNumberOfClasses.protocol is not a learning protocol!");
            }
        }
        return false;
    }
}
