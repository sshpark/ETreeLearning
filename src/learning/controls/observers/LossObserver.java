package learning.controls.observers;

import learning.interfaces.LearningProtocol;
import learning.interfaces.Model;
import learning.interfaces.ProbabilityModel;
import learning.main.Main;
import learning.models.LogisticRegression;
import learning.utils.SparseVector;
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
    private long cycle = 0;

    private double crossEntropyLoss(double y, double[] y_pred) {
        // clipping
        double eps = 1e-8;
        int label = (int)y;
        y_pred[label] = Math.max(y_pred[label], eps);
        y_pred[label] = Math.min(y_pred[label], 1.0-eps);
        return -Math.log(y_pred[label]);
    }

    @Override
    public boolean execute() {
        updateGraph();
        double errs = 0.0;
        double losses = 0.0;
        for (int i = 0; i < Network.size(); i++) {
            Protocol p = ((Node) g.getNode(i)).getProtocol(pid);
            double temp_errs = 0.0;
            double temp_losses = 0.0;
            if (p instanceof LearningProtocol) {
                Model model = ((LearningProtocol) p).getWorkerModel();
                for (int testIdx = 0; eval != null && testIdx < eval.size(); testIdx++) {
                    SparseVector testInstance = eval.getInstance(testIdx);
                    double y = eval.getLabel(testIdx);
                    double pred = model.predict(testInstance);
                    temp_errs += (y == pred) ? 0.0 : 1.0;

                    double[] y_pred = ((ProbabilityModel) model).distributionForInstance(testInstance);
                    temp_losses += crossEntropyLoss(y, y_pred);
                }
            }
            errs += temp_errs/eval.size();
            losses += temp_losses/eval.size();
        }
        errs /= Network.size();
        losses /= Network.size();
        cycle++;
        // TODO: Change it when you use it in the future
        Main.addLoss(CommonState.getTime(), losses, 1.0-errs);
        System.err.println("Time: "+ CommonState.getTime() + ", loss: " + losses + ", Accuracy: " + (1.0-errs));
        return false;
    }
}
