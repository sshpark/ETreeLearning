package learning.controls.observers;

import learning.interfaces.LearningProtocol;
import learning.main.Main;
import learning.models.LogisticRegression;
import learning.utils.SparseVector;
import peersim.core.CommonState;
import peersim.core.Node;
import peersim.core.Protocol;

/**
 * @author sshpark
 * @date 3/2/2020
 */
public class FedLossObserver extends PredictionObserver {
    public FedLossObserver(String prefix) throws Exception {
        super(prefix);
    }
    private long cycle = 0;

    @Override
    public boolean execute() {
        updateGraph();

        double errs = 0.0;

        Protocol p = ((Node) g.getNode(0)).getProtocol(pid);
        if (p instanceof LearningProtocol) {
            LogisticRegression model = (LogisticRegression) ((LearningProtocol) p).getWorkerModel();
            for (int testIdx = 0; eval != null && testIdx < eval.size(); testIdx++) {
                SparseVector testInstance = eval.getInstance(testIdx);
                double y = eval.getLabel(testIdx);
                double pred = model.predict(testInstance);
                errs += (y == pred) ? 0.0 : 1.0;
            }
        }

        errs = errs / eval.size();
        cycle++;
        // TODO: Change it when you use it in the future
        //Main.addLoss(CommonState.getTime(), errs);
        System.err.println("Cycle: "+ cycle + " Fed 0-1 error: " + errs);
        return false;
    }
}
