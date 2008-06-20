/*
Copyright � 1999 CERN - European Organization for Nuclear Research.
Permission to use, copy, modify, distribute and sell this software and its documentation for any purpose 
is hereby granted without fee, provided that the above copyright notice appear in all copies and 
that both that copyright notice and this permission notice appear in supporting documentation. 
CERN makes no representations about the suitability of this software for any purpose. 
It is provided "as is" without expressed or implied warranty.
 */
package cern.colt.matrix.impl;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import cern.colt.Utils;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.matrix.DComplexMatrix2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import edu.emory.mathcs.jtransforms.dct.DoubleDCT_2D;
import edu.emory.mathcs.jtransforms.dst.DoubleDST_2D;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_2D;

/**
 * Dense 2-d matrix holding <tt>double</tt> elements. First see the <a
 * href="package-summary.html">package summary</a> and javadoc <a
 * href="package-tree.html">tree view</a> to get the broad picture.
 * <p>
 * <b>Implementation:</b>
 * <p>
 * Internally holds one single contigous one-dimensional array, addressed in row
 * major. Note that this implementation is not synchronized.
 * <p>
 * <b>Memory requirements:</b>
 * <p>
 * <tt>memory [bytes] = 8*rows()*columns()</tt>. Thus, a 1000*1000 matrix
 * uses 8 MB.
 * <p>
 * <b>Time complexity:</b>
 * <p>
 * <tt>O(1)</tt> (i.e. constant time) for the basic operations <tt>get</tt>,
 * <tt>getQuick</tt>, <tt>set</tt>, <tt>setQuick</tt> and <tt>size</tt>,
 * <p>
 * Cells are internally addressed in row-major. Applications demanding utmost
 * speed can exploit this fact. Setting/getting values in a loop row-by-row is
 * quicker than column-by-column. Thus
 * 
 * <pre>
 * for (int row = 0; row &lt; rows; row++) {
 * 	for (int column = 0; column &lt; columns; column++) {
 * 		matrix.setQuick(row, column, someValue);
 * 	}
 * }
 * </pre>
 * 
 * is quicker than
 * 
 * <pre>
 * for (int column = 0; column &lt; columns; column++) {
 * 	for (int row = 0; row &lt; rows; row++) {
 * 		matrix.setQuick(row, column, someValue);
 * 	}
 * }
 * </pre>
 * 
 * @author wolfgang.hoschek@cern.ch
 * @version 1.0, 09/24/99
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class DenseDoubleMatrix2D extends DoubleMatrix2D {
	static final long serialVersionUID = 1020177651L;

	private DoubleFFT_2D fft2;

	private DoubleDCT_2D dct2;

	private DoubleDST_2D dst2;

	/**
	 * The elements of this matrix. elements are stored in row major, i.e.
	 * index==row*columns + column columnOf(index)==index%columns
	 * rowOf(index)==index/columns i.e. {row0 column0..m}, {row1 column0..m},
	 * ..., {rown column0..m}
	 */
	protected double[] elements;

	/**
	 * Constructs a matrix with a copy of the given values. <tt>values</tt> is
	 * required to have the form <tt>values[row][column]</tt> and have exactly
	 * the same number of columns in every row.
	 * <p>
	 * The values are copied. So subsequent changes in <tt>values</tt> are not
	 * reflected in the matrix, and vice-versa.
	 * 
	 * @param values
	 *            The values to be filled into the new matrix.
	 * @throws IllegalArgumentException
	 *             if
	 *             <tt>for any 1 &lt;= row &lt; values.length: values[row].length != values[row-1].length</tt>.
	 */
	public DenseDoubleMatrix2D(double[][] values) {
		this(values.length, values.length == 0 ? 0 : values[0].length);
		assign(values);
	}

	/**
	 * Constructs a matrix with a given number of rows and columns. All entries
	 * are initially <tt>0</tt>.
	 * 
	 * @param rows
	 *            the number of rows the matrix shall have.
	 * @param columns
	 *            the number of columns the matrix shall have.
	 * @throws IllegalArgumentException
	 *             if
	 *             <tt>rows<0 || columns<0 || (double)columns*rows > Integer.MAX_VALUE</tt>.
	 */
	public DenseDoubleMatrix2D(int rows, int columns) {
		setUp(rows, columns);
		this.elements = new double[rows * columns];
	}

	/**
	 * Constructs a view with the given parameters.
	 * 
	 * @param rows
	 *            the number of rows the matrix shall have.
	 * @param columns
	 *            the number of columns the matrix shall have.
	 * @param elements
	 *            the cells.
	 * @param rowZero
	 *            the position of the first element.
	 * @param columnZero
	 *            the position of the first element.
	 * @param rowStride
	 *            the number of elements between two rows, i.e.
	 *            <tt>index(i+1,j)-index(i,j)</tt>.
	 * @param columnStride
	 *            the number of elements between two columns, i.e.
	 *            <tt>index(i,j+1)-index(i,j)</tt>.
	 * @throws IllegalArgumentException
	 *             if
	 *             <tt>rows<0 || columns<0 || (double)columns*rows > Integer.MAX_VALUE</tt>
	 *             or flip's are illegal.
	 */
	protected DenseDoubleMatrix2D(int rows, int columns, double[] elements, int rowZero, int columnZero, int rowStride, int columnStride) {
		setUp(rows, columns, rowZero, columnZero, rowStride, columnStride);
		this.elements = elements;
		this.isNoView = false;
	}

	public double aggregate(final cern.colt.function.DoubleDoubleFunction aggr, final cern.colt.function.DoubleFunction f) {
		if (size() == 0)
			return Double.NaN;
		final int zero = index(0, 0);
		double a = 0;
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			Double[] results = new Double[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Callable<Double>() {

					public Double call() throws Exception {
						double a = f.apply(elements[zero + startrow * rowStride]);
						int d = 1;
						for (int r = startrow; r < stoprow; r++) {
							for (int c = d; c < columns; c++) {
								a = aggr.apply(a, f.apply(elements[zero + r * rowStride + c * columnStride]));
							}
							d = 0;
						}
						return Double.valueOf(a);
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					results[j] = (Double) futures[j].get();
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
			a = f.apply(elements[zero]);
			int d = 1; // first cell already done
			for (int r = 0; r < rows; r++) {
				for (int c = d; c < columns; c++) {
					a = aggr.apply(a, f.apply(elements[zero + r * rowStride + c * columnStride]));
				}
				d = 0;
			}
		}
		return a;
	}

	public double aggregate(final cern.colt.function.DoubleDoubleFunction aggr, final cern.colt.function.DoubleFunction f, final cern.colt.function.DoubleProcedure cond) {
		if (size() == 0)
			return Double.NaN;
		final int zero = index(0, 0);
		double a = 0;
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			Double[] results = new Double[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Callable<Double>() {

					public Double call() throws Exception {
						double elem = elements[zero + startrow * rowStride];
						double a = 0;
						if (cond.apply(elem) == true) {
							a = f.apply(elem);
						}
						int d = 1;
						for (int r = startrow; r < stoprow; r++) {
							for (int c = d; c < columns; c++) {
								elem = elements[zero + r * rowStride + c * columnStride];
								if (cond.apply(elem) == true) {
									a = aggr.apply(a, f.apply(elem));
								}
							}
							d = 0;
						}
						return Double.valueOf(a);
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					results[j] = (Double) futures[j].get();
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
			double elem = elements[zero];
			if (cond.apply(elem) == true) {
				a = f.apply(elements[zero]);
			}
			int d = 1; // first cell already done
			for (int r = 0; r < rows; r++) {
				for (int c = d; c < columns; c++) {
					elem = elements[zero + r * rowStride + c * columnStride];
					if (cond.apply(elem) == true) {
						a = aggr.apply(a, f.apply(elem));
					}
				}
				d = 0;
			}
		}
		return a;
	}

	public double aggregate(final cern.colt.function.DoubleDoubleFunction aggr, final cern.colt.function.DoubleFunction f, Set<int[]> indexes) {
		if (size() == 0)
			return Double.NaN;
		final int zero = index(0, 0);
		int n = indexes.size();
		final int[][] indexesArray = indexes.toArray(new int[0][0]);
		double a = 0;
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			Double[] results = new Double[np];
			int k = n / np;
			for (int j = 0; j < np; j++) {
				final int startidx = j * k;
				final int stopidx;
				if (j == np - 1) {
					stopidx = n;
				} else {
					stopidx = startidx + k;
				}
				futures[j] = Utils.threadPool.submit(new Callable<Double>() {

					public Double call() throws Exception {
						double a = f.apply(getQuick(indexesArray[startidx][0], indexesArray[startidx][1]));
						double elem;
						for (int r = startidx + 1; r < stopidx; r++) {
							elem = elements[zero + indexesArray[r][0] * rowStride + indexesArray[r][1] * columnStride];
							a = aggr.apply(a, f.apply(elem));
						}
						return a;
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					results[j] = (Double) futures[j].get();
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
			double elem;
			a = f.apply(elements[zero + indexesArray[0][0] * rowStride + indexesArray[0][1] * columnStride]);
			for (int r = 1; r < n; r++) {
				elem = elements[zero + indexesArray[r][0] * rowStride + indexesArray[r][1] * columnStride];
				a = aggr.apply(a, f.apply(elem));
			}
		}
		return a;
	}

	public double aggregate(final DoubleMatrix2D other, final cern.colt.function.DoubleDoubleFunction aggr, final cern.colt.function.DoubleDoubleFunction f) {
		if (!(other instanceof DenseDoubleMatrix2D)) {
			return super.aggregate(other, aggr, f);
		}
		checkShape(other);
		if (size() == 0)
			return Double.NaN;
		final int zero = index(0, 0);
		final int zeroOther = other.index(0, 0);
		final int rowStrideOther = other.rowStride;
		final int colStrideOther = other.columnStride;
		final double[] elemsOther = (double[]) other.getElements();
		double a = 0;
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			Double[] results = new Double[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Callable<Double>() {

					public Double call() throws Exception {
						double a = f.apply(elements[zero + startrow * rowStride], elemsOther[zeroOther + startrow * rowStrideOther]);
						int d = 1;
						for (int r = startrow; r < stoprow; r++) {
							for (int c = d; c < columns; c++) {
								a = aggr.apply(a, f.apply(elements[zero + r * rowStride + c * columnStride], elemsOther[zeroOther + r * rowStrideOther + c * colStrideOther]));
							}
							d = 0;
						}
						return Double.valueOf(a);
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					results[j] = (Double) futures[j].get();
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
			int d = 1; // first cell already done
			a = f.apply(elements[zero], elemsOther[zeroOther]);
			for (int r = 0; r < rows; r++) {
				for (int c = d; c < columns; c++) {
					a = aggr.apply(a, f.apply(elements[zero + r * rowStride + c * columnStride], elemsOther[zeroOther + r * rowStrideOther + c * colStrideOther]));
				}
				d = 0;
			}
		}
		return a;
	}

	public DoubleMatrix2D assign(final cern.colt.function.DoubleFunction function) {
		final double[] elems = this.elements;
		if (elems == null)
			throw new InternalError();
		final int zero = index(0, 0);
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
			if (function instanceof cern.jet.math.DoubleMult) { // x[i] =
				// mult*x[i]
				double multiplicator = ((cern.jet.math.DoubleMult) function).multiplicator;
				if (multiplicator == 1)
					return this;
				if (multiplicator == 0)
					return assign(0);
			}
			Future[] futures = new Future[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {

					public void run() {
						int idx;
						// specialization for speed
						if (function instanceof cern.jet.math.DoubleMult) {
							// x[i] = mult*x[i]
							double multiplicator = ((cern.jet.math.DoubleMult) function).multiplicator;
							idx = zero + startrow * rowStride;
							for (int r = startrow; r < stoprow; r++) {
								for (int i = idx, c = 0; c < columns; c++) {
									elems[i] *= multiplicator;
									i += columnStride;
								}
								idx += rowStride;
							}
						} else {
							// the general case x[i] = f(x[i])
							idx = zero + startrow * rowStride;
							for (int r = startrow; r < stoprow; r++) {
								for (int i = idx, c = 0; c < columns; c++) {
									elems[i] = function.apply(elems[i]);
									i += columnStride;
								}
								idx += rowStride;
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
			int idx;
			// specialization for speed
			if (function instanceof cern.jet.math.DoubleMult) { // x[i] =
				// mult*x[i]
				double multiplicator = ((cern.jet.math.DoubleMult) function).multiplicator;
				if (multiplicator == 1)
					return this;
				if (multiplicator == 0)
					return assign(0);
				idx = zero;
				for (int r = 0; r < rows; r++) { // the general case
					for (int i = idx, c = 0; c < columns; c++) {
						elems[i] *= multiplicator;
						i += columnStride;
					}
					idx += rowStride;
				}
			} else { // the general case x[i] = f(x[i])
				idx = zero;
				for (int r = 0; r < rows; r++) {
					for (int i = idx, c = 0; c < columns; c++) {
						elems[i] = function.apply(elems[i]);
						i += columnStride;
					}
					idx += rowStride;
				}
			}
		}
		return this;
	}

	public DoubleMatrix2D assign(final cern.colt.function.DoubleProcedure cond, final cern.colt.function.DoubleFunction function) {
		final int zero = index(0, 0);
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {

					public void run() {
						double elem;
						int idx = zero + startrow * rowStride;
						for (int r = startrow; r < stoprow; r++) {
							for (int i = idx, c = 0; c < columns; c++) {
								elem = elements[i];
								if (cond.apply(elem) == true) {
									elements[i] = function.apply(elem);
								}
								i += columnStride;
							}
							idx += rowStride;
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
			double elem;
			int idx = zero;
			for (int r = 0; r < rows; r++) {
				for (int i = idx, c = 0; c < columns; c++) {
					elem = elements[i];
					if (cond.apply(elem) == true) {
						elements[i] = function.apply(elem);
					}
					i += columnStride;
				}
				idx += rowStride;
			}
		}
		return this;
	}

	public DoubleMatrix2D assign(final cern.colt.function.DoubleProcedure cond, final double value) {
		final int zero = index(0, 0);
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {

					public void run() {
						double elem;
						int idx = zero + startrow * rowStride;
						for (int r = startrow; r < stoprow; r++) {
							for (int i = idx, c = 0; c < columns; c++) {
								elem = elements[i];
								if (cond.apply(elem) == true) {
									elements[i] = value;
								}
								i += columnStride;
							}
							idx += rowStride;
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
			double elem;
			int idx = zero;
			for (int r = 0; r < rows; r++) {
				for (int i = idx, c = 0; c < columns; c++) {
					elem = elements[i];
					if (cond.apply(elem) == true) {
						elements[i] = value;
					}
					i += columnStride;
				}
				idx += rowStride;
			}
		}
		return this;
	}

	public DoubleMatrix2D assign(final double value) {
		final double[] elems = this.elements;
		final int zero = index(0, 0);
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						int idx = zero + startrow * rowStride;
						for (int r = startrow; r < stoprow; r++) {
							for (int i = idx, c = 0; c < columns; c++) {
								elems[i] = value;
								i += columnStride;
							}
							idx += rowStride;
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
			for (int r = 0; r < rows; r++) {
				for (int i = idx, c = 0; c < columns; c++) {
					elems[i] = value;
					i += columnStride;
				}
				idx += rowStride;
			}
		}
		return this;
	}

	public DoubleMatrix2D assign(final double[] values) {
		if (values.length != size())
			throw new IllegalArgumentException("Must have same length: length=" + values.length + "rows()*columns()=" + rows() * columns());
		int np = Utils.getNP();
		if (this.isNoView) {
			int size = elements.length;
			if ((np > 1) && (size >= Utils.getThreadsBeginN_2D())) {
				Future[] futures = new Future[np];
				int k = size / np;
				for (int j = 0; j < np; j++) {
					final int startidx = j * k;
					final int length;
					if (j == np - 1) {
						length = size - startidx;
					} else {
						length = k;
					}
					futures[j] = Utils.threadPool.submit(new Runnable() {
						public void run() {
							System.arraycopy(values, startidx, elements, startidx, length);
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
				System.arraycopy(values, 0, this.elements, 0, values.length);
			}
		} else {
			final int zero = index(0, 0);
			if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
				Future[] futures = new Future[np];
				int k = rows / np;
				for (int j = 0; j < np; j++) {
					final int startrow = j * k;
					final int stoprow;
					final int glob_idxOther = j * k * columns;
					if (j == np - 1) {
						stoprow = rows;
					} else {
						stoprow = startrow + k;
					}
					futures[j] = Utils.threadPool.submit(new Runnable() {

						public void run() {
							int idxOther = glob_idxOther;
							int idx = zero + startrow * rowStride;
							for (int r = startrow; r < stoprow; r++) {
								for (int i = idx, c = 0; c < columns; c++) {
									elements[i] = values[idxOther++];
									i += columnStride;
								}
								idx += rowStride;
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

				int idxOther = 0;
				int idx = zero;
				for (int r = 0; r < rows; r++) {
					for (int i = idx, c = 0; c < columns; c++) {
						elements[i] = values[idxOther++];
						i += columnStride;
					}
					idx += rowStride;
				}
			}
		}
		return this;
	}

	public DoubleMatrix2D assign(final double[][] values) {
		if (values.length != rows)
			throw new IllegalArgumentException("Must have same number of rows: rows=" + values.length + "rows()=" + rows());
		int np = Utils.getNP();
		if (this.isNoView) {
			if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
				Future[] futures = new Future[np];
				int k = rows / np;
				for (int j = 0; j < np; j++) {
					final int startrow = j * k;
					final int stoprow;
					if (j == np - 1) {
						stoprow = rows;
					} else {
						stoprow = startrow + k;
					}
					futures[j] = Utils.threadPool.submit(new Runnable() {
						public void run() {
							int i = startrow * rowStride;
							for (int r = startrow; r < stoprow; r++) {
								double[] currentRow = values[r];
								if (currentRow.length != columns)
									throw new IllegalArgumentException("Must have same number of columns in every row: columns=" + currentRow.length + "columns()=" + columns());
								System.arraycopy(currentRow, 0, elements, i, columns);
								i += columns;
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
				int i = 0;
				for (int r = 0; r < rows; r++) {
					double[] currentRow = values[r];
					if (currentRow.length != columns)
						throw new IllegalArgumentException("Must have same number of columns in every row: columns=" + currentRow.length + "columns()=" + columns());
					System.arraycopy(currentRow, 0, this.elements, i, columns);
					i += columns;
				}
			}
		} else {
			final int zero = index(0, 0);
			if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
				Future[] futures = new Future[np];
				int k = rows / np;
				for (int j = 0; j < np; j++) {
					final int startrow = j * k;
					final int stoprow;
					if (j == np - 1) {
						stoprow = rows;
					} else {
						stoprow = startrow + k;
					}
					futures[j] = Utils.threadPool.submit(new Runnable() {

						public void run() {
							int idx = zero + startrow * rowStride;
							for (int r = startrow; r < stoprow; r++) {
								double[] currentRow = values[r];
								if (currentRow.length != columns)
									throw new IllegalArgumentException("Must have same number of columns in every row: columns=" + currentRow.length + "columns()=" + columns());
								for (int i = idx, c = 0; c < columns; c++) {
									elements[i] = currentRow[c];
									i += columnStride;
								}
								idx += rowStride;
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
				for (int r = 0; r < rows; r++) {
					double[] currentRow = values[r];
					if (currentRow.length != columns)
						throw new IllegalArgumentException("Must have same number of columns in every row: columns=" + currentRow.length + "columns()=" + columns());
					for (int i = idx, c = 0; c < columns; c++) {
						elements[i] = currentRow[c];
						i += columnStride;
					}
					idx += rowStride;
				}
			}
			return this;
		}
		return this;
	}

	public DoubleMatrix2D assign(final DoubleMatrix2D source) {
		// overriden for performance only
		if (!(source instanceof DenseDoubleMatrix2D)) {
			super.assign(source);
			return this;
		}
		final DenseDoubleMatrix2D other_final = (DenseDoubleMatrix2D) source;
		if (other_final == this)
			return this; // nothing to do
		checkShape(other_final);
		int np = Utils.getNP();
		if (this.isNoView && other_final.isNoView) { // quickest
			int size = elements.length;
			if ((np > 1) && (size >= Utils.getThreadsBeginN_2D())) {
				Future[] futures = new Future[np];
				int k = size / np;
				for (int j = 0; j < np; j++) {
					final int startidx = j * k;
					final int length;
					if (j == np - 1) {
						length = size - startidx;
					} else {
						length = k;
					}
					futures[j] = Utils.threadPool.submit(new Runnable() {
						public void run() {
							System.arraycopy(other_final.elements, startidx, elements, startidx, length);
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
				System.arraycopy(other_final.elements, 0, this.elements, 0, this.elements.length);
			}
			return this;
		}
		DenseDoubleMatrix2D other = (DenseDoubleMatrix2D) source;
		if (haveSharedCells(other)) {
			DoubleMatrix2D c = other.copy();
			if (!(c instanceof DenseDoubleMatrix2D)) { // should not happen
				super.assign(other);
				return this;
			}
			other = (DenseDoubleMatrix2D) c;
		}

		final double[] elemsOther = other.elements;
		if (elements == null || elemsOther == null)
			throw new InternalError();
		final int zeroOther = other.index(0, 0);
		final int zero = index(0, 0);
		final int columnStrideOther = other.columnStride;
		final int rowStrideOther = other.rowStride;
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						int idx = zero + startrow * rowStride;
						int idxOther = zeroOther + startrow * rowStrideOther;
						for (int r = startrow; r < stoprow; r++) {
							for (int i = idx, j = idxOther, c = 0; c < columns; c++) {
								elements[i] = elemsOther[j];
								i += columnStride;
								j += columnStrideOther;
							}
							idx += rowStride;
							idxOther += rowStrideOther;
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
			for (int r = 0; r < rows; r++) {
				for (int i = idx, j = idxOther, c = 0; c < columns; c++) {
					elements[i] = elemsOther[j];
					i += columnStride;
					j += columnStrideOther;
				}
				idx += rowStride;
				idxOther += rowStrideOther;
			}
		}
		return this;
	}

	public DoubleMatrix2D assign(final DoubleMatrix2D y, final cern.colt.function.DoubleDoubleFunction function) {
		// overriden for performance only
		if (!(y instanceof DenseDoubleMatrix2D)) {
			super.assign(y, function);
			return this;
		}
		DenseDoubleMatrix2D other = (DenseDoubleMatrix2D) y;
		checkShape(y);
		final double[] elemsOther = other.elements;
		if (elements == null || elemsOther == null)
			throw new InternalError();
		final int zeroOther = other.index(0, 0);
		final int zero = index(0, 0);
		final int columnStrideOther = other.columnStride;
		final int rowStrideOther = other.rowStride;
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
			if (function instanceof cern.jet.math.DoublePlusMult) {
				double multiplicator = ((cern.jet.math.DoublePlusMult) function).multiplicator;
				if (multiplicator == 0) { // x[i] = x[i] + 0*y[i]
					return this;
				}
			}
			Future[] futures = new Future[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {

					public void run() {
						int idx;
						int idxOther;
						// specialized for speed
						if (function == cern.jet.math.DoubleFunctions.mult) {
							// x[i] = x[i]*y[i]
							idx = zero + startrow * rowStride;
							idxOther = zeroOther + startrow * rowStrideOther;
							for (int r = startrow; r < stoprow; r++) {
								for (int i = idx, j = idxOther, c = 0; c < columns; c++) {
									elements[i] *= elemsOther[j];
									i += columnStride;
									j += columnStrideOther;
								}
								idx += rowStride;
								idxOther += rowStrideOther;
							}
						} else if (function == cern.jet.math.DoubleFunctions.div) {
							// x[i] = x[i] / y[i]
							idx = zero + startrow * rowStride;
							idxOther = zeroOther + startrow * rowStrideOther;
							for (int r = startrow; r < stoprow; r++) {
								for (int i = idx, j = idxOther, c = 0; c < columns; c++) {
									elements[i] /= elemsOther[j];
									i += columnStride;
									j += columnStrideOther;
								}
								idx += rowStride;
								idxOther += rowStrideOther;
							}
						} else if (function instanceof cern.jet.math.DoublePlusMult) {
							double multiplicator = ((cern.jet.math.DoublePlusMult) function).multiplicator;
							if (multiplicator == 1) {
								// x[i] = x[i] + y[i]
								idx = zero + startrow * rowStride;
								idxOther = zeroOther + startrow * rowStrideOther;
								for (int r = startrow; r < stoprow; r++) {
									for (int i = idx, j = idxOther, c = 0; c < columns; c++) {
										elements[i] += elemsOther[j];
										i += columnStride;
										j += columnStrideOther;
									}
									idx += rowStride;
									idxOther += rowStrideOther;
								}
							} else if (multiplicator == -1) {
								// x[i] = x[i] - y[i]
								idx = zero + startrow * rowStride;
								idxOther = zeroOther + startrow * rowStrideOther;
								for (int r = startrow; r < stoprow; r++) {
									for (int i = idx, j = idxOther, c = 0; c < columns; c++) {
										elements[i] -= elemsOther[j];
										i += columnStride;
										j += columnStrideOther;
									}
									idx += rowStride;
									idxOther += rowStrideOther;
								}
							} else { // the general case
								// x[i] = x[i] + mult*y[i]
								idx = zero + startrow * rowStride;
								idxOther = zeroOther + startrow * rowStrideOther;
								for (int r = startrow; r < stoprow; r++) {
									for (int i = idx, j = idxOther, c = 0; c < columns; c++) {
										elements[i] += multiplicator * elemsOther[j];
										i += columnStride;
										j += columnStrideOther;
									}
									idx += rowStride;
									idxOther += rowStrideOther;
								}
							}
						} else { // the general case x[i] = f(x[i],y[i])
							idx = zero + startrow * rowStride;
							idxOther = zeroOther + startrow * rowStrideOther;
							for (int r = startrow; r < stoprow; r++) {
								for (int i = idx, j = idxOther, c = 0; c < columns; c++) {
									elements[i] = function.apply(elements[i], elemsOther[j]);
									i += columnStride;
									j += columnStrideOther;
								}
								idx += rowStride;
								idxOther += rowStrideOther;
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
			int idx;
			int idxOther;
			// specialized for speed
			if (function == cern.jet.math.DoubleFunctions.mult) {
				// x[i] = x[i] * y[i]
				idx = zero;
				idxOther = zeroOther;
				for (int r = 0; r < rows; r++) {
					for (int i = idx, j = idxOther, c = 0; c < columns; c++) {
						elements[i] *= elemsOther[j];
						i += columnStride;
						j += columnStrideOther;
					}
					idx += rowStride;
					idxOther += rowStrideOther;
				}
			} else if (function == cern.jet.math.DoubleFunctions.div) {
				// x[i] = x[i] / y[i]
				idx = zero;
				idxOther = zeroOther;
				for (int r = 0; r < rows; r++) {
					for (int i = idx, j = idxOther, c = 0; c < columns; c++) {
						elements[i] /= elemsOther[j];
						i += columnStride;
						j += columnStrideOther;
					}
					idx += rowStride;
					idxOther += rowStrideOther;
				}
			} else if (function instanceof cern.jet.math.DoublePlusMult) {
				double multiplicator = ((cern.jet.math.DoublePlusMult) function).multiplicator;
				if (multiplicator == 0) { // x[i] = x[i] + 0*y[i]
					return this;
				} else if (multiplicator == 1) { // x[i] = x[i] + y[i]
					idx = zero;
					idxOther = zeroOther;
					for (int r = 0; r < rows; r++) {
						for (int i = idx, j = idxOther, c = 0; c < columns; c++) {
							elements[i] += elemsOther[j];
							i += columnStride;
							j += columnStrideOther;
						}
						idx += rowStride;
						idxOther += rowStrideOther;
					}

				} else if (multiplicator == -1) { // x[i] = x[i] - y[i]
					idx = zero;
					idxOther = zeroOther;
					for (int r = 0; r < rows; r++) {
						for (int i = idx, j = idxOther, c = 0; c < columns; c++) {
							elements[i] -= elemsOther[j];
							i += columnStride;
							j += columnStrideOther;
						}
						idx += rowStride;
						idxOther += rowStrideOther;
					}
				} else { // the general case
					// x[i] = x[i] + mult*y[i]
					idx = zero;
					idxOther = zeroOther;
					for (int r = 0; r < rows; r++) {
						for (int i = idx, j = idxOther, c = 0; c < columns; c++) {
							elements[i] += multiplicator * elemsOther[j];
							i += columnStride;
							j += columnStrideOther;
						}
						idx += rowStride;
						idxOther += rowStrideOther;
					}
				}
			} else { // the general case x[i] = f(x[i],y[i])
				idx = zero;
				idxOther = zeroOther;
				for (int r = 0; r < rows; r++) {
					for (int i = idx, j = idxOther, c = 0; c < columns; c++) {
						elements[i] = function.apply(elements[i], elemsOther[j]);
						i += columnStride;
						j += columnStrideOther;
					}
					idx += rowStride;
					idxOther += rowStrideOther;
				}
			}
		}
		return this;
	}

	public DoubleMatrix2D assign(final float[] values) {
		if (values.length != size())
			throw new IllegalArgumentException("Must have same length: length=" + values.length + "rows()*columns()=" + rows() * columns());
		final int zero = index(0, 0);
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				final int startidx = j * k * columns;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {

					public void run() {
						int idx = zero + startrow * rowStride;
						int idxOther = startidx;
						for (int r = startrow; r < stoprow; r++) {
							for (int i = idx, c = 0; c < columns; c++) {
								elements[i] = values[idxOther++];
								i += columnStride;
							}
							idx += rowStride;
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
			int idxOther = 0;
			int idx = zero;
			for (int r = 0; r < rows; r++) {
				for (int i = idx, c = 0; c < columns; c++) {
					elements[i] = values[idxOther++];
					i += columnStride;
				}
				idx += rowStride;
			}
		}
		return this;
	}

	public int cardinality() {
		int cardinality = 0;
		int np = Utils.getNP();
		final int zero = index(0, 0);
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			Integer[] results = new Integer[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Callable<Integer>() {
					public Integer call() throws Exception {
						int cardinality = 0;
						int idx = zero + startrow * rowStride;
						for (int r = startrow; r < stoprow; r++) {
							for (int i = idx, c = 0; c < columns; c++) {
								if (elements[i] != 0)
									cardinality++;
								i += columnStride;
							}
							idx += rowStride;
						}
						return Integer.valueOf(cardinality);
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					results[j] = (Integer) futures[j].get();
				}
				cardinality = results[0].intValue();
				for (int j = 1; j < np; j++) {
					cardinality += results[j].intValue();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			int idx = zero;
			for (int r = 0; r < rows; r++) {
				for (int i = idx, c = 0; c < columns; c++) {
					if (elements[i] != 0)
						cardinality++;
					i += columnStride;
				}
				idx += rowStride;
			}
		}
		return cardinality;
	}

	public void dct2(boolean scale) {
		if (dct2 == null) {
			dct2 = new DoubleDCT_2D(rows, columns);
		}
		dct2.forward(elements, scale);

	}

	public void dctColumns(boolean scale) {
		DoubleMatrix1D column;
		for (int c = 0; c < columns; c++) {
			column = viewColumn(c).copy();
			column.dct(scale);
			viewColumn(c).assign(column);
		}
	}

	public void dctRows(boolean scale) {
		DoubleMatrix1D row;
		for (int r = 0; r < rows; r++) {
			row = viewRow(r).copy();
			row.dct(scale);
			viewRow(r).assign(row);
		}
	}

	public void dst2(boolean scale) {
		if (dst2 == null) {
			dst2 = new DoubleDST_2D(rows, columns);
		}
		dst2.forward(elements, scale);

	}

	public void dstColumns(boolean scale) {
		DoubleMatrix1D column;
		for (int c = 0; c < columns; c++) {
			column = viewColumn(c).copy();
			column.dst(scale);
			viewColumn(c).assign(column);
		}
	}

	public void dstRows(boolean scale) {
		DoubleMatrix1D row;
		for (int r = 0; r < rows; r++) {
			row = viewRow(r).copy();
			row.dst(scale);
			viewRow(r).assign(row);
		}
	}

	public DoubleMatrix2D forEachNonZero(final cern.colt.function.IntIntDoubleFunction function) {
		final int zero = index(0, 0);
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						int idx = zero + startrow * rowStride;
						for (int r = startrow; r < stoprow; r++) {
							for (int i = idx, c = 0; c < columns; c++) {
								double value = elements[i];
								if (value != 0) {
									elements[i] = function.apply(r, c, value);
								}
								i += columnStride;
							}
							idx += rowStride;
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
			for (int r = 0; r < rows; r++) {
				for (int i = idx, c = 0; c < columns; c++) {
					double value = elements[i];
					if (value != 0) {
						elements[i] = function.apply(r, c, value);
					}
					i += columnStride;
				}
				idx += rowStride;
			}
		}
		return this;
	}

	public double[] getElements() {
		return elements;
	}

	public DComplexMatrix2D getFft2() {
		DComplexMatrix2D C = new DenseDComplexMatrix2D(rows, columns);
		final double[] cElems = (double[]) ((DenseDComplexMatrix2D) C).getElements();
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {

					public void run() {
						for (int r = startrow; r < stoprow; r++) {
							System.arraycopy(elements, r * columns, cElems, r * columns, columns);
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
			for (int r = 0; r < rows; r++) {
				System.arraycopy(elements, r * columns, cElems, r * columns, columns);
			}
		}
		if (fft2 == null) {
			fft2 = new DoubleFFT_2D(rows, columns);
		}
		fft2.realForwardFull(cElems);
		return C;
	}

	public DComplexMatrix2D getFftColumns() {
		DComplexMatrix2D C = new DenseDComplexMatrix2D(rows, columns);
		DoubleMatrix1D column;
		for (int c = 0; c < columns; c++) {
			column = viewColumn(c).copy();
			C.viewColumn(c).assign(column.getFft());
		}
		return C;
	}

	public DComplexMatrix2D getFftRows() {
		DComplexMatrix2D C = new DenseDComplexMatrix2D(rows, columns);
		DoubleMatrix1D row;
		for (int r = 0; r < rows; r++) {
			row = viewRow(r).copy();
			C.viewRow(r).assign(row.getFft());
		}
		return C;
	}

	public DComplexMatrix2D getIfft2(boolean scale) {
		DComplexMatrix2D C = new DenseDComplexMatrix2D(rows, columns);
		final double[] cElems = (double[]) ((DenseDComplexMatrix2D) C).getElements();
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {

					public void run() {
						for (int r = startrow; r < stoprow; r++) {
							System.arraycopy(elements, r * columns, cElems, r * columns, columns);
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
			for (int r = 0; r < rows; r++) {
				System.arraycopy(elements, r * columns, cElems, r * columns, columns);
			}
		}
		if (fft2 == null) {
			fft2 = new DoubleFFT_2D(rows, columns);
		}
		fft2.realInverseFull(cElems, scale);
		return C;
	}

	public DComplexMatrix2D getIfftColumns(boolean scale) {
		DComplexMatrix2D C = new DenseDComplexMatrix2D(rows, columns);
		DoubleMatrix1D column;
		for (int c = 0; c < columns; c++) {
			column = viewColumn(c).copy();
			C.viewColumn(c).assign(column.getIfft(scale));
		}
		return C;
	}

	public DComplexMatrix2D getIfftRows(boolean scale) {
		DComplexMatrix2D C = new DenseDComplexMatrix2D(rows, columns);
		DoubleMatrix1D row;
		for (int r = 0; r < rows; r++) {
			row = viewRow(r).copy();
			C.viewRow(r).assign(row.getIfft(scale));
		}
		return C;
	}

	public double[] getMaxLocation() {
		int rowLocation = 0;
		int columnLocation = 0;
		final int zero = index(0, 0);
		double maxValue = 0;
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			double[][] results = new double[np][2];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Callable<double[]>() {
					public double[] call() throws Exception {
						double maxValue = elements[zero + startrow * rowStride];
						int rowLocation = startrow;
						int colLocation = 0;
						double elem;
						int d = 1;
						for (int r = startrow; r < stoprow; r++) {
							for (int c = d; c < columns; c++) {
								elem = elements[zero + r * rowStride + c * columnStride];
								if (maxValue < elem) {
									maxValue = elem;
									rowLocation = r;
									colLocation = c;
								}
							}
							d = 0;
						}
						return new double[] { maxValue, rowLocation, colLocation };
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					results[j] = (double[]) futures[j].get();
				}
				maxValue = results[0][0];
				rowLocation = (int) results[0][1];
				columnLocation = (int) results[0][2];
				for (int j = 1; j < np; j++) {
					if (maxValue < results[j][0]) {
						maxValue = results[j][0];
						rowLocation = (int) results[j][1];
						columnLocation = (int) results[j][2];
					}
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			maxValue = elements[zero];
			int d = 1;
			double elem;
			for (int r = 0; r < rows; r++) {
				for (int c = d; c < columns; c++) {
					elem = elements[zero + r * rowStride + c * columnStride];
					if (maxValue < elem) {
						maxValue = elem;
						rowLocation = r;
						columnLocation = c;
					}
				}
				d = 0;
			}
		}
		return new double[] { maxValue, rowLocation, columnLocation };
	}

	public double[] getMinLocation() {
		int rowLocation = 0;
		int columnLocation = 0;
		final int zero = index(0, 0);
		double minValue = 0;
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			double[][] results = new double[np][2];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Callable<double[]>() {
					public double[] call() throws Exception {
						int rowLocation = startrow;
						int columnLocation = 0;
						double minValue = elements[zero + startrow * rowStride];
						double elem;
						int d = 1;
						for (int r = startrow; r < stoprow; r++) {
							for (int c = d; c < columns; c++) {
								elem = elements[zero + r * rowStride + c * columnStride];
								if (minValue > elem) {
									minValue = elem;
									rowLocation = r;
									columnLocation = c;
								}
							}
							d = 0;
						}
						return new double[] { minValue, rowLocation, columnLocation };
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					results[j] = (double[]) futures[j].get();
				}
				minValue = results[0][0];
				rowLocation = (int) results[0][1];
				columnLocation = (int) results[0][2];
				for (int j = 1; j < np; j++) {
					if (minValue > results[j][0]) {
						minValue = results[j][0];
						rowLocation = (int) results[j][1];
						columnLocation = (int) results[j][2];
					}
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			minValue = elements[zero];
			int d = 1;
			double elem;
			for (int r = 0; r < rows; r++) {
				for (int c = d; c < columns; c++) {
					elem = elements[zero + r * rowStride + c * columnStride];
					if (minValue > elem) {
						minValue = elem;
						rowLocation = r;
						columnLocation = c;
					}
				}
				d = 0;
			}
		}
		return new double[] { minValue, rowLocation, columnLocation };
	}

	public ConcurrentHashMap<int[], Double> getNegativeValues() {
		final ConcurrentHashMap<int[], Double> result = new ConcurrentHashMap<int[], Double>();
		final int zero = index(0, 0);
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {

					public void run() {
						int idx = zero + startrow * rowStride;
						for (int r = startrow; r < stoprow; r++) {
							for (int i = idx, c = 0; c < columns; c++) {
								double value = elements[i];
								if (value < 0) {
									result.put(new int[] { r, c }, value);
								}
								i += columnStride;
							}
							idx += rowStride;
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
			for (int r = 0; r < rows; r++) {
				for (int i = idx, c = 0; c < columns; c++) {
					double value = elements[i];
					if (value < 0) {
						result.put(new int[] { r, c }, value);
					}
					i += columnStride;
				}
				idx += rowStride;
			}
		}
		return result;
	}

	public void getNonZeros(IntArrayList rowList, IntArrayList columnList, DoubleArrayList valueList) {
		rowList.clear();
		columnList.clear();
		valueList.clear();
		int zero = index(0, 0);
		int idx = zero;
		for (int r = 0; r < rows; r++) {
			for (int i = idx, c = 0; c < columns; c++) {
				double value = elements[i];
				if (value != 0) {
					rowList.add(r);
					columnList.add(c);
					valueList.add(value);
				}
				i += columnStride;
			}
			idx += rowStride;
		}
	}

	public ConcurrentHashMap<int[], Double> getPositiveValues() {
		final ConcurrentHashMap<int[], Double> result = new ConcurrentHashMap<int[], Double>();
		final int zero = index(0, 0);
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {

					public void run() {
						int idx = zero + startrow * rowStride;
						for (int r = startrow; r < stoprow; r++) {
							for (int i = idx, c = 0; c < columns; c++) {
								double value = elements[i];
								if (value > 0) {
									result.put(new int[] { r, c }, value);
								}
								i += columnStride;
							}
							idx += rowStride;
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
			for (int r = 0; r < rows; r++) {
				for (int i = idx, c = 0; c < columns; c++) {
					double value = elements[i];
					if (value > 0) {
						result.put(new int[] { r, c }, value);
					}
					i += columnStride;
				}
				idx += rowStride;
			}
		}
		return result;
	}

	public double getQuick(int row, int column) {
		return elements[rowZero + row * rowStride + columnZero + column * columnStride];
	}

	public ConcurrentHashMap<int[], Double> getValuesSuchThat(final cern.colt.function.DoubleProcedure cond) {
		final ConcurrentHashMap<int[], Double> result = new ConcurrentHashMap<int[], Double>();
		final int zero = index(0, 0);
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {

					public void run() {
						int idx = zero + startrow * rowStride;
						for (int r = startrow; r < stoprow; r++) {
							for (int i = idx, c = 0; c < columns; c++) {
								double value = elements[i];
								if (cond.apply(value) == true) {
									result.put(new int[] { r, c }, value);
								}
								i += columnStride;
							}
							idx += rowStride;
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
			for (int r = 0; r < rows; r++) {
				for (int i = idx, c = 0; c < columns; c++) {
					double value = elements[i];
					if (cond.apply(value) == true) {
						result.put(new int[] { r, c }, value);
					}
					i += columnStride;
				}
				idx += rowStride;
			}
		}
		return result;
	}

	public void idct2(boolean scale) {
		if (dct2 == null) {
			dct2 = new DoubleDCT_2D(rows, columns);
		}
		dct2.inverse(elements, scale);

	}

	public void idctColumns(boolean scale) {
		DoubleMatrix1D column;
		for (int c = 0; c < columns; c++) {
			column = viewColumn(c).copy();
			column.idct(scale);
			viewColumn(c).assign(column);
		}
	}

	public void idctRows(boolean scale) {
		DoubleMatrix1D row;
		for (int r = 0; r < rows; r++) {
			row = viewRow(r).copy();
			row.idct(scale);
			viewRow(r).assign(row);
		}
	}

	public void idst2(boolean scale) {
		if (dst2 == null) {
			dst2 = new DoubleDST_2D(rows, columns);
		}
		dst2.inverse(elements, scale);

	}

	public void idstColumns(boolean scale) {
		DoubleMatrix1D column;
		for (int c = 0; c < columns; c++) {
			column = viewColumn(c).copy();
			column.idst(scale);
			viewColumn(c).assign(column);
		}
	}

	public void idstRows(boolean scale) {
		DoubleMatrix1D row;
		for (int r = 0; r < rows; r++) {
			row = viewRow(r).copy();
			row.idst(scale);
			viewRow(r).assign(row);
		}
	}

	public DoubleMatrix2D like(int rows, int columns) {
		return new DenseDoubleMatrix2D(rows, columns);
	}

	public DoubleMatrix1D like1D(int size) {
		return new DenseDoubleMatrix1D(size);
	}

	public void setQuick(int row, int column, double value) {
		elements[rowZero + row * rowStride + columnZero + column * columnStride] = value;
	}

	public double[][] toArray() {
		final double[][] values = new double[rows][columns];
		int np = Utils.getNP();
		final int zero = index(0, 0);
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						int idx = zero + startrow * rowStride;
						for (int r = startrow; r < stoprow; r++) {
							double[] currentRow = values[r];
							for (int i = idx, c = 0; c < columns; c++) {
								currentRow[c] = elements[i];
								i += columnStride;
							}
							idx += rowStride;
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
			for (int r = 0; r < rows; r++) {
				double[] currentRow = values[r];
				for (int i = idx, c = 0; c < columns; c++) {
					currentRow[c] = elements[i];
					i += columnStride;
				}
				idx += rowStride;
			}
		}
		return values;
	}

	public DoubleMatrix1D vectorize() {
		final DenseDoubleMatrix1D v = new DenseDoubleMatrix1D(size());
		final int zero = index(0, 0);
		final int zeroOther = v.index(0);
		final int strideOther = v.stride;
		final double[] elemsOther = (double[]) v.getElements();
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			int k = columns / np;
			for (int j = 0; j < np; j++) {
				final int startcol = j * k;
				final int stopcol;
				final int startidx = j * k * rows;
				if (j == np - 1) {
					stopcol = columns;
				} else {
					stopcol = startcol + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {

					public void run() {
						int idx = 0;
						int idxOther = zeroOther + startidx * strideOther;
						for (int c = startcol; c < stopcol; c++) {
							idx = zero + c * columnStride;
							for (int r = 0; r < rows; r++) {
								elemsOther[idxOther] = elements[idx];
								idx += rowStride;
								idxOther += strideOther;
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
			int idx = zero;
			int idxOther = zeroOther;
			for (int c = 0; c < columns; c++) {
				idx = zero + c * columnStride;
				for (int r = 0; r < rows; r++) {
					elemsOther[idxOther] = elements[idx];
					idx += rowStride;
					idxOther += strideOther;
				}
			}
		}
		// }
		return v;
	}

	public void zAssign8Neighbors(DoubleMatrix2D B, cern.colt.function.Double9Function function) {
		// 1. using only 4-5 out of the 9 cells in "function" is *not* the
		// limiting factor for performance.

		// 2. if the "function" would be hardwired into the innermost loop, a
		// speedup of 1.5-2.0 would be seen
		// but then the multi-purpose interface is gone...

		if (!(B instanceof DenseDoubleMatrix2D)) {
			super.zAssign8Neighbors(B, function);
			return;
		}
		if (function == null)
			throw new NullPointerException("function must not be null.");
		checkShape(B);
		int r = rows - 1;
		int c = columns - 1;
		if (rows < 3 || columns < 3)
			return; // nothing to do

		DenseDoubleMatrix2D BB = (DenseDoubleMatrix2D) B;
		int A_rs = rowStride;
		int B_rs = BB.rowStride;
		int A_cs = columnStride;
		int B_cs = BB.columnStride;
		double[] elems = this.elements;
		double[] B_elems = BB.elements;
		if (elems == null || B_elems == null)
			throw new InternalError();

		int A_index = index(1, 1);
		int B_index = BB.index(1, 1);
		for (int i = 1; i < r; i++) {
			double a00, a01, a02;
			double a10, a11, a12;
			double a20, a21, a22;

			int B11 = B_index;

			int A02 = A_index - A_rs - A_cs;
			int A12 = A02 + A_rs;
			int A22 = A12 + A_rs;

			// in each step six cells can be remembered in registers - they
			// don't need to be reread from slow memory
			a00 = elems[A02];
			A02 += A_cs;
			a01 = elems[A02]; // A02+=A_cs;
			a10 = elems[A12];
			A12 += A_cs;
			a11 = elems[A12]; // A12+=A_cs;
			a20 = elems[A22];
			A22 += A_cs;
			a21 = elems[A22]; // A22+=A_cs;

			for (int j = 1; j < c; j++) {
				// in each step 3 instead of 9 cells need to be read from
				// memory.
				a02 = elems[A02 += A_cs];
				a12 = elems[A12 += A_cs];
				a22 = elems[A22 += A_cs];

				B_elems[B11] = function.apply(a00, a01, a02, a10, a11, a12, a20, a21, a22);
				B11 += B_cs;

				// move remembered cells
				a00 = a01;
				a01 = a02;
				a10 = a11;
				a11 = a12;
				a20 = a21;
				a21 = a22;
			}
			A_index += A_rs;
			B_index += B_rs;
		}

	}

	public DoubleMatrix1D zMult(final DoubleMatrix1D y, DoubleMatrix1D z, final double alpha, final double beta, final boolean transposeA) {
		final DoubleMatrix1D z_loc;
		if (z == null) {
			z_loc = new DenseDoubleMatrix1D(this.rows);
		} else {
			z_loc = z;
		}
		if (transposeA)
			return viewDice().zMult(y, z_loc, alpha, beta, false);
		if (!(y instanceof DenseDoubleMatrix1D && z_loc instanceof DenseDoubleMatrix1D))
			return super.zMult(y, z_loc, alpha, beta, transposeA);

		if (columns != y.size || rows > z_loc.size)
			throw new IllegalArgumentException("Incompatible args: " + toStringShort() + ", " + y.toStringShort() + ", " + z_loc.toStringShort());

		final double[] elemsY = (double[]) y.getElements();
		final double[] elemsZ = (double[]) z_loc.getElements();
		if (elements == null || elemsY == null || elemsZ == null)
			throw new InternalError();
		final int strideY = y.stride;
		final int strideZ = z_loc.stride;
		final int zero = index(0, 0);
		final int zeroY = y.index(0);
		final int zeroZ = z_loc.index(0);
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						int idxZero = zero + startrow * rowStride;
						int idxZeroZ = zeroZ + startrow * strideZ;
						for (int r = startrow; r < stoprow; r++) {
							double sum = 0;
							int idx = idxZero;
							int idxY = zeroY;
							for (int c = 0; c < columns; c++) {
								sum += elements[idx] * elemsY[idxY];
								idx += columnStride;
								idxY += strideY;
							}
							elemsZ[idxZeroZ] = alpha * sum + beta * elemsZ[idxZeroZ];
							idxZero += rowStride;
							idxZeroZ += strideZ;
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
			int idxZero = zero;
			int idxZeroZ = zeroZ;
			for (int r = 0; r < rows; r++) {
				double sum = 0;
				int idx = idxZero;
				int idxY = zeroY;
				for (int c = 0; c < columns; c++) {
					sum += elements[idx] * elemsY[idxY];
					idx += columnStride;
					idxY += strideY;
				}
				elemsZ[idxZeroZ] = alpha * sum + beta * elemsZ[idxZeroZ];
				idxZero += rowStride;
				idxZeroZ += strideZ;
			}
		}
		z = z_loc;
		return z;
	}

	public DoubleMatrix2D zMult(final DoubleMatrix2D B, DoubleMatrix2D C, final double alpha, final double beta, final boolean transposeA, final boolean transposeB) {
		final int m = rows;
		final int n = columns;
		final int p = B.columns;
		if (C == null)
			C = new DenseDoubleMatrix2D(m, p);
		/*
		 * determine how to split and parallelize best into blocks if more
		 * B.columns than tasks --> split B.columns, as follows:
		 * 
		 * xx|xx|xxx B xx|xx|xxx xx|xx|xxx A xxx xx|xx|xxx C xxx xx|xx|xxx xxx
		 * xx|xx|xxx xxx xx|xx|xxx xxx xx|xx|xxx
		 * 
		 * if less B.columns than tasks --> split A.rows, as follows:
		 * 
		 * xxxxxxx B xxxxxxx xxxxxxx A xxx xxxxxxx C xxx xxxxxxx --- ------- xxx
		 * xxxxxxx xxx xxxxxxx --- ------- xxx xxxxxxx
		 * 
		 */
		if (transposeA)
			return viewDice().zMult(B, C, alpha, beta, false, transposeB);
		if (B instanceof SparseDoubleMatrix2D || B instanceof RCDoubleMatrix2D) {
			// exploit quick sparse mult
			// A*B = (B' * A')'
			if (C == null) {
				return B.zMult(this, null, alpha, beta, !transposeB, true).viewDice();
			} else {
				B.zMult(this, C.viewDice(), alpha, beta, !transposeB, true);
				return C;
			}
		}
		if (transposeB)
			return this.zMult(B.viewDice(), C, alpha, beta, transposeA, false);

		if (!(C instanceof DenseDoubleMatrix2D))
			return super.zMult(B, C, alpha, beta, transposeA, transposeB);

		if (B.rows() != n)
			throw new IllegalArgumentException("Matrix2D inner dimensions must agree:" + this.toStringShort() + ", " + B.toStringShort());
		if (C.rows() != m || C.columns() != p)
			throw new IllegalArgumentException("Incompatibe result matrix: " + this.toStringShort() + ", " + B.toStringShort() + ", " + C.toStringShort());
		if (this == C || B == C)
			throw new IllegalArgumentException("Matrices must not be identical");

		long flops = 2L * m * n * p;
		int noOfTasks = (int) Math.min(flops / 30000, Utils.getNP()); // each
		/* thread should process at least 30000 flops */
		boolean splitB = (p >= noOfTasks);
		int width = splitB ? p : m;
		noOfTasks = Math.min(width, noOfTasks);

		if (noOfTasks < 2) { /*
								 * parallelization doesn't pay off (too much
								 * start up overhead)
								 */
			return this.zMultSeq(B, C, alpha, beta, transposeA, transposeB);
		}

		// set up concurrent tasks
		int span = width / noOfTasks;
		final Future[] subTasks = new Future[noOfTasks];
		for (int i = 0; i < noOfTasks; i++) {
			final int offset = i * span;
			if (i == noOfTasks - 1)
				span = width - span * i; // last span may be a bit larger

			final DoubleMatrix2D AA, BB, CC;
			if (splitB) {
				// split B along columns into blocks
				AA = this;
				BB = B.viewPart(0, offset, n, span);
				CC = C.viewPart(0, offset, m, span);
			} else {
				// split A along rows into blocks
				AA = this.viewPart(offset, 0, span, n);
				BB = B;
				CC = C.viewPart(offset, 0, span, p);
			}

			subTasks[i] = Utils.threadPool.submit(new Runnable() {
				public void run() {
					((DenseDoubleMatrix2D) AA).zMultSeq(BB, CC, alpha, beta, transposeA, transposeB);
				}
			});
		}

		try {
			for (int j = 0; j < noOfTasks; j++) {
				subTasks[j].get();
			}
		} catch (ExecutionException ex) {
			ex.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return C;
	}

	protected DoubleMatrix2D zMultSeq(DoubleMatrix2D B, DoubleMatrix2D C, double alpha, double beta, boolean transposeA, boolean transposeB) {
		if (transposeA)
			return viewDice().zMult(B, C, alpha, beta, false, transposeB);
		if (B instanceof SparseDoubleMatrix2D || B instanceof RCDoubleMatrix2D) {
			// exploit quick sparse mult
			// A*B = (B' * A')'
			if (C == null) {
				return B.zMult(this, null, alpha, beta, !transposeB, true).viewDice();
			} else {
				B.zMult(this, C.viewDice(), alpha, beta, !transposeB, true);
				return C;
			}
		}
		if (transposeB)
			return this.zMult(B.viewDice(), C, alpha, beta, transposeA, false);

		int m = rows;
		int n = columns;
		int p = B.columns;
		if (C == null)
			C = new DenseDoubleMatrix2D(m, p);
		if (!(C instanceof DenseDoubleMatrix2D))
			return super.zMult(B, C, alpha, beta, transposeA, transposeB);
		if (B.rows != n)
			throw new IllegalArgumentException("Matrix2D inner dimensions must agree:" + toStringShort() + ", " + B.toStringShort());
		if (C.rows != m || C.columns != p)
			throw new IllegalArgumentException("Incompatibel result matrix: " + toStringShort() + ", " + B.toStringShort() + ", " + C.toStringShort());
		if (this == C || B == C)
			throw new IllegalArgumentException("Matrices must not be identical");

		DenseDoubleMatrix2D BB = (DenseDoubleMatrix2D) B;
		DenseDoubleMatrix2D CC = (DenseDoubleMatrix2D) C;
		final double[] AElems = this.elements;
		final double[] BElems = BB.elements;
		final double[] CElems = CC.elements;
		if (AElems == null || BElems == null || CElems == null)
			throw new InternalError();

		int cA = this.columnStride;
		int cB = BB.columnStride;
		int cC = CC.columnStride;

		int rA = this.rowStride;
		int rB = BB.rowStride;
		int rC = CC.rowStride;

		/*
		 * A is blocked to hide memory latency xxxxxxx B xxxxxxx xxxxxxx A xxx
		 * xxxxxxx C xxx xxxxxxx --- ------- xxx xxxxxxx xxx xxxxxxx --- -------
		 * xxx xxxxxxx
		 */
		final int BLOCK_SIZE = 30000; // * 8 == Level 2 cache in bytes
		int m_optimal = (BLOCK_SIZE - n) / (n + 1);
		if (m_optimal <= 0)
			m_optimal = 1;
		int blocks = m / m_optimal;
		int rr = 0;
		if (m % m_optimal != 0)
			blocks++;
		for (; --blocks >= 0;) {
			int jB = BB.index(0, 0);
			int indexA = index(rr, 0);
			int jC = CC.index(rr, 0);
			rr += m_optimal;
			if (blocks == 0)
				m_optimal += m - rr;

			for (int j = p; --j >= 0;) {
				int iA = indexA;
				int iC = jC;
				for (int i = m_optimal; --i >= 0;) {
					int kA = iA;
					int kB = jB;
					double s = 0;

					// loop unrolled
					kA -= cA;
					kB -= rB;

					for (int k = n % 4; --k >= 0;) {
						s += AElems[kA += cA] * BElems[kB += rB];
					}
					for (int k = n / 4; --k >= 0;) {
						s += AElems[kA += cA] * BElems[kB += rB] + AElems[kA += cA] * BElems[kB += rB] + AElems[kA += cA] * BElems[kB += rB] + AElems[kA += cA] * BElems[kB += rB];
					}

					CElems[iC] = alpha * s + beta * CElems[iC];
					iA += rA;
					iC += rC;
				}
				jB += cB;
				jC += cC;
			}
		}
		return C;
	}

	public double zSum() {
		double sum = 0;
		if (elements == null)
			throw new InternalError();
		final int zero = index(0, 0);
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Callable<Double>() {

					public Double call() throws Exception {
						double sum = 0;
						int idx = zero + startrow * rowStride;
						for (int r = startrow; r < stoprow; r++) {
							for (int i = idx, c = 0; c < columns; c++) {
								sum += elements[i];
								i += columnStride;
							}
							idx += rowStride;
						}
						return sum;
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					sum += (Double) futures[j].get();
				}
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			int idx = zero;
			for (int r = 0; r < rows; r++) {
				for (int i = idx, c = 0; c < columns; c++) {
					sum += elements[i];
					i += columnStride;
				}
				idx += rowStride;
			}
		}
		return sum;
	}

	protected boolean haveSharedCellsRaw(DoubleMatrix2D other) {
		if (other instanceof SelectedDenseDoubleMatrix2D) {
			SelectedDenseDoubleMatrix2D otherMatrix = (SelectedDenseDoubleMatrix2D) other;
			return this.elements == otherMatrix.elements;
		} else if (other instanceof DenseDoubleMatrix2D) {
			DenseDoubleMatrix2D otherMatrix = (DenseDoubleMatrix2D) other;
			return this.elements == otherMatrix.elements;
		}
		return false;
	}

	protected int index(int row, int column) {
		return rowZero + row * rowStride + columnZero + column * columnStride;
	}

	protected DoubleMatrix1D like1D(int size, int zero, int stride) {
		return new DenseDoubleMatrix1D(size, this.elements, zero, stride);
	}

	protected DoubleMatrix2D viewSelectionLike(int[] rowOffsets, int[] columnOffsets) {
		return new SelectedDenseDoubleMatrix2D(this.elements, rowOffsets, columnOffsets, 0);
	}
}
