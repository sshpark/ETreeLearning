package learning.node;

import peersim.core.GeneralNode;
import peersim.core.Network;

import java.util.ArrayList;

/**
 * @author sshpark
 * @date 14/2/2020
 */
public class ETreeNode extends GeneralNode {
    private ArrayList<Integer> parentNodeID;
    private ArrayList<ArrayList<Integer>> childNodeIDList;

    public ETreeNode(String prefix) {
        super(prefix);
        parentNodeID = new ArrayList<>();
    }

    public void setParentNode(int layer, int parentID) {
        parentNodeID.set(layer, parentID);
    }

    public Integer getParentNode(int layer) {
        return parentNodeID.get(layer);
    }

    public ArrayList<Integer> getChildNodeList(int layer) {
        return childNodeIDList.get(layer);
    }

    public void setChildNodeList(ArrayList<ArrayList<Integer>> childNodeIDList) {
        this.childNodeIDList = childNodeIDList;
    }
}
