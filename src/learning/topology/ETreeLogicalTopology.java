package learning.topology;

import learning.controls.initializers.StartETreeMessageInitializer;
import learning.node.ETreeNode;
import learning.protocols.ETreeLearningProtocol;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.dynamics.WireGraph;
import peersim.graph.Graph;

import java.util.ArrayList;

/**
 * @author sshpark
 * @date 17/2/2020
 */
public class ETreeLogicalTopology extends WireGraph {
    private final String topoFilePath;

    private final static String PAR_LAYERS = "layers";
    private final int layers;

    private final static String PAR_GROUPS = "groups";
    private final int[] groups;

    private static final String PAR_RECVPERCENT = "recvPercent";
    private final double recvPercent;


    /**@hidden */
    private int[][] graph;


    /**
     * Standard constructor that reads the configuration parameters.
     * Invoked by the simulation engine.
     * @param prefix the configuration prefix for this class
     */
    public ETreeLogicalTopology(String prefix) {
        super(prefix);
        topoFilePath = Configuration.getString("TOPO_FILEPATH");
        layers = Configuration.getInt(prefix + "." + PAR_LAYERS);
        recvPercent = Configuration.getDouble(prefix + "." + PAR_RECVPERCENT);
        // init groups
        String[] temp_groups = Configuration.getString(prefix + "." + PAR_GROUPS).split(",");
        groups = new int[layers];
        for (int i = 0; i < layers; i++)
            groups[i] = Integer.parseInt(temp_groups[i]);

        // gets physical network topology
        graph = TopoUtil.getGraph(Network.size(), topoFilePath);
    }

    //--------------------------------------------------------------------------
    //Methods
    //--------------------------------------------------------------------------
    @Override
    public void wire(Graph g) {
        // node size
        final int n = Network.size();
        
        int clusteringMode = Configuration.getInt("clustering.mode", 0);
        
        // if using the clustering algorithm based on accuracies, do this step.
//        if (clusteringMode == 1) {
        	TopoUtil.pretrainForClustering();
//        }

        ArrayList<Integer> lastNodeIndexes = new ArrayList<Integer>() {{
                for (int i = 0; i < n; i++) add(i);
            }};

        // This variable is useless, just for debugging
        // It's useful now, 2020/02/24
        ArrayList<ArrayList<Integer>> layersNodeID = new ArrayList<>();

        // gets the grouping result int the 0st layer
        ArrayList<ArrayList<Integer>> res = new ArrayList<ArrayList<Integer>>();
        for (int layer = 0; layer < layers; layer++) {
            layersNodeID.add(new ArrayList<>(lastNodeIndexes));
            
            /*
             *  gets the grouping result for the current layer
             */
            // if it's not the first layer, use kmeans
            if (layer > 0) {
            	res = TopoUtil.getGraphPartitionResult(graph, lastNodeIndexes, groups[layer]);
            } else {
            	switch (clusteringMode) {
				case 0:
					// kmeans
		            res = TopoUtil.getGraphPartitionResult(graph, lastNodeIndexes, groups[layer]);
					break;
				case 1:
					// kmeans based on average accuracy
		            if (groups[layer] > 1) {
		            	res = TopoUtil.getGraphPartitionByDistanceAndDataDistribution(graph, lastNodeIndexes, groups[layer]);
		            } else {
		            	res = new ArrayList<ArrayList<Integer>>();
		            	int[][] minDelayMatrix = TopoUtil.generateMinDelayMatrix(graph);
		            	int centerId = TopoUtil.findCenterId(lastNodeIndexes, minDelayMatrix);
		            	lastNodeIndexes.add(centerId);
		            	res.add((ArrayList<Integer>) lastNodeIndexes.clone());
		            }
					break;
				case 2:
					// kmeans based on ununiform data distribution
		            if (groups[layer] > 1) {
		            	res = TopoUtil.getUnuniformGraphPartition(graph, lastNodeIndexes, groups[layer]);
		            } else {
		            	res = new ArrayList<ArrayList<Integer>>();
		            	int[][] minDelayMatrix = TopoUtil.generateMinDelayMatrix(graph);
		            	int centerId = TopoUtil.findCenterId(lastNodeIndexes, minDelayMatrix);
		            	lastNodeIndexes.add(centerId);
		            	res.add((ArrayList<Integer>) lastNodeIndexes.clone());
		            }
					break;
				case 3:
					// only consider uniform kma
		            if (groups[layer] > 1) {
		            	res = TopoUtil.getUniformGraphPartition(graph, lastNodeIndexes, groups[layer]);
		            } else {
		            	res = new ArrayList<ArrayList<Integer>>();
		            	int[][] minDelayMatrix = TopoUtil.generateMinDelayMatrix(graph);
		            	int centerId = TopoUtil.findCenterId(lastNodeIndexes, minDelayMatrix);
		            	lastNodeIndexes.add(centerId);
		            	res.add((ArrayList<Integer>) lastNodeIndexes.clone());
		            }
					break;
				case 4:
					// only consider uniform class distribution
		            if (groups[layer] > 1) {
		            	try {
							res = TopoUtil.getUniformDistributionGraphPartition(graph, lastNodeIndexes, groups[layer]);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
		            } else {
		            	res = new ArrayList<ArrayList<Integer>>();
		            	int[][] minDelayMatrix = TopoUtil.generateMinDelayMatrix(graph);
		            	int centerId = TopoUtil.findCenterId(lastNodeIndexes, minDelayMatrix);
		            	lastNodeIndexes.add(centerId);
		            	res.add((ArrayList<Integer>) lastNodeIndexes.clone());
		            }
					break;
				default:
					// kmeans
		            res = TopoUtil.getGraphPartitionResult(graph, lastNodeIndexes, groups[layer]);
					break;
				}
            }
            

            lastNodeIndexes.clear();
            for (ArrayList<Integer> group : res) {
                // gets aggregate node
                ETreeNode agg_node = (ETreeNode) Network.get(group.get( group.size()-1 ));
                // update lastNodeIndexes
                lastNodeIndexes.add((int)agg_node.getID());

                for (int i = 0; i < group.size()-1; i++) {
                    ETreeNode node = (ETreeNode) Network.get(group.get(i));
                    node.addParentNode((int) agg_node.getID());
                    agg_node.addChildNode(layer+1, (int) node.getID());
                }
            }
        }

        ETreeLearningProtocol.setLayersNodeID(layersNodeID);
        StartETreeMessageInitializer.setLayersNodeID(layersNodeID);

        /* ---------- debug output -------------
        for (ArrayList<Integer> temp : layersNodeID) {
            System.out.print("size " + (temp.size()) + ": ");
            for (Integer id : temp) {
                System.out.print(id + " ");
            }
            System.out.println();
        }

        // output node
        for (int layer = 0; layer < layers; layer++) {
            System.out.println("Layer: " + layer);
            for (Integer id : layersNodeID.get(layer)) {
                System.out.println("Node " + id +": ");
                ETreeNode node = (ETreeNode) Network.get(id);
                System.out.println("Parent node: " + node.getParentNode(layer));
                System.out.print("Child Node: ");
                for (Integer nodeid : node.getChildNodeList(layer)) {
                    System.out.print(nodeid + " ");
                }
                System.out.println();
            }
            System.out.println();
        }
        /*---------- debug finished --------- */
    }
}
