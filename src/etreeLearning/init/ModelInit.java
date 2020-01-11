package etreeLearning.init;

import etreeLearning.protocol.ETreeLearningProtocol;
import gossipLearning.DataBaseReader;
import gossipLearning.InstanceHolder;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;

import java.io.File;
import java.io.FileNotFoundException;

public class ModelInit implements Control {
    private static final String PAR_TRAINING = "trainingFile";
    private static final String PAR_EVAL = "evaluationFile";
    private static final String PAR_PROT = "protocol";


    private final int pid;
    private final File tFile;
    private final File eFile;

    public ModelInit(String prefix) throws Exception{
        pid = Configuration.getPid(prefix + "." + PAR_PROT);
        tFile = new File(Configuration.getString(prefix + "." + PAR_TRAINING));
        eFile = new File(Configuration.getString(prefix + "." + PAR_EVAL));
    }

    @Override
    public boolean execute() {
        final int n = Network.size();

        try {
            DataBaseReader reader = DataBaseReader.createDataBaseReader("gossipLearning.DataBaseReader", tFile, eFile);
            InstanceHolder instances = new InstanceHolder(reader.getTrainingSet().getNumberOfClasses(), reader.getTrainingSet().getNumberOfFeatures());
            System.err.println(reader.getTrainingSet().getNumberOfFeatures());
            for (int i = 0; i < n; i++) {
                ETreeLearningProtocol et = (ETreeLearningProtocol) Network.get(i).getProtocol(pid);
                et.setInstanceHolder(instances);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}
