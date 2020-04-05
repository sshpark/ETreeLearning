package learning.transport;

import learning.messages.MessageUp;
import learning.node.ETreeNode;
import learning.topology.TopoUtil;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import peersim.transport.Transport;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Delay = transmission delay + propagation delay + processing delay + queuing delay
 *
 * @author sshpark
 * @date 21/3/2020
 */
public class ETreeRouting implements Transport {
    private final int processing_delay = 5; // ms
    private final int propagation_delay = 1; // ms
    private final int transmission_delay = 100; // ms
    private final int max_load_per_link = 2;

    /*@hidden */
    private int[][] graph; // Physical topology
    private int[][][] minDelayMatrix;
    private int layers;
    private boolean hasInit;

    private ArrayList<ArrayList<Integer>> layersNodeID;


    public ETreeRouting(String prefix) {
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

        hasInit = false;
    }

    @Override
    public void send(Node src, Node dest, Object msg, int pid) {
        if (!hasInit) {
            generatedDelayMatrix();
            test_output("res/delay/ETree.txt");
            hasInit = true;
        }

        int start = src.getIndex();
        int end = dest.getIndex();
        int layer = ((MessageUp) msg).getLayer() + 1;
        int delay = minDelayMatrix[layer][start][end] == 0 ? minDelayMatrix[layer][end][start] : minDelayMatrix[layer][start][end];
//        System.out.println("src: " + start + ", dest: " + end + ", delay: " + delay);
        EDSimulator.add(delay+((MessageUp) msg).getComputeDelay(), msg, dest, pid);
    }

    @Override
    public long getLatency(Node src, Node dest) {
        return 0;
    }

    @Override
    public Object clone() {
        return this;
    }

    /**
     * Calculate delay matrix
     * @param start
     * @param layer
     * @param graph
     * @param load
     * @param flag
     */
    private void shortestPath(int start, int layer, int[][] graph, int[][] load, boolean flag) {
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
                    int delay = graph[u][to];

                    if (!vis[to] && dis[to] > dis[u] + delay) {
                        dis[to] = dis[u] + delay;
                        path[to] = u;
                        que.add(new Edge(to, dis[to]));
                    }
                }
            }
        }

        if (flag) {
            // child node
            for (Integer destId : ((ETreeNode) Network.get(start)).getChildNodeList(layer)) {
                int p = destId;
                while (path[p] != -1) {
                    load[p][path[p]] = 1;
                    p = path[p];
                }
            }
        } else {
            for (Integer destId : ((ETreeNode) Network.get(start)).getChildNodeList(layer)) {
                load[start][destId] = dis[destId];
            }
        }
    }

    /**
     * Generated delay matrix between different layers.
     */
    private void generatedDelayMatrix() {
        int n = Network.size();
        generatedLayersNodeID();

        for (int layer = 1; layer < layers; layer++) {
            int[][] load = new int[n][n];
            for (int i = 0; i < n; i++)
                Arrays.fill(load[i], Integer.MAX_VALUE);

            for (Integer rootId : layersNodeID.get(layer)) {
                shortestPath(rootId, layer, graph, load, true);
            }
            calculateDelay(load);

            int[][] res = new int[n][n];
            for (Integer rootId : layersNodeID.get(layer)) {
                shortestPath(rootId, layer, load, res, false);
            }
            minDelayMatrix[layer] = res;
        }
    }

    /**
     * Returns the delay matrix between [layer-1, layer].
     *
     * @param load
     */
    private void calculateDelay(int[][] load) {
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
                if (load[i][j] != Integer.MAX_VALUE) {
                    load[i][j] = out[i] + in[i];
                    load[i][j] = processing_delay + transmission_delay + propagation_delay
                            + ((int) Math.ceil(Math.max(load[i][j] - max_load_per_link, 0) * 1.0 / max_load_per_link))
                            * transmission_delay;
                    load[j][i] = load[i][j];
                }
            }
        }
    }

    /**
     * Get node indexes in each layer.
     */
    private void generatedLayersNodeID() {
        int n = Network.size();
        layersNodeID = new ArrayList<>(layers);

        for (int i = 0; i < layers; i++) {
            if (i == 0) {
                layersNodeID.add(new ArrayList<Integer>() {{
                    for (int j = 0; j < n; j++) add(j);
                }});
            } else {
                Set<Integer> temp = new HashSet<>();
                for (Integer id : layersNodeID.get(i - 1))
                    temp.add(((ETreeNode) Network.get(id)).getParentNode(i));
                layersNodeID.add(new ArrayList<>(temp));
            }
        }
    }

    private void test_output(String filepath) {
        try {
            FileWriter fileWriter = new FileWriter(filepath);
            for (int i = 1; i < layers; i++) {
                for (int j = 0; j < Network.size(); j++)
                    for (int k = 0; k < Network.size(); k++)
                        if (minDelayMatrix[i][j][k] != 0)
                            fileWriter.write(minDelayMatrix[i][j][k] + " ");
            }
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
