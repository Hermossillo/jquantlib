/*
Copyright � 1999 CERN - European Organization for Nuclear Research.
Permission to use, copy, modify, distribute and sell this software and its documentation for any purpose 
is hereby granted without fee, provided that the above copyright notice appear in all copies and 
that both that copyright notice and this permission notice appear in supporting documentation. 
CERN makes no representations about the suitability of this software for any purpose. 
It is provided "as is" without expressed or implied warranty.
 */
package cern.colt.matrix.impl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import cern.colt.Utils;
import cern.colt.matrix.FComplexMatrix1D;
import cern.colt.matrix.FComplexMatrix2D;
import cern.colt.matrix.FloatMatrix2D;

/**
 * Selection view on dense 2-d matrices holding <tt>complex</tt> elements.
 * <b>Implementation:</b>
 * <p>
 * Objects of this class are typically constructed via <tt>viewIndexes</tt>
 * methods on some source matrix. The interface introduced in abstract super
 * classes defines everything a user can do. From a user point of view there is
 * nothing special about this class; it presents the same functionality with the
 * same signatures and semantics as its abstract superclass(es) while
 * introducing no additional functionality. Thus, this class need not be visible
 * to users.
 * <p>
 * This class uses no delegation. Its instances point directly to the data. Cell
 * addressing overhead is 1 additional int addition and 2 additional array index
 * accesses per get/set.
 * <p>
 * Note that this implementation is not synchronized.
 * <p>
 * <b>Memory requirements:</b>
 * <p>
 * <tt>memory [bytes] = 4*(rowIndexes.length+columnIndexes.length)</tt>.
 * Thus, an index view with 1000 x 1000 indexes additionally uses 8 KB.
 * <p>
 * <b>Time complexity:</b>
 * <p>
 * Depends on the parent view holding cells.
 * <p>
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * @version 1.0, 11/26/2007
 */
class SelectedDenseFComplexMatrix2D extends FComplexMatrix2D {

	private static final long serialVersionUID = -6212926540247811567L;

	/**
	 * The elements of this matrix.
	 */
	protected float[] elements;

	/**
	 * The offsets of the visible cells of this matrix.
	 */
	protected int[] rowOffsets;

	protected int[] columnOffsets;

	/**
	 * The offset.
	 */
	protected int offset;

	/**
	 * Constructs a matrix view with the given parameters.
	 * 
	 * @param elements
	 *            the cells.
	 * @param rowOffsets
	 *            The row offsets of the cells that shall be visible.
	 * @param columnOffsets
	 *            The column offsets of the cells that shall be visible.
	 * @param offset
	 */
	protected SelectedDenseFComplexMatrix2D(float[] elements, int[] rowOffsets, int[] columnOffsets, int offset) {
		this(rowOffsets.length, columnOffsets.length, elements, 0, 0, 1, 1, rowOffsets, columnOffsets, offset);
	}

	/**
	 * Constructs a matrix view with the given parameters.
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
	 * @param rowOffsets
	 *            The row offsets of the cells that shall be visible.
	 * @param columnOffsets
	 *            The column offsets of the cells that shall be visible.
	 * @param offset
	 */
	protected SelectedDenseFComplexMatrix2D(int rows, int columns, float[] elements, int rowZero, int columnZero, int rowStride, int columnStride, int[] rowOffsets, int[] columnOffsets, int offset) {
		// be sure parameters are valid, we do not check...
		setUp(rows, columns, rowZero, columnZero, rowStride, columnStride);

		this.elements = elements;
		this.rowOffsets = rowOffsets;
		this.columnOffsets = columnOffsets;
		this.offset = offset;

		this.isNoView = false;
	}

	/**
	 * Returns the position of the given absolute rank within the (virtual or
	 * non-virtual) internal 1-dimensional array. Default implementation.
	 * Override, if necessary.
	 * 
	 * @param rank
	 *            the absolute rank of the element.
	 * @return the position.
	 */
	protected int _columnOffset(int absRank) {
		return columnOffsets[absRank];
	}

	/**
	 * Returns the position of the given absolute rank within the (virtual or
	 * non-virtual) internal 1-dimensional array. Default implementation.
	 * Override, if necessary.
	 * 
	 * @param rank
	 *            the absolute rank of the element.
	 * @return the position.
	 */
	protected int _rowOffset(int absRank) {
		return rowOffsets[absRank];
	}

	public void fft2() {
		throw new IllegalArgumentException("This method is not supported yet");
	}

	public void ifft2(boolean scale) {
		throw new IllegalArgumentException("This method is not supported yet");
	}

	public void fftRows() {
		throw new IllegalArgumentException("This method is not supported yet");
	}

	public void ifftRows(boolean scale) {
		throw new IllegalArgumentException("This method is not supported yet");
	}

	public void fftColumns() {
		throw new IllegalArgumentException("This method is not supported yet");
	}

	public void ifftColumns(boolean scale) {
		throw new IllegalArgumentException("This method is not supported yet");
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
	public float[] getQuick(int row, int column) {
		int idxr = rowZero + row * rowStride;
		int idxc = columnZero + column * columnStride;
		return new float[] { elements[offset + rowOffsets[idxr] + columnOffsets[idxc]], elements[offset + rowOffsets[idxr] + columnOffsets[idxc] + 1] };
	}

	/**
	 * This method is not supported for SelectedDenseComplexMatrix2D.
	 * 
	 * @return
	 * @throws IllegalAccessException
	 *             always.
	 */
	public float[] getElements() {
		throw new IllegalAccessError("getElements() is not supported for SelectedDenseComplexMatrix2D.");

	}

	/**
	 * Returns <tt>true</tt> if both matrices share common cells. More
	 * formally, returns <tt>true</tt> if <tt>other != null</tt> and at
	 * least one of the following conditions is met
	 * <ul>
	 * <li>the receiver is a view of the other matrix
	 * <li>the other matrix is a view of the receiver
	 * <li><tt>this == other</tt>
	 * </ul>
	 */
	protected boolean haveSharedCellsRaw(FComplexMatrix2D other) {
		if (other instanceof SelectedDenseFComplexMatrix2D) {
			SelectedDenseFComplexMatrix2D otherMatrix = (SelectedDenseFComplexMatrix2D) other;
			return this.elements == otherMatrix.elements;
		} else if (other instanceof DenseFComplexMatrix2D) {
			DenseFComplexMatrix2D otherMatrix = (DenseFComplexMatrix2D) other;
			return this.elements == otherMatrix.elements;
		}
		return false;
	}

	/**
	 * Returns the position of the given coordinate within the (virtual or
	 * non-virtual) internal 1-dimensional array.
	 * 
	 * @param row
	 *            the index of the row-coordinate.
	 * @param column
	 *            the index of the column-coordinate.
	 */
	protected int index(int row, int column) {
		return this.offset + rowOffsets[rowZero + row * rowStride] + columnOffsets[columnZero + column * columnStride];
	}

	/**
	 * Construct and returns a new empty matrix <i>of the same dynamic type</i>
	 * as the receiver, having the specified number of rows and columns. For
	 * example, if the receiver is an instance of type
	 * <tt>DenseComplexMatrix2D</tt> the new matrix must also be of type
	 * <tt>DenseComplexMatrix2D</tt>. In general, the new matrix should have
	 * internal parametrization as similar as possible.
	 * 
	 * @param rows
	 *            the number of rows the matrix shall have.
	 * @param columns
	 *            the number of columns the matrix shall have.
	 * @return a new empty matrix of the same dynamic type.
	 */
	public FComplexMatrix2D like(int rows, int columns) {
		return new DenseFComplexMatrix2D(rows, columns);
	}

	/**
	 * Construct and returns a new 1-d matrix <i>of the corresponding dynamic
	 * type</i>, entirelly independent of the receiver. For example, if the
	 * receiver is an instance of type <tt>DenseComplexMatrix2D</tt> the new
	 * matrix must be of type <tt>DenseComplexMatrix1D</tt>.
	 * 
	 * @param size
	 *            the number of cells the matrix shall have.
	 * @return a new matrix of the corresponding dynamic type.
	 */
	public FComplexMatrix1D like1D(int size) {
		return new DenseFComplexMatrix1D(size);
	}

	/**
	 * Construct and returns a new 1-d matrix <i>of the corresponding dynamic
	 * type</i>, sharing the same cells. For example, if the receiver is an
	 * instance of type <tt>DenseComplexMatrix2D</tt> the new matrix must be
	 * of type <tt>DenseComplexMatrix1D</tt>.
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
	protected FComplexMatrix1D like1D(int size, int zero, int stride) {
		throw new InternalError(); // this method is never called since
		// viewRow() and viewColumn are overridden
		// properly.
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
	public void setQuick(int row, int column, float[] value) {
		int idxr = rowZero + row * rowStride;
		int idxc = columnZero + column * columnStride;
		elements[offset + rowOffsets[idxr] + columnOffsets[idxc]] = value[0];
		elements[offset + rowOffsets[idxr] + columnOffsets[idxc] + 1] = value[1];
	}

	/**
	 * Returns a vector obtained by stacking the columns of the matrix on top of
	 * one another.
	 * 
	 * @return
	 */
	public FComplexMatrix1D vectorize() {
		DenseFComplexMatrix1D v = new DenseFComplexMatrix1D(size());
		int idx = 0;
		for (int c = 0; c < columns; c++) {
			for (int r = 0; r < rows; r++) {
				v.setQuick(idx++, getQuick(c, r));
			}
		}
		return v;
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
	 * @param re
	 *            the real part of the value to be filled into the specified
	 *            cell.
	 * @param im
	 *            the imaginary part of the value to be filled into the
	 *            specified cell.
	 */
	public void setQuick(int row, int column, float re, float im) {
		int idxr = rowZero + row * rowStride;
		int idxc = columnZero + column * columnStride;
		elements[offset + rowOffsets[idxr] + columnOffsets[idxc]] = re;
		elements[offset + rowOffsets[idxr] + columnOffsets[idxc] + 1] = im;
	}

	/**
	 * Sets up a matrix with a given number of rows and columns.
	 * 
	 * @param rows
	 *            the number of rows the matrix shall have.
	 * @param columns
	 *            the number of columns the matrix shall have.
	 * @throws IllegalArgumentException
	 *             if <tt>(float)columns*rows > Integer.MAX_VALUE</tt>.
	 */
	protected void setUp(int rows, int columns) {
		super.setUp(rows, columns);
		this.rowStride = 1;
		this.columnStride = 2;
		this.offset = 0;
	}

	/**
	 * Self modifying version of viewDice().
	 */
	protected AbstractMatrix2D vDice() {
		super.vDice();
		// swap
		int[] tmp = rowOffsets;
		rowOffsets = columnOffsets;
		columnOffsets = tmp;

		this.isNoView = false;
		return this;
	}

	/**
	 * Constructs and returns a new <i>slice view</i> representing the rows of
	 * the given column. The returned view is backed by this matrix, so changes
	 * in the returned view are reflected in this matrix, and vice-versa. To
	 * obtain a slice view on subranges, construct a sub-ranging view (<tt>viewPart(...)</tt>),
	 * then apply this method to the sub-range view.
	 * 
	 * @param the
	 *            column to fix.
	 * @return a new slice view.
	 * @throws IllegalArgumentException
	 *             if <tt>column < 0 || column >= columns()</tt>.
	 * @see #viewRow(int)
	 */
	public FComplexMatrix1D viewColumn(int column) {
		checkColumn(column);
		int viewSize = this.rows;
		int viewZero = this.rowZero;
		int viewStride = this.rowStride;
		int[] viewOffsets = this.rowOffsets;
		int viewOffset = this.offset + _columnOffset(_columnRank(column));
		return new SelectedDenseFComplexMatrix1D(viewSize, this.elements, viewZero, viewStride, viewOffsets, viewOffset);
	}

	/**
	 * Constructs and returns a new <i>slice view</i> representing the columns
	 * of the given row. The returned view is backed by this matrix, so changes
	 * in the returned view are reflected in this matrix, and vice-versa. To
	 * obtain a slice view on subranges, construct a sub-ranging view (<tt>viewPart(...)</tt>),
	 * then apply this method to the sub-range view.
	 * 
	 * @param the
	 *            row to fix.
	 * @return a new slice view.
	 * @throws IndexOutOfBoundsException
	 *             if <tt>row < 0 || row >= rows()</tt>.
	 * @see #viewColumn(int)
	 */
	public FComplexMatrix1D viewRow(int row) {
		checkRow(row);
		int viewSize = this.columns;
		int viewZero = columnZero;
		int viewStride = this.columnStride;
		int[] viewOffsets = this.columnOffsets;
		int viewOffset = this.offset + _rowOffset(_rowRank(row));
		return new SelectedDenseFComplexMatrix1D(viewSize, this.elements, viewZero, viewStride, viewOffsets, viewOffset);
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
	protected FComplexMatrix2D viewSelectionLike(int[] rowOffsets, int[] columnOffsets) {
		return new SelectedDenseFComplexMatrix2D(this.elements, rowOffsets, columnOffsets, this.offset);
	}

	/**
	 * Returns the real part of this matrix
	 * 
	 * @return the real part
	 */
	public FloatMatrix2D getRealPart() {
		final DenseFloatMatrix2D R = new DenseFloatMatrix2D(rows, columns);
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
						float[] tmp;
						for (int r = startrow; r < stoprow; r++) {
							for (int c = 0; c < columns; c++) {
								tmp = getQuick(r, c);
								R.setQuick(r, c, tmp[0]);
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
			float[] tmp;
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < columns; c++) {
					tmp = getQuick(r, c);
					R.setQuick(r, c, tmp[0]);
				}
			}
		}
		return R;
	}

	/**
	 * Returns the imaginary part of this matrix
	 * 
	 * @return the imaginary part
	 */
	public FloatMatrix2D getImaginaryPart() {
		final DenseFloatMatrix2D Im = new DenseFloatMatrix2D(rows, columns);
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
						float[] tmp;
						for (int r = startrow; r < stoprow; r++) {
							for (int c = 0; c < columns; c++) {
								tmp = getQuick(r, c);
								Im.setQuick(r, c, tmp[1]);
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
			float[] tmp;
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < columns; c++) {
					tmp = getQuick(r, c);
					Im.setQuick(r, c, tmp[1]);
				}
			}
		}
		return Im;
	}
}
