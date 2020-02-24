package learning.models;

import learning.interfaces.ModelHolder;
import learning.utils.Utils;
import peersim.config.Configuration;
import learning.interfaces.Mergeable;
import learning.utils.SparseVector;

import java.util.ArrayList;


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

    public MergeableLogisticRegression clone(){
        return new MergeableLogisticRegression(w, age, lambda, numberOfClasses, bias);
    }

    public void init(String prefix) {
        super.init(prefix);
        lambda = Configuration.getDouble(prefix + "." + PAR_LAMBDA, 0.05);
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
     * Compress weight
     * @param compress
     */
    public MergeableLogisticRegression compressSubsampling(int compress) {
        final int n = w.size();
        final int comprss_num = (int)(n*(compress/100.0));
        double[] compress_weight = new double[n];
        for (int i = 0; i < n; i++) compress_weight[i] = 0.0;
        ArrayList<Integer> indexes = Utils.randomArray(0, n-1, comprss_num);

        for (int i = 0; i < comprss_num; i++)
            compress_weight[indexes.get(i)] = w.get(indexes.get(i));

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
        double agg_lambda = Double.MAX_VALUE;
        // aggregate age
        for (int i = 0; i < models.size(); i++) {
            agg_age = Math.max(agg_age, ((MergeableLogisticRegression) models.getModel(i)).age);
            agg_lambda = Math.min(agg_lambda, ((MergeableLogisticRegression) models.getModel(i)).lambda);
        }

        SparseVector temp_w = new SparseVector();
        double temp_b = 0.0;

        for (int i = 0; i < models.size(); i++) {
            MergeableLogisticRegression model = (MergeableLogisticRegression) models.getModel(i);
            temp_w.add(model.w);
            temp_b += model.bias;
        }
        temp_w.mul(1.0 / models.size());
        temp_b /= models.size();

        return new MergeableLogisticRegression(temp_w, agg_age, agg_lambda, numberOfClasses, temp_b);
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
}
