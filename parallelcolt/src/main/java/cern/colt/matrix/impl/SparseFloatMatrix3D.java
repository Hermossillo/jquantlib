/*
Copyright � 1999 CERN - European Organization for Nuclear Research.
Permission to use, copy, modify, distribute and sell this software and its documentation for any purpose 
is hereby granted without fee, provided that the above copyright notice appear in all copies and 
that both that copyright notice and this permission notice appear in supporting documentation. 
CERN makes no representations about the suitability of this software for any purpose. 
It is provided "as is" without expressed or implied warranty.
 */
package cern.colt.matrix.impl;

import cern.colt.map.AbstractIntFloatMap;
import cern.colt.map.OpenIntFloatHashMap;
import cern.colt.matrix.FComplexMatrix3D;
import cern.colt.matrix.FloatMatrix1D;
import cern.colt.matrix.FloatMatrix2D;
import cern.colt.matrix.FloatMatrix3D;

/**
 * Sparse hashed 3-d matrix holding <tt>float</tt> elements. First see the <a
 * href="package-summary.html">package summary</a> and javadoc <a
 * href="package-tree.html">tree view</a> to get the broad picture.
 * <p>
 * <b>Implementation:</b>
 * <p>
 * Note that this implementation is not synchronized. Uses a
 * {@link cern.colt.map.OpenIntFloatHashMap}, which is a compact and performant
 * hashing technique.
 * <p>
 * <b>Memory requirements:</b>
 * <p>
 * Cells that
 * <ul>
 * <li>are never set to non-zero values do not use any memory.
 * <li>switch from zero to non-zero state do use memory.
 * <li>switch back from non-zero to zero state also do use memory. However,
 * their memory is automatically reclaimed from time to time. It can also
 * manually be reclaimed by calling {@link #trimToSize()}.
 * </ul>
 * <p>
 * worst case: <tt>memory [bytes] = (1/minLoadFactor) * nonZeros * 13</tt>.
 * <br>
 * best case: <tt>memory [bytes] = (1/maxLoadFactor) * nonZeros * 13</tt>.
 * <br>
 * Where <tt>nonZeros = cardinality()</tt> is the number of non-zero cells.
 * Thus, a 100 x 100 x 100 matrix with minLoadFactor=0.25 and maxLoadFactor=0.5
 * and 1000000 non-zero cells consumes between 25 MB and 50 MB. The same 100 x
 * 100 x 100 matrix with 1000 non-zero cells consumes between 25 and 50 KB.
 * <p>
 * <b>Time complexity:</b>
 * <p>
 * This class offers <i>expected</i> time complexity <tt>O(1)</tt> (i.e.
 * constant time) for the basic operations <tt>get</tt>, <tt>getQuick</tt>,
 * <tt>set</tt>, <tt>setQuick</tt> and <tt>size</tt> assuming the hash
 * function disperses the elements properly among the buckets. Otherwise,
 * pathological cases, although highly improbable, can occur, degrading
 * performance to <tt>O(N)</tt> in the worst case. As such this sparse class
 * is expected to have no worse time complexity than its dense counterpart
 * {@link DenseFloatMatrix2D}. However, constant factors are considerably
 * larger.
 * <p>
 * Cells are internally addressed in (in decreasing order of significance):
 * slice major, row major, column major. Applications demanding utmost speed can
 * exploit this fact. Setting/getting values in a loop slice-by-slice,
 * row-by-row, column-by-column is quicker than, for example, column-by-column,
 * row-by-row, slice-by-slice. Thus
 * 
 * <pre>
 * for (int slice = 0; slice &lt; slices; slice++) {
 * 	for (int row = 0; row &lt; rows; row++) {
 * 		for (int column = 0; column &lt; columns; column++) {
 * 			matrix.setQuick(slice, row, column, someValue);
 * 		}
 * 	}
 * }
 * </pre>
 * 
 * is quicker than
 * 
 * <pre>
 * for (int column = 0; column &lt; columns; column++) {
 * 	for (int row = 0; row &lt; rows; row++) {
 * 		for (int slice = 0; slice &lt; slices; slice++) {
 * 			matrix.setQuick(slice, row, column, someValue);
 * 		}
 * 	}
 * }
 * </pre>
 * 
 * @see cern.colt.map
 * @see cern.colt.map.OpenIntFloatHashMap
 * @author wolfgang.hoschek@cern.ch
 * @version 1.0, 09/24/99
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * @version 1.1, 08/22/2007
 */
public class SparseFloatMatrix3D extends FloatMatrix3D {
	/*
	 * The elements of the matrix.
	 */
	protected AbstractIntFloatMap elements;

	/**
	 * Constructs a matrix with a copy of the given values. <tt>values</tt> is
	 * required to have the form <tt>values[slice][row][column]</tt> and have
	 * exactly the same number of rows in in every slice and exactly the same
	 * number of columns in in every row.
	 * <p>
	 * The values are copied. So subsequent changes in <tt>values</tt> are not
	 * reflected in the matrix, and vice-versa.
	 * 
	 * @param values
	 *            The values to be filled into the new matrix.
	 * @throws IllegalArgumentException
	 *             if
	 *             <tt>for any 1 &lt;= slice &lt; values.length: values[slice].length != values[slice-1].length</tt>.
	 * @throws IllegalArgumentException
	 *             if
	 *             <tt>for any 1 &lt;= row &lt; values[0].length: values[slice][row].length != values[slice][row-1].length</tt>.
	 */
	public SparseFloatMatrix3D(float[][][] values) {
		this(values.length, (values.length == 0 ? 0 : values[0].length), (values.length == 0 ? 0 : values[0].length == 0 ? 0 : values[0][0].length));
		assign(values);
	}

	/**
	 * Constructs a matrix with a given number of slices, rows and columns and
	 * default memory usage. All entries are initially <tt>0</tt>.
	 * 
	 * @param slices
	 *            the number of slices the matrix shall have.
	 * @param rows
	 *            the number of rows the matrix shall have.
	 * @param columns
	 *            the number of columns the matrix shall have.
	 * @throws IllegalArgumentException
	 *             if <tt>(float)slices*columns*rows > Integer.MAX_VALUE</tt>.
	 * @throws IllegalArgumentException
	 *             if <tt>slices<0 || rows<0 || columns<0</tt>.
	 */
	public SparseFloatMatrix3D(int slices, int rows, int columns) {
		this(slices, rows, columns, slices * rows * (columns / 1000), 0.2f, 0.5f);
	}

	/**
	 * Constructs a matrix with a given number of slices, rows and columns using
	 * memory as specified. All entries are initially <tt>0</tt>. For details
	 * related to memory usage see {@link cern.colt.map.OpenIntFloatHashMap}.
	 * 
	 * @param slices
	 *            the number of slices the matrix shall have.
	 * @param rows
	 *            the number of rows the matrix shall have.
	 * @param columns
	 *            the number of columns the matrix shall have.
	 * @param initialCapacity
	 *            the initial capacity of the hash map. If not known, set
	 *            <tt>initialCapacity=0</tt> or small.
	 * @param minLoadFactor
	 *            the minimum load factor of the hash map.
	 * @param maxLoadFactor
	 *            the maximum load factor of the hash map.
	 * @throws IllegalArgumentException
	 *             if
	 *             <tt>initialCapacity < 0 || (minLoadFactor < 0.0 || minLoadFactor >= 1.0) || (maxLoadFactor <= 0.0 || maxLoadFactor >= 1.0) || (minLoadFactor >= maxLoadFactor)</tt>.
	 * @throws IllegalArgumentException
	 *             if <tt>(float)columns*rows > Integer.MAX_VALUE</tt>.
	 * @throws IllegalArgumentException
	 *             if <tt>slices<0 || rows<0 || columns<0</tt>.
	 */
	public SparseFloatMatrix3D(int slices, int rows, int columns, int initialCapacity, float minLoadFactor, float maxLoadFactor) {
		setUp(slices, rows, columns);
		this.elements = new OpenIntFloatHashMap(initialCapacity, minLoadFactor, maxLoadFactor);
	}

	/**
	 * Constructs a view with the given parameters.
	 * 
	 * @param slices
	 *            the number of slices the matrix shall have.
	 * @param rows
	 *            the number of rows the matrix shall have.
	 * @param columns
	 *            the number of columns the matrix shall have.
	 * @param elements
	 *            the cells.
	 * @param sliceZero
	 *            the position of the first element.
	 * @param rowZero
	 *            the position of the first element.
	 * @param columnZero
	 *            the position of the first element.
	 * @param sliceStride
	 *            the number of elements between two slices, i.e.
	 *            <tt>index(k+1,i,j)-index(k,i,j)</tt>.
	 * @param rowStride
	 *            the number of elements between two rows, i.e.
	 *            <tt>index(k,i+1,j)-index(k,i,j)</tt>.
	 * @param columnnStride
	 *            the number of elements between two columns, i.e.
	 *            <tt>index(k,i,j+1)-index(k,i,j)</tt>.
	 * @throws IllegalArgumentException
	 *             if <tt>(float)slices*columns*rows > Integer.MAX_VALUE</tt>.
	 * @throws IllegalArgumentException
	 *             if <tt>slices<0 || rows<0 || columns<0</tt>.
	 */
	protected SparseFloatMatrix3D(int slices, int rows, int columns, AbstractIntFloatMap elements, int sliceZero, int rowZero, int columnZero, int sliceStride, int rowStride, int columnStride) {
		setUp(slices, rows, columns, sliceZero, rowZero, columnZero, sliceStride, rowStride, columnStride);
		this.elements = elements;
		this.isNoView = false;
	}

	public FloatMatrix3D assign(float value) {
		// overriden for performance only
		if (this.isNoView && value == 0)
			this.elements.clear();
		else
			super.assign(value);
		return this;
	}

	public FComplexMatrix3D getFft3() {
		throw new IllegalArgumentException("this method is not supported yet");

	}

	public FComplexMatrix3D getFft2Slices() {
		throw new IllegalArgumentException("this method is not supported yet");

	}

	public FComplexMatrix3D getIfft3(boolean scale) {
		throw new IllegalArgumentException("this method is not supported yet");

	}

	public FComplexMatrix3D getIfft2Slices(boolean scale) {
		throw new IllegalArgumentException("this method is not supported yet");

	}

	public void dct3(boolean scale) {
		throw new IllegalArgumentException("this method is not supported yet");

	}

	public void dct2Slices(boolean scale) {
		throw new IllegalArgumentException("this method is not supported yet");

	}

	public void idct3(boolean scale) {
		throw new IllegalArgumentException("this method is not supported yet");

	}

	public void idct2Slices(boolean scale) {
		throw new IllegalArgumentException("this method is not supported yet");

	}

	public FloatMatrix1D vectorize() {
		throw new IllegalArgumentException("This method is not supported yet");
	}

	public void dst3(boolean scale) {
		throw new IllegalArgumentException("this method is not supported yet");

	}

	public void dst2Slices(boolean scale) {
		throw new IllegalArgumentException("this method is not supported yet");

	}

	public void idst3(boolean scale) {
		throw new IllegalArgumentException("this method is not supported yet");

	}

	public void idst2Slices(boolean scale) {
		throw new IllegalArgumentException("this method is not supported yet");

	}

	public int cardinality() {
		if (this.isNoView)
			return this.elements.size();
		else
			return super.cardinality();
	}

	public void ensureCapacity(int minCapacity) {
		this.elements.ensureCapacity(minCapacity);
	}

	public float getQuick(int slice, int row, int column) {
		// if (debug) if (slice<0 || slice>=slices || row<0 || row>=rows ||
		// column<0 || column>=columns) throw new
		// IndexOutOfBoundsException("slice:"+slice+", row:"+row+",
		// column:"+column);
		// return elements.get(index(slice,row,column));
		// manually inlined:
		return elements.get(sliceZero + slice * sliceStride + rowZero + row * rowStride + columnZero + column * columnStride);
	}

	public AbstractIntFloatMap getElements() {
		return elements;
	}

	protected boolean haveSharedCellsRaw(FloatMatrix3D other) {
		if (other instanceof SelectedSparseFloatMatrix3D) {
			SelectedSparseFloatMatrix3D otherMatrix = (SelectedSparseFloatMatrix3D) other;
			return this.elements == otherMatrix.elements;
		} else if (other instanceof SparseFloatMatrix3D) {
			SparseFloatMatrix3D otherMatrix = (SparseFloatMatrix3D) other;
			return this.elements == otherMatrix.elements;
		}
		return false;
	}

	protected int index(int slice, int row, int column) {
		// return _sliceOffset(_sliceRank(slice)) + _rowOffset(_rowRank(row)) +
		// _columnOffset(_columnRank(column));
		// manually inlined:
		return sliceZero + slice * sliceStride + rowZero + row * rowStride + columnZero + column * columnStride;
	}

	public FloatMatrix3D like(int slices, int rows, int columns) {
		return new SparseFloatMatrix3D(slices, rows, columns);
	}

	protected FloatMatrix2D like2D(int rows, int columns, int rowZero, int columnZero, int rowStride, int columnStride) {
		return new SparseFloatMatrix2D(rows, columns, this.elements, rowZero, columnZero, rowStride, columnStride);
	}

	public void setQuick(int slice, int row, int column, float value) {
		// if (debug) if (slice<0 || slice>=slices || row<0 || row>=rows ||
		// column<0 || column>=columns) throw new
		// IndexOutOfBoundsException("slice:"+slice+", row:"+row+",
		// column:"+column);
		// int index = index(slice,row,column);
		// manually inlined:
		int index = sliceZero + slice * sliceStride + rowZero + row * rowStride + columnZero + column * columnStride;
		if (value == 0)
			this.elements.removeKey(index);
		else
			this.elements.put(index, value);
	}

	public void trimToSize() {
		this.elements.trimToSize();
	}

	protected FloatMatrix3D viewSelectionLike(int[] sliceOffsets, int[] rowOffsets, int[] columnOffsets) {
		return new SelectedSparseFloatMatrix3D(this.elements, sliceOffsets, rowOffsets, columnOffsets, 0);
	}
}
