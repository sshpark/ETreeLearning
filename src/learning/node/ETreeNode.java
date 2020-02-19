package learning.node;

import peersim.config.Configuration;
import peersim.core.GeneralNode;

import java.util.ArrayList;

/**
 * @author sshpark
 * @date 14/2/2020
 */
public class ETreeNode extends GeneralNode {
    private ArrayList<Integer> parentNodeID;
    private ArrayList<Integer>[] childNodeIDList;

    @Override
    public Object clone() {
        ETreeNode result = null;
        result=(ETreeNode) super.clone();
        int layers = Configuration.getInt("LAYERS");
        result.parentNodeID = new ArrayList<>(layers);
        result.childNodeIDList = new ArrayList[layers];
        for (int i = 0; i < layers; i++)
            result.childNodeIDList[i] = new ArrayList<>();
        return result;
    }

    public ETreeNode(String prefix) {
        super(prefix);
        int layers = Configuration.getInt("LAYERS");
        parentNodeID = new ArrayList<>(layers);
        childNodeIDList = new ArrayList[layers];
        for (int i = 0; i < layers; i++)
            childNodeIDList[i] = new ArrayList<>();

    }

    public void addParentNode(int parentID) {
        parentNodeID.add(parentID);
    }

    public Integer getParentNode(int layer) {
        if (layer == parentNodeID.size()) layer--;
        return parentNodeID.get(layer);
    }

    public ArrayList<Integer> getChildNodeList(int layer) {
        return childNodeIDList[layer];
    }

    public void addChildNode(int layer, int nodeid) {
        if (layer >= childNodeIDList.length) return;
        childNodeIDList[layer].add(nodeid);
    }

    public void setChildNodeIDList(int layer, ArrayList<Integer> nodelist) {
        childNodeIDList[layer] = nodelist;
    }
}
