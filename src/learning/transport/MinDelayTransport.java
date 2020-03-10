package learning.transport;

import learning.topology.TopoUtil;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import peersim.transport.Transport;

/**
 * Gets the fastest message transmission time between two nodes
 * according to the node delay of the current network.
 * @author sshpark
 * @date 19/2/2020
 */
public class MinDelayTransport implements Transport {
    private final String topoFilePath;

    /*@hidden */
    private int[][] graph;
    private int[][] minDelayMatrix;

    public MinDelayTransport(String prefix) {
        topoFilePath = Configuration.getString("TOPO_FILEPATH");
        graph = TopoUtil.getGraph(Network.size(), topoFilePath);
        minDelayMatrix = TopoUtil.generateMinDelayMatrix(graph);
    }

    @Override
    public void send(Node src, Node dest, Object msg, int pid) {
        int delay = minDelayMatrix[src.getIndex()][dest.getIndex()];
//        System.out.println("From " + src.getIndex() + " to " + dest.getIndex() + ", time: " + delay);
        EDSimulator.add(delay, msg, dest, pid);
    }

    @Override
    public long getLatency(Node src, Node dest) {
        return 0;
    }

    @Override
    public Object clone() {
        return this;
    }
}
