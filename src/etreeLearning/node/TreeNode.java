package etreeLearning.node;

import peersim.core.GeneralNode;

/**
 * Tree node for E-Tree Learning
 */
public class TreeNode extends GeneralNode {

    /**
     * type = 0: root
     * type = 1: node in depth=2
     * type = 2: leaf node
     */
    private int type;

    public TreeNode(String prefix) {
        super(prefix);
    }


    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
