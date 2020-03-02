package learning.controls.initializers;

import learning.interfaces.AbstractProtocol;
import learning.node.ETreeNode;
import learning.protocols.FederatedLearningProtocol;
import learning.topology.TopoUtil;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Protocol;

import java.io.FileWriter;
import java.io.IOException;

/**
 * The minDelayMatrix is set up for the protocol of each node
 * because of the high cost of generating the mindelay matrix.
 * @author sshpark
 * @date 20/2/2020
 */
public class SetMinDelayMatrixForProtocol implements Control {
    private static final String PAR_PROT = "protocol";
    private final int pid;

    private final String topoFilePath;
    private int[][] graph;
    private int[][] minDelayMatrix;


    public SetMinDelayMatrixForProtocol(String prefix) {
        pid = Configuration.getPid(prefix + "." + PAR_PROT);
        topoFilePath = Configuration.getString("TOPO_FILEPATH");
        graph = TopoUtil.getGraph(Network.size(), topoFilePath);
        minDelayMatrix = TopoUtil.generateMinDelayMatrix(graph);
    }

    @Override
    public boolean execute() {
        outputDelay();
        for (int i = 0; i < Network.size(); i++) {
            Protocol abstractProtocolP = Network.get(i).getProtocol(pid);
            if (abstractProtocolP instanceof AbstractProtocol) {
                // get the learning protocol
                AbstractProtocol abstractProtocol = (AbstractProtocol) abstractProtocolP;
                abstractProtocol.setMinDelayMatrix(minDelayMatrix);
            } else {
                throw new RuntimeException("The given protocol in initializer setMinDelayMatrix.protocol is not a Abstract protocol!");
            }
        }

        return false;
    }

    private void outputDelay() {
        try {
            String filepath = Configuration.getString("DELAY_OUTPUT_FILEPATH");
            FileWriter fileWriter = new FileWriter(filepath);


            if (filepath.contains("delay_etree")) {
                for (int id = 0; id < Network.size(); id++) {
                    ETreeNode node = (ETreeNode) Network.get(id);
                    ETreeNode par_node = (ETreeNode) Network.get(node.getParentNode(0));
                    fileWriter.write(minDelayMatrix[id][node.getParentNode(0)] + " ");
                    fileWriter.write(minDelayMatrix[node.getParentNode(0)][par_node.getParentNode(1)] + " ");
                }
            } else {
                int masterId = FederatedLearningProtocol.getMasterID();
                for (int id = 0; id < Network.size(); id++) {
                    if (id != masterId)
                        fileWriter.write(minDelayMatrix[id][masterId] + " ");
                }
            }
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
