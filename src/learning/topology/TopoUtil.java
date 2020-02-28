package learning.topology;

import peersim.config.Configuration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * @author sshpark
 * @date 17/2/2020
 */
public class TopoUtil {

    /**
     * Returns the adjacency matrix of the network,
     * If value is Integer.MAX_VALUE, then there is no edge between two nodes
     * else it represents the delay between two nodes.
     *
     * Notice that the index of node starts with 0.
     *
     * @return Adjacency matrix
     */
    public static int[][] getGraph(int n, String filePath) {
        int[][] graph = new int[n][n];

        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                graph[i][j] = i == j ? 0 : Integer.MAX_VALUE;

        try {
            FileReader fr = new FileReader(filePath);
            BufferedReader bf = new BufferedReader(fr);
            String str;

            while ((str = bf.readLine()) != null) {
                String[] temp = str.split(" ");
                int from = Integer.parseInt(temp[0])-1;
                int to = Integer.parseInt(temp[1])-1;
                graph[from][to] = Integer.parseInt(temp[2]);
                graph[to][from] = Integer.parseInt(temp[2]);
            }
            bf.close();
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return graph;
    }

    /**
     * Returns the minimum delay from start(node index) to end(node index),
     * if minDelay = Integet.MAX_VALUE, it means that message can not from start to end.
     *
     * Implemented by Dijkstra with heap
     *
     * @param graph
     * @param start message from
     * @return the minimum delay
     */
    private static int[] getSingelNodeMinDelay(int[][] graph, int start) {
        class Edge implements Comparable<Edge>{
            int to , cost;
            Edge(int to_,int cost_){
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
        // init dis
        for (int i = 0; i < n; i++) dis[i] = Integer.MAX_VALUE;
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
                        dis[to] = dis[u]+delay;
                        que.add(new Edge(to, dis[to]));
                    }
                }
            }
        }
        return dis;
    }

    /**
     * Returns the shortest path from node to node.
     * @param graph
     * @return
     */
    public static int[][] generateMinDelayMatrix(int[][] graph) {
        int[][] minDelayMatrix = new int[graph.length][graph.length];
        for (int i = 0; i < graph.length; i++)
            for (int j = 0; j < graph.length; j++)
                minDelayMatrix[i][j] = i == j ? 1 : Integer.MAX_VALUE;

        for (int nodeIndex = 0; nodeIndex < graph.length; nodeIndex++) {
            int[] singleNodeDelayArray = getSingelNodeMinDelay(graph, nodeIndex);
            for (int i = 0; i < graph.length; i++) {
                minDelayMatrix[nodeIndex][i] = singleNodeDelayArray[i];
            }
        }
        return minDelayMatrix;
    }

    /**
     *
     * @param graph
     * @param nodeIdList
     * @param aggregationRatio percentage of the model to begin aggregating
     * @return
     */
    public static int findParameterServerId(int[][] graph, ArrayList<Integer> nodeIdList, float aggregationRatio) {
        int[][] minDelayMatrix = generateMinDelayMatrix(graph);

        ArrayList<Integer> theDelaysAtAggregationRatio = new ArrayList<>();
        int k = Math.round(nodeIdList.size() * (1 - aggregationRatio)) + 1;
        for (int i = 0; i < nodeIdList.size(); i++) {
            PriorityQueue<Integer> largeK = new PriorityQueue<>(k + 1);
            for (int j = 0; j < nodeIdList.size(); j++) {
                if (i == j) {
                    continue;
                }
                largeK.add(minDelayMatrix[nodeIdList.get(i)][nodeIdList.get(j)]);
                if (largeK.size() > k) {
                    largeK.poll();
                }
            }
            theDelaysAtAggregationRatio.add(largeK.poll());
        }
//        System.out.println(theDelaysAtAggregationRatio);
        int selectedNodeId = nodeIdList.get(0);
        int minDelay = theDelaysAtAggregationRatio.get(0);
        for (int nodeIndex = 1; nodeIndex < theDelaysAtAggregationRatio.size(); nodeIndex++) {
            if (theDelaysAtAggregationRatio.get(nodeIndex) < minDelay) {
                minDelay = theDelaysAtAggregationRatio.get(nodeIndex);
                selectedNodeId = nodeIdList.get(nodeIndex);
            }
        }
        return selectedNodeId;
    }

    /**
     * Returns the result of grouping, the aggregate node is the last index
     * of ArrayList<group_num>
     * @param graph
     * @param nodeIdList nodes need to part
     * @param aggregationRatio
     * @return
     */
    public static ArrayList<ArrayList<Integer>> 
    getGraphPartitionResult(int[][] graph, ArrayList<Integer> nodeIdList, float aggregationRatio) {
        int minClusterDelay = Integer.MAX_VALUE;
        int tmpMinClusterDelay = Integer.MAX_VALUE;
        int[][] minDelayMatrix = generateMinDelayMatrix(graph);
        ArrayList<ArrayList<Integer>> finalClusterList = new ArrayList<>();
        int startClusterNum = nodeIdList.size() / 10;
        int[] finalClusterCenterId = new int[startClusterNum];
        for (int k = startClusterNum; k < startClusterNum + 100; k = k + 10) {
            for (int it = 0; it < 100; it++) {
                ArrayList<ArrayList<Integer>> clusterList = new ArrayList<>(k);
                for (int i = 0; i < k; i++) {
                    ArrayList<Integer> cluster = new ArrayList<>(nodeIdList.size());
                    clusterList.add(cluster);
                }
                Random random = new Random();
                int[] clusterCenterNodeId = new int[k];
                HashSet<Integer> hashSet = new HashSet<>();
                for (int i = 0; i < k; i++) {
                    int randomNodeIndex = random.nextInt(nodeIdList.size());
                    while (hashSet.contains(randomNodeIndex)) {
                        randomNodeIndex = random.nextInt(nodeIdList.size());
                    }
                    hashSet.add(randomNodeIndex);
                    clusterCenterNodeId[i] = nodeIdList.get(randomNodeIndex);
                }
                boolean terminateFlag = false;
                while (!terminateFlag) {
                    terminateFlag = true;
                    for (int i = 0; i < k; i++) {
                        clusterList.get(i).clear();
                    }
                    for (int i = 0; i < nodeIdList.size(); i++) {
                        int nearestClusterCenter = 0;
                        int minDelay = minDelayMatrix[nodeIdList.get(i)][clusterCenterNodeId[0]];
                        for (int j = 1; j < k; j++) {
                            if (minDelayMatrix[nodeIdList.get(i)][clusterCenterNodeId[j]] < minDelay) {
                                nearestClusterCenter = j;
                                minDelay = minDelayMatrix[nodeIdList.get(i)][clusterCenterNodeId[j]];
                            }
                        }
                        clusterList.get(nearestClusterCenter).add(nodeIdList.get(i));
                    }
                    int maxClusterDelay = 0;
                    for (int i = 0; i < k; i++) {
                        int minTotalDelay = Integer.MAX_VALUE;
                        int newCenterNodeId = clusterCenterNodeId[i];
                        int maxDelay = 0;
                        int n = Math.round(nodeIdList.size() * (1 - aggregationRatio)) + 1;
                        PriorityQueue<Integer> largeK = new PriorityQueue<>(n + 1);
                        for (int j = 0; j < clusterList.get(i).size(); j++) {
                            int totalDelay = 0;
                            for (Integer nodeId : clusterList.get(i)) {
                                if (nodeId == clusterList.get(i).get(j)) {
                                    continue;
                                }
                                totalDelay += minDelayMatrix[nodeId][clusterList.get(i).get(j)];
                                if (minDelayMatrix[nodeId][clusterList.get(i).get(j)] > maxDelay) {
                                    maxDelay = minDelayMatrix[nodeId][clusterList.get(i).get(j)];
                                }
                                largeK.add(minDelayMatrix[nodeId][clusterList.get(i).get(j)]);
                                if (largeK.size() > n) {
                                    largeK.poll();
                                }
                            }
                            if (totalDelay < minTotalDelay) {
                                minTotalDelay = totalDelay;
                                newCenterNodeId = clusterList.get(i).get(j);
                            }
                        }
                        if (newCenterNodeId != clusterCenterNodeId[i]) {
                            terminateFlag = false;
                            clusterCenterNodeId[i] = newCenterNodeId;
                        }
                        Object tmp = largeK.poll();
                        int tmpMaxDelay = 0;
                        if (tmp != null) {
                            tmpMaxDelay = (Integer) tmp;
                        }
                        if (tmpMaxDelay > maxClusterDelay) {
                            maxClusterDelay = tmpMaxDelay;
                        }
                    }
                    // System.out.println(maxClusterDelay);
                    tmpMinClusterDelay = maxClusterDelay;
                }
                if (tmpMinClusterDelay < minClusterDelay) {
                    minClusterDelay = tmpMinClusterDelay;
                    // System.out.println(minClusterDelay);
                    finalClusterList = clusterList;
                    finalClusterCenterId = clusterCenterNodeId;
                }
            }
        }
        for (int i = 0; i < finalClusterCenterId.length; i++) {
            // System.out.println(finalClusterCenterId[i]);
            finalClusterList.get(i).add(finalClusterCenterId[i]);
            // System.out.println(finalClusterList.get(i));
        }
        return finalClusterList;
    }

    public static ArrayList<ArrayList<Integer>> getGraphPartitionResult(int[][] graph, ArrayList<Integer> nodeIdList, double aggregationRatio, int k) {
        int minClusterDelay = Integer.MAX_VALUE;
        int tmpMinClusterDelay = Integer.MAX_VALUE;
        int[][] minDelayMatrix = generateMinDelayMatrix(graph);
        ArrayList<ArrayList<Integer>> finalClusterList = new ArrayList<>();
        int[] finalClusterCenterId = new int[k];
        for (int it = 0; it < 10000; it++) {
            ArrayList<ArrayList<Integer>> clusterList = new ArrayList<>(k);
            for (int i = 0; i < k; i++) {
                ArrayList<Integer> cluster = new ArrayList<>(nodeIdList.size());
                clusterList.add(cluster);
            }
            Random random = new Random();
            int[] clusterCenterNodeId = new int[k];
            HashSet<Integer> hashSet = new HashSet<>();
            for (int i = 0; i < k; i++) {
                int randomNodeIndex = random.nextInt(nodeIdList.size());
                while (hashSet.contains(randomNodeIndex)) {
                    randomNodeIndex = random.nextInt(nodeIdList.size());
                }
                hashSet.add(randomNodeIndex);
                clusterCenterNodeId[i] = nodeIdList.get(randomNodeIndex);
            }
            boolean terminateFlag = false;
            while (!terminateFlag) {
                terminateFlag = true;
                for (int i = 0; i < k; i++) {
                    clusterList.get(i).clear();
                }
                for (int i = 0; i < nodeIdList.size(); i++) {
                    int nearestClusterCenter = 0;
                    int minDelay = minDelayMatrix[nodeIdList.get(i)][clusterCenterNodeId[0]];
                    for (int j = 1; j < k; j++) {
                        if (minDelayMatrix[nodeIdList.get(i)][clusterCenterNodeId[j]] < minDelay) {
                            nearestClusterCenter = j;
                            minDelay = minDelayMatrix[nodeIdList.get(i)][clusterCenterNodeId[j]];
                        }
                    }
                    clusterList.get(nearestClusterCenter).add(nodeIdList.get(i));
                }
                int maxClusterDelay = 0;
                for (int i = 0; i < k; i++) {
                    int minTotalDelay = Integer.MAX_VALUE;
                    int newCenterNodeId = clusterCenterNodeId[i];
                    int maxDelay = 0;
                    int n = (int) (Math.round(nodeIdList.size() * (1 - aggregationRatio)) + 1);
                    PriorityQueue<Integer> largeK = new PriorityQueue<>(n + 1);
                    for (int j = 0; j < clusterList.get(i).size(); j++) {
                        int totalDelay = 0;
                        for (Integer nodeId : clusterList.get(i)) {
                            if (nodeId == clusterList.get(i).get(j)) {
                                continue;
                            }
                            totalDelay += minDelayMatrix[nodeId][clusterList.get(i).get(j)];
                            if (minDelayMatrix[nodeId][clusterList.get(i).get(j)] > maxDelay) {
                                maxDelay = minDelayMatrix[nodeId][clusterList.get(i).get(j)];
                            }
                            largeK.add(minDelayMatrix[nodeId][clusterList.get(i).get(j)]);
                            if (largeK.size() > n) {
                                largeK.poll();
                            }
                        }
                        if (totalDelay < minTotalDelay) {
                            minTotalDelay = totalDelay;
                            newCenterNodeId = clusterList.get(i).get(j);
                        }
                    }
                    if (newCenterNodeId != clusterCenterNodeId[i]) {
                        terminateFlag = false;
                        clusterCenterNodeId[i] = newCenterNodeId;
                    }
                    Object tmp = largeK.poll();
                    int tmpMaxDelay = 0;
                    if (tmp != null) {
                        tmpMaxDelay = (Integer) tmp;
                    }
                    if (tmpMaxDelay > maxClusterDelay) {
                        maxClusterDelay = tmpMaxDelay;
                    }
                }
                // System.out.println(maxClusterDelay);
                tmpMinClusterDelay = maxClusterDelay;
            }
            if (tmpMinClusterDelay < minClusterDelay) {
                minClusterDelay = tmpMinClusterDelay;
//                System.out.println(minClusterDelay);
                finalClusterList = clusterList;
                finalClusterCenterId = clusterCenterNodeId;
            }
        }

        for (int i = 0; i < finalClusterCenterId.length; i++) {
            // System.out.println(finalClusterCenterId[i]);
            finalClusterList.get(i).add(finalClusterCenterId[i]);
            // System.out.println(finalClusterList.get(i));
        }
        return finalClusterList;
    }
}
