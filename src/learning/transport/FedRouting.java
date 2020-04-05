package learning.transport;

import learning.messages.ModelMessage;
import learning.topology.TopoUtil;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import peersim.transport.Transport;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * @author sshpark
 * @date 27/3/2020
 */
public class FedRouting implements Transport {
    private final int processing_delay = 5; // ms
    private final int propagation_delay = 1; // ms
    private final int transmission_delay = 100; // ms
    private final int max_load_per_link = 2;



    /*@hidden */
    private int[][] graph; // Physical topology
    private int[][] minDelayMatrix;
    private int masterId;
    private boolean hasInit;

    public FedRouting(String prefix) {
        int n = Network.size();
        String topoFilePath = Configuration.getString("TOPO_FILEPATH");
        graph = TopoUtil.getGraph(n, topoFilePath);
        hasInit = false;
        minDelayMatrix = new int[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                minDelayMatrix[i][j] = i == j ? 0 : 1;
    }

    @Override
    public void send(Node src, Node dest, Object msg, int pid) {
        if (!hasInit) {
            masterId = dest.getIndex();
            generatedDelayMatrix();
            test_output("res/delay/fed.txt");
            hasInit = true;
        }

        int start = src.getIndex();
        int end = dest.getIndex();
        int delay = minDelayMatrix[start][end] == 0 ? minDelayMatrix[end][start] : minDelayMatrix[start][end];
        EDSimulator.add(delay+((ModelMessage) msg).getComputeDelay(), msg, dest, pid);

    }

    /**
     * Calculate delay matrix
     * @param start
     * @param graph
     * @param load
     * @param flag
     */
    private void shortestPath(int start, int[][] graph, int[][] load, boolean flag) {
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
            for (int i = 0; i < Network.size(); i++) {
                if (i != masterId) {
                    int p = i;
//                    System.out.print(p);
                    while (path[p] != -1) {
//                        System.out.print(" -> " + path[p]);
                        load[p][path[p]] = 1;
                        p = path[p];
                    }
//                    System.out.println();
                }
            }
        } else {
            load[start] = dis;
        }
    }

    /**
     * Generated delay matrix between different layers.
     */
    private void generatedDelayMatrix() {
        int n = Network.size();


        int[][] load = new int[n][n];
        for (int i = 0; i < n; i++)
            Arrays.fill(load[i], Integer.MAX_VALUE);

        shortestPath(masterId, graph, load, true);
        calculateDelay(load);

        int[][] res = new int[n][n];
        shortestPath(masterId, load, res, false);
        minDelayMatrix = res;
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
        System.out.println("master: " + out[39]);

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

    @Override
    public long getLatency(Node src, Node dest) {
        return 0;
    }

    @Override
    public Object clone() {
        return this;
    }

    private void test_output(String filepath) {
        try {
            FileWriter fileWriter = new FileWriter(filepath);

            for (int j = 0; j < Network.size(); j++)
                for (int k = 0; k < Network.size(); k++)
                    if (minDelayMatrix[j][k] != 0)
                        fileWriter.write(minDelayMatrix[j][k] + " ");

            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}