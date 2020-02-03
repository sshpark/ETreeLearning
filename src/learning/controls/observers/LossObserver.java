package learning.controls.observers;

import learning.interfaces.LearningProtocol;
import learning.main.Main;
import learning.models.LogisticRegression;
import learning.utils.SparseVector;
import peersim.Simulator;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.core.Protocol;

import java.util.Set;

/**
 * @author sshpark
 * @date 28/1/2020
 */
public class LossObserver extends PredictionObserver {
    public LossObserver(String prefix) throws Exception {
        super(prefix);
    }

    @Override
    public boolean execute() {
        updateGraph();

        Set<Integer> idxSet = generateIndices();
        double errs = 0.0;
        for (int i : idxSet) {
            Protocol p = ((Node) g.getNode(i)).getProtocol(pid);
            if (p instanceof LearningProtocol) {
                LogisticRegression model = (LogisticRegression) ((LearningProtocol) p).getWorkerModel();
                for (int testIdx = 0; eval != null && testIdx < eval.size(); testIdx++) {
                    SparseVector testInstance = eval.getInstance(testIdx);
                    double y = eval.getLabel(testIdx);
                    double pred = model.predict(testInstance);
                    errs += (y == pred) ? 0.0 : 1.0;
                }
            }
        }
        errs = errs / (eval.size() * Network.size());
        Main.addLoss(CommonState.getTime(), errs);
        System.err.println("0-1 error: " + errs);
        return false;
    }
}
