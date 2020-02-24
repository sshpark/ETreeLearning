package learning.controls.initializers;

import learning.messages.ActiveThreadMessage;
import learning.node.ETreeNode;
import learning.protocols.ETreeLearningProtocol;
import learning.utils.Utils;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

/**
 * @author sshpark
 * @date 24/2/2020
 */
public class StartETreeMessageInitializer implements Control {
    private static final String PAR_PROT = "protocol";
    private final int pid;
    private static final String PAR_DELAY = "delay";
    private final int delay;
    private static final String PAR_RECVPERCENT = "recvPercent";
    private final double recvPercent;

    private int layers;

    public StartETreeMessageInitializer(String prefix) {
        pid = Configuration.getPid(prefix + "." + PAR_PROT);
        delay = Configuration.getInt(prefix + "." + PAR_DELAY, 0);
        recvPercent = Configuration.getDouble(prefix + "." + PAR_RECVPERCENT, 1.0);
        layers = Configuration.getInt("LAYERS");
    }

    public boolean execute() {
        int n = Network.size();
        // init desriptor lists
        for (int i = 0; i < Network.size(); i++) {
            ETreeNode node = (ETreeNode) Network.get(i);
            for (int layer = 1; layer < layers; layer++) {
                // TODO: It should be set according to the number of different child nodes
                int selected_num = (int)(n * recvPercent);
                ((ETreeLearningProtocol) node.getProtocol(pid)).setLayersReceivedID(
                        layer, Utils.randomArray(0, n, selected_num)
                );
            }
            // schedule starter alarm
            EDSimulator.add(delay, ActiveThreadMessage.getInstance(), node, pid);
        }
        return false;
    }
}
