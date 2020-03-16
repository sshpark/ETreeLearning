package learning.controls.initializers;

import learning.DataBaseReader;
import learning.InstanceHolder;
import learning.controls.observers.PredictionObserver;
import learning.interfaces.LearningProtocol;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.core.Protocol;

import java.io.*;
import java.util.Vector;

/**
 * @author sshpark
 * @date 13/3/2020
 */
public class InstanceLoadFromSynthetic implements Control {
    private static final String PAR_PROT = "protocol";
    private static final String PAR_TFILE = "trainingFile";
    private static final String PAR_EFILE = "evaluationFile";
    private static final String PAR_SFILE = "numSamplesFile";
    private static final String PAR_READERCLASS = "readerClass";

    protected final int pid;
    /** @hidden */
    protected final File tFile;
    /** @hidden */
    protected Vector<PredictionObserver> observers;
    /** @hidden */
    protected String readerClassName;
    protected DataBaseReader reader;
    /** @hidden */
    protected final File eFile;
    /** @hidden */
    protected final File sFile;

    public InstanceLoadFromSynthetic(String prefix) {
        pid = Configuration.getPid(prefix + "." + PAR_PROT);
        tFile = new File(Configuration.getString(prefix + "." + PAR_TFILE));
        eFile = new File(Configuration.getString(prefix + "." + PAR_EFILE));
        sFile = new File(Configuration.getString(prefix + "." + PAR_SFILE));
        readerClassName = Configuration.getString(prefix + "." + PAR_READERCLASS, "learning.DataBaseReader");
        observers = new Vector<PredictionObserver>();
    }

    public boolean execute(){
        try {
            // read instances
            reader = DataBaseReader.createDataBaseReader(readerClassName, tFile, eFile);

            // InstanceLoader initializes the evaluation set of prediction observer
            for (PredictionObserver observer : observers) {
                observer.setEvalSet(reader.getEvalSet());
            }

            BufferedReader br = new BufferedReader(new FileReader(sFile));
            String[] line = br.readLine().split(" ");
            int[] samplesOfNode = new int[line.length];
            int xx = 0;
            for (int i = 0; i < line.length; i++) {
                samplesOfNode[i] = Integer.parseInt(line[i]);
                xx += samplesOfNode[i];
            }

            int hasReadSamples = 0;

            for (int i = 0; i < Network.size(); i++) {
                Node node = Network.get(i);
                Protocol protocol = node.getProtocol(pid);
                if (protocol instanceof LearningProtocol) {
                    LearningProtocol learningProtocol = (LearningProtocol) protocol;
                    InstanceHolder instances = new InstanceHolder(reader.getTrainingSet().getNumberOfClasses(), reader.getTrainingSet().getNumberOfFeatures());
                    for (int j = 0; j < samplesOfNode[i]; j++){
                        instances.add(reader.getTrainingSet().getInstance(hasReadSamples + j), reader.getTrainingSet().getLabel(hasReadSamples + j));
                    }
                    hasReadSamples += samplesOfNode[i];
                    // set the instances for current node
                    learningProtocol.setInstenceHolder(instances);
                } else {
                    throw new RuntimeException("The protocol " + pid + " have to implement LearningProtocol interface!");
                }

            }
        } catch (Exception ex) {
            throw new RuntimeException("Exception has occurred in InstanceLoader!", ex);
        }

        return false;
    }

    /**
     * Sets the specified prediction observer.
     * @param observer prediction observer
     */
    public void setPredictionObserver(PredictionObserver observer) {
        observers.add(observer);
    }
}
