package learning.utils;

import java.util.*;

public class Utils {
  
  /**
   * Computes the liner regression line for the values of the specified double array.</br>
   * a * x + b
   * @param array array of values to be approximated
   * @return double[]{a,b}
   */
  public static double[] regression(double[] array) {
    double a = 0.0;
    double b = 0.0;
    double cov = 0.0;
    double sumx = 0.0;
    double sumy = 0.0;
    double sum2x = 0.0;
    for (int i = 0; i < array.length; i++) {
      cov += (i+1)*array[i];
      sumx += (i+1);
      sumy += array[i];
      sum2x += (i+1)*(i+1);
    }
    a = (array.length * cov - (sumx * sumy)) / (array.length * sum2x - (sumx * sumx));
    b = sumy / array.length - a * sumx / array.length;
    return new double[]{a*array.length, b};
  }
  
  private static void polyGen(int d, int n, Stack<Integer> s, Vector<Vector<Integer>> result, boolean generateAll) {
    if ((generateAll || n == 0) && s.size() > 0) {
      Stack<Integer> retS = new Stack<Integer>();
      retS.addAll(s);
      result.add(retS);
    }
    if (n <= 0) {
      return;
    }
    for (int i = (s.size() > 0) ? s.peek() : 0; i < d; i ++) {
      s.push(i);
      polyGen(d, n-1, s, result, generateAll);
      s.pop();
    }
  }
  
  public static Vector<Vector<Integer>> polyGen(int d, int n, boolean generateAll) {
    Vector<Vector<Integer>> result = new Vector<Vector<Integer>>();
    Stack<Integer> stack = new Stack<Integer>();
    polyGen(d, n, stack, result, generateAll);
    return result;
  }
  
  /**
   * Returns true if the specified number is the power of the 2.
   * @param t to be checked
   * @return is power of 2
   */
  public static boolean isPower2(double t) {
    final long tl = (long) t;
    return (tl & (tl - 1)) == 0;
  }
  
  /**
   * It computes the inner product between the two input vectors.
   *
   * @param x first vector
   * @param y second vector
   * @return innerProduct
   */
  public static double innerProduct(final Map<Integer, Double> x, final Map<Integer, Double> y) {
    /*if (x == null || y == null || x.size() == 0 || y.size() == 0) {
      return 0.0;
    }*/
    Map<Integer, Double> x2, y2;
    if (y.size() < x.size()) {
      x2 = y;
      y2 = x;
    } else {
      x2 = x;
      y2 = y;
    }
    double ret = 0.0;
    Double yval;
    for (Map.Entry<Integer, Double> e : x2.entrySet()) {
      yval = y2.get(e.getKey());
      if (yval != null) {
        ret += e.getValue() * yval;
      }
    }
    return ret;
  }

  /**
   * It computes the Cosine similarity between two models which are vectors in sparse representation (Map).
   * 
   * @param x first model
   * @param y second model
   * @return Cosine similarity between the models. If both of them are 0 vectors, the method returns 1.0. Otherwise if either of them is zero vector or null, it return 0.0.
   */
  public static double computeSimilarity(final Map<Integer, Double> x, final Map<Integer, Double> y) {
    if (x != null && x.size() == 0 && y != null && y.size() == 0) {
      return 1.0;
    } else if (x.size() == 0 || y.size() == 0 || x == null || y == null) {
      return -1.0;
    }

    double yN = 0.0, xN = 0.0;
    double innerP = 0.0;
    for (int i : x.keySet()) {
      double xI = x.get(i);
      if (y.containsKey(i)) {
        innerP += xI * y.get(i);
      }
      xN += xI * xI;
    }
    for (int i : y.keySet()) {
      double yI = y.get(i);
      yN += yI * yI;
    }
    return innerP / Math.sqrt(xN * yN);
  }

  /**
   * Returns the normalized vector of the specified vector.
   * @param vector vector to be normalized
   * @return normalized vector
   */
  public static Map<Integer, Double> normalize(final Map<Integer, Double> vector){
    double norm = 0.0;
    for (int i : vector.keySet()){
      norm += vector.get(i) * vector.get(i);
    }
    norm = Math.sqrt(norm);
    Map<Integer, Double> normalized = new TreeMap<Integer, Double>();
    for (int i : vector.keySet()){
      normalized.put(i, vector.get(i) / norm);
    }
    return normalized;
  }
  
  /**
   * Returns the normalized vector of the specified vector.
   * @param vector vector to be normalized
   * @return normalized vector
   */
  public static double[] normalize(final double[] vector){
    double norm = 0.0;
    for (int i = 0; i < vector.length; i++){
      norm += vector[i] * vector[i];
    }
    norm = Math.sqrt(norm);
    double[] normalized = new double[vector.length];
    if (norm == 0.0) {
      return normalized;
    }
    for (int i = 0; i < vector.length; i++){
      normalized[i] = vector[i] / norm;
    }
    return normalized;
  }

  /**
   * Returns the squared norm of the specified vector.
   * @param vector vector to get squared norm
   * @return squared norm
   */
  public static double getNorm(final Map<Integer, Double> vector){
    double norm = 0.0;
    for (int i : vector.keySet()){
      Double valueD = vector.get(i);
      double value = valueD == null ? Double.NaN : valueD.doubleValue();
      norm += value * value;
    }
    norm = Math.sqrt(norm);
    return norm;
  }

  /**
   * Finds the maximal index of vectors a and b.
   * @param a vector a
   * @param b vector b
   * @return maximal index
   */
  public static int findMaxIdx(final Map<Integer, Double> a, final Map<Integer, Double> b) {
    if (a.size() > 0 && b.size() > 0) {
      return Math.max(((TreeMap<Integer, Double>) a).lastKey(), ((TreeMap<Integer, Double>) b).lastKey());
    }
    int max = Integer.MIN_VALUE;
    for (int d : a.keySet()) {
      if (d > max) {
        max = d;
      }
    }
    for (int d : b.keySet()) {
      if (d > max) {
        max = d;
      }
    }
    return max;
  }
  
  /**
   * Returns the value of Gauss error function or the cumulative distribution 
   * function (cdf) respect to the parameter z. Uses Taylor series for approximation. 
   * @param z is (x-mu)/(sqrt(2)*sigma), where mu and sigma are the parameters of the
   * Gauss distribution function
   * @param components the number of the Taylor components
   * @return value of the cdf
   */
  public static double erf(double z, int components) {
    double ret = z;
    double delimiter = 1.0;
    double factorial = 1.0;
    double sign = 1.0;
    double cumZ = z;
    double z2 = z*z;
    for (int i = 1; i < components; i++) {
      sign *= -1.0;
      factorial *= i;
      delimiter += 2.0;
      cumZ *= z2;
      ret += sign * (cumZ / (delimiter * factorial));
    }
    ret /= Math.sqrt(Math.PI);
    ret += 0.5;
    return ret;
  }
  
  /**
   * Shuffles the specified array using the specified random object.
   * @param r used for shuffling
   * @param array to be shuffled
   */
  public static void arrayShuffle(Random r, int[] array) {
    arrayShuffle(r, array, 0, array.length);
  }
  
  /**
   * Shuffles the specified array using the specified random object from 
   * the specified position to the spefified position.
   * @param r used for shuffling
   * @param array to be shuffled
   * @param from from index
   * @param to to index
   */
  public static void arrayShuffle(Random r, int[] array, int from, int to) {
    for (int i=from; i<to; i++) {
      int randomPosition = from + r.nextInt(to - from);
      int temp = array[i];
      array[i] = array[randomPosition];
      array[randomPosition] = temp;
    }
  }
  
  /**
   * Returns the auto-correlation of the specified array.
   * @param array compute on
   * @return auto-correlation
   */
  public static double[] autoCorrelate(double[] array) {
    double[] result = new double[array.length];
    Arrays.fill(result, 0.0);
    for (int i = 0; i < array.length; i++) {
      for (int j = 0; i + j < array.length; j++) {
        result[j] += array[i] * array[i + j];
      }
    }
    for (int i = array.length -1; i >= 0; i--) {
      result[i] /= result[0];
    }
    return result;
  }
  
  /**
   * Returns the auto-correlation of the specified array, the values are 
   * divided by the length of the specified array.
   * @param array compute on
   * @return auto-correlation
   */
  public static double[] autoCorrelate2(double[] array) {
    double[] result = new double[array.length];
    Arrays.fill(result, 0.0);
    for (int i = 0; i < array.length; i++) {
      for (int j = 0; j < array.length; j++) {
        result[j] += array[i] * array[(i + j)%array.length];
      }
    }
    for (int i = array.length -1; i >= 0; i--) {
      result[i] /= result[0];
    }
    return result;
  }
  
  /**
   * Returns the cosine similarity of the specified vectors.
   * @param a first vector
   * @param b second vector
   * @return cosine similarity
   */
  public static double computeSimilarity(double[] a, double[] b) {
    if (a.length != b.length) {
      throw new RuntimeException("Parameters have different sizes:" + a.length + " and " + b.length);
    }
    double mul = 0.0;
    double nA = 0.0;
    double nB = 0.0;
    for (int i = 0; i < a.length; i++) {
      mul += a[i] * b[i];
      nA += a[i] * a[i];
      nB += b[i] * b[i];
    }
    if (nA == 0.0 || nB == 0.0) {
      return 0.0;
    }
    return mul / Math.sqrt(nA * nB);
  }

  /**
   * Returns N unduplicated numbers in a randomly specified range
   * @param max max value in specified range
   * @param min min value in specified range
   * @param n Random number
   * @return int[] Result set of random number
   */
  public static ArrayList<Integer> randomArray(int min,int max,int n) {
    int len = max-min+1;

    if(max < min || n > len){
      return null;
    }

    int[] source = new int[len];
    for (int i = min; i < min+len; i++){
      source[i-min] = i;
    }

    ArrayList<Integer> result = new ArrayList<>();
    Random rd = new Random();
    int index = 0;
    for (int i = 0; i < result.size(); i++) {
      index = rd.nextInt(len-i);
      result.set(i, source[index]);

      int temp = source[index];
      source[index] = source[len-1-i];
      source[len-1-i] = temp;
    }
    return result;
  }

  // TODO: Adding randomArray()'s overloaded method
}
