package learning.models;

import learning.interfaces.Mergeable;
import learning.interfaces.ModelHolder;
import learning.interfaces.ProbabilityModel;
import learning.utils.Matrix;
import learning.utils.SparseVector;
import peersim.config.Configuration;

/**
 * It is a single layer neural network.
 * @author sshpark
 * @date 28/2/2020
 */
public class SoftmaxRegression extends ProbabilityModel implements Mergeable<SoftmaxRegression> {
    private static final long serialVersionUID = 911792657475763732L;

    /**
     * The learning parameter is 0.01 by default.
     */
    protected static final String PAR_LAMBDA = "LogisticRegression.lambda";
    protected double lambda = 0.01;

    /**@hidden */
    protected Matrix w;
    protected double bias;
    protected int numberOfFeatures;
    protected int numberOfClasses;

    /**
     * Initializes the hyperplane as 0 vector.
     */
    public SoftmaxRegression(String prefix) {
        this.w = new Matrix(numberOfFeatures, numberOfClasses);
        this.bias = 0;
    }

    /**
     * Returns a new Softmax Regression object that initializes its variable with
     * the deep copy of the specified parameters.
     * @param w hyperplane
     * @param lambda learning parameter
     * @param numberOfFeatures features
     * @param numberOfClasses classes
     * @param bias bias
     */
    protected SoftmaxRegression(Matrix w, double lambda, int numberOfFeatures, int numberOfClasses, double bias){
        this.w = (Matrix) w.clone();
        this.lambda = lambda;
        this.numberOfFeatures = numberOfFeatures;
        this.numberOfClasses = numberOfClasses;
        this.bias = bias;
    }

    @Override
    public Object clone() {
        return new SoftmaxRegression(w, lambda, numberOfFeatures, numberOfClasses, bias);
    }

    private double[] getNetOutput(Matrix instance) {
        Matrix ins = new Matrix(instance);
        Matrix output = ins.mul(w).add(new Matrix(1, numberOfClasses, bias));
        double[] res = new double[numberOfClasses];
        for (int i = 0; i < numberOfClasses; i++) res[i] = output.getValue(0, i);
        return res;
    }

    /**
     * Computes the probability that the specified instance belongs to the positive class i.e.
     * @param instance instance for computing distribution
     * @return
     */
    @Override
    public double[] distributionForInstance(SparseVector instance) {
        Matrix sample = new Matrix(instance.toDoubleArray(), true);
        double[] distribution = new double[numberOfClasses];
        double[] outputs = getNetOutput(sample);
        double sum_exp = 0.0;
        for (int i = 0; i < numberOfClasses; i++) {
            distribution[i] = Math.exp(outputs[i]);
            sum_exp += distribution[i];
        }
        for (int i = 0; i < numberOfClasses; i++)
            distribution[i] /= sum_exp;
        return distribution;
    }

    @Override
    public void init(String prefix) {
        w = new Matrix(numberOfFeatures, numberOfClasses);
        lambda = Configuration.getDouble(prefix + "." + PAR_LAMBDA, 0.01);
    }

    @Override
    public void update(SparseVector instance, double label) {
        double[] prop = distributionForInstance(instance);
        double err = prop[(int)label] - 1.0;
        Matrix dw = new Matrix(instance.mul(err).toDoubleArray(), true);
        w = w.add(dw.mul(-lambda));
        bias -= err * lambda;
    }

    @Override
    public int getNumberOfClasses() {
        return numberOfClasses;
    }

    public void setNumberOfFeatures(int numberOfFeatures) {
        this.numberOfFeatures = numberOfFeatures;
    }
    @Override
    public void setNumberOfClasses(int numberOfClasses) {
        this.numberOfClasses = numberOfClasses;
    }

    @Override
    public SoftmaxRegression aggregateDefault(ModelHolder models) {
        double agg_lambda = Double.MAX_VALUE;

        // aggregate age
        for (int i = 0; i < models.size(); i++) {
            agg_lambda = Math.min(agg_lambda, ((MergeableLogisticRegression) models.getModel(i)).lambda);
        }

        Matrix temp_w = new Matrix(numberOfFeatures, numberOfClasses);
        double temp_b = 0.0;

        for (int i = 0; i < models.size(); i++) {
            MergeableLogisticRegression model = (MergeableLogisticRegression) models.getModel(i);
            temp_w.add(model.w);
            temp_b += model.bias;
        }
        temp_w.mul(1.0 / models.size());
        temp_b /= models.size();

        return null;
    }

    @Override
    public SoftmaxRegression merge(SoftmaxRegression model) {
        return null;
    }
}
