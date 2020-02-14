package learning.node;

import peersim.core.GeneralNode;
import peersim.core.Network;

/**
 * @author sshpark
 * @date 14/2/2020
 */
public class ETreeNode extends GeneralNode {
    private final int MAX_LAYERS = Network.size();
    private int[] parentNodeID;

    public ETreeNode(String prefix) {
        super(prefix);
        parentNodeID = new int[MAX_LAYERS];
    }

    public void setParentNode(int layer, int parentID) {
        if (layer > MAX_LAYERS) throw new RuntimeException("layer must be less than Network size!");
        parentNodeID[layer] = parentID;
    }

    public int getParentNode(int layer) {
        if (layer > MAX_LAYERS) throw new RuntimeException("layer must be less than Network size!");
        return parentNodeID[layer];
    }
}
