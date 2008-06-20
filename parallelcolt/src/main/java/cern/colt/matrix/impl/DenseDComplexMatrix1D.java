/*
Copyright � 1999 CERN - European Organization for Nuclear Research.
Permission to use, copy, modify, distribute and sell this software and its documentation for any purpose 
is hereby granted without fee, provided that the above copyright notice appear in all copies and 
that both that copyright notice and this permission notice appear in supporting documentation. 
CERN makes no representations about the suitability of this software for any purpose. 
It is provided "as is" without expressed or implied warranty.
 */
package cern.colt.matrix.impl;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import cern.colt.Utils;
import cern.colt.matrix.DComplexMatrix1D;
import cern.colt.matrix.DComplexMatrix2D;
import cern.colt.matrix.DComplexMatrix3D;
import cern.colt.matrix.DoubleMatrix1D;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

/**
 * Dense 1-d matrix (aka <i>vector</i>) holding <tt>complex</tt> elements.
 * <b>Implementation:</b>
 * <p>
 * Internally holds one single contiguous one-dimensional array. Complex data is
 * represented by 2 double values in sequence, i.e. elements[zero + 2 * k *
 * stride] constitute real part and elements[zero + 2 * k * stride + 1]
 * constitute imaginary part (k=0,...,size()-1).
 * <p>
 * <b>Memory requirements:</b>
 * <p>
 * <tt>memory [bytes] = 8*2*size()</tt>. Thus, a 1000000 matrix uses 16 MB.
 * <p>
 * <b>Time complexity:</b>
 * <p>
 * <tt>O(1)</tt> (i.e. constant time) for the basic operations <tt>get</tt>,
 * <tt>getQuick</tt>, <tt>set</tt>, <tt>setQuick</tt> and <tt>size</tt>,
 * <p>
 * 
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class DenseDComplexMatrix1D extends DComplexMatrix1D {

	private static final long serialVersionUID = 7295427570770814934L;

	private DoubleFFT_1D fft;

	/**
	 * The elements of this matrix. Complex data is represented by 2 double
	 * values in sequence, i.e. elements[zero + 2 * k * stride] constitute real
	 * part and elements[zero + 2 * k * stride] constitute imaginary part
	 * (k=0,...,size()-1).
	 */
	protected double[] elements;

	/**
	 * Constructs a matrix with a copy of the given values. The values are
	 * copied. So subsequent changes in <tt>values</tt> are not reflected in
	 * the matrix, and vice-versa. Due to the fact that complex data is
	 * represented by 2 double values in sequence: the real and imaginary parts,
	 * the size of new matrix will be equal to values.length / 2.
	 * 
	 * @param values
	 *            The values to be filled into the new matrix.
	 */
	public DenseDComplexMatrix1D(double[] values) {
		this(values.length / 2);
		assign(values);
	}

	/**
	 * Constructs a complex matrix with the same size as <tt>realPart</tt>
	 * matrix and fills the real part of this matrix with elements of
	 * <tt>realPart</tt>.
	 * 
	 * @param realPart
	 *            a real matrix whose elements become a real part of this matrix
	 * @throws IllegalArgumentException
	 *             if <tt>size<0</tt>.
	 */
	public DenseDComplexMatrix1D(DoubleMatrix1D realPart) {
		this(realPart.size);
		assignReal(realPart);
	}

	/**
	 * Constructs a matrix with a given number of cells. All entries are
	 * initially <tt>0</tt>.
	 * 
	 * @param size
	 *            the number of cells the matrix shall have.
	 * @throws IllegalArgumentException
	 *             if <tt>size<0</tt>.
	 */
	public DenseDComplexMatrix1D(int size) {
		setUp(size, 0, 2);
		this.isNoView = true;
		this.elements = new double[2 * size];
	}

	/**
	 * Constructs a matrix view with the given parameters.
	 * 
	 * @param size
	 *            the number of cells the matrix shall have.
	 * @param elements
	 *            the cells.
	 * @param zero
	 *            the index of the first element.
	 * @param stride
	 *            the number of indexes between any two elements, i.e.
	 *            <tt>index(i+1)-index(i)</tt>.
	 * @throws IllegalArgumentException
	 *             if <tt>size<0</tt>.
	 */
	protected DenseDComplexMatrix1D(int size, double[] elements, int zero, int stride) {
		setUp(size, zero, stride);
		this.elements = elements;
		this.isNoView = false;
	}

	public double[] aggregate(final cern.colt.function.DComplexDComplexDComplexFunction aggr, final cern.colt.function.DComplexDComplexFunction f) {
		double[] b = new double[2];
		if (size == 0) {
			b[0] = Double.NaN;
			b[1] = Double.NaN;
			return b;
		}
		double[] a = null;
		int np = Utils.getNP();
		if ((np > 1) && (size >= Utils.getThreadsBeginN_1D())) {
			Future[] futures = new Future[np];
			double[][] results = new double[np][2];
			int k = size / np;
			for (int j = 0; j < np; j++) {
				final int startidx = j * k;
				final int stopidx;
				if (j == np - 1) {
					stopidx = size;
				} else {
					stopidx = startidx + k;
				}
				futures[j] = Utils.threadPool.submit(new Callable<double[]>() {
					public double[] call() throws Exception {
						int idx = zero + startidx * stride;
						double[] a = f.apply(new double[] { elements[idx], elements[idx + 1] });
						for (int i = startidx + 1; i < stopidx; i++) {
							idx += stride;
							a = aggr.apply(a, f.apply(new double[] { elements[idx], elements[idx + 1] }));
						}
						return a;
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					results[j] = (double[]) futures[j].get();
				}
				a = results[0];
				for (int j = 1; j < np; j++) {
					a = aggr.apply(a, results[j]);
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			a = f.apply(new double[] { elements[zero], elements[zero + 1] });
			int idx = zero;
			for (int i = 1; i < size; i++) {
				idx += stride;
				a = aggr.apply(a, f.apply(new double[] { elements[idx], elements[idx + 1] }));
			}
		}
		return a;
	}

	public double[] aggregate(final DComplexMatrix1D other, final cern.colt.function.DComplexDComplexDComplexFunction aggr, final cern.colt.function.DComplexDComplexDComplexFunction f) {
		if (!(other instanceof DenseDComplexMatrix1D)) {
			return super.aggregate(other, aggr, f);
		}
		checkSize(other);
		if (size == 0) {
			double[] b = new double[2];
			b[0] = Double.NaN;
			b[1] = Double.NaN;
			return b;
		}
		final int zeroOther = other.zero;
		final int strideOther = other.stride;
		final double[] elemsOther = (double[]) other.getElements();
		double[] a = null;
		int np = Utils.getNP();
		if ((np > 1) && (size >= Utils.getThreadsBeginN_1D())) {
			Future[] futures = new Future[np];
			double[][] results = new double[np][2];
			int k = size / np;
			for (int j = 0; j < np; j++) {
				final int startidx = j * k;
				final int stopidx;
				if (j == np - 1) {
					stopidx = size;
				} else {
					stopidx = startidx + k;
				}
				futures[j] = Utils.threadPool.submit(new Callable<double[]>() {
					public double[] call() throws Exception {
						int idx = zero + startidx * stride;
						int idxOther = zeroOther + startidx * strideOther;
						double[] a = f.apply(new double[] { elements[idx], elements[idx + 1] }, new double[] { elemsOther[idxOther], elemsOther[idxOther + 1] });
						for (int i = startidx + 1; i < stopidx; i++) {
							idx += stride;
							idxOther += strideOther;
							a = aggr.apply(a, f.apply(new double[] { elements[idx], elements[idx + 1] }, new double[] { elemsOther[idxOther], elemsOther[idxOther + 1] }));
						}
						return a;
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					results[j] = (double[]) futures[j].get();
				}
				a = results[0];
				for (int j = 1; j < np; j++) {
					a = aggr.apply(a, results[j]);
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			int idx = zero;
			int idxOther = zeroOther;
			a = f.apply(new double[] { elements[zero], elements[zero + 1] }, new double[] { elemsOther[zeroOther], elemsOther[zeroOther + 1] });
			for (int i = 1; i < size; i++) {
				idx += stride;
				idxOther += strideOther;
				a = aggr.apply(a, f.apply(new double[] { elements[idx], elements[idx + 1] }, new double[] { elemsOther[idxOther], elemsOther[idxOther + 1] }));
			}
		}
		return a;
	}

	public DComplexMatrix1D assign(final cern.colt.function.DComplexDComplexFunction function) {
		if (this.elements == null)
			throw new InternalError();
		int np = Utils.getNP();
		if ((np > 1) && (size >= Utils.getThreadsBeginN_1D())) {
			if (function instanceof cern.jet.math.DComplexMult) {
				double[] multiplicator = ((cern.jet.math.DComplexMult) function).multiplicator;
				if (multiplicator[0] == 1 && multiplicator[1] == 0)
					return this;
			}
			Future[] futures = new Future[np];
			int k = size / np;
			for (int j = 0; j < np; j++) {
				final int startidx = j * k;
				final int stopidx;
				if (j == np - 1) {
					stopidx = size;
				} else {
					stopidx = startidx + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						double[] tmp = new double[2];
						int idx = zero + startidx * stride;
						for (int k = startidx; k < stopidx; k++) {
							tmp[0] = elements[idx];
							tmp[1] = elements[idx + 1];
							tmp = function.apply(tmp);
							elements[idx] = tmp[0];
							elements[idx + 1] = tmp[1];
							idx += stride;
						}
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			double[] tmp = new double[2];
			int idx = zero;
			for (int k = 0; k < size; k++) {
				tmp[0] = elements[idx];
				tmp[1] = elements[idx + 1];
				tmp = function.apply(tmp);
				elements[idx] = tmp[0];
				elements[idx + 1] = tmp[1];
				idx += stride;
			}
		}
		return this;
	}

	public DComplexMatrix1D assign(final cern.colt.function.DComplexProcedure cond, final cern.colt.function.DComplexDComplexFunction function) {
		int np = Utils.getNP();
		if ((np > 1) && (size >= Utils.getThreadsBeginN_1D())) {
			Future[] futures = new Future[np];
			int k = size / np;
			for (int j = 0; j < np; j++) {
				final int startidx = j * k;
				final int stopidx;
				if (j == np - 1) {
					stopidx = size;
				} else {
					stopidx = startidx + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {

					public void run() {
						double[] elem = new double[2];
						int idx = zero + startidx * stride;
						for (int i = startidx; i < stopidx; i++) {
							elem[0] = elements[idx];
							elem[1] = elements[idx + 1];
							if (cond.apply(elem) == true) {
								elem = function.apply(elem);
								elements[idx] = elem[0];
								elements[idx + 1] = elem[1];
							}
							idx += stride;
						}
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			double[] elem = new double[2];
			int idx = zero;
			for (int i = 0; i < size; i++) {
				elem[0] = elements[idx];
				elem[1] = elements[idx + 1];
				if (cond.apply(elem) == true) {
					elem = function.apply(elem);
					elements[idx] = elem[0];
					elements[idx + 1] = elem[1];
				}
				idx += stride;
			}
		}
		return this;
	}

	public DComplexMatrix1D assign(final cern.colt.function.DComplexProcedure cond, final double[] value) {
		int np = Utils.getNP();
		if ((np > 1) && (size >= Utils.getThreadsBeginN_1D())) {
			Future[] futures = new Future[np];
			int k = size / np;
			for (int j = 0; j < np; j++) {
				final int startidx = j * k;
				final int stopidx;
				if (j == np - 1) {
					stopidx = size;
				} else {
					stopidx = startidx + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						double[] elem = new double[2];
						int idx = zero + startidx * stride;
						for (int i = startidx; i < stopidx; i++) {
							elem[0] = elements[idx];
							elem[1] = elements[idx + 1];
							if (cond.apply(elem) == true) {
								elements[idx] = value[0];
								elements[idx + 1] = value[1];
							}
							idx += stride;
						}
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			double[] elem = new double[2];
			int idx = zero;
			for (int i = 0; i < size; i++) {
				elem[0] = elements[idx];
				elem[1] = elements[idx + 1];
				if (cond.apply(elem) == true) {
					elements[idx] = value[0];
					elements[idx + 1] = value[1];
				}
				idx += stride;
			}
		}
		return this;
	}

	public DComplexMatrix1D assign(final cern.colt.function.DComplexRealFunction function) {
		if (this.elements == null)
			throw new InternalError();
		int np = Utils.getNP();
		if ((np > 1) && (size >= Utils.getThreadsBeginN_1D())) {
			Future[] futures = new Future[np];
			int k = size / np;
			for (int j = 0; j < np; j++) {
				final int startidx = j * k;
				final int stopidx;
				if (j == np - 1) {
					stopidx = size;
				} else {
					stopidx = startidx + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						double[] tmp = new double[2];
						int idx = zero + startidx * stride;
						for (int k = startidx; k < stopidx; k++) {
							tmp[0] = elements[idx];
							tmp[1] = elements[idx + 1];
							tmp[0] = function.apply(tmp);
							elements[idx] = tmp[0];
							elements[idx + 1] = 0;
							idx += stride;
						}
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			double[] tmp = new double[2];
			int idx = zero;
			for (int k = 0; k < size; k++) {
				tmp[0] = elements[idx];
				tmp[1] = elements[idx + 1];
				tmp[0] = function.apply(tmp);
				elements[idx] = tmp[0];
				elements[idx + 1] = 0;
				idx += stride;
			}
		}
		return this;
	}

	public DComplexMatrix1D assign(DComplexMatrix1D source) {
		if (!(source instanceof DenseDComplexMatrix1D)) {
			return super.assign(source);
		}
		DenseDComplexMatrix1D other = (DenseDComplexMatrix1D) source;
		if (other == this)
			return this;
		checkSize(other);
		if (isNoView && other.isNoView) { // quickest
			System.arraycopy(other.elements, 0, this.elements, 0, this.elements.length);
			return this;
		}
		if (haveSharedCells(other)) {
			DComplexMatrix1D c = other.copy();
			if (!(c instanceof DenseDComplexMatrix1D)) { // should not happen
				return super.assign(source);
			}
			other = (DenseDComplexMatrix1D) c;
		}

		final double[] elemsOther = other.elements;
		if (elements == null || elemsOther == null)
			throw new InternalError();
		final int strideOther = other.stride;
		final int zeroOther = other.index(0);

		int np = Utils.getNP();
		if ((np > 1) && (size >= Utils.getThreadsBeginN_1D())) {
			Future[] futures = new Future[np];
			int k = size / np;
			for (int j = 0; j < np; j++) {
				final int startidx = j * k;
				final int stopidx;
				if (j == np - 1) {
					stopidx = size;
				} else {
					stopidx = startidx + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						int idx = zero + startidx * stride;
						int idxOther = zeroOther + startidx * strideOther;
						for (int k = startidx; k < stopidx; k++) {
							elements[idx] = elemsOther[idxOther];
							elements[idx + 1] = elemsOther[idxOther + 1];
							idx += stride;
							idxOther += strideOther;
						}
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			int idx = zero;
			int idxOther = zeroOther;
			for (int k = 0; k < size; k++) {
				elements[idx] = elemsOther[idxOther];
				elements[idx + 1] = elemsOther[idxOther + 1];
				idx += stride;
				idxOther += strideOther;
			}
		}
		return this;
	}

	public DComplexMatrix1D assign(DComplexMatrix1D y, final cern.colt.function.DComplexDComplexDComplexFunction function) {
		if (!(y instanceof DenseDComplexMatrix1D)) {
			return super.assign(y, function);
		}
		checkSize(y);
		final double[] elemsOther = (double[]) y.getElements();
		final int zeroOther = y.index(0);
		final int strideOther = y.stride;

		if (elements == null || elemsOther == null)
			throw new InternalError();
		int np = Utils.getNP();
		if ((np > 1) && (size >= Utils.getThreadsBeginN_1D())) {
			Future[] futures = new Future[np];
			int k = size / np;
			for (int j = 0; j < np; j++) {
				final int startidx = j * k;
				final int stopidx;
				if (j == np - 1) {
					stopidx = size;
				} else {
					stopidx = startidx + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						double[] tmp1 = new double[2];
						double[] tmp2 = new double[2];
						int idx = zero + startidx * stride;
						int idxOther = zeroOther + startidx * strideOther;
						for (int k = startidx; k < stopidx; k++) {
							tmp1[0] = elements[idx];
							tmp1[1] = elements[idx + 1];
							tmp2[0] = elemsOther[idxOther];
							tmp2[1] = elemsOther[idxOther + 1];
							tmp1 = function.apply(tmp1, tmp2);
							elements[idx] = tmp1[0];
							elements[idx + 1] = tmp1[1];
							idx += stride;
							idxOther += strideOther;
						}
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			double[] tmp1 = new double[2];
			double[] tmp2 = new double[2];
			int idx = zero;
			int idxOther = zeroOther;
			for (int k = 0; k < size; k++) {
				tmp1[0] = elements[idx];
				tmp1[1] = elements[idx + 1];
				tmp2[0] = elemsOther[idxOther];
				tmp2[1] = elemsOther[idxOther + 1];
				tmp1 = function.apply(tmp1, tmp2);
				elements[idx] = tmp1[0];
				elements[idx + 1] = tmp1[1];
				idx += stride;
				idxOther += strideOther;
			}
		}
		return this;
	}

	public DComplexMatrix1D assign(final double re, final double im) {
		int np = Utils.getNP();
		if ((np > 1) && (size >= Utils.getThreadsBeginN_1D())) {
			Future[] futures = new Future[np];
			int k = size / np;
			for (int j = 0; j < np; j++) {
				final int startidx = j * k;
				final int stopidx;
				if (j == np - 1) {
					stopidx = size;
				} else {
					stopidx = startidx + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						int idx = zero + startidx * stride;
						for (int k = startidx; k < stopidx; k++) {
							elements[idx] = re;
							elements[idx + 1] = im;
							idx += stride;
						}
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			int idx = zero;
			for (int i = 0; i < size; i++) {
				this.elements[idx] = re;
				this.elements[idx + 1] = im;
				idx += stride;
			}
		}
		return this;
	}

	public DComplexMatrix1D assign(double[] values) {
		if (isNoView) {
			if (values.length != 2 * size)
				throw new IllegalArgumentException("The length of values[] must be equal to 2*size()=" + 2 * size());
			System.arraycopy(values, 0, this.elements, 0, values.length);
		} else {
			super.assign(values);
		}
		return this;
	}

	public DComplexMatrix1D assignImaginary(final DoubleMatrix1D other) {
		if (!(other instanceof DenseDoubleMatrix1D)) {
			return super.assignImaginary(other);
		}
		checkSize(other);
		final int zeroOther = other.zero;
		final int strideOther = other.stride;
		final double[] elemsOther = (double[]) other.getElements();
		int np = Utils.getNP();
		if ((np > 1) && (size >= Utils.getThreadsBeginN_1D())) {
			Future[] futures = new Future[np];
			int k = size / np;
			for (int j = 0; j < np; j++) {
				final int startidx = j * k;
				final int stopidx;
				if (j == np - 1) {
					stopidx = size;
				} else {
					stopidx = startidx + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						int idx = zero + startidx * stride;
						int idxOther = zeroOther + startidx * strideOther;
						for (int i = startidx; i < stopidx; i++) {
							elements[idx] = 0;
							elements[idx + 1] = elemsOther[idxOther];
							idx += stride;
							idxOther += strideOther;
						}
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			int idx = zero;
			int idxOther = zeroOther;
			for (int i = 0; i < size; i++) {
				elements[idx] = 0;
				elements[idx + 1] = elemsOther[idxOther];
				idx += stride;
				idxOther += strideOther;
			}
		}
		return this;
	}

	public DComplexMatrix1D assignReal(final DoubleMatrix1D other) {
		if (!(other instanceof DenseDoubleMatrix1D)) {
			return super.assignReal(other);
		}
		checkSize(other);
		final int zeroOther = other.zero;
		final int strideOther = other.stride;
		final double[] elemsOther = (double[]) other.getElements();
		int np = Utils.getNP();
		if ((np > 1) && (size >= Utils.getThreadsBeginN_1D())) {
			Future[] futures = new Future[np];
			int k = size / np;
			for (int j = 0; j < np; j++) {
				final int startidx = j * k;
				final int stopidx;
				if (j == np - 1) {
					stopidx = size;
				} else {
					stopidx = startidx + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						int idx = zero + startidx * stride;
						int idxOther = zeroOther + startidx * strideOther;
						for (int i = startidx; i < stopidx; i++) {
							elements[idx] = elemsOther[idxOther];
							elements[idx + 1] = 0;
							idx += stride;
							idxOther += strideOther;
						}
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			int idx = zero;
			int idxOther = zeroOther;
			for (int i = 0; i < size; i++) {
				elements[idx] = elemsOther[idxOther];
				elements[idx + 1] = 0;
				idx += stride;
				idxOther += strideOther;
			}
		}
		return this;
	}

	public void fft() {
		if (fft == null) {
			fft = new DoubleFFT_1D(size);
		}
		fft.complexForward(elements);
	}

	public double[] getElements() {
		return elements;
	}

	public DoubleMatrix1D getImaginaryPart() {
		final DenseDoubleMatrix1D Im = new DenseDoubleMatrix1D(size);
		final double[] elemsOther = (double[]) Im.getElements();
		final int zeroOther = Im.index(0);
		final int strideOther = Im.stride;
		int np = Utils.getNP();
		if ((np > 1) && (size >= Utils.getThreadsBeginN_1D())) {
			Future[] futures = new Future[np];
			int k = size / np;
			for (int j = 0; j < np; j++) {
				final int startidx = j * k;
				final int stopidx;
				if (j == np - 1) {
					stopidx = size;
				} else {
					stopidx = startidx + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						int idx = zero + startidx * stride;
						int idxOther = zeroOther + startidx * strideOther;
						for (int k = startidx; k < stopidx; k++) {
							elemsOther[idxOther] = elements[idx + 1];
							idx += stride;
							idxOther += strideOther;
						}
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			int idx = zero;
			int idxOther = zeroOther;
			for (int i = 0; i < size; i++) {
				elemsOther[idxOther] = elements[idx + 1];
				idx += stride;
				idxOther += strideOther;
			}
		}
		return Im;
	}

	public double[] getQuick(int index) {
		int idx = zero + index * stride;
		return new double[] { elements[idx], elements[idx + 1] };
	}

	public DoubleMatrix1D getRealPart() {
		final DenseDoubleMatrix1D R = new DenseDoubleMatrix1D(size);
		final double[] elemsOther = (double[]) R.getElements();
		final int zeroOther = R.index(0);
		final int strideOther = R.stride;
		int np = Utils.getNP();
		if ((np > 1) && (size >= Utils.getThreadsBeginN_1D())) {
			Future[] futures = new Future[np];
			int k = size / np;
			for (int j = 0; j < np; j++) {
				final int startidx = j * k;
				final int stopidx;
				if (j == np - 1) {
					stopidx = size;
				} else {
					stopidx = startidx + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						int idx = zero + startidx * stride;
						int idxOther = zeroOther + startidx * strideOther;
						for (int k = startidx; k < stopidx; k++) {
							elemsOther[idxOther] = elements[idx];
							idx += stride;
							idxOther += strideOther;
						}
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			int idx = zero;
			int idxOther = zeroOther;
			for (int i = 0; i < size; i++) {
				elemsOther[idxOther] = elements[idx];
				idx += stride;
				idxOther += strideOther;
			}
		}
		return R;
	}

	public void ifft(boolean scale) {
		if (fft == null) {
			fft = new DoubleFFT_1D(size);
		}
		fft.complexInverse(elements, scale);
	}

	public DComplexMatrix1D like(int size) {
		return new DenseDComplexMatrix1D(size);
	}

	public DComplexMatrix2D like2D(int rows, int columns) {
		return new DenseDComplexMatrix2D(rows, columns);
	}

	public DComplexMatrix2D reshape(final int rows, final int cols) {
		if (rows * cols != size) {
			throw new IllegalArgumentException("rows*cols != size");
		}
		DComplexMatrix2D M = new DenseDComplexMatrix2D(rows, cols);
		final double[] elemsOther = (double[]) M.getElements();
		final int zeroOther = M.index(0, 0);
		final int rowStrideOther = M.rowStride;
		final int colStrideOther = M.columnStride;
		int np = Utils.getNP();
		if ((np > 1) && (size >= Utils.getThreadsBeginN_1D())) {
			Future[] futures = new Future[np];
			int k = cols / np;
			for (int j = 0; j < np; j++) {
				final int startcol = j * k;
				final int stopcol;
				if (j == np - 1) {
					stopcol = cols;
				} else {
					stopcol = startcol + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						int idx;
						int idxOther;
						for (int c = startcol; c < stopcol; c++) {
							idxOther = zeroOther + c * colStrideOther;
							idx = zero + (c * rows) * stride;
							for (int r = 0; r < rows; r++) {
								elemsOther[idxOther] = elements[idx];
								elemsOther[idxOther + 1] = elements[idx + 1];
								idxOther += rowStrideOther;
								idx += stride;
							}
						}
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			int idxOther;
			int idx = zero;
			for (int c = 0; c < cols; c++) {
				idxOther = zeroOther + c * colStrideOther;
				for (int r = 0; r < rows; r++) {
					elemsOther[idxOther] = elements[idx];
					elemsOther[idxOther + 1] = elements[idx + 1];
					idxOther += rowStrideOther;
					idx += stride;
				}
			}
		}
		return M;
	}

	public DComplexMatrix3D reshape(final int slices, final int rows, final int cols) {
		if (slices * rows * cols != size) {
			throw new IllegalArgumentException("slices*rows*cols != size");
		}
		DComplexMatrix3D M = new DenseDComplexMatrix3D(slices, rows, cols);
		final double[] elemsOther = (double[]) M.getElements();
		final int zeroOther = M.index(0, 0, 0);
		final int sliceStrideOther = M.sliceStride;
		final int rowStrideOther = M.rowStride;
		final int colStrideOther = M.columnStride;
		int np = Utils.getNP();
		if ((np > 1) && (size >= Utils.getThreadsBeginN_1D())) {
			Future[] futures = new Future[np];
			int k = slices / np;
			for (int j = 0; j < np; j++) {
				final int startslice = j * k;
				final int stopslice;
				if (j == np - 1) {
					stopslice = slices;
				} else {
					stopslice = startslice + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						int idx;
						int idxOther;
						for (int s = startslice; s < stopslice; s++) {
							for (int c = 0; c < cols; c++) {
								idxOther = zeroOther + s * sliceStrideOther + c * colStrideOther;
								idx = zero + (s * rows * cols + c * rows) * stride;
								for (int r = 0; r < rows; r++) {
									elemsOther[idxOther] = elements[idx];
									elemsOther[idxOther + 1] = elements[idx + 1];
									idxOther += rowStrideOther;
									idx += stride;
								}
							}
						}
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			int idxOther;
			int idx = zero;
			for (int s = 0; s < slices; s++) {
				for (int c = 0; c < cols; c++) {
					idxOther = zeroOther + s * sliceStrideOther + c * colStrideOther;
					for (int r = 0; r < rows; r++) {
						elemsOther[idxOther] = elements[idx];
						elemsOther[idxOther + 1] = elements[idx + 1];
						idxOther += rowStrideOther;
						idx += stride;
					}
				}
			}
		}
		return M;
	}

	public void setQuick(int index, double re, double im) {
		int idx = zero + index * stride;
		this.elements[idx] = re;
		this.elements[idx + 1] = im;
	}

	public void setQuick(int index, double[] value) {
		int idx = zero + index * stride;
		this.elements[idx] = value[0];
		this.elements[idx + 1] = value[1];
	}

	public void swap(DComplexMatrix1D other) {
		if (!(other instanceof DenseDComplexMatrix1D)) {
			super.swap(other);
		}
		DenseDComplexMatrix1D y = (DenseDComplexMatrix1D) other;
		if (y == this)
			return;
		checkSize(y);

		final double[] elemsOther = y.elements;
		if (elements == null || elemsOther == null)
			throw new InternalError();
		final int strideOther = y.stride;
		final int zeroOther = y.index(0);

		int np = Utils.getNP();
		if ((np > 1) && (size >= Utils.getThreadsBeginN_1D())) {
			Future[] futures = new Future[np];
			int k = size / np;
			for (int j = 0; j < np; j++) {
				final int startidx = j * k;
				final int stopidx;
				if (j == np - 1) {
					stopidx = size;
				} else {
					stopidx = startidx + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						int idx = zero + startidx * stride;
						int idxOther = zeroOther + startidx * strideOther;
						double tmp;
						for (int k = startidx; k < stopidx; k++) {
							tmp = elements[idx];
							elements[idx] = elemsOther[idxOther];
							elemsOther[idxOther] = tmp;
							tmp = elements[idx + 1];
							elements[idx + 1] = elemsOther[idxOther + 1];
							elemsOther[idxOther + 1] = tmp;
							idx += stride;
							idxOther += strideOther;
						}
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			int idx = zero;
			int idxOther = zeroOther;
			double tmp;
			for (int k = 0; k < size; k++) {
				tmp = elements[idx];
				elements[idx] = elemsOther[idxOther];
				elemsOther[idxOther] = tmp;
				tmp = elements[idx + 1];
				elements[idx + 1] = elemsOther[idxOther + 1];
				elemsOther[idxOther + 1] = tmp;
				idx += stride;
				idxOther += strideOther;
			}
		}
	}

	public void toArray(double[] values) {
		if (values.length < 2 * size)
			throw new IllegalArgumentException("values too small");
		if (this.isNoView)
			System.arraycopy(this.elements, 0, values, 0, this.elements.length);
		else
			super.toArray(values);
	}

	public double[] zSum() {
		double[] sum = new double[2];
		if (this.elements == null)
			throw new InternalError();
		int np = Utils.getNP();
		if ((np > 1) && (size >= Utils.getThreadsBeginN_1D())) {
			Future[] futures = new Future[np];
			double[][] results = new double[np][2];
			int k = size / np;
			for (int j = 0; j < np; j++) {
				final int startidx = j * k;
				final int stopidx;
				if (j == np - 1) {
					stopidx = size;
				} else {
					stopidx = startidx + k;
				}

				futures[j] = Utils.threadPool.submit(new Callable<double[]>() {
					public double[] call() throws Exception {
						double[] sum = new double[2];
						int idx = zero + startidx * stride;
						for (int k = startidx; k < stopidx; k++) {
							sum[0] += elements[idx];
							sum[1] += elements[idx + 1];
							idx += stride;
						}
						return sum;
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					results[j] = (double[]) futures[j].get();
				}
				sum = results[0];
				for (int j = 1; j < np; j++) {
					sum[0] = sum[0] + results[j][0];
					sum[1] = sum[1] + results[j][1];
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			int idx = zero;
			for (int k = 0; k < size; k++) {
				sum[0] += elements[idx];
				sum[1] += elements[idx + 1];
				idx += stride;
			}
		}
		return sum;
	}

	protected int cardinality(int maxCardinality) {
		int cardinality = 0;
		int idx = zero;
		int i = 0;
		while (i < size && cardinality < maxCardinality) {
			if (elements[idx] != 0 || elements[idx + 1] != 0)
				cardinality++;
			idx += stride;
			i++;
		}
		return cardinality;
	}

	protected boolean haveSharedCellsRaw(DComplexMatrix1D other) {
		if (other instanceof SelectedDenseDComplexMatrix1D) {
			SelectedDenseDComplexMatrix1D otherMatrix = (SelectedDenseDComplexMatrix1D) other;
			return this.elements == otherMatrix.elements;
		} else if (other instanceof DenseDComplexMatrix1D) {
			DenseDComplexMatrix1D otherMatrix = (DenseDComplexMatrix1D) other;
			return this.elements == otherMatrix.elements;
		}
		return false;
	}

	protected int index(int rank) {
		return zero + rank * stride;
	}

	protected DComplexMatrix1D viewSelectionLike(int[] offsets) {
		return new SelectedDenseDComplexMatrix1D(this.elements, offsets);
	}
}
