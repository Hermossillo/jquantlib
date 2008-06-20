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
import cern.colt.matrix.FComplexMatrix1D;
import cern.colt.matrix.FComplexMatrix2D;
import cern.colt.matrix.FComplexMatrix3D;
import cern.colt.matrix.FloatMatrix1D;
import edu.emory.mathcs.jtransforms.fft.FloatFFT_1D;

/**
 * Dense 1-d matrix (aka <i>vector</i>) holding <tt>complex</tt> elements.
 * <b>Implementation:</b>
 * <p>
 * Internally holds one single contiguous one-dimensional array. Complex data is
 * represented by 2 float values in sequence, i.e. elements[zero + 2 * k *
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
public class DenseFComplexMatrix1D extends FComplexMatrix1D {

	private static final long serialVersionUID = 7295427570770814934L;

	private FloatFFT_1D fft;

	/**
	 * The elements of this matrix. Complex data is represented by 2 float
	 * values in sequence, i.e. elements[zero + 2 * k * stride] constitute real
	 * part and elements[zero + 2 * k * stride] constitute imaginary part
	 * (k=0,...,size()-1).
	 */
	protected float[] elements;

	/**
	 * Constructs a matrix with a copy of the given values. The values are
	 * copied. So subsequent changes in <tt>values</tt> are not reflected in
	 * the matrix, and vice-versa. Due to the fact that complex data is
	 * represented by 2 float values in sequence: the real and imaginary parts,
	 * the size of new matrix will be equal to values.length / 2.
	 * 
	 * @param values
	 *            The values to be filled into the new matrix.
	 */
	public DenseFComplexMatrix1D(float[] values) {
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
	public DenseFComplexMatrix1D(FloatMatrix1D realPart) {
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
	public DenseFComplexMatrix1D(int size) {
		setUp(size, 0, 2);
		this.isNoView = true;
		this.elements = new float[2 * size];
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
	protected DenseFComplexMatrix1D(int size, float[] elements, int zero, int stride) {
		setUp(size, zero, stride);
		this.elements = elements;
		this.isNoView = false;
	}

	public float[] aggregate(final cern.colt.function.FComplexFComplexFComplexFunction aggr, final cern.colt.function.FComplexFComplexFunction f) {
		float[] b = new float[2];
		if (size == 0) {
			b[0] = Float.NaN;
			b[1] = Float.NaN;
			return b;
		}
		float[] a = null;
		int np = Utils.getNP();
		if ((np > 1) && (size >= Utils.getThreadsBeginN_1D())) {
			Future[] futures = new Future[np];
			float[][] results = new float[np][2];
			int k = size / np;
			for (int j = 0; j < np; j++) {
				final int startidx = j * k;
				final int stopidx;
				if (j == np - 1) {
					stopidx = size;
				} else {
					stopidx = startidx + k;
				}
				futures[j] = Utils.threadPool.submit(new Callable<float[]>() {
					public float[] call() throws Exception {
						int idx = zero + startidx * stride;
						float[] a = f.apply(new float[] { elements[idx], elements[idx + 1] });
						for (int i = startidx + 1; i < stopidx; i++) {
							idx += stride;
							a = aggr.apply(a, f.apply(new float[] { elements[idx], elements[idx + 1] }));
						}
						return a;
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					results[j] = (float[]) futures[j].get();
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
			a = f.apply(new float[] { elements[zero], elements[zero + 1] });
			int idx = zero;
			for (int i = 1; i < size; i++) {
				idx += stride;
				a = aggr.apply(a, f.apply(new float[] { elements[idx], elements[idx + 1] }));
			}
		}
		return a;
	}

	public float[] aggregate(final FComplexMatrix1D other, final cern.colt.function.FComplexFComplexFComplexFunction aggr, final cern.colt.function.FComplexFComplexFComplexFunction f) {
		if (!(other instanceof DenseFComplexMatrix1D)) {
			return super.aggregate(other, aggr, f);
		}
		checkSize(other);
		if (size == 0) {
			float[] b = new float[2];
			b[0] = Float.NaN;
			b[1] = Float.NaN;
			return b;
		}
		final int zeroOther = other.zero;
		final int strideOther = other.stride;
		final float[] elemsOther = (float[]) other.getElements();
		float[] a = null;
		int np = Utils.getNP();
		if ((np > 1) && (size >= Utils.getThreadsBeginN_1D())) {
			Future[] futures = new Future[np];
			float[][] results = new float[np][2];
			int k = size / np;
			for (int j = 0; j < np; j++) {
				final int startidx = j * k;
				final int stopidx;
				if (j == np - 1) {
					stopidx = size;
				} else {
					stopidx = startidx + k;
				}
				futures[j] = Utils.threadPool.submit(new Callable<float[]>() {
					public float[] call() throws Exception {
						int idx = zero + startidx * stride;
						int idxOther = zeroOther + startidx * strideOther;
						float[] a = f.apply(new float[] { elements[idx], elements[idx + 1] }, new float[] { elemsOther[idxOther], elemsOther[idxOther + 1] });
						for (int i = startidx + 1; i < stopidx; i++) {
							idx += stride;
							idxOther += strideOther;
							a = aggr.apply(a, f.apply(new float[] { elements[idx], elements[idx + 1] }, new float[] { elemsOther[idxOther], elemsOther[idxOther + 1] }));
						}
						return a;
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					results[j] = (float[]) futures[j].get();
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
			a = f.apply(new float[] { elements[zero], elements[zero + 1] }, new float[] { elemsOther[zeroOther], elemsOther[zeroOther + 1] });
			for (int i = 1; i < size; i++) {
				idx += stride;
				idxOther += strideOther;
				a = aggr.apply(a, f.apply(new float[] { elements[idx], elements[idx + 1] }, new float[] { elemsOther[idxOther], elemsOther[idxOther + 1] }));
			}
		}
		return a;
	}

	public FComplexMatrix1D assign(final cern.colt.function.FComplexFComplexFunction function) {
		if (this.elements == null)
			throw new InternalError();
		int np = Utils.getNP();
		if ((np > 1) && (size >= Utils.getThreadsBeginN_1D())) {
			if (function instanceof cern.jet.math.FComplexMult) {
				float[] multiplicator = ((cern.jet.math.FComplexMult) function).multiplicator;
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
						float[] tmp = new float[2];
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
			float[] tmp = new float[2];
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

	public FComplexMatrix1D assign(final cern.colt.function.FComplexProcedure cond, final cern.colt.function.FComplexFComplexFunction function) {
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
						float[] elem = new float[2];
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
			float[] elem = new float[2];
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

	public FComplexMatrix1D assign(final cern.colt.function.FComplexProcedure cond, final float[] value) {
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
						float[] elem = new float[2];
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
			float[] elem = new float[2];
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

	public FComplexMatrix1D assign(final cern.colt.function.FComplexRealFunction function) {
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
						float[] tmp = new float[2];
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
			float[] tmp = new float[2];
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

	public FComplexMatrix1D assign(FComplexMatrix1D source) {
		if (!(source instanceof DenseFComplexMatrix1D)) {
			return super.assign(source);
		}
		DenseFComplexMatrix1D other = (DenseFComplexMatrix1D) source;
		if (other == this)
			return this;
		checkSize(other);
		if (isNoView && other.isNoView) { // quickest
			System.arraycopy(other.elements, 0, this.elements, 0, this.elements.length);
			return this;
		}
		if (haveSharedCells(other)) {
			FComplexMatrix1D c = other.copy();
			if (!(c instanceof DenseFComplexMatrix1D)) { // should not happen
				return super.assign(source);
			}
			other = (DenseFComplexMatrix1D) c;
		}

		final float[] elemsOther = other.elements;
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

	public FComplexMatrix1D assign(FComplexMatrix1D y, final cern.colt.function.FComplexFComplexFComplexFunction function) {
		if (!(y instanceof DenseFComplexMatrix1D)) {
			return super.assign(y, function);
		}
		checkSize(y);
		final float[] elemsOther = (float[]) y.getElements();
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
						float[] tmp1 = new float[2];
						float[] tmp2 = new float[2];
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
			float[] tmp1 = new float[2];
			float[] tmp2 = new float[2];
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

	public FComplexMatrix1D assign(final float re, final float im) {
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

	public FComplexMatrix1D assign(float[] values) {
		if (isNoView) {
			if (values.length != 2 * size)
				throw new IllegalArgumentException("The length of values[] must be equal to 2*size()=" + 2 * size());
			System.arraycopy(values, 0, this.elements, 0, values.length);
		} else {
			super.assign(values);
		}
		return this;
	}

	public FComplexMatrix1D assignImaginary(final FloatMatrix1D other) {
		if (!(other instanceof DenseFloatMatrix1D)) {
			return super.assignImaginary(other);
		}
		checkSize(other);
		final int zeroOther = other.zero;
		final int strideOther = other.stride;
		final float[] elemsOther = (float[]) other.getElements();
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

	public FComplexMatrix1D assignReal(final FloatMatrix1D other) {
		if (!(other instanceof DenseFloatMatrix1D)) {
			return super.assignReal(other);
		}
		checkSize(other);
		final int zeroOther = other.zero;
		final int strideOther = other.stride;
		final float[] elemsOther = (float[]) other.getElements();
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
			fft = new FloatFFT_1D(size);
		}
		fft.complexForward(elements);
	}

	public float[] getElements() {
		return elements;
	}

	public FloatMatrix1D getImaginaryPart() {
		final DenseFloatMatrix1D Im = new DenseFloatMatrix1D(size);
		final float[] elemsOther = (float[]) Im.getElements();
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

	public float[] getQuick(int index) {
		int idx = zero + index * stride;
		return new float[] { elements[idx], elements[idx + 1] };
	}

	public FloatMatrix1D getRealPart() {
		final DenseFloatMatrix1D R = new DenseFloatMatrix1D(size);
		final float[] elemsOther = (float[]) R.getElements();
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
			fft = new FloatFFT_1D(size);
		}
		fft.complexInverse(elements, scale);
	}

	public FComplexMatrix1D like(int size) {
		return new DenseFComplexMatrix1D(size);
	}

	public FComplexMatrix2D like2D(int rows, int columns) {
		return new DenseFComplexMatrix2D(rows, columns);
	}

	public FComplexMatrix2D reshape(final int rows, final int cols) {
		if (rows * cols != size) {
			throw new IllegalArgumentException("rows*cols != size");
		}
		FComplexMatrix2D M = new DenseFComplexMatrix2D(rows, cols);
		final float[] elemsOther = (float[]) M.getElements();
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

	public FComplexMatrix3D reshape(final int slices, final int rows, final int cols) {
		if (slices * rows * cols != size) {
			throw new IllegalArgumentException("slices*rows*cols != size");
		}
		FComplexMatrix3D M = new DenseFComplexMatrix3D(slices, rows, cols);
		final float[] elemsOther = (float[]) M.getElements();
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

	public void setQuick(int index, float re, float im) {
		int idx = zero + index * stride;
		this.elements[idx] = re;
		this.elements[idx + 1] = im;
	}

	public void setQuick(int index, float[] value) {
		int idx = zero + index * stride;
		this.elements[idx] = value[0];
		this.elements[idx + 1] = value[1];
	}

	public void swap(FComplexMatrix1D other) {
		if (!(other instanceof DenseFComplexMatrix1D)) {
			super.swap(other);
		}
		DenseFComplexMatrix1D y = (DenseFComplexMatrix1D) other;
		if (y == this)
			return;
		checkSize(y);

		final float[] elemsOther = y.elements;
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
						float tmp;
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
			float tmp;
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

	public void toArray(float[] values) {
		if (values.length < 2 * size)
			throw new IllegalArgumentException("values too small");
		if (this.isNoView)
			System.arraycopy(this.elements, 0, values, 0, this.elements.length);
		else
			super.toArray(values);
	}

	public float[] zSum() {
		float[] sum = new float[2];
		if (this.elements == null)
			throw new InternalError();
		int np = Utils.getNP();
		if ((np > 1) && (size >= Utils.getThreadsBeginN_1D())) {
			Future[] futures = new Future[np];
			float[][] results = new float[np][2];
			int k = size / np;
			for (int j = 0; j < np; j++) {
				final int startidx = j * k;
				final int stopidx;
				if (j == np - 1) {
					stopidx = size;
				} else {
					stopidx = startidx + k;
				}

				futures[j] = Utils.threadPool.submit(new Callable<float[]>() {
					public float[] call() throws Exception {
						float[] sum = new float[2];
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
					results[j] = (float[]) futures[j].get();
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

	protected boolean haveSharedCellsRaw(FComplexMatrix1D other) {
		if (other instanceof SelectedDenseFComplexMatrix1D) {
			SelectedDenseFComplexMatrix1D otherMatrix = (SelectedDenseFComplexMatrix1D) other;
			return this.elements == otherMatrix.elements;
		} else if (other instanceof DenseFComplexMatrix1D) {
			DenseFComplexMatrix1D otherMatrix = (DenseFComplexMatrix1D) other;
			return this.elements == otherMatrix.elements;
		}
		return false;
	}

	protected int index(int rank) {
		return zero + rank * stride;
	}

	protected FComplexMatrix1D viewSelectionLike(int[] offsets) {
		return new SelectedDenseFComplexMatrix1D(this.elements, offsets);
	}
}
