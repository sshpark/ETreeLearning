package learning.models;

import learning.interfaces.Mergeable;
import learning.interfaces.ModelHolder;
import learning.interfaces.ProbabilityModel;
import learning.utils.Matrix;
import learning.utils.SparseVector;
import peersim.config.Configuration;
import peersim.core.CommonState;

/**
 * It is a single layer neural network.
 *
 * @author sshpark
 * @date 28/2/2020
 */
public class SoftmaxRegression extends ProbabilityModel implements Mergeable<SoftmaxRegression> {
    private static final long serialVersionUID = 911792657475763732L;

    /**
     * The learning parameter is 0.01 by default.
     */
    protected static final String PAR_LAMBDA = "SoftmaxRegression.lambda";
    protected double lambda = 0.01;

    /**
     * @hidden
     */
    protected Matrix w;
    protected Matrix bias;
    protected int numberOfFeatures = 561;
    protected int numberOfClasses = 6;

    /**
     * Initializes the hyperplane as 0 vector.
     */
    public SoftmaxRegression() {
        double[][] matrix_w = new double[numberOfFeatures][numberOfClasses];
        double[] matrix_b = new double[numberOfClasses];
        for (int i = 0; i < numberOfFeatures; i++) {
            for (int j = 0; j < numberOfClasses; j++) {
                matrix_w[i][j] = CommonState.r.nextGaussian();
            }
        }
        for (int i = 0; i < numberOfClasses; i++)
            matrix_b[i] = CommonState.r.nextGaussian();

        this.w = new Matrix(matrix_w);
        this.bias = new Matrix(matrix_b, false);
    }

    /**
     * Returns a new Softmax Regression object that initializes its variable with
     * the deep copy of the specified parameters.
     *
     * @param w                hyperplane
     * @param lambda           learning parameter
     * @param numberOfFeatures features
     * @param numberOfClasses  classes
     * @param bias             bias
     */
    protected SoftmaxRegression(Matrix w, double lambda, int numberOfFeatures, int numberOfClasses, Matrix bias) {
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
        Matrix output = ins.mul(w).transpose().add(bias);
        double[] res = new double[numberOfClasses];
        for (int i = 0; i < numberOfClasses; i++) res[i] = output.getValue(i, 0);
        return res;
    }

    /**
     * Computes the probability that the specified instance belongs to the positive class i.e.
     *
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
        Matrix ins = new Matrix(instance.toDoubleArray(), true);
//        for (int i = 0; i < numberOfClasses; i++) System.out.print(i + " " + prop[i] + " ");
//        System.out.println();
        prop[(int) label] -= 1;
        Matrix err = new Matrix(prop, false);
        Matrix dw = err.mul(ins).transpose();
        w = w.add(dw.mul(-lambda));
        bias = bias.add(err.mul(-lambda));
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
            agg_lambda = Math.min(agg_lambda, ((SoftmaxRegression) models.getModel(i)).lambda);
        }

        Matrix temp_w = new Matrix(numberOfFeatures, numberOfClasses);
        Matrix temp_b = new Matrix(new double[numberOfClasses], false);

        for (int i = 0; i < models.size(); i++) {
            SoftmaxRegression model = (SoftmaxRegression) models.getModel(i);
            temp_w = temp_w.add(model.w);
            temp_b = temp_b.add(model.bias);
        }
        temp_w = temp_w.mul(1.0 / models.size());
        temp_b = temp_b.mul(1.0 / models.size());

        return new SoftmaxRegression(temp_w, lambda, numberOfFeatures, numberOfClasses, temp_b);
    }

    @Override
    public SoftmaxRegression merge(SoftmaxRegression model) {
        return null;
    }
}
