package learning.transport;

import learning.node.ETreeNode;
import learning.topology.TopoUtil;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.transport.Transport;

import java.util.*;

/**
 * Delay = transmission delay + propagation delay + processing delay + queuing delay
 *
 * @author sshpark
 * @date 21/3/2020
 */
public class Routing implements Transport {
    private final int processing_delay = 5; // ms
    private final int propagation_delay = 1; // ms
    private final int transmission_delay = 10; // ms

    /*@hidden */
    private int[][] graph; // Physical topology
    private int[][][] minDelayMatrix;
    private int layers;

    private static ArrayList<ArrayList<Integer>> layersNodeID;


    public Routing(String prefix) {
        int n = Network.size();
        String topoFilePath = Configuration.getString("TOPO_FILEPATH");
        layers = Configuration.getInt("LAYERS");

        graph = TopoUtil.getGraph(n, topoFilePath);
        minDelayMatrix = new int[layers][n][n];
        for (int k = 0; k < layers; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    minDelayMatrix[k][i][j] = i == j ? 0 : 1;
                }
            }
        }
    }

    @Override
    public void send(Node src, Node dest, Object msg, int pid) {

    }

    @Override
    public long getLatency(Node src, Node dest) {
        return 0;
    }

    @Override
    public Object clone() {
        return this;
    }

    private int[] shortestPath(int start, int layer, int[][] load) {
        class Edge implements Comparable<Edge> {
            int to, cost;

            Edge(int to_, int cost_) {
                to = to_;
                cost = cost_;
            }

            @Override
            public int compareTo(Edge o) {
                return this.cost - o.cost;
            }
        }

        int n = graph.length;
        boolean[] vis = new boolean[n];
        int[] dis = new int[n];
        int[] path = new int[n];
        // init dis
        for (int i = 0; i < n; i++) {
            dis[i] = Integer.MAX_VALUE;
            path[i] = -1;
        }
        Queue<Edge> que = new PriorityQueue<>();
        que.add(new Edge(start, 0));
        dis[start] = 0;
        while (!que.isEmpty()) {
            Edge top = que.poll();
            int u = top.to;

            if (dis[u] < top.cost) continue;
            if (vis[u]) continue;

            vis[u] = true;

            for (int to = 0; to < n; to++) {
                if (u != to && graph[u][to] != Integer.MAX_VALUE) {
                    int delay = minDelayMatrix[layer][u][to];

                    if (!vis[to] && dis[to] > dis[u] + delay) {
                        dis[to] = dis[u] + delay;
                        path[to] = u;
                        que.add(new Edge(to, dis[to]));
                    }
                }
            }
        }

        // child node
        for (Integer destId :((ETreeNode) Network.get(start)).getChildNodeList(layer)) {
            int p = destId;
            while (path[p] != -1) {
                load[p][path[p]] = 1;
                p = path[p];
            }
        }

        return dis;
    }


    private void generatedDelayMatrix() {
        int n = Network.size();

        for (int layer = 1; layer < layers; layer++) {
            int[][] load = new int[n][n];
            for (Integer rootId : layersNodeID.get(layer)) {
                shortestPath(rootId, layer, load);
            }

        }
    }

    private void calculateDelay(int[][] load, int layer) {
        int n = Network.size();
        int[] in = new int[n];
        int[] out = new int[n];


        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (load[i][j] == 1) {
                    out[i]++;
                    in[j]++;
                }
            }
        }

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (load[i][j] == 1) {
                    load[i][j] = out[i]+in[i];
                    load[i][j] = processing_delay+transmission_delay+propagation_delay;
                }
            }
        }
    }

    public static void setLayersNodeID(ArrayList<ArrayList<Integer>> layersNode) {
        layersNodeID = layersNode;
    }
}
