package learning.controls.initializers;

import learning.messages.ActiveThreadMessage;
import learning.protocols.FederatedLearningProtocol;
import learning.utils.Utils;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

import java.util.ArrayList;

/**
 * @author sshpark
 * @date 2/2/2020
 */
public class StartFedMessageInitializer implements Control {
    private static final String PAR_PROT = "protocol";
    private final int pid;
    private static final String PAR_DELAY = "delay";
    private final int delay;
    private static final String PAR_RECVPERCENT = "recvPercent";
    private final double recvPercent;

    public StartFedMessageInitializer(String prefix) {
        pid = Configuration.getPid(prefix + "." + PAR_PROT);
        delay = Configuration.getInt(prefix + "." + PAR_DELAY, 0);
        recvPercent = Configuration.getDouble(prefix + "." + PAR_RECVPERCENT, 1.0);
    }

    public boolean execute() {
        // init desriptor lists
        for (int i = 0; i < Network.size(); i++) {
            Node node = Network.get(i);
            // schedule starter alarm
            EDSimulator.add(delay, ActiveThreadMessage.getInstance(), node, pid);
        }
        // init randomly selected workers
        int masterId = FederatedLearningProtocol.getMasterID();
        int selectedNum = Math.max((int)((Network.size()-1)*recvPercent), 1);
        ArrayList<Integer> workers = new ArrayList<Integer>(){{
            for (int i = 0; i < Network.size(); i++)
                if (i != masterId) add(i);
        }};
        FederatedLearningProtocol node_pro = (FederatedLearningProtocol) Network.get(masterId).getProtocol(pid);
        node_pro.setSelectedID( Utils.randomArray(selectedNum, workers) );
        return false;
    }
}
