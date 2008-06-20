/*
Copyright � 1999 CERN - European Organization for Nuclear Research.
Permission to use, copy, modify, distribute and sell this software and its documentation for any purpose 
is hereby granted without fee, provided that the above copyright notice appear in all copies and 
that both that copyright notice and this permission notice appear in supporting documentation. 
CERN makes no representations about the suitability of this software for any purpose. 
It is provided "as is" without expressed or implied warranty.
 */
package cern.colt.matrix.impl;

import java.util.concurrent.ConcurrentHashMap;

import cern.colt.matrix.DComplexMatrix1D;
import cern.colt.matrix.DComplexMatrix2D;
import cern.colt.matrix.DComplexMatrix3D;
import cern.colt.matrix.DoubleMatrix1D;

/**
 * Sparse hashed 1-d matrix (aka <i>vector</i>) holding <tt>complex</tt>
 * elements. Note that this implementation uses ConcurrentHashMap
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * @version 1.0, 12/10/2007
 */
public class SparseDComplexMatrix1D extends DComplexMatrix1D {
	private static final long serialVersionUID = -7792866167410993582L;

	/*
	 * The elements of the matrix.
	 */
	protected ConcurrentHashMap<Integer, double[]> elements;

	/**
	 * Constructs a matrix with a copy of the given values. The values are
	 * copied. So subsequent changes in <tt>values</tt> are not reflected in
	 * the matrix, and vice-versa.
	 * 
	 * @param values
	 *            The values to be filled into the new matrix.
	 */
	public SparseDComplexMatrix1D(double[] values) {
		this(values.length);
		assign(values);
	}

	/**
	 * Constructs a matrix with a given number of cells.
	 * 
	 * @param size
	 *            the number of cells the matrix shall have.
	 * @throws IllegalArgumentException
	 *             if <tt>size<0</tt>.
	 */
	public SparseDComplexMatrix1D(int size) {
		setUp(size);
		this.elements = new ConcurrentHashMap<Integer, double[]>(size / 1000);
	}

	/**
	 * Constructs a matrix view with a given number of parameters.
	 * 
	 * @param size
	 *            the number of cells the matrix shall have.
	 * @param elements
	 *            the cells.
	 * @param offset
	 *            the index of the first element.
	 * @param stride
	 *            the number of indexes between any two elements, i.e.
	 *            <tt>index(i+1)-index(i)</tt>.
	 * @throws IllegalArgumentException
	 *             if <tt>size<0</tt>.
	 */
	protected SparseDComplexMatrix1D(int size, ConcurrentHashMap<Integer, double[]> elements, int offset, int stride) {
		setUp(size, offset, stride);
		this.elements = elements;
		this.isNoView = false;
	}

	/**
	 * Sets all cells to the state specified by <tt>value</tt>.
	 * 
	 * @param value
	 *            the value to be filled into the cells.
	 * @return <tt>this</tt> (for convenience only).
	 */
	public DComplexMatrix1D assign(double[] value) {
		// overriden for performance only
		if (this.isNoView && value[0] == 0 && value[1] == 0)
			this.elements.clear();
		else
			super.assign(value);
		return this;
	}

	/**
	 * Returns the number of cells having non-zero values.
	 */
	public int cardinality() {
		if (this.isNoView)
			return this.elements.size();
		else
			return super.cardinality();
	}

	public void fft() {
		throw new IllegalArgumentException("fft() is not supported yet");
	}

	public void ifft(boolean scale) {
		throw new IllegalArgumentException("ifft() is not supported yet");
	}

	/**
	 * Returns the matrix cell value at coordinate <tt>index</tt>.
	 * 
	 * <p>
	 * Provided with invalid parameters this method may return invalid objects
	 * without throwing any exception. <b>You should only use this method when
	 * you are absolutely sure that the coordinate is within bounds.</b>
	 * Precondition (unchecked): <tt>index&lt;0 || index&gt;=size()</tt>.
	 * 
	 * @param index
	 *            the index of the cell.
	 * @return the value of the specified cell.
	 */
	public double[] getQuick(int index) {
		return elements.get(zero + index * stride);
	}

	/**
	 * Returns the elements of this matrix.
	 * 
	 * @return the elements
	 */
	public ConcurrentHashMap<Integer, double[]> getElements() {
		return elements;
	}

	/**
	 * Returns <tt>true</tt> if both matrices share at least one identical
	 * cell.
	 */
	protected boolean haveSharedCellsRaw(DComplexMatrix1D other) {
		if (other instanceof SelectedSparseDComplexMatrix1D) {
			SelectedSparseDComplexMatrix1D otherMatrix = (SelectedSparseDComplexMatrix1D) other;
			return this.elements == otherMatrix.elements;
		} else if (other instanceof SparseDComplexMatrix1D) {
			SparseDComplexMatrix1D otherMatrix = (SparseDComplexMatrix1D) other;
			return this.elements == otherMatrix.elements;
		}
		return false;
	}

	/**
	 * Returns the position of the element with the given relative rank within
	 * the (virtual or non-virtual) internal 1-dimensional array. You may want
	 * to override this method for performance.
	 * 
	 * @param rank
	 *            the rank of the element.
	 */
	protected int index(int rank) {
		return zero + rank * stride;
	}

	/**
	 * Construct and returns a new empty matrix <i>of the same dynamic type</i>
	 * as the receiver, having the specified size. For example, if the receiver
	 * is an instance of type <tt>DenseComplexMatrix1D</tt> the new matrix
	 * must also be of type <tt>DenseComplexMatrix1D</tt>, if the receiver is
	 * an instance of type <tt>SparseComplexMatrix1D</tt> the new matrix must
	 * also be of type <tt>SparseComplexMatrix1D</tt>, etc. In general, the
	 * new matrix should have internal parametrization as similar as possible.
	 * 
	 * @param size
	 *            the number of cell the matrix shall have.
	 * @return a new empty matrix of the same dynamic type.
	 */
	public DComplexMatrix1D like(int size) {
		return new SparseDComplexMatrix1D(size);
	}

	/**
	 * Construct and returns a new 2-d matrix <i>of the corresponding dynamic
	 * type</i>, entirelly independent of the receiver. For example, if the
	 * receiver is an instance of type <tt>DenseComplexMatrix1D</tt> the new
	 * matrix must be of type <tt>DenseComplexMatrix2D</tt>, if the receiver
	 * is an instance of type <tt>SparseComplexMatrix1D</tt> the new matrix
	 * must be of type <tt>SparseComplexMatrix2D</tt>, etc.
	 * 
	 * @param rows
	 *            the number of rows the matrix shall have.
	 * @param columns
	 *            the number of columns the matrix shall have.
	 * @return a new matrix of the corresponding dynamic type.
	 */
	public DComplexMatrix2D like2D(int rows, int columns) {
		return new SparseDComplexMatrix2D(rows, columns);
	}

	public DComplexMatrix2D reshape(int rows, int cols) {
		throw new IllegalAccessError("reshape is not supported.");
	}

	public DComplexMatrix3D reshape(int slices, int rows, int cols) {
		throw new IllegalAccessError("reshape is not supported.");
	}

	/**
	 * Sets the matrix cell at coordinate <tt>index</tt> to the specified
	 * value.
	 * 
	 * <p>
	 * Provided with invalid parameters this method may access illegal indexes
	 * without throwing any exception. <b>You should only use this method when
	 * you are absolutely sure that the coordinate is within bounds.</b>
	 * Precondition (unchecked): <tt>index&lt;0 || index&gt;=size()</tt>.
	 * 
	 * @param index
	 *            the index of the cell.
	 * @param value
	 *            the value to be filled into the specified cell.
	 */
	public void setQuick(int index, double[] value) {
		int i = zero + index * stride;
		if (value[0] == 0 && value[1] == 0)
			this.elements.remove(i);
		else
			this.elements.put(i, value);
	}

	/**
	 * Sets the matrix cell at coordinate <tt>index</tt> to the specified
	 * value.
	 * 
	 * <p>
	 * Provided with invalid parameters this method may access illegal indexes
	 * without throwing any exception. <b>You should only use this method when
	 * you are absolutely sure that the coordinate is within bounds.</b>
	 * Precondition (unchecked): <tt>index&lt;0 || index&gt;=size()</tt>.
	 * 
	 * @param index
	 *            the index of the cell.
	 * @param re
	 *            the real part of the value to be filled into the specified
	 *            cell.
	 * @param im
	 *            the imaginary part of the value to be filled into the
	 *            specified cell.
	 * 
	 */
	public void setQuick(int index, double re, double im) {
		int i = zero + index * stride;
		if (re == 0 && im == 0)
			this.elements.remove(i);
		else
			this.elements.put(i, new double[] { re, im });
	}

	/**
	 * Construct and returns a new selection view.
	 * 
	 * @param offsets
	 *            the offsets of the visible elements.
	 * @return a new view.
	 */
	protected DComplexMatrix1D viewSelectionLike(int[] offsets) {
		return new SelectedSparseDComplexMatrix1D(this.elements, offsets);
	}

	public DoubleMatrix1D getImaginaryPart() {
		int n = size();
		DoubleMatrix1D Im = new SparseDoubleMatrix1D(n);
		double[] tmp = new double[2];
		for (int i = 0; i < n; i++) {
			tmp = getQuick(i);
			Im.setQuick(i, tmp[1]);
		}
		return Im;
	}

	public DoubleMatrix1D getRealPart() {
		int n = size();
		DoubleMatrix1D R = new SparseDoubleMatrix1D(n);
		double[] tmp = new double[2];
		for (int i = 0; i < n; i++) {
			tmp = getQuick(i);
			R.setQuick(i, tmp[0]);
		}
		return R;
	}
}
