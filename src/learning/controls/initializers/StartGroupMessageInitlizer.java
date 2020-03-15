package learning.controls.initializers;

import learning.messages.ActiveThreadMessage;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

/**
 * @author sshpark
 * @date 9/3/2020
 */
public class StartGroupMessageInitlizer implements Control {
    private static final String PAR_PROT = "protocol";
    private final int pid;
    private static final String PAR_DELAY = "delay";
    private final int delay;

    public StartGroupMessageInitlizer(String prefix) {
        pid = Configuration.getPid(prefix + "." + PAR_PROT);
        delay = Configuration.getInt(prefix + "." + PAR_DELAY, 0);
    }

    public boolean execute() {
        CommonState.setGlobalName("compute", 0);
        // init desriptor lists
        for (int i = 0; i < Network.size(); i++) {
            Node node = Network.get(i);
            // schedule starter alarm
            EDSimulator.add(delay, ActiveThreadMessage.getInstance(), node, pid);
        }
        return false;
    }
}
