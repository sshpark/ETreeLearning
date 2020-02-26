package learning.controls.initializers;

import learning.messages.ActiveThreadMessage;
import learning.node.ETreeNode;
import learning.protocols.ETreeLearningProtocol;
import learning.utils.Utils;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

import java.util.ArrayList;

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
    private static ArrayList<ArrayList<Integer>> layersNodeID;

    private int layers;

    public StartETreeMessageInitializer(String prefix) {
        pid = Configuration.getPid(prefix + "." + PAR_PROT);
        delay = Configuration.getInt(prefix + "." + PAR_DELAY, 0);
        recvPercent = Configuration.getDouble(prefix + "." + PAR_RECVPERCENT, 1.0);
        layers = Configuration.getInt("LAYERS");
    }

    public boolean execute() {
        // init phase
        CommonState.setPhase(1);

        for (int layer = 1; layer < layers; layer++) {
            for (Integer id : layersNodeID.get(layer)) {
                ETreeNode node = (ETreeNode) Network.get(id);
                ArrayList<Integer> childList = new ArrayList<>(node.getChildNodeList(layer));
                ((ETreeLearningProtocol) node.getProtocol(pid)).setLayersReceivedID(
                        layer, Utils.randomArray(Math.max((int) (childList.size() * recvPercent), 1), childList)
                );
            }
        }

        for (int i = 0; i < Network.size(); i++) {
            Node node = Network.get(i);
            // schedule starter alarm
            EDSimulator.add(delay, ActiveThreadMessage.getInstance(), node, pid);
        }
        return false;
    }

    public static void setLayersNodeID(ArrayList<ArrayList<Integer>> layersNode) {
        layersNodeID = layersNode;
    }
}
