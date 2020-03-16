package learning.topology;

import peersim.config.Configuration;
import peersim.core.Network;
import peersim.dynamics.WireGraph;
import peersim.graph.Graph;

/**
 * @author sshpark
 * @date 20/2/2020
 */
public class GossipLogicalTopology extends WireGraph {
    private final String topoFilePath;

    /**@hidden */
    private int[][] graph;

    public GossipLogicalTopology(String prefix) {
        super(prefix);
        topoFilePath = Configuration.getString("TOPO_FILEPATH");
        graph = TopoUtil.getGraph(Network.size(), topoFilePath);
    }

    @Override
    public void wire(Graph g) {
        int n = graph.length;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j && graph[i][j] != Integer.MAX_VALUE) {
                    g.setEdge(i, j);
                    g.setEdge(j, i);
                }
            }
        }
    }
}
