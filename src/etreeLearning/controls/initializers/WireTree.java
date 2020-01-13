package etreeLearning.controls.initializers;

import etreeLearning.node.TreeNode;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.dynamics.WireGraph;
import peersim.graph.Graph;

import java.util.*;

public class WireTree extends WireGraph {
    /**
     * the range of child nodes
     */
    private static final String PAR_LOW = "low";
    private static final String PAR_HIGH = "high";

    private int l;
    private int r;

    public WireTree(String prefix) {
        super(prefix);

        l = Configuration.getInt(prefix + "." + PAR_LOW);
        r = Configuration.getInt(prefix + "." + PAR_HIGH);
    }


    public void wire(Graph graph) {
        final int n = graph.size();

        if (r-l > n || l > r)
            throw new RuntimeException("Error boundary: " + l + ", " + r);

        List tree = new ArrayList(n);

        /** shuffle the list **/
        for (int i = 0; i < n; i++) tree.add(i);
        Collections.shuffle(tree);

        Random rd = new Random();

        /** selected the root node */
        int root = rd.nextInt(n);
        TreeNode node = (TreeNode)Network.get( (int)tree.get(root) );
        node.setType(0);
        LinkedList q = new LinkedList();
        q.push( tree.get(root) );
        System.out.println( (int)tree.get(root) );

        tree.remove(root);

        while (!tree.isEmpty()) {
            int top = (int) q.poll();
            node = (TreeNode) Network.get(top);

            int rng = rd.nextInt( r-l+1 )+l;
            int len = tree.size();
            for (int i = 0; i < rng; i++) {

                int index = rd.nextInt(len);

                graph.setEdge(top, (int)tree.get(index) );
                graph.setEdge((int)tree.get(index), top );
                q.push( tree.get(index) );
                tree.remove( index );
                len--;
                if (len == 0) break;
            }
        }

//        for (int i = 0; i < n; i++) {
//            Collection temp = graph.getNeighbours(i);
//            System.out.println(i + ": " + temp);
//        }

    }
}
