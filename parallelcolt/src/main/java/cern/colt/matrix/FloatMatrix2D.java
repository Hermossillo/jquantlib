/*
Copyright � 1999 CERN - European Organization for Nuclear Research.
Permission to use, copy, modify, distribute and sell this software and its documentation for any purpose 
is hereby granted without fee, provided that the above copyright notice appear in all copies and 
that both that copyright notice and this permission notice appear in supporting documentation. 
CERN makes no representations about the suitability of this software for any purpose. 
It is provided "as is" without expressed or implied warranty.
 */
package cern.colt.matrix;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import cern.colt.Utils;
import cern.colt.list.FloatArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.matrix.impl.AbstractMatrix2D;
import cern.colt.matrix.impl.DenseFloatMatrix1D;
import cern.colt.matrix.impl.DenseFloatMatrix2D;
import cern.jet.math.FloatFunctions;

/**
 * Abstract base class for 2-d matrices holding <tt>float</tt> elements. First
 * see the <a href="package-summary.html">package summary</a> and javadoc <a
 * href="package-tree.html">tree view</a> to get the broad picture.
 * <p>
 * A matrix has a number of rows and columns, which are assigned upon instance
 * construction - The matrix's size is then <tt>rows()*columns()</tt>.
 * Elements are accessed via <tt>[row,column]</tt> coordinates. Legal
 * coordinates range from <tt>[0,0]</tt> to <tt>[rows()-1,columns()-1]</tt>.
 * Any attempt to access an element at a coordinate
 * <tt>column&lt;0 || column&gt;=columns() || row&lt;0 || row&gt;=rows()</tt>
 * will throw an <tt>IndexOutOfBoundsException</tt>.
 * <p>
 * <b>Note</b> that this implementation is not synchronized.
 * 
 * @author wolfgang.hoschek@cern.ch
 * @version 1.0, 09/24/99
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public abstract class FloatMatrix2D extends AbstractMatrix2D {
	/**
	 * Makes this class non instantiable, but still let's others inherit from
	 * it.
	 */
	protected FloatMatrix2D() {
	}

	/**
	 * Applies a function to each cell and aggregates the results. Returns a
	 * value <tt>v</tt> such that <tt>v==a(size())</tt> where
	 * <tt>a(i) == aggr( a(i-1), f(get(row,column)) )</tt> and terminators are
	 * <tt>a(1) == f(get(0,0)), a(0)==Float.NaN</tt>.
	 * <p>
	 * <b>Example:</b>
	 * 
	 * <pre>
	 * 	 cern.jet.math.Functions F = cern.jet.math.Functions.functions;
	 * 	 2 x 2 matrix
	 * 	 0 1
	 * 	 2 3
	 * 
	 * 	 // Sum( x[row,col]*x[row,col] ) 
	 * 	 matrix.aggregate(F.plus,F.square);
	 * 	 --&gt; 14
	 * 	
	 * </pre>
	 * 
	 * For further examples, see the <a
	 * href="package-summary.html#FunctionObjects">package doc</a>.
	 * 
	 * @param aggr
	 *            an aggregation function taking as first argument the current
	 *            aggregation and as second argument the transformed current
	 *            cell value.
	 * @param f
	 *            a function transforming the current cell value.
	 * @return the aggregated measure.
	 * @see cern.jet.math.FloatFunctions
	 */
	public float aggregate(final cern.colt.function.FloatFloatFunction aggr, final cern.colt.function.FloatFunction f) {
		if (size() == 0)
			return Float.NaN;
		float a = 0;
		int np = Utils.getNP();
		if ((np > 1) && (rows * columns >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			Float[] results = new Float[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Callable<Float>() {

					public Float call() throws Exception {
						float a = f.apply(getQuick(startrow, 0));
						int d = 1;
						for (int r = startrow; r < stoprow; r++) {
							for (int c = d; c < columns; c++) {
								a = aggr.apply(a, f.apply(getQuick(r, c)));
							}
							d = 0;
						}
						return Float.valueOf(a);
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					results[j] = (Float) futures[j].get();
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
			a = f.apply(getQuick(0, 0));
			int d = 1; // first cell already done
			for (int r = 0; r < rows; r++) {
				for (int c = d; c < columns; c++) {
					a = aggr.apply(a, f.apply(getQuick(r, c)));
				}
				d = 0;
			}
		}
		return a;
	}

	/**
	 * Applies a function to each cell that satisfies a condition and aggregates
	 * the results.
	 * 
	 * @param aggr
	 *            an aggregation function taking as first argument the current
	 *            aggregation and as second argument the transformed current
	 *            cell value.
	 * @param f
	 *            a function transforming the current cell value.
	 * @param cond
	 *            a condition.
	 * @return the aggregated measure.
	 * @see cern.jet.math.FloatFunctions
	 */
	public float aggregate(final cern.colt.function.FloatFloatFunction aggr, final cern.colt.function.FloatFunction f, final cern.colt.function.FloatProcedure cond) {
		if (size() == 0)
			return Float.NaN;
		float a = 0;
		int np = Utils.getNP();
		if ((np > 1) && (rows * columns >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			Float[] results = new Float[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Callable<Float>() {

					public Float call() throws Exception {
						float elem = getQuick(startrow, 0);
						float a = 0;
						if (cond.apply(elem) == true) {
							a = aggr.apply(a, f.apply(elem));
						}
						int d = 1;
						for (int r = startrow; r < stoprow; r++) {
							for (int c = d; c < columns; c++) {
								elem = getQuick(r, c);
								if (cond.apply(elem) == true) {
									a = aggr.apply(a, f.apply(elem));
								}
							}
							d = 0;
						}
						return Float.valueOf(a);
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					results[j] = (Float) futures[j].get();
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
			float elem = getQuick(0, 0);
			if (cond.apply(elem) == true) {
				a = aggr.apply(a, f.apply(elem));
			}
			int d = 1; // first cell already done
			for (int r = 0; r < rows; r++) {
				for (int c = d; c < columns; c++) {
					elem = getQuick(r, c);
					if (cond.apply(elem) == true) {
						a = aggr.apply(a, f.apply(elem));
					}
				}
				d = 0;
			}
		}
		return a;
	}

	/**
	 * Applies a function to all cells with a given indices and aggregates the
	 * results.
	 * 
	 * @param aggr
	 *            an aggregation function taking as first argument the current
	 *            aggregation and as second argument the transformed current
	 *            cell value.
	 * @param f
	 *            a function transforming the current cell value.
	 * @param indexes
	 *            a set of indices.
	 * @return the aggregated measure.
	 * @see cern.jet.math.FloatFunctions
	 */
	public float aggregate(final cern.colt.function.FloatFloatFunction aggr, final cern.colt.function.FloatFunction f, Set<int[]> indexes) {
		if (size() == 0)
			return Float.NaN;
		int n = indexes.size();
		final int[][] indexesArray = indexes.toArray(new int[0][0]);
		float a = 0;
		int np = Utils.getNP();
		if ((np > 1) && (rows * columns >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			Float[] results = new Float[np];
			int k = n / np;
			for (int j = 0; j < np; j++) {
				final int startidx = j * k;
				final int stopidx;
				if (j == np - 1) {
					stopidx = n;
				} else {
					stopidx = startidx + k;
				}
				futures[j] = Utils.threadPool.submit(new Callable<Float>() {

					public Float call() throws Exception {
						float a = f.apply(getQuick(indexesArray[startidx][0], indexesArray[startidx][1]));
						float elem;
						for (int r = startidx + 1; r < stopidx; r++) {
							elem = getQuick(indexesArray[r][0], indexesArray[r][1]);
							a = aggr.apply(a, f.apply(elem));
						}
						return a;
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					results[j] = (Float) futures[j].get();
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
			float elem;
			a = f.apply(getQuick(indexesArray[0][0], indexesArray[0][1]));
			for (int r = 1; r < n; r++) {
				elem = getQuick(indexesArray[r][0], indexesArray[r][1]);
				a = aggr.apply(a, f.apply(elem));
			}
		}
		return a;
	}

	/**
	 * Applies a function to each corresponding cell of two matrices and
	 * aggregates the results. Returns a value <tt>v</tt> such that
	 * <tt>v==a(size())</tt> where
	 * <tt>a(i) == aggr( a(i-1), f(get(row,column),other.get(row,column)) )</tt>
	 * and terminators are
	 * <tt>a(1) == f(get(0,0),other.get(0,0)), a(0)==Float.NaN</tt>.
	 * <p>
	 * <b>Example:</b>
	 * 
	 * <pre>
	 * 	 cern.jet.math.Functions F = cern.jet.math.Functions.functions;
	 * 	 x == 2 x 2 matrix
	 * 	 0 1
	 * 	 2 3
	 * 
	 * 	 y == 2 x 2 matrix
	 * 	 0 1
	 * 	 2 3
	 * 
	 * 	 // Sum( x[row,col] * y[row,col] ) 
	 * 	 x.aggregate(y, F.plus, F.mult);
	 * 	 --&gt; 14
	 * 
	 * 	 // Sum( (x[row,col] + y[row,col])&circ;2 )
	 * 	 x.aggregate(y, F.plus, F.chain(F.square,F.plus));
	 * 	 --&gt; 56
	 * 	
	 * </pre>
	 * 
	 * For further examples, see the <a
	 * href="package-summary.html#FunctionObjects">package doc</a>.
	 * 
	 * @param aggr
	 *            an aggregation function taking as first argument the current
	 *            aggregation and as second argument the transformed current
	 *            cell values.
	 * @param f
	 *            a function transforming the current cell values.
	 * @return the aggregated measure.
	 * @throws IllegalArgumentException
	 *             if
	 *             <tt>columns() != other.columns() || rows() != other.rows()</tt>
	 * @see cern.jet.math.FloatFunctions
	 */
	public float aggregate(final FloatMatrix2D other, final cern.colt.function.FloatFloatFunction aggr, final cern.colt.function.FloatFloatFunction f) {
		checkShape(other);
		if (size() == 0)
			return Float.NaN;
		float a = 0;
		int np = Utils.getNP();
		if ((np > 1) && (rows * columns >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			Float[] results = new Float[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Callable<Float>() {

					public Float call() throws Exception {
						float a = f.apply(getQuick(startrow, 0), other.getQuick(startrow, 0));
						int d = 1;
						for (int r = startrow; r < stoprow; r++) {
							for (int c = d; c < columns; c++) {
								a = aggr.apply(a, f.apply(getQuick(r, c), other.getQuick(r, c)));
							}
							d = 0;
						}
						return Float.valueOf(a);
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					results[j] = (Float) futures[j].get();
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
			a = f.apply(getQuick(0, 0), other.getQuick(0, 0));
			int d = 1; // first cell already done
			for (int r = 0; r < rows; r++) {
				for (int c = d; c < columns; c++) {
					a = aggr.apply(a, f.apply(getQuick(r, c), other.getQuick(r, c)));
				}
				d = 0;
			}
		}
		return a;
	}

	/**
	 * Assigns the result of a function to each cell;
	 * <tt>x[row,col] = function(x[row,col])</tt>.
	 * <p>
	 * <b>Example:</b>
	 * 
	 * <pre>
	 * 	 matrix = 2 x 2 matrix 
	 * 	 0.5 1.5      
	 * 	 2.5 3.5
	 * 
	 * 	 // change each cell to its sine
	 * 	 matrix.assign(cern.jet.math.Functions.sin);
	 * 	 --&gt;
	 * 	 2 x 2 matrix
	 * 	 0.479426  0.997495 
	 * 	 0.598472 -0.350783
	 * 	
	 * </pre>
	 * 
	 * For further examples, see the <a
	 * href="package-summary.html#FunctionObjects">package doc</a>.
	 * 
	 * @param f
	 *            a function object taking as argument the current cell's value.
	 * @return <tt>this</tt> (for convenience only).
	 * @see cern.jet.math.FloatFunctions
	 */
	public FloatMatrix2D assign(final cern.colt.function.FloatFunction f) {
		int np = Utils.getNP();
		if ((np > 1) && (rows * columns >= Utils.getThreadsBeginN_2D())) {
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
							for (int c = 0; c < columns; c++) {
								setQuick(r, c, f.apply(getQuick(r, c)));
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
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < columns; c++) {
					setQuick(r, c, f.apply(getQuick(r, c)));
				}
			}
		}
		return this;
	}

	/**
	 * Assigns the result of a function to all cells that satisfy a condition.
	 * 
	 * @param cond
	 *            a condition.
	 * 
	 * @param f
	 *            a function object.
	 * @return <tt>this</tt> (for convenience only).
	 * @see cern.jet.math.FloatFunctions
	 */
	public FloatMatrix2D assign(final cern.colt.function.FloatProcedure cond, final cern.colt.function.FloatFunction f) {
		int np = Utils.getNP();
		if ((np > 1) && (rows * columns >= Utils.getThreadsBeginN_2D())) {
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
						float elem;
						for (int r = startrow; r < stoprow; r++) {
							for (int c = 0; c < columns; c++) {
								elem = getQuick(r, c);
								if (cond.apply(elem) == true) {
									setQuick(r, c, f.apply(elem));
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
			float elem;
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < columns; c++) {
					elem = getQuick(r, c);
					if (cond.apply(elem) == true) {
						setQuick(r, c, f.apply(elem));
					}
				}
			}
		}
		return this;
	}

	/**
	 * Assigns a value to all cells that satisfy a condition.
	 * 
	 * @param cond
	 *            a condition.
	 * 
	 * @param value
	 *            a value.
	 * @return <tt>this</tt> (for convenience only).
	 * 
	 */
	public FloatMatrix2D assign(final cern.colt.function.FloatProcedure cond, final float value) {
		int np = Utils.getNP();
		if ((np > 1) && (rows * columns >= Utils.getThreadsBeginN_2D())) {
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
						float elem;
						for (int r = startrow; r < stoprow; r++) {
							for (int c = 0; c < columns; c++) {
								elem = getQuick(r, c);
								if (cond.apply(elem) == true) {
									setQuick(r, c, value);
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
			float elem;
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < columns; c++) {
					elem = getQuick(r, c);
					if (cond.apply(elem) == true) {
						setQuick(r, c, value);
					}
				}
			}
		}
		return this;
	}

	/**
	 * Sets all cells to the state specified by <tt>value</tt>.
	 * 
	 * @param value
	 *            the value to be filled into the cells.
	 * @return <tt>this</tt> (for convenience only).
	 */
	public FloatMatrix2D assign(final float value) {
		int np = Utils.getNP();
		if ((np > 1) && (rows * columns >= Utils.getThreadsBeginN_2D())) {
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
							for (int c = 0; c < columns; c++) {
								setQuick(r, c, value);
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
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < columns; c++) {
					setQuick(r, c, value);
				}
			}
		}
		return this;
	}

	/**
	 * Sets all cells to the state specified by <tt>values</tt>.
	 * <tt>values</tt> is required to have the form
	 * <tt>values[row*column]</tt> and elements have to be stored in a
	 * row-wise order.
	 * <p>
	 * The values are copied. So subsequent changes in <tt>values</tt> are not
	 * reflected in the matrix, and vice-versa.
	 * 
	 * @param values
	 *            the values to be filled into the cells.
	 * @return <tt>this</tt> (for convenience only).
	 * @throws IllegalArgumentException
	 *             if <tt>values.length != rows()*columns()</tt>.
	 */
	public FloatMatrix2D assign(final float[] values) {
		if (values.length != rows * columns)
			throw new IllegalArgumentException("Must have same length: length=" + values.length + "rows()*columns()=" + rows() * columns());
		int np = Utils.getNP();
		if ((np > 1) && (rows * columns >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				final int glob_idx = j * k * columns;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {

					public void run() {
						int idx = glob_idx;
						for (int r = startrow; r < stoprow; r++) {
							for (int c = 0; c < columns; c++) {
								setQuick(r, c, values[idx++]);
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

			int idx = 0;
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < columns; c++) {
					setQuick(r, c, values[idx++]);
				}
			}
		}

		return this;
	}

	/**
	 * Sets all cells to the state specified by <tt>values</tt>.
	 * <tt>values</tt> is required to have the form
	 * <tt>values[row][column]</tt> and have exactly the same number of rows
	 * and columns as the receiver.
	 * <p>
	 * The values are copied. So subsequent changes in <tt>values</tt> are not
	 * reflected in the matrix, and vice-versa.
	 * 
	 * @param values
	 *            the values to be filled into the cells.
	 * @return <tt>this</tt> (for convenience only).
	 * @throws IllegalArgumentException
	 *             if
	 *             <tt>values.length != rows() || for any 0 &lt;= row &lt; rows(): values[row].length != columns()</tt>.
	 */
	public FloatMatrix2D assign(final float[][] values) {
		if (values.length != rows)
			throw new IllegalArgumentException("Must have same number of rows: rows=" + values.length + "rows()=" + rows());
		int np = Utils.getNP();
		if ((np > 1) && (rows * columns >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {

					public void run() {
						for (int r = startrow; r < stoprow; r++) {
							float[] currentRow = values[r];
							if (currentRow.length != columns)
								throw new IllegalArgumentException("Must have same number of columns in every row: columns=" + currentRow.length + "columns()=" + columns());
							for (int c = 0; c < columns; c++) {
								setQuick(r, c, currentRow[c]);
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
			for (int r = 0; r < rows; r++) {
				float[] currentRow = values[r];
				if (currentRow.length != columns)
					throw new IllegalArgumentException("Must have same number of columns in every row: columns=" + currentRow.length + "columns()=" + columns());
				for (int c = 0; c < columns; c++) {
					setQuick(r, c, currentRow[c]);
				}
			}
		}
		return this;
	}

	/**
	 * Replaces all cell values of the receiver with the values of another
	 * matrix. Both matrices must have the same number of rows and columns. If
	 * both matrices share the same cells (as is the case if they are views
	 * derived from the same matrix) and intersect in an ambiguous way, then
	 * replaces <i>as if</i> using an intermediate auxiliary deep copy of
	 * <tt>other</tt>.
	 * 
	 * @param other
	 *            the source matrix to copy from (may be identical to the
	 *            receiver).
	 * @return <tt>this</tt> (for convenience only).
	 * @throws IllegalArgumentException
	 *             if
	 *             <tt>columns() != other.columns() || rows() != other.rows()</tt>
	 */
	public FloatMatrix2D assign(FloatMatrix2D other) {
		if (other == this)
			return this;
		checkShape(other);
		final FloatMatrix2D other_loc;
		if (haveSharedCells(other)) {
			other_loc = other.copy();
		} else {
			other_loc = other;
		}
		int np = Utils.getNP();
		if ((np > 1) && (rows * columns >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {

					public void run() {
						for (int r = startrow; r < stoprow; r++) {
							for (int c = 0; c < columns; c++) {
								setQuick(r, c, other_loc.getQuick(r, c));
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
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < columns; c++) {
					setQuick(r, c, other_loc.getQuick(r, c));
				}
			}
		}
		return this;
	}

	/**
	 * Assigns the result of a function to each cell;
	 * <tt>x[row,col] = function(x[row,col],y[row,col])</tt>.
	 * <p>
	 * <b>Example:</b>
	 * 
	 * <pre>
	 * 	 // assign x[row,col] = x[row,col]&lt;sup&gt;y[row,col]&lt;/sup&gt;
	 * 	 m1 = 2 x 2 matrix 
	 * 	 0 1 
	 * 	 2 3
	 * 
	 * 	 m2 = 2 x 2 matrix 
	 * 	 0 2 
	 * 	 4 6
	 * 
	 * 	 m1.assign(m2, cern.jet.math.Functions.pow);
	 * 	 --&gt;
	 * 	 m1 == 2 x 2 matrix
	 * 	 1   1 
	 * 	 16 729
	 * 	
	 * </pre>
	 * 
	 * For further examples, see the <a
	 * href="package-summary.html#FunctionObjects">package doc</a>.
	 * 
	 * @param y
	 *            the secondary matrix to operate on.
	 * @param function
	 *            a function object taking as first argument the current cell's
	 *            value of <tt>this</tt>, and as second argument the current
	 *            cell's value of <tt>y</tt>,
	 * @return <tt>this</tt> (for convenience only).
	 * @throws IllegalArgumentException
	 *             if
	 *             <tt>columns() != other.columns() || rows() != other.rows()</tt>
	 * @see cern.jet.math.FloatFunctions
	 */
	public FloatMatrix2D assign(final FloatMatrix2D y, final cern.colt.function.FloatFloatFunction function) {
		checkShape(y);
		int np = Utils.getNP();
		if ((np > 1) && (rows * columns >= Utils.getThreadsBeginN_2D())) {
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
							for (int c = 0; c < columns; c++) {
								setQuick(r, c, function.apply(getQuick(r, c), y.getQuick(r, c)));
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
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < columns; c++) {
					setQuick(r, c, function.apply(getQuick(r, c), y.getQuick(r, c)));
				}
			}
		}
		return this;
	}

	/**
	 * Assigns the result of a function to all cells with a given indices
	 * 
	 * @param y
	 *            the secondary matrix to operate on.
	 * @param function
	 *            a function object taking as first argument the current cell's
	 *            value of <tt>this</tt>, and as second argument the current
	 *            cell's value of <tt>y</tt>,
	 * @param indexes
	 *            a set of indices.
	 * @return <tt>this</tt> (for convenience only).
	 * @throws IllegalArgumentException
	 *             if
	 *             <tt>columns() != other.columns() || rows() != other.rows()</tt>
	 * @see cern.jet.math.FloatFunctions
	 */
	public FloatMatrix2D assign(final FloatMatrix2D y, final cern.colt.function.FloatFloatFunction function, Set<int[]> indexes) {
		checkShape(y);
		int n = indexes.size();
		final int[][] indexesArray = indexes.toArray(new int[0][0]);
		int np = Utils.getNP();
		if ((np > 1) && (rows * columns >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			int k = n / np;
			for (int j = 0; j < np; j++) {
				final int startidx = j * k;
				final int stopidx;
				if (j == np - 1) {
					stopidx = n;
				} else {
					stopidx = startidx + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {

					public void run() {
						for (int r = startidx; r < stopidx; r++) {
							setQuick(indexesArray[r][0], indexesArray[r][1], function.apply(getQuick(indexesArray[r][0], indexesArray[r][1]), y.getQuick(indexesArray[r][0], indexesArray[r][1])));
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
			for (int r = 0; r < n; r++) {
				setQuick(indexesArray[r][0], indexesArray[r][1], function.apply(getQuick(indexesArray[r][0], indexesArray[r][1]), y.getQuick(indexesArray[r][0], indexesArray[r][1])));
			}
		}
		return this;
	}

	/**
	 * Returns the number of cells having non-zero values; ignores tolerance.
	 * 
	 * @return cardinality
	 */
	public int cardinality() {
		int cardinality = 0;
		int np = Utils.getNP();
		if ((np > 1) && (rows * columns >= Utils.getThreadsBeginN_2D())) {
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
						for (int r = startrow; r < stoprow; r++) {
							for (int c = 0; c < columns; c++) {
								if (getQuick(r, c) != 0)
									cardinality++;
							}
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
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < columns; c++) {
					if (getQuick(r, c) != 0)
						cardinality++;
				}
			}
		}
		return cardinality;
	}

	/**
	 * Constructs and returns a deep copy of the receiver.
	 * <p>
	 * <b>Note that the returned matrix is an independent deep copy.</b> The
	 * returned matrix is not backed by this matrix, so changes in the returned
	 * matrix are not reflected in this matrix, and vice-versa.
	 * 
	 * @return a deep copy of the receiver.
	 */
	public FloatMatrix2D copy() {
		return like().assign(this);
	}

	/**
	 * Computes the 2D discrete cosine transform (DCT-II) of this matrix. Throws
	 * IllegalArgumentException if the row size or column size of this matrix is
	 * not a power of 2 number.
	 * 
	 * @param scale
	 *            if true then scaling is performed
	 * 
	 */
	public abstract void dct2(boolean scale);

	/**
	 * Computes the discrete cosine transform (DCT-II) of each column of this
	 * matrix. Throws IllegalArgumentException if the column size of this matrix
	 * is not a power of 2 number.
	 * 
	 * @param scale
	 *            if true then scaling is performed
	 * 
	 */
	public abstract void dctColumns(boolean scale);

	/**
	 * Computes the discrete cosine transform (DCT-II) of each row of this
	 * matrix. Throws IllegalArgumentException if the column size of this matrix
	 * is not a power of 2 number.
	 * 
	 * @param scale
	 *            if true then scaling is performed
	 * 
	 */
	public abstract void dctRows(boolean scale);

	/**
	 * Computes the 2D discrete sine transform (DST-II) of this matrix. Throws
	 * IllegalArgumentException if the row size or column size of this matrix is
	 * not a power of 2 number.
	 * 
	 * @param scale
	 *            if true then scaling is performed
	 * 
	 */
	public abstract void dst2(boolean scale);

	/**
	 * Computes the discrete sine transform (DST-II) of each column of this
	 * matrix. Throws IllegalArgumentException if the column size of this matrix
	 * is not a power of 2 number.
	 * 
	 * @param scale
	 *            if true then scaling is performed
	 * 
	 */
	public abstract void dstColumns(boolean scale);

	/**
	 * Computes the discrete sine transform (DST-II) of each row of this matrix.
	 * Throws IllegalArgumentException if the column size of this matrix is not
	 * a power of 2 number.
	 * 
	 * @param scale
	 *            if true then scaling is performed
	 * 
	 */
	public abstract void dstRows(boolean scale);

	/**
	 * Returns whether all cells are equal to the given value.
	 * 
	 * @param value
	 *            the value to test against.
	 * @return <tt>true</tt> if all cells are equal to the given value,
	 *         <tt>false</tt> otherwise.
	 */
	public boolean equals(float value) {
		return cern.colt.matrix.linalg.FloatProperty.DEFAULT.equals(this, value);
	}

	/**
	 * Compares this object against the specified object. The result is
	 * <code>true</code> if and only if the argument is not <code>null</code>
	 * and is at least a <code>FloatMatrix2D</code> object that has the same
	 * number of columns and rows as the receiver and has exactly the same
	 * values at the same coordinates.
	 * 
	 * @param obj
	 *            the object to compare with.
	 * @return <code>true</code> if the objects are the same;
	 *         <code>false</code> otherwise.
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof FloatMatrix2D))
			return false;

		return cern.colt.matrix.linalg.FloatProperty.DEFAULT.equals(this, (FloatMatrix2D) obj);
	}

	/**
	 * Assigns the result of a function to each <i>non-zero</i> cell;
	 * <tt>x[row,col] = function(x[row,col])</tt>. Use this method for fast
	 * special-purpose iteration. If you want to modify another matrix instead
	 * of <tt>this</tt> (i.e. work in read-only mode), simply return the input
	 * value unchanged.
	 * 
	 * Parameters to function are as follows: <tt>first==row</tt>,
	 * <tt>second==column</tt>, <tt>third==nonZeroValue</tt>.
	 * 
	 * @param function
	 *            a function object taking as argument the current non-zero
	 *            cell's row, column and value.
	 * @return <tt>this</tt> (for convenience only).
	 */
	public FloatMatrix2D forEachNonZero(final cern.colt.function.IntIntFloatFunction function) {
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
							for (int c = 0; c < columns; c++) {
								float value = getQuick(r, c);
								if (value != 0) {
									float a = function.apply(r, c, value);
									if (a != value)
										setQuick(r, c, a);
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
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < columns; c++) {
					float value = getQuick(r, c);
					if (value != 0) {
						float a = function.apply(r, c, value);
						if (a != value)
							setQuick(r, c, a);
					}
				}
			}
		}
		return this;
	}

	/**
	 * Returns the matrix cell value at coordinate <tt>[row,column]</tt>.
	 * 
	 * @param row
	 *            the index of the row-coordinate.
	 * @param column
	 *            the index of the column-coordinate.
	 * @return the value of the specified cell.
	 * @throws IndexOutOfBoundsException
	 *             if
	 *             <tt>column&lt;0 || column&gt;=columns() || row&lt;0 || row&gt;=rows()</tt>
	 */
	public float get(int row, int column) {
		if (column < 0 || column >= columns || row < 0 || row >= rows)
			throw new IndexOutOfBoundsException("row:" + row + ", column:" + column);
		return getQuick(row, column);
	}

	/**
	 * Returns the elements of this matrix.
	 * 
	 * @return the elements
	 */
	public abstract Object getElements();

	/**
	 * Returns new complex matrix which is the 2D discrete Fourier transform
	 * (DFT) of this matrix. Throws IllegalArgumentException if the row size or
	 * the column size of this matrix is not a power of 2 number.
	 * 
	 * @return the 2D discrete Fourier transform (DFT) of this matrix.
	 */
	public abstract FComplexMatrix2D getFft2();

	/**
	 * Returns new complex matrix which is the discrete Fourier transform (DFT)
	 * of each column of this matrix. Throws IllegalArgumentException if the
	 * size of a column is not a power of 2 number.
	 * 
	 * @return the discrete Fourier transform (DFT) of each column of this
	 *         matrix.
	 */
	public abstract FComplexMatrix2D getFftColumns();

	/**
	 * Returns new complex matrix which is the discrete Fourier transform (DFT)
	 * of each row of this matrix. Throws IllegalArgumentException if the size
	 * of a row is not a power of 2 number.
	 * 
	 * @return the discrete Fourier transform (DFT) of each row of this matrix.
	 */
	public abstract FComplexMatrix2D getFftRows();

	/**
	 * Returns new complex matrix which is the 2D inverse of the discrete
	 * Fourier transform (IDFT) of this matrix. Throws IllegalArgumentException
	 * if the row size or the column size of this matrix is not a power of 2
	 * number.
	 * 
	 * @return the 2D inverse of the discrete Fourier transform (IDFT) of this
	 *         matrix.
	 */
	public abstract FComplexMatrix2D getIfft2(boolean scale);

	/**
	 * Returns new complex matrix which is the inverse of the discrete Fourier
	 * transform (IDFT) of each column of this matrix. Throws
	 * IllegalArgumentException if the size of a column is not a power of 2
	 * number.
	 * 
	 * @return the inverse of the discrete Fourier transform (IDFT) of each
	 *         column of this matrix.
	 */
	public abstract FComplexMatrix2D getIfftColumns(boolean scale);

	/**
	 * Returns new complex matrix which is the inverse of the discrete Fourier
	 * transform (IDFT) of each row of this matrix. Throws
	 * IllegalArgumentException if the size of a row is not a power of 2 number.
	 * 
	 * @return the inverse of the discrete Fourier transform (IDFT) of each row
	 *         of this matrix.
	 */
	public abstract FComplexMatrix2D getIfftRows(boolean scale);

	/**
	 * Return the maximum value of this matrix together with its location
	 * 
	 * @return { maximum_value, row_location, column_location };
	 */
	public float[] getMaxLocation() {
		int rowLocation = 0;
		int columnLocation = 0;
		float maxValue = 0;
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			float[][] results = new float[np][2];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Callable<float[]>() {
					public float[] call() throws Exception {
						int rowLocation = startrow;
						int columnLocation = 0;
						float maxValue = getQuick(rowLocation, 0);
						int d = 1;
						float elem;
						for (int r = startrow; r < stoprow; r++) {
							for (int c = d; c < columns; c++) {
								elem = getQuick(r, c);
								if (maxValue < elem) {
									maxValue = elem;
									rowLocation = r;
									columnLocation = c;
								}
							}
							d = 0;
						}
						return new float[] { maxValue, rowLocation, columnLocation };
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					results[j] = (float[]) futures[j].get();
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
			maxValue = getQuick(0, 0);
			float elem;
			int d = 1;
			for (int r = 0; r < rows; r++) {
				for (int c = d; c < columns; c++) {
					elem = getQuick(r, c);
					if (maxValue < elem) {
						maxValue = elem;
						rowLocation = r;
						columnLocation = c;
					}
				}
				d = 0;
			}
		}
		return new float[] { maxValue, rowLocation, columnLocation };
	}

	/**
	 * Return the minimum value of this matrix together with its location
	 * 
	 * @return { minimum_value, row_location, column_location};
	 */
	public float[] getMinLocation() {
		int rowLocation = 0;
		int columnLocation = 0;
		float minValue = 0;
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			float[][] results = new float[np][2];
			int k = rows / np;
			for (int j = 0; j < np; j++) {
				final int startrow = j * k;
				final int stoprow;
				if (j == np - 1) {
					stoprow = rows;
				} else {
					stoprow = startrow + k;
				}
				futures[j] = Utils.threadPool.submit(new Callable<float[]>() {
					public float[] call() throws Exception {
						int rowLocation = startrow;
						int columnLocation = 0;
						float minValue = getQuick(rowLocation, 0);
						int d = 1;
						float elem;
						for (int r = startrow; r < stoprow; r++) {
							for (int c = d; c < columns; c++) {
								elem = getQuick(r, c);
								if (minValue > elem) {
									minValue = elem;
									rowLocation = r;
									columnLocation = c;
								}
							}
							d = 0;
						}
						return new float[] { minValue, rowLocation, columnLocation };
					}
				});
			}
			try {
				for (int j = 0; j < np; j++) {
					results[j] = (float[]) futures[j].get();
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
			minValue = getQuick(0, 0);
			float elem;
			int d = 1;
			for (int r = 0; r < rows; r++) {
				for (int c = d; c < columns; c++) {
					elem = getQuick(r, c);
					if (minValue > elem) {
						minValue = elem;
						rowLocation = r;
						columnLocation = c;
					}
				}
				d = 0;
			}
		}
		return new float[] { minValue, rowLocation, columnLocation };
	}

	/**
	 * Return all negative values of this matrix together with the locations
	 * 
	 * @return a HashMap with keys being the locations ({row_location,
	 *         column_location}) and values being the negative values of this
	 *         matrix.
	 */
	public ConcurrentHashMap<int[], Float> getNegativeValues() {
		final ConcurrentHashMap<int[], Float> result = new ConcurrentHashMap<int[], Float>();
		int np = Utils.getNP();
		if ((np > 1) && (rows * columns >= Utils.getThreadsBeginN_2D())) {
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
							for (int c = 0; c < columns; c++) {
								float value = getQuick(r, c);
								if (value < 0) {
									result.put(new int[] { r, c }, value);
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
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < columns; c++) {
					float value = getQuick(r, c);
					if (value < 0) {
						result.put(new int[] { r, c }, value);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Fills the coordinates and values of cells having non-zero values into the
	 * specified lists. Fills into the lists, starting at index 0. After this
	 * call returns the specified lists all have a new size, the number of
	 * non-zero values.
	 * <p>
	 * In general, fill order is <i>unspecified</i>. This implementation fills
	 * like <tt>for (row = 0..rows-1) for (column = 0..columns-1) do ... </tt>.
	 * However, subclasses are free to us any other order, even an order that
	 * may change over time as cell values are changed. (Of course, result lists
	 * indexes are guaranteed to correspond to the same cell).
	 * <p>
	 * <b>Example:</b> <br>
	 * 
	 * <pre>
	 * 	 2 x 3 matrix:
	 * 	 0, 0, 8
	 * 	 0, 7, 0
	 * 	 --&gt;
	 * 	 rowList    = (0,1)
	 * 	 columnList = (2,1)
	 * 	 valueList  = (8,7)
	 * 	
	 * </pre>
	 * 
	 * In other words, <tt>get(0,2)==8, get(1,1)==7</tt>.
	 * 
	 * @param rowList
	 *            the list to be filled with row indexes, can have any size.
	 * @param columnList
	 *            the list to be filled with column indexes, can have any size.
	 * @param valueList
	 *            the list to be filled with values, can have any size.
	 */
	public void getNonZeros(IntArrayList rowList, IntArrayList columnList, FloatArrayList valueList) {
		rowList.clear();
		columnList.clear();
		valueList.clear();
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < columns; c++) {
				float value = getQuick(r, c);
				if (value != 0) {
					rowList.add(r);
					columnList.add(c);
					valueList.add(value);
				}
			}
		}
	}

	/**
	 * Return all positive values of this matrix together with the locations
	 * 
	 * @return a HashMap with keys being the locations ({row_location,
	 *         column_location}) and values being the positive values of this
	 *         matrix.
	 */
	public ConcurrentHashMap<int[], Float> getPositiveValues() {
		final ConcurrentHashMap<int[], Float> result = new ConcurrentHashMap<int[], Float>();
		int np = Utils.getNP();
		if ((np > 1) && (rows * columns >= Utils.getThreadsBeginN_2D())) {
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
							for (int c = 0; c < columns; c++) {
								float value = getQuick(r, c);
								if (value > 0) {
									result.put(new int[] { r, c }, value);
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
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < columns; c++) {
					float value = getQuick(r, c);
					if (value > 0) {
						result.put(new int[] { r, c }, value);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Returns the matrix cell value at coordinate <tt>[row,column]</tt>.
	 * 
	 * <p>
	 * Provided with invalid parameters this method may return invalid objects
	 * without throwing any exception. <b>You should only use this method when
	 * you are absolutely sure that the coordinate is within bounds.</b>
	 * Precondition (unchecked):
	 * <tt>0 &lt;= column &lt; columns() && 0 &lt;= row &lt; rows()</tt>.
	 * 
	 * @param row
	 *            the index of the row-coordinate.
	 * @param column
	 *            the index of the column-coordinate.
	 * @return the value at the specified coordinate.
	 */
	public abstract float getQuick(int row, int column);

	/**
	 * Return all values of this matrix that satisfy a condition together with
	 * the locations
	 * 
	 * @return a HashMap with keys being the locations ({row_location,
	 *         column_location}) and values being the values that satisfy a
	 *         condition.
	 */
	public ConcurrentHashMap<int[], Float> getValuesSuchThat(final cern.colt.function.FloatProcedure cond) {
		final ConcurrentHashMap<int[], Float> result = new ConcurrentHashMap<int[], Float>();
		int np = Utils.getNP();
		if ((np > 1) && (rows * columns >= Utils.getThreadsBeginN_2D())) {
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
							for (int c = 0; c < columns; c++) {
								float value = getQuick(r, c);
								if (cond.apply(value) == true) {
									result.put(new int[] { r, c }, value);
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
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < columns; c++) {
					float value = getQuick(r, c);
					if (cond.apply(value) == true) {
						result.put(new int[] { r, c }, value);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Computes the 2D inverse of the discrete cosine transform (DCT-III) of
	 * this matrix. Throws IllegalArgumentException if the row size or column
	 * size of this matrix is not a power of 2 number.
	 * 
	 * @param scale
	 *            if true then scaling is performed
	 * 
	 */
	public abstract void idct2(boolean scale);

	/**
	 * Computes the inverse of the discrete cosine transform (DCT-III) of each
	 * column of this matrix. Throws IllegalArgumentException if the column size
	 * of this matrix is not a power of 2 number.
	 * 
	 * @param scale
	 *            if true then scaling is performed
	 * 
	 */
	public abstract void idctColumns(boolean scale);

	/**
	 * Computes the inverse of the discrete cosine transform (DCT-III) of each
	 * row of this matrix. Throws IllegalArgumentException if the row size of
	 * this matrix is not a power of 2 number.
	 * 
	 * @param scale
	 *            if true then scaling is performed
	 * 
	 */
	public abstract void idctRows(boolean scale);

	/**
	 * Computes the 2D inverse of the discrete sine transform (DST-III) of this
	 * matrix. Throws IllegalArgumentException if the row size or column size of
	 * this matrix is not a power of 2 number.
	 * 
	 * @param scale
	 *            if true then scaling is performed
	 * 
	 */
	public abstract void idst2(boolean scale);

	/**
	 * Computes the inverse of the discrete sine transform (DST-III) of each
	 * column of this matrix. Throws IllegalArgumentException if the column size
	 * of this matrix is not a power of 2 number.
	 * 
	 * @param scale
	 *            if true then scaling is performed
	 * 
	 */
	public abstract void idstColumns(boolean scale);

	/**
	 * Computes the inverse of the discrete sine transform (DST-III) of each row
	 * of this matrix. Throws IllegalArgumentException if the row size of this
	 * matrix is not a power of 2 number.
	 * 
	 * @param scale
	 *            if true then scaling is performed
	 * 
	 */
	public abstract void idstRows(boolean scale);

	/**
	 * Construct and returns a new empty matrix <i>of the same dynamic type</i>
	 * as the receiver, having the same number of rows and columns. For example,
	 * if the receiver is an instance of type <tt>DenseFloatMatrix2D</tt> the
	 * new matrix must also be of type <tt>DenseFloatMatrix2D</tt>, if the
	 * receiver is an instance of type <tt>SparseFloatMatrix2D</tt> the new
	 * matrix must also be of type <tt>SparseFloatMatrix2D</tt>, etc. In
	 * general, the new matrix should have internal parametrization as similar
	 * as possible.
	 * 
	 * @return a new empty matrix of the same dynamic type.
	 */
	public FloatMatrix2D like() {
		return like(rows, columns);
	}

	/**
	 * Construct and returns a new empty matrix <i>of the same dynamic type</i>
	 * as the receiver, having the specified number of rows and columns. For
	 * example, if the receiver is an instance of type
	 * <tt>DenseFloatMatrix2D</tt> the new matrix must also be of type
	 * <tt>DenseFloatMatrix2D</tt>, if the receiver is an instance of type
	 * <tt>SparseFloatMatrix2D</tt> the new matrix must also be of type
	 * <tt>SparseFloatMatrix2D</tt>, etc. In general, the new matrix should
	 * have internal parametrization as similar as possible.
	 * 
	 * @param rows
	 *            the number of rows the matrix shall have.
	 * @param columns
	 *            the number of columns the matrix shall have.
	 * @return a new empty matrix of the same dynamic type.
	 */
	public abstract FloatMatrix2D like(int rows, int columns);

	/**
	 * Construct and returns a new 1-d matrix <i>of the corresponding dynamic
	 * type</i>, entirelly independent of the receiver. For example, if the
	 * receiver is an instance of type <tt>DenseFloatMatrix2D</tt> the new
	 * matrix must be of type <tt>DenseFloatMatrix1D</tt>, if the receiver is
	 * an instance of type <tt>SparseFloatMatrix2D</tt> the new matrix must be
	 * of type <tt>SparseFloatMatrix1D</tt>, etc.
	 * 
	 * @param size
	 *            the number of cells the matrix shall have.
	 * @return a new matrix of the corresponding dynamic type.
	 */
	public abstract FloatMatrix1D like1D(int size);

	/**
	 * Normalizes this matrix, i.e. makes the sum of all elements equal to 1.0
	 * 
	 */
	public void normalize() {
		float[] tmp = getMinLocation();
		float min = tmp[0];
		if (min != 0) {
			assign(FloatFunctions.minus(min));
		}
		float sumScaleFactor = zSum();
		sumScaleFactor = (float) (1.0 / sumScaleFactor);
		assign(FloatFunctions.mult(sumScaleFactor));
	}

	/**
	 * Sets the matrix cell at coordinate <tt>[row,column]</tt> to the
	 * specified value.
	 * 
	 * @param row
	 *            the index of the row-coordinate.
	 * @param column
	 *            the index of the column-coordinate.
	 * @param value
	 *            the value to be filled into the specified cell.
	 * @throws IndexOutOfBoundsException
	 *             if
	 *             <tt>column&lt;0 || column&gt;=columns() || row&lt;0 || row&gt;=rows()</tt>
	 */
	public void set(int row, int column, float value) {
		if (column < 0 || column >= columns || row < 0 || row >= rows)
			throw new IndexOutOfBoundsException("row:" + row + ", column:" + column);
		setQuick(row, column, value);
	}

	/**
	 * Sets the matrix cell at coordinate <tt>[row,column]</tt> to the
	 * specified value.
	 * 
	 * <p>
	 * Provided with invalid parameters this method may access illegal indexes
	 * without throwing any exception. <b>You should only use this method when
	 * you are absolutely sure that the coordinate is within bounds.</b>
	 * Precondition (unchecked):
	 * <tt>0 &lt;= column &lt; columns() && 0 &lt;= row &lt; rows()</tt>.
	 * 
	 * @param row
	 *            the index of the row-coordinate.
	 * @param column
	 *            the index of the column-coordinate.
	 * @param value
	 *            the value to be filled into the specified cell.
	 */
	public abstract void setQuick(int row, int column, float value);

	/**
	 * Constructs and returns a 2-dimensional array containing the cell values.
	 * The returned array <tt>values</tt> has the form
	 * <tt>values[row][column]</tt> and has the same number of rows and
	 * columns as the receiver.
	 * <p>
	 * The values are copied. So subsequent changes in <tt>values</tt> are not
	 * reflected in the matrix, and vice-versa.
	 * 
	 * @return an array filled with the values of the cells.
	 */
	public float[][] toArray() {
		final float[][] values = new float[rows][columns];
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
							float[] currentRow = values[r];
							for (int c = 0; c < columns; c++) {
								currentRow[c] = getQuick(r, c);
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
			for (int r = 0; r < rows; r++) {
				float[] currentRow = values[r];
				for (int c = 0; c < columns; c++) {
					currentRow[c] = getQuick(r, c);
				}
			}
		}
		return values;
	}

	/**
	 * Returns a string representation using default formatting.
	 * 
	 * @see cern.colt.matrix.floatalgo.Formatter
	 */
	public String toString() {
		return new cern.colt.matrix.floatalgo.Formatter().toString(this);
	}

	/**
	 * Returns a vector obtained by stacking the columns of the matrix on top of
	 * one another.
	 * 
	 * @return a vector of columns of this matrix.
	 */
	public abstract FloatMatrix1D vectorize();

	/**
	 * Constructs and returns a new <i>slice view</i> representing the rows of
	 * the given column. The returned view is backed by this matrix, so changes
	 * in the returned view are reflected in this matrix, and vice-versa. To
	 * obtain a slice view on subranges, construct a sub-ranging view (<tt>viewPart(...)</tt>),
	 * then apply this method to the sub-range view.
	 * <p>
	 * <b>Example:</b> <table border="0">
	 * <tr nowrap>
	 * <td valign="top">2 x 3 matrix: <br>
	 * 1, 2, 3<br>
	 * 4, 5, 6 </td>
	 * <td>viewColumn(0) ==></td>
	 * <td valign="top">Matrix1D of size 2:<br>
	 * 1, 4</td>
	 * </tr>
	 * </table>
	 * 
	 * @param column
	 *            the column to fix.
	 * @return a new slice view.
	 * @throws IndexOutOfBoundsException
	 *             if <tt>column < 0 || column >= columns()</tt>.
	 * @see #viewRow(int)
	 */
	public FloatMatrix1D viewColumn(int column) {
		checkColumn(column);
		int viewSize = this.rows;
		int viewZero = index(0, column);
		int viewStride = this.rowStride;
		return like1D(viewSize, viewZero, viewStride);
	}

	/**
	 * Constructs and returns a new <i>flip view</i> along the column axis.
	 * What used to be column <tt>0</tt> is now column <tt>columns()-1</tt>,
	 * ..., what used to be column <tt>columns()-1</tt> is now column
	 * <tt>0</tt>. The returned view is backed by this matrix, so changes in
	 * the returned view are reflected in this matrix, and vice-versa.
	 * <p>
	 * <b>Example:</b> <table border="0">
	 * <tr nowrap>
	 * <td valign="top">2 x 3 matrix: <br>
	 * 1, 2, 3<br>
	 * 4, 5, 6 </td>
	 * <td>columnFlip ==></td>
	 * <td valign="top">2 x 3 matrix:<br>
	 * 3, 2, 1 <br>
	 * 6, 5, 4</td>
	 * <td>columnFlip ==></td>
	 * <td valign="top">2 x 3 matrix: <br>
	 * 1, 2, 3<br>
	 * 4, 5, 6 </td>
	 * </tr>
	 * </table>
	 * 
	 * @return a new flip view.
	 * @see #viewRowFlip()
	 */
	public FloatMatrix2D viewColumnFlip() {
		return (FloatMatrix2D) (view().vColumnFlip());
	}

	/**
	 * Constructs and returns a new <i>dice (transposition) view</i>; Swaps
	 * axes; example: 3 x 4 matrix --> 4 x 3 matrix. The view has both
	 * dimensions exchanged; what used to be columns become rows, what used to
	 * be rows become columns. In other words:
	 * <tt>view.get(row,column)==this.get(column,row)</tt>. This is a
	 * zero-copy transposition, taking O(1), i.e. constant time. The returned
	 * view is backed by this matrix, so changes in the returned view are
	 * reflected in this matrix, and vice-versa. Use idioms like
	 * <tt>result = viewDice(A).copy()</tt> to generate an independent
	 * transposed matrix.
	 * <p>
	 * <b>Example:</b> <table border="0">
	 * <tr nowrap>
	 * <td valign="top">2 x 3 matrix: <br>
	 * 1, 2, 3<br>
	 * 4, 5, 6 </td>
	 * <td>transpose ==></td>
	 * <td valign="top">3 x 2 matrix:<br>
	 * 1, 4 <br>
	 * 2, 5 <br>
	 * 3, 6</td>
	 * <td>transpose ==></td>
	 * <td valign="top">2 x 3 matrix: <br>
	 * 1, 2, 3<br>
	 * 4, 5, 6 </td>
	 * </tr>
	 * </table>
	 * 
	 * @return a new dice view.
	 */
	public FloatMatrix2D viewDice() {
		return (FloatMatrix2D) (view().vDice());
	}

	/**
	 * Constructs and returns a new <i>sub-range view</i> that is a
	 * <tt>height x width</tt> sub matrix starting at <tt>[row,column]</tt>.
	 * 
	 * Operations on the returned view can only be applied to the restricted
	 * range. Any attempt to access coordinates not contained in the view will
	 * throw an <tt>IndexOutOfBoundsException</tt>.
	 * <p>
	 * <b>Note that the view is really just a range restriction:</b> The
	 * returned matrix is backed by this matrix, so changes in the returned
	 * matrix are reflected in this matrix, and vice-versa.
	 * <p>
	 * The view contains the cells from <tt>[row,column]</tt> to
	 * <tt>[row+height-1,column+width-1]</tt>, all inclusive. and has
	 * <tt>view.rows() == height; view.columns() == width;</tt>. A view's
	 * legal coordinates are again zero based, as usual. In other words, legal
	 * coordinates of the view range from <tt>[0,0]</tt> to
	 * <tt>[view.rows()-1==height-1,view.columns()-1==width-1]</tt>. As
	 * usual, any attempt to access a cell at a coordinate
	 * <tt>column&lt;0 || column&gt;=view.columns() || row&lt;0 || row&gt;=view.rows()</tt>
	 * will throw an <tt>IndexOutOfBoundsException</tt>.
	 * 
	 * @param row
	 *            The index of the row-coordinate.
	 * @param column
	 *            The index of the column-coordinate.
	 * @param height
	 *            The height of the box.
	 * @param width
	 *            The width of the box.
	 * @throws IndexOutOfBoundsException
	 *             if
	 *             <tt>column<0 || width<0 || column+width>columns() || row<0 || height<0 || row+height>rows()</tt>
	 * @return the new view.
	 * 
	 */
	public FloatMatrix2D viewPart(int row, int column, int height, int width) {
		return (FloatMatrix2D) (view().vPart(row, column, height, width));
	}

	/**
	 * Constructs and returns a new <i>slice view</i> representing the columns
	 * of the given row. The returned view is backed by this matrix, so changes
	 * in the returned view are reflected in this matrix, and vice-versa. To
	 * obtain a slice view on subranges, construct a sub-ranging view (<tt>viewPart(...)</tt>),
	 * then apply this method to the sub-range view.
	 * <p>
	 * <b>Example:</b> <table border="0">
	 * <tr nowrap>
	 * <td valign="top">2 x 3 matrix: <br>
	 * 1, 2, 3<br>
	 * 4, 5, 6 </td>
	 * <td>viewRow(0) ==></td>
	 * <td valign="top">Matrix1D of size 3:<br>
	 * 1, 2, 3</td>
	 * </tr>
	 * </table>
	 * 
	 * @param row
	 *            the row to fix.
	 * @return a new slice view.
	 * @throws IndexOutOfBoundsException
	 *             if <tt>row < 0 || row >= rows()</tt>.
	 * @see #viewColumn(int)
	 */
	public FloatMatrix1D viewRow(int row) {
		checkRow(row);
		int viewSize = this.columns;
		int viewZero = index(row, 0);
		int viewStride = this.columnStride;
		return like1D(viewSize, viewZero, viewStride);
	}

	/**
	 * Constructs and returns a new <i>flip view</i> along the row axis. What
	 * used to be row <tt>0</tt> is now row <tt>rows()-1</tt>, ..., what
	 * used to be row <tt>rows()-1</tt> is now row <tt>0</tt>. The returned
	 * view is backed by this matrix, so changes in the returned view are
	 * reflected in this matrix, and vice-versa.
	 * <p>
	 * <b>Example:</b> <table border="0">
	 * <tr nowrap>
	 * <td valign="top">2 x 3 matrix: <br>
	 * 1, 2, 3<br>
	 * 4, 5, 6 </td>
	 * <td>rowFlip ==></td>
	 * <td valign="top">2 x 3 matrix:<br>
	 * 4, 5, 6 <br>
	 * 1, 2, 3</td>
	 * <td>rowFlip ==></td>
	 * <td valign="top">2 x 3 matrix: <br>
	 * 1, 2, 3<br>
	 * 4, 5, 6 </td>
	 * </tr>
	 * </table>
	 * 
	 * @return a new flip view.
	 * @see #viewColumnFlip()
	 */
	public FloatMatrix2D viewRowFlip() {
		return (FloatMatrix2D) (view().vRowFlip());
	}

	/**
	 * Constructs and returns a new <i>selection view</i> that is a matrix
	 * holding all <b>rows</b> matching the given condition. Applies the
	 * condition to each row and takes only those row where
	 * <tt>condition.apply(viewRow(i))</tt> yields <tt>true</tt>. To match
	 * columns, use a dice view.
	 * <p>
	 * <b>Example:</b> <br>
	 * 
	 * <pre>
	 * 	 // extract and view all rows which have a value &lt; threshold in the first column (representing &quot;age&quot;)
	 * 	 final float threshold = 16;
	 * 	 matrix.viewSelection( 
	 * 	    new FloatMatrix1DProcedure() {
	 * 	       public final boolean apply(FloatMatrix1D m) { return m.get(0) &lt; threshold; }
	 * 	    }
	 * 	 );
	 * 
	 * 	 // extract and view all rows with RMS &lt; threshold
	 * 	 // The RMS (Root-Mean-Square) is a measure of the average &quot;size&quot; of the elements of a data sequence.
	 * 	 matrix = 0 1 2 3
	 * 	 final float threshold = 0.5;
	 * 	 matrix.viewSelection( 
	 * 	    new FloatMatrix1DProcedure() {
	 * 	       public final boolean apply(FloatMatrix1D m) { return Math.sqrt(m.aggregate(F.plus,F.square) / m.size()) &lt; threshold; }
	 * 	    }
	 * 	 );
	 * 	
	 * </pre>
	 * 
	 * For further examples, see the <a
	 * href="package-summary.html#FunctionObjects">package doc</a>. The
	 * returned view is backed by this matrix, so changes in the returned view
	 * are reflected in this matrix, and vice-versa.
	 * 
	 * @param condition
	 *            The condition to be matched.
	 * @return the new view.
	 */
	public FloatMatrix2D viewSelection(FloatMatrix1DProcedure condition) {
		IntArrayList matches = new IntArrayList();
		for (int i = 0; i < rows; i++) {
			if (condition.apply(viewRow(i)))
				matches.add(i);
		}

		matches.trimToSize();
		return viewSelection(matches.elements(), null); // take all columns
	}

	/**
	 * Constructs and returns a new <i>selection view</i> that is a matrix
	 * holding the indicated cells. There holds
	 * <tt>view.rows() == rowIndexes.length, view.columns() == columnIndexes.length</tt>
	 * and <tt>view.get(i,j) == this.get(rowIndexes[i],columnIndexes[j])</tt>.
	 * Indexes can occur multiple times and can be in arbitrary order.
	 * <p>
	 * <b>Example:</b>
	 * 
	 * <pre>
	 * 	 this = 2 x 3 matrix:
	 * 	 1, 2, 3
	 * 	 4, 5, 6
	 * 	 rowIndexes     = (0,1)
	 * 	 columnIndexes  = (1,0,1,0)
	 * 	 --&gt;
	 * 	 view = 2 x 4 matrix:
	 * 	 2, 1, 2, 1
	 * 	 5, 4, 5, 4
	 * 	
	 * </pre>
	 * 
	 * Note that modifying the index arguments after this call has returned has
	 * no effect on the view. The returned view is backed by this matrix, so
	 * changes in the returned view are reflected in this matrix, and
	 * vice-versa.
	 * <p>
	 * To indicate "all" rows or "all columns", simply set the respective
	 * parameter
	 * 
	 * @param rowIndexes
	 *            The rows of the cells that shall be visible in the new view.
	 *            To indicate that <i>all</i> rows shall be visible, simply set
	 *            this parameter to <tt>null</tt>.
	 * @param columnIndexes
	 *            The columns of the cells that shall be visible in the new
	 *            view. To indicate that <i>all</i> columns shall be visible,
	 *            simply set this parameter to <tt>null</tt>.
	 * @return the new view.
	 * @throws IndexOutOfBoundsException
	 *             if <tt>!(0 <= rowIndexes[i] < rows())</tt> for any
	 *             <tt>i=0..rowIndexes.length()-1</tt>.
	 * @throws IndexOutOfBoundsException
	 *             if <tt>!(0 <= columnIndexes[i] < columns())</tt> for any
	 *             <tt>i=0..columnIndexes.length()-1</tt>.
	 */
	public FloatMatrix2D viewSelection(int[] rowIndexes, int[] columnIndexes) {
		// check for "all"
		if (rowIndexes == null) {
			rowIndexes = new int[rows];
			for (int i = 0; i < rows; i++)
				rowIndexes[i] = i;
		}
		if (columnIndexes == null) {
			columnIndexes = new int[columns];
			for (int i = 0; i < columns; i++)
				columnIndexes[i] = i;
		}

		checkRowIndexes(rowIndexes);
		checkColumnIndexes(columnIndexes);
		int[] rowOffsets = new int[rowIndexes.length];
		int[] columnOffsets = new int[columnIndexes.length];
		for (int i = 0; i < rowIndexes.length; i++) {
			rowOffsets[i] = _rowOffset(_rowRank(rowIndexes[i]));
		}
		for (int i = 0; i < columnIndexes.length; i++) {
			columnOffsets[i] = _columnOffset(_columnRank(columnIndexes[i]));
		}
		return viewSelectionLike(rowOffsets, columnOffsets);
	}

	public FloatMatrix2D viewSelection(Set<int[]> indexes) {
		int n = indexes.size();
		int[] rowIndexes = new int[n];
		int[] columnIndexes = new int[n];
		int idx = 0;
		for (Iterator<int[]> iterator = indexes.iterator(); iterator.hasNext();) {
			int[] is = (int[]) iterator.next();
			rowIndexes[idx] = is[0];
			columnIndexes[idx] = is[1];
			idx++;
		}
		checkRowIndexes(rowIndexes);
		checkColumnIndexes(columnIndexes);
		int[] rowOffsets = new int[rowIndexes.length];
		int[] columnOffsets = new int[columnIndexes.length];
		for (int i = 0; i < rowIndexes.length; i++) {
			rowOffsets[i] = _rowOffset(_rowRank(rowIndexes[i]));
		}
		for (int i = 0; i < columnIndexes.length; i++) {
			columnOffsets[i] = _columnOffset(_columnRank(columnIndexes[i]));
		}
		return viewSelectionLike(rowOffsets, columnOffsets);
	}

	/**
	 * Sorts the matrix rows into ascending order, according to the <i>natural
	 * ordering</i> of the matrix values in the given column. This sort is
	 * guaranteed to be <i>stable</i>. For further information, see
	 * {@link cern.colt.matrix.floatalgo.Sorting#sort(FloatMatrix2D,int)}. For
	 * more advanced sorting functionality, see
	 * {@link cern.colt.matrix.floatalgo.Sorting}.
	 * 
	 * @return a new sorted vector (matrix) view.
	 * @throws IndexOutOfBoundsException
	 *             if <tt>column < 0 || column >= columns()</tt>.
	 */
	public FloatMatrix2D viewSorted(int column) {
		return cern.colt.matrix.floatalgo.Sorting.mergeSort.sort(this, column);
	}

	/**
	 * Constructs and returns a new <i>stride view</i> which is a sub matrix
	 * consisting of every i-th cell. More specifically, the view has
	 * <tt>this.rows()/rowStride</tt> rows and
	 * <tt>this.columns()/columnStride</tt> columns holding cells
	 * <tt>this.get(i*rowStride,j*columnStride)</tt> for all
	 * <tt>i = 0..rows()/rowStride - 1, j = 0..columns()/columnStride - 1</tt>.
	 * The returned view is backed by this matrix, so changes in the returned
	 * view are reflected in this matrix, and vice-versa.
	 * 
	 * @param rowStride
	 *            the row step factor.
	 * @param columnStride
	 *            the column step factor.
	 * @return a new view.
	 * @throws IndexOutOfBoundsException
	 *             if <tt>rowStride<=0 || columnStride<=0</tt>.
	 */
	public FloatMatrix2D viewStrides(int rowStride, int columnStride) {
		return (FloatMatrix2D) (view().vStrides(rowStride, columnStride));
	}

	/**
	 * 8 neighbor stencil transformation. For efficient finite difference
	 * operations. Applies a function to a moving <tt>3 x 3</tt> window. Does
	 * nothing if <tt>rows() < 3 || columns() < 3</tt>.
	 * 
	 * <pre>
	 * 	 B[i,j] = function.apply(
	 * 	    A[i-1,j-1], A[i-1,j], A[i-1,j+1],
	 * 	    A[i,  j-1], A[i,  j], A[i,  j+1],
	 * 	    A[i+1,j-1], A[i+1,j], A[i+1,j+1]
	 * 	    )
	 * 
	 * 	 x x x -     - x x x     - - - - 
	 * 	 x o x -     - x o x     - - - - 
	 * 	 x x x -     - x x x ... - x x x 
	 * 	 - - - -     - - - -     - x o x 
	 * 	 - - - -     - - - -     - x x x 
	 * 	
	 * </pre>
	 * 
	 * Make sure that cells of <tt>this</tt> and <tt>B</tt> do not overlap.
	 * In case of overlapping views, behaviour is unspecified.
	 * 
	 * </pre>
	 * 
	 * <p>
	 * <b>Example:</b>
	 * 
	 * <pre>
	 * 
	 * final float alpha = 0.25; final float beta = 0.75; // 8 neighbors
	 * cern.colt.function.Float9Function f = new
	 * cern.colt.function.Float9Function() { &nbsp;&nbsp;&nbsp;public final
	 * float apply( &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;float a00, float a01,
	 * float a02, &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;float a10, float a11,
	 * float a12, &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;float a20, float a21,
	 * float a22) { &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;return
	 * beta*a11 + alpha*(a00+a01+a02 + a10+a12 + a20+a21+a22);
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;} }; A.zAssign8Neighbors(B,f); // 4
	 * neighbors cern.colt.function.Float9Function g = new
	 * cern.colt.function.Float9Function() { &nbsp;&nbsp;&nbsp;public final
	 * float apply( &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;float a00, float a01,
	 * float a02, &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;float a10, float a11,
	 * float a12, &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;float a20, float a21,
	 * float a22) { &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;return beta*a11 +
	 * alpha*(a01+a10+a12+a21); &nbsp;&nbsp;&nbsp;} C.zAssign8Neighbors(B,g); //
	 * fast, even though it doesn't look like it };
	 * 
	 * </pre>
	 * 
	 * @param B
	 *            the matrix to hold the results.
	 * @param function
	 *            the function to be applied to the 9 cells.
	 * @throws NullPointerException
	 *             if <tt>function==null</tt>.
	 * @throws IllegalArgumentException
	 *             if <tt>rows() != B.rows() || columns() != B.columns()</tt>.
	 */
	public void zAssign8Neighbors(FloatMatrix2D B, cern.colt.function.Float9Function function) {
		if (function == null)
			throw new NullPointerException("function must not be null.");
		checkShape(B);
		if (rows < 3 || columns < 3)
			return; // nothing to do
		int r = rows - 1;
		int c = columns - 1;
		float a00, a01, a02;
		float a10, a11, a12;
		float a20, a21, a22;
		for (int i = 1; i < r; i++) {
			a00 = getQuick(i - 1, 0);
			a01 = getQuick(i - 1, 1);
			a10 = getQuick(i, 0);
			a11 = getQuick(i, 1);
			a20 = getQuick(i + 1, 0);
			a21 = getQuick(i + 1, 1);

			for (int j = 1; j < c; j++) {
				// in each step six cells can be remembered in registers - they
				// don't need to be reread from slow memory
				// in each step 3 instead of 9 cells need to be read from
				// memory.
				a02 = getQuick(i - 1, j + 1);
				a12 = getQuick(i, j + 1);
				a22 = getQuick(i + 1, j + 1);

				B.setQuick(i, j, function.apply(a00, a01, a02, a10, a11, a12, a20, a21, a22));

				a00 = a01;
				a10 = a11;
				a20 = a21;

				a01 = a02;
				a11 = a12;
				a21 = a22;
			}
		}
	}

	/**
	 * Linear algebraic matrix-vector multiplication; <tt>z = A * y</tt>;
	 * Equivalent to <tt>return A.zMult(y,z,1,0);</tt>
	 */
	public FloatMatrix1D zMult(FloatMatrix1D y, FloatMatrix1D z) {
		return zMult(y, z, 1, (z == null ? 1 : 0), false);
	}

	/**
	 * Linear algebraic matrix-vector multiplication;
	 * <tt>z = alpha * A * y + beta*z</tt>.
	 * <tt>z[i] = alpha*Sum(A[i,j] * y[j]) + beta*z[i], i=0..A.rows()-1, j=0..y.size()-1</tt>.
	 * Where <tt>A == this</tt>. <br>
	 * Note: Matrix shape conformance is checked <i>after</i> potential
	 * transpositions.
	 * 
	 * @param y
	 *            the source vector.
	 * @param z
	 *            the vector where results are to be stored. Set this parameter
	 *            to <tt>null</tt> to indicate that a new result vector shall
	 *            be constructed.
	 * @return z (for convenience only).
	 * 
	 * @throws IllegalArgumentException
	 *             if <tt>A.columns() != y.size() || A.rows() > z.size())</tt>.
	 */
	public FloatMatrix1D zMult(final FloatMatrix1D y, FloatMatrix1D z, final float alpha, final float beta, final boolean transposeA) {
		if (transposeA)
			return viewDice().zMult(y, z, alpha, beta, false);
		final FloatMatrix1D z_loc;
		if (z == null) {
			z_loc = new DenseFloatMatrix1D(this.rows);
		} else {
			z_loc = z;
		}
		if (columns != y.size() || rows > z_loc.size())
			throw new IllegalArgumentException("Incompatible args: " + toStringShort() + ", " + y.toStringShort() + ", " + z_loc.toStringShort());

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
					float[] s = new float[2];

					public void run() {
						for (int r = startrow; r < stoprow; r++) {
							float s = 0;
							for (int c = 0; c < columns; c++) {
								s += getQuick(r, c) * y.getQuick(c);
							}
							z_loc.setQuick(r, alpha * s + beta * z_loc.getQuick(r));
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
				float s = 0;
				for (int c = 0; c < columns; c++) {
					s += getQuick(r, c) * y.getQuick(c);
				}
				z_loc.setQuick(r, alpha * s + beta * z_loc.getQuick(r));
			}
		}
		z = z_loc;
		return z;
	}

	/**
	 * Linear algebraic matrix-matrix multiplication; <tt>C = A x B</tt>;
	 * Equivalent to <tt>A.zMult(B,C,1,0,false,false)</tt>.
	 */
	public FloatMatrix2D zMult(FloatMatrix2D B, FloatMatrix2D C) {
		return zMult(B, C, 1, (C == null ? 1 : 0), false, false);
	}

	/**
	 * Linear algebraic matrix-matrix multiplication;
	 * <tt>C = alpha * A x B + beta*C</tt>.
	 * <tt>C[i,j] = alpha*Sum(A[i,k] * B[k,j]) + beta*C[i,j], k=0..n-1</tt>.
	 * <br>
	 * Matrix shapes: <tt>A(m x n), B(n x p), C(m x p)</tt>. <br>
	 * Note: Matrix shape conformance is checked <i>after</i> potential
	 * transpositions.
	 * 
	 * @param B
	 *            the second source matrix.
	 * @param C
	 *            the matrix where results are to be stored. Set this parameter
	 *            to <tt>null</tt> to indicate that a new result matrix shall
	 *            be constructed.
	 * @return C (for convenience only).
	 * 
	 * @throws IllegalArgumentException
	 *             if <tt>B.rows() != A.columns()</tt>.
	 * @throws IllegalArgumentException
	 *             if
	 *             <tt>C.rows() != A.rows() || C.columns() != B.columns()</tt>.
	 * @throws IllegalArgumentException
	 *             if <tt>A == C || B == C</tt>.
	 */
	public FloatMatrix2D zMult(final FloatMatrix2D B, FloatMatrix2D C, final float alpha, final float beta, final boolean transposeA, final boolean transposeB) {
		if (transposeA)
			return viewDice().zMult(B, C, alpha, beta, false, transposeB);
		if (transposeB)
			return this.zMult(B.viewDice(), C, alpha, beta, transposeA, false);

		final int m = rows;
		final int n = columns;
		final int p = B.columns;
		final FloatMatrix2D C_loc;
		if (C == null) {
			C_loc = new DenseFloatMatrix2D(m, p);
		} else {
			C_loc = C;
		}
		if (B.rows != n)
			throw new IllegalArgumentException("Matrix2D inner dimensions must agree:" + toStringShort() + ", " + B.toStringShort());
		if (C_loc.rows != m || C_loc.columns != p)
			throw new IllegalArgumentException("Incompatibe result matrix: " + toStringShort() + ", " + B.toStringShort() + ", " + C_loc.toStringShort());
		if (this == C_loc || B == C_loc)
			throw new IllegalArgumentException("Matrices must not be identical");
		int np = Utils.getNP();
		if ((np > 1) && (size() >= Utils.getThreadsBeginN_2D())) {
			Future[] futures = new Future[np];
			int k = p / np;
			for (int j = 0; j < np; j++) {
				final int starta = j * k;
				final int stopa;
				if (j == np - 1) {
					stopa = p;
				} else {
					stopa = starta + k;
				}
				futures[j] = Utils.threadPool.submit(new Runnable() {
					public void run() {
						for (int a = starta; a < stopa; a++) {
							for (int b = 0; b < m; b++) {
								float s = 0;
								for (int c = 0; c < n; c++) {
									s += getQuick(b, c) * B.getQuick(c, a);
								}
								C_loc.setQuick(b, a, alpha * s + beta * C_loc.getQuick(b, a));
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
			for (int a = 0; a < p; a++) {
				for (int b = 0; b < m; b++) {
					float s = 0;
					for (int c = 0; c < n; c++) {
						s += getQuick(b, c) * B.getQuick(c, a);
					}
					C_loc.setQuick(b, a, alpha * s + beta * C_loc.getQuick(b, a));
				}
			}
		}
		C = C_loc;
		return C;
	}

	/**
	 * Returns the sum of all cells; <tt>Sum( x[i,j] )</tt>.
	 * 
	 * @return the sum.
	 */
	public float zSum() {
		if (size() == 0)
			return 0;
		return aggregate(cern.jet.math.FloatFunctions.plus, cern.jet.math.FloatFunctions.identity);
	}

	/**
	 * Returns the content of this matrix if it is a wrapper; or <tt>this</tt>
	 * otherwise. Override this method in wrappers.
	 */
	protected FloatMatrix2D getContent() {
		return this;
	}

	/**
	 * Returns <tt>true</tt> if both matrices share at least one identical
	 * cell.
	 */
	protected boolean haveSharedCells(FloatMatrix2D other) {
		if (other == null)
			return false;
		if (this == other)
			return true;
		return getContent().haveSharedCellsRaw(other.getContent());
	}

	/**
	 * Returns <tt>true</tt> if both matrices share at least one identical
	 * cell.
	 */
	protected boolean haveSharedCellsRaw(FloatMatrix2D other) {
		return false;
	}

	/**
	 * Construct and returns a new 1-d matrix <i>of the corresponding dynamic
	 * type</i>, sharing the same cells. For example, if the receiver is an
	 * instance of type <tt>DenseFloatMatrix2D</tt> the new matrix must be of
	 * type <tt>DenseFloatMatrix1D</tt>, if the receiver is an instance of
	 * type <tt>SparseFloatMatrix2D</tt> the new matrix must be of type
	 * <tt>SparseFloatMatrix1D</tt>, etc.
	 * 
	 * @param size
	 *            the number of cells the matrix shall have.
	 * @param zero
	 *            the index of the first element.
	 * @param stride
	 *            the number of indexes between any two elements, i.e.
	 *            <tt>index(i+1)-index(i)</tt>.
	 * @return a new matrix of the corresponding dynamic type.
	 */
	protected abstract FloatMatrix1D like1D(int size, int zero, int stride);

	/**
	 * Constructs and returns a new view equal to the receiver. The view is a
	 * shallow clone. Calls <code>clone()</code> and casts the result.
	 * <p>
	 * <b>Note that the view is not a deep copy.</b> The returned matrix is
	 * backed by this matrix, so changes in the returned matrix are reflected in
	 * this matrix, and vice-versa.
	 * <p>
	 * Use {@link #copy()} to construct an independent deep copy rather than a
	 * new view.
	 * 
	 * @return a new view of the receiver.
	 */
	protected FloatMatrix2D view() {
		return (FloatMatrix2D) clone();
	}

	/**
	 * Construct and returns a new selection view.
	 * 
	 * @param rowOffsets
	 *            the offsets of the visible elements.
	 * @param columnOffsets
	 *            the offsets of the visible elements.
	 * @return a new view.
	 */
	protected abstract FloatMatrix2D viewSelectionLike(int[] rowOffsets, int[] columnOffsets);
}
