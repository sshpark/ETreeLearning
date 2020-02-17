package learning.models;

import learning.interfaces.Model;
import learning.interfaces.ModelHolder;
import peersim.config.Configuration;
import learning.interfaces.Mergeable;
import learning.utils.SparseVector;

import java.util.Random;

public class MergeableLogisticRegression extends LogisticRegression implements Mergeable<MergeableLogisticRegression>{
    private static final long serialVersionUID = -4465428750554412761L;

    protected static final String PAR_LAMBDA = "MergeableLogisticRegression.lambda";

    public MergeableLogisticRegression(){
        super();
    }

    /**
     * Returns a new mergeable logistic regression object that initializes its variable with
     * the deep copy of the specified parameters using the super constructor.
     * @param w hyperplane
     * @param age model age
     * @param lambda learning parameter
     */
    protected MergeableLogisticRegression(SparseVector w, double age, double lambda, int numberOfClasses, double bias){
        super(w, age, lambda, numberOfClasses, bias);
    }

    public Object clone(){
        return new MergeableLogisticRegression(w, age, lambda, numberOfClasses, bias);
    }

    public void init(String prefix) {
        super.init(prefix);
        lambda = Configuration.getDouble(prefix + "." + PAR_LAMBDA, 0.01);
    }

    @Override
    public MergeableLogisticRegression merge(final MergeableLogisticRegression model) {
        SparseVector mergedw = new SparseVector(w);
        double age = Math.max(this.age, model.age);
        double bias = (this.bias + model.bias) / 2.0;
        mergedw.mul(0.5);
        mergedw.add(model.w, 0.5);

        return new MergeableLogisticRegression(mergedw, age, lambda, numberOfClasses, bias);
    }

    /**
     * this.w + model.w * alpha
     * this.bias + model.bias * alpha
     * why? Because (this) is started from (w = 0, bias = 0)
     * @param model
     * @param alpha
     * @return
     */
    public MergeableLogisticRegression merge(final MergeableLogisticRegression model, double alpha) {
        SparseVector mergedw = new SparseVector(w);
        double age = Math.max(this.age, model.age);
        double bias = this.bias + model.bias * alpha;
        mergedw.add(model.w, alpha);

        return new MergeableLogisticRegression(mergedw, age, lambda, numberOfClasses, bias);
    }


    /**
     * Compress weight
     * @param compress
     */
    public MergeableLogisticRegression compressSubsampling(int compress) {
        final int n = w.size();
        final int comprss_num = (int)(n*(compress/100.0));
        double[] compress_weight = new double[n];
        for (int i = 0; i < n; i++) compress_weight[i] = 0.0;
        int[] indexes = randomArray(0, n-1, comprss_num);

        for (int i = 0; i < comprss_num; i++)
            compress_weight[indexes[i]] = w.get(indexes[i]);

        w = new SparseVector(compress_weight);

        return new MergeableLogisticRegression(w, age, lambda, numberOfClasses, bias);
    }

    /**
     * Aggregate models
     * @param models receive models
     * @return
     */
    public MergeableLogisticRegression aggregateDefault(ModelHolder models) {
        double agg_age = 0.0;

        // aggregate age
        for (int i = 0; i < models.size(); i++)
            agg_age = Math.max(agg_age, ((MergeableLogisticRegression)models.getModel(i)).age);

        SparseVector temp = new SparseVector();

        for (int i = 0; i < models.size(); i++) {
            MergeableLogisticRegression model = (MergeableLogisticRegression) models.getModel(i);
            temp = temp.add(model.w);
        }
        temp = temp.mul(1.0 / models.size());

        return new MergeableLogisticRegression(temp, agg_age, lambda, numberOfClasses, bias);
}


    /**
     * Aggregate models
     * @param models receive models
     * @return
     */
    public MergeableLogisticRegression aggregateSubsampledImproved(ModelHolder models) {
        final int n = w.size();
        double[] agg_w = new double[n];
        double agg_age = 0.0;

        // aggregate age
        for (int i = 0; i < models.size(); i++)
            agg_age = Math.max(agg_age, ((MergeableLogisticRegression)models.getModel(i)).age);

        // aggregate weight
        for (int i = 0; i < n; i++) {
            int cnt = 0;
            double sum = 0.0;

            for (int j = 0; j < models.size(); j++) {
                MergeableLogisticRegression model = (MergeableLogisticRegression) models.getModel(j);
                if (model.w.get(i) != 0.0) {
                    sum += model.w.get(i);
                    cnt++;
                }
            }
            if (cnt != 0)
                agg_w[i] = sum/cnt;
        }
        return new MergeableLogisticRegression(new SparseVector(agg_w), agg_age, lambda, numberOfClasses, bias);
    }

    /**
     * Returns N unduplicated numbers in a randomly specified range
     * @param max max value in specified range
     * @param min min value in specified range
     * @param n Random number
     * @return int[] Result set of random number
     */
    private int[] randomArray(int min,int max,int n) {
        int len = max-min+1;

        if(max < min || n > len){
            return null;
        }

        int[] source = new int[len];
        for (int i = min; i < min+len; i++){
            source[i-min] = i;
        }

        int[] result = new int[n];
        Random rd = new Random();
        int index = 0;
        for (int i = 0; i < result.length; i++) {
            index = rd.nextInt(len-i);
            result[i] = source[index];

            int temp = source[index];
            source[index] = source[len-1-i];
            source[len-1-i] = temp;
        }
        return result;
    }
}
