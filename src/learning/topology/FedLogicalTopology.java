package learning.topology;

import learning.protocols.FederatedLearningProtocol;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.dynamics.WireGraph;
import peersim.graph.Graph;

import java.util.ArrayList;

/**
 * @author sshpark
 * @date 20/2/2020
 */
public class FedLogicalTopology extends WireGraph {
    private final String topoFilePath;

    private static final String PAR_RECVPERCENT = "recvPercent";
    private final double recvPercent;

    /**@hidden */
    private int[][] graph;

    public FedLogicalTopology(String prefix) {
        super(prefix);
        topoFilePath = Configuration.getString("TOPO_FILEPATH");
        recvPercent = Configuration.getDouble(prefix + "." + PAR_RECVPERCENT);

        // gets physical network topology
        graph = TopoUtil.getGraph(Network.size(), topoFilePath);
    }

    @Override
    public void wire(Graph g) {
        ArrayList<Integer> nodeIdList = new ArrayList<Integer>() {{
            for (int i = 0; i < Network.size(); i++) add(i);
        }};

        int masterId = TopoUtil.findParameterServerId(graph, nodeIdList, recvPercent);
        // set FederatedLearningProtocol's masterId
        FederatedLearningProtocol.setMasterID(masterId);
    }
}
