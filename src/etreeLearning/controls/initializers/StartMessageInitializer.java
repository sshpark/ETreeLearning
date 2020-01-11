package etreeLearning.controls.initializers;

import gossipLearning.messages.ActiveThreadMessage;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

public class StartMessageInitializer implements Control {
    private static final String PAR_PORT = "protocol";
    private final int pid;
    private static final String PAR_DELAY = "delay";
    private final int delay;

    public StartMessageInitializer(String prefix) {
        pid = Configuration.getPid(prefix + "." + PAR_PORT);
        delay = Configuration.getInt(prefix + "." + PAR_DELAY);
    }


    @Override
    public boolean execute() {
        for (int i = 0; i < Network.size(); i++) {
            Node node = Network.get(i);

            EDSimulator.add(delay, ActiveThreadMessage.getInstance(), node, pid);
        }
        return false;
    }
}
