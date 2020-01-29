package learning.controls.observers;

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

        return false;
    }
}
