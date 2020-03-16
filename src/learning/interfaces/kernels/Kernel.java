package learning.interfaces.kernels;

import learning.utils.SparseVector;

public interface Kernel {
  public double kernel(SparseVector x, SparseVector y);

}
