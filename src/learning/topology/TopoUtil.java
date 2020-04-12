package learning.topology;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.core.Protocol;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import org.graalvm.compiler.core.gen.NodeLIRBuilder;

import learning.protocols.ETreeLearningProtocol;

/**
 * @author sshpark
 * @date 17/2/2020
 */
public class TopoUtil {
	
	// The evaluation set for clustering
	private static double[] accuracies = new double[Network.size()];
	private static ArrayList<HashSet<Double>> classDistribution = new ArrayList<HashSet<Double>>(Network.size());

	// the difference allowed between cluster's average accuracy and global average accuracy
	private static double delta = Configuration.getDouble("clustering.delta");
	private static int iterations = Configuration.getInt("clustering.iterations");
	private static int checkNearestRatio = Configuration.getInt("clustering.checkNearestRatio");
	private static double globalAvgAcc = 0.0;

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
    public static int findParameterServerId(int[][] graph, ArrayList<Integer> nodeIdList, double aggregationRatio) {
        int[][] minDelayMatrix = generateMinDelayMatrix(graph);

        ArrayList<Integer> theDelaysAtAggregationRatio = new ArrayList<>();
        int k = (int)Math.round(nodeIdList.size() * (1 - aggregationRatio)) + 1;
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
    
    // find the center id of a list
    public static int findCenterId(ArrayList<Integer> list, int[][] minDelayMatrix) {
    	int minTotalDelay = Integer.MAX_VALUE;
        int newCenterNodeId = list.get(0);
        for (int j = 0; j < list.size(); j++) {
            int totalDelay = 0;
            for (Integer nodeId : list) {
                if (nodeId.equals(list.get(j))) {
                    continue;
                }
                totalDelay += minDelayMatrix[nodeId][list.get(j)];
            }
            if (totalDelay < minTotalDelay) {
                minTotalDelay = totalDelay;
                newCenterNodeId = list.get(j);
            }
        }
    	return newCenterNodeId;
    }
    
    // pretrain on each node and obtain the average accuracy for each node
    public static void pretrainForClustering() {
    	double sum = 0.0;
    	double[] subsum = new double[10];
    	for (int i = 0; i < 10; i++) {
    		subsum[i] = 0.0;
    	}
    	
    	for (int i = 0; i < Network.size(); i++) {
    		Node node = Network.get(i);
            Protocol protocol = node.getProtocol(0);
            if (protocol instanceof ETreeLearningProtocol) {
	            ETreeLearningProtocol learningProtocol = (ETreeLearningProtocol) protocol;
	            accuracies[i] = learningProtocol.pretrain();
	            sum += accuracies[i];
	            
	            // class distribution
	            HashSet<Double> classes = learningProtocol.classDistribution();
	            classDistribution.add(classes);
            } else {
                throw new RuntimeException("The protocol " + 0 + " have to implement ETreeLearningProtocol interface!");
            }
    	}
    	
    	globalAvgAcc = sum / (Network.size()*1.0);
    }
    
    public static boolean inArray(int id, int[] nodeList) {
    	for (int i = 0; i < nodeList.length; i++) {
    		if (id == nodeList[i]) {
    			return true;
    		}
    	}
    	return false;
    }
    
    // compute average losses of each cluster
    public static double[] computeAvgLosses(ArrayList<ArrayList<Integer>> clusterList) {
		double[] avgLosses = new double[clusterList.size()];
		for (int i = 0; i < clusterList.size(); i++) {
			double sum = 0.0;
			for (int j = 0; j < clusterList.get(i).size(); j++) {
				sum += accuracies[clusterList.get(i).get(j)];
			}
			avgLosses[i] = sum / clusterList.get(i).size();
		}
		return avgLosses;
	}
    
    // clustering only accounting the ununiform data distribution
    public static ArrayList<ArrayList<Integer>> getUnuniformGraphPartition(
    		int[][] graph, ArrayList<Integer> nodeIdList, int k) {
    	
    	int[][] minDelayMatrix = generateMinDelayMatrix(graph);
    	ArrayList<ArrayList<Integer>> clusterList = new ArrayList<>(3);
        for (int i = 0; i < k; i++) {
            ArrayList<Integer> cluster = new ArrayList<>(nodeIdList.size());
            clusterList.add(cluster);
        }
        int[] nodeIds = new int[nodeIdList.size()];
        for (int i = 0; i < nodeIdList.size(); i++) {
        	nodeIds[i] = nodeIdList.get(i);
        }
        Arrays.sort(nodeIds);
        int numOfNodePerCluster = nodeIdList.size() / k;
        int left = nodeIdList.size() % k;
        int nodeIndex = 0;
        
        for (int i = 0; i < k; i++) {
        	int numOfNodes = numOfNodePerCluster;
        	if (i < left) {
        		numOfNodes++;
        	}
        	int temp = nodeIndex;
        	nodeIndex += numOfNodes;
        	for (int j = temp; j < nodeIndex; j++) {
        		clusterList.get(i).add(nodeIds[j]);
        	}
        }
        
        double[] avgLosses = computeAvgLosses(clusterList);
        System.out.println(avgLosses);
        
        for (int i = 0; i < k; i++) {
        	int centerId = findCenterId(clusterList.get(i), minDelayMatrix);
        	clusterList.get(i).add(centerId);
        }
        
        return clusterList;
    }
    
    // clustering based on k-means and average accuracy
    public static ArrayList<ArrayList<Integer>> getGraphPartitionByDistanceAndDataDistribution(
    		int[][] graph, ArrayList<Integer> nodeIdList, int k) {
    	
    	int[][] minDelayMatrix = generateMinDelayMatrix(graph);
    	ArrayList<Double> differences = new ArrayList<Double>();
    	
    	// 初始化包含三个列表的分组列表，每个列表的大小为待分组的节点数
    	// initiate a ArrayList including k ArrayLists,
        // where each ArrayList's size is the number of nodes to be clustered.
        ArrayList<ArrayList<Integer>> clusterList = new ArrayList<>(3);
        for (int i = 0; i < k; i++) {
            ArrayList<Integer> cluster = new ArrayList<>(nodeIdList.size());
            clusterList.add(cluster);
        }
        
        // record current k center nodes.
        int[] clusterCenterNodeId = new int[k];
        
        // temp array to save the existing center nodes.
        HashSet<Integer> hashSet = new HashSet<>();
        
        // the result k center nodes.
        int[] finalClusterCenterId = new int[k];

        // randomly choose k center nodes.
        for (int i = 0; i < k; i++) {
            int randomNodeIndex = CommonState.r.nextInt(nodeIdList.size());
            while (hashSet.contains(randomNodeIndex)) {
                randomNodeIndex = CommonState.r.nextInt(nodeIdList.size());
            }
            hashSet.add(randomNodeIndex);
            clusterCenterNodeId[i] = nodeIdList.get(randomNodeIndex);
        }
        
        boolean terminateFlag = false;
        int iteration = 0;
        while (!terminateFlag && iteration < iterations) {
            terminateFlag = true;
            
            // clear the previous result.
            for (int i = 0; i < k; i++) {
                clusterList.get(i).clear();
            }
            
            // put every node to the nearest cluster or accuracy
            double[] currentAvgAccuracies = new double[k];
            for (int i = 0; i < k; i++) {
            	currentAvgAccuracies[i] = accuracies[clusterCenterNodeId[i]];
            	clusterList.get(i).add(clusterCenterNodeId[i]);
            }
            for (int i = 0; i < nodeIdList.size(); i++) {
            	int currentNodeId = nodeIdList.get(i);
            	if (inArray(currentNodeId, clusterCenterNodeId)) {
            		continue;
            	}
            	
            	// sort the center nodes based on the distance between current node and center node.
            	int[] sortedDistance = new int[k];
            	int[] sortedIndexes = new int[k]; // the center nodes' indexes in clusterCenterNodeId
            	for (int j = 0; j < k; j++) {
            		sortedDistance[j] = minDelayMatrix[currentNodeId][clusterCenterNodeId[j]];
            		sortedIndexes[j] = j;
            	}
            	for (int j = 0; j < (k - 1); j++) {
            		int nearestClusterCenter = j;
            		int minDelay = sortedDistance[j];
            		for (int m = (j + 1); m < k; m++) {
            			if (sortedDistance[m] < minDelay) {
            				nearestClusterCenter = m;
            				minDelay = sortedDistance[m];
            			}
            		}
            		int temp = sortedDistance[nearestClusterCenter];
            		int temp2 = sortedIndexes[nearestClusterCenter];
            		sortedDistance[nearestClusterCenter] = sortedDistance[j];
            		sortedIndexes[nearestClusterCenter] = sortedIndexes[j];
            		sortedDistance[j] = temp;
            		sortedIndexes[j] = temp2;
            	}
            	
            	// find the cluster whose average accuracy is appropriate from the nearest to the furthest.
            	double accOfCurrentNode = accuracies[currentNodeId];
            	boolean added = false;
            	for (int j = 0; j < (k / checkNearestRatio); j++) {
            		int originalIndex = sortedIndexes[j];
            		double avgAcc = currentAvgAccuracies[originalIndex];
            		int currentSize = clusterList.get(originalIndex).size();
            		double newAvg = (avgAcc*currentSize + accOfCurrentNode) / (currentSize + 1);
            	    double difference = newAvg - globalAvgAcc;
            	    difference = Math.abs(difference);
            	    differences.add(difference);
            	    if (difference < delta) {
            	    	added = true;
            	    	clusterList.get(originalIndex).add(currentNodeId);
            	    	currentAvgAccuracies[originalIndex] = newAvg;
            	    	break;
            	    } else {
            	    	System.out.println();
            	    }
            	}
            	// if no cluster fits the request, add the current node to the nearest cluster.
            	if (!added) {
            		int nearestIndex = sortedIndexes[0];
            		double avgAcc = currentAvgAccuracies[nearestIndex];
            		int size = clusterList.get(nearestIndex).size();
            		double newAvg = (avgAcc*size + accOfCurrentNode) / (size + 1);
            		clusterList.get(nearestIndex).add(currentNodeId);
            		currentAvgAccuracies[nearestIndex] = newAvg;
            	}
                
            }
            
            // compute new centerNodeId, 
            // choose the node that the total delay between it
            // and the other nodes in the cluster is the smallest.
            for (int i = 0; i < k; i++) {
            	
            	int newCenterNodeId = findCenterId(clusterList.get(i), minDelayMatrix);
                
                // not end until the center ids of all clusters don't change.
                if (newCenterNodeId != clusterCenterNodeId[i]) {
                    terminateFlag = false;
                    clusterCenterNodeId[i] = newCenterNodeId;
                }
            }
            
            finalClusterCenterId = clusterCenterNodeId;
            iteration++;
        }
        
        double[] avgLosses = computeAvgLosses(clusterList);
        System.out.println(avgLosses);
        
        for (int i = 0; i < finalClusterCenterId.length; i++) {
            clusterList.get(i).add(finalClusterCenterId[i]);
        }
        return clusterList;
    }

    // kmeans
    public static ArrayList<ArrayList<Integer>>
    getGraphPartitionResult(int[][] graph, ArrayList<Integer> nodeIdList, int k) {
        int[][] minDelayMatrix = generateMinDelayMatrix(graph);
        ArrayList<ArrayList<Integer>> clusterList = new ArrayList<>(3);
        for (int i = 0; i < k; i++) {
            ArrayList<Integer> cluster = new ArrayList<>(nodeIdList.size());
            clusterList.add(cluster);
        }
        int[] clusterCenterNodeId = new int[k];
        HashSet<Integer> hashSet = new HashSet<>();
        int[] finalClusterCenterId = new int[k];

        for (int i = 0; i < k; i++) {
            int randomNodeIndex = CommonState.r.nextInt(nodeIdList.size());
            while (hashSet.contains(randomNodeIndex)) {
                randomNodeIndex = CommonState.r.nextInt(nodeIdList.size());
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
            for (int i = 0; i < nodeIdList.size(); i++) { // put every node to the nearest cluster
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
            for (int i = 0; i < k; i++) {
                int minTotalDelay = Integer.MAX_VALUE;
                int newCenterNodeId = clusterCenterNodeId[i];
                for (int j = 0; j < clusterList.get(i).size(); j++) { // choose the node that the total delay between it and the other nodes in the cluster is the smallest
                    int totalDelay = 0;
                    for (Integer nodeId : clusterList.get(i)) {
                        if (nodeId.equals(clusterList.get(i).get(j))) {
                            continue;
                        }
                        totalDelay += minDelayMatrix[nodeId][clusterList.get(i).get(j)];
                    }
                    if (totalDelay < minTotalDelay) {
                        minTotalDelay = totalDelay;
                        newCenterNodeId = clusterList.get(i).get(j);
                    }
                }
                if (newCenterNodeId != clusterCenterNodeId[i]) { // not end until the center ids of all clusters don't change
                    terminateFlag = false;
                    clusterCenterNodeId[i] = newCenterNodeId;
                }
                finalClusterCenterId = clusterCenterNodeId;
            }
        }
        
        double[] avgLosses = computeAvgLosses(clusterList);
        System.out.println(avgLosses);
        
        for (int i = 0; i < finalClusterCenterId.length; i++) {
            // System.out.println(finalClusterCenterId[i]);
            clusterList.get(i).add(finalClusterCenterId[i]);
            // System.out.println(finalClusterList.get(i));
        }
        return clusterList;
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
            int[] clusterCenterNodeId = new int[k];
            HashSet<Integer> hashSet = new HashSet<>();
            for (int i = 0; i < k; i++) {
                int randomNodeIndex = CommonState.r.nextInt(nodeIdList.size());
                while (hashSet.contains(randomNodeIndex)) {
                    randomNodeIndex = CommonState.r.nextInt(nodeIdList.size());
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
