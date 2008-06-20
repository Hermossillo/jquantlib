package cern.colt.matrix.impl.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cern.colt.Utils;
import cern.colt.function.DoubleProcedure;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.DoubleMatrix3D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.jet.math.DoubleFunctions;

public class Test_DenseDoubleMatrix1D {
	private static final int size = 2 * 17 * 5;

	private static final double tol = 1e-10;

	private static final int nThreads = 3;

	private static final int nThreadsBegin = 1;

	double[] a, b;

	@Before
	public void setUp() throws Exception {
		// generate test matrices
		Random r = new Random(0);

		a = new double[size];
		for (int i = 0; i < a.length; i++) {
			a[i] = r.nextDouble();
		}

		b = new double[size];
		for (int i = 0; i < b.length; i++) {
			b[i] = r.nextDouble();
		}

	}

	@After
	public void tearDown() throws Exception {
		a = null;
		b = null;
		System.gc();
	}

	@Test
	public void testAggregateDoubleDoubleFunctionDoubleFunction() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(a);
		double aSum = A.aggregate(DoubleFunctions.plus, DoubleFunctions.square);
		double tmpSum = 0;
		for (int i = 0; i < a.length; i++) {
			tmpSum += a[i] * a[i];
		}
		assertEquals(tmpSum, aSum, tol);
		// multithreaded
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		aSum = A.aggregate(DoubleFunctions.plus, DoubleFunctions.square);
		tmpSum = 0;
		for (int i = 0; i < a.length; i++) {
			tmpSum += a[i] * a[i];
		}
		assertEquals(tmpSum, aSum, tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D Av = A.viewFlip();
		aSum = Av.aggregate(DoubleFunctions.plus, DoubleFunctions.square);
		tmpSum = 0;
		for (int i = 0; i < a.length; i++) {
			tmpSum += a[i] * a[i];
		}
		assertEquals(tmpSum, aSum, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		Av = A.viewFlip();
		aSum = Av.aggregate(DoubleFunctions.plus, DoubleFunctions.square);
		tmpSum = 0;
		for (int i = 0; i < a.length; i++) {
			tmpSum += a[i] * a[i];
		}
		assertEquals(tmpSum, aSum, tol);
	}

	@Test
	public void testAggregateDoubleMatrix1DDoubleDoubleFunctionDoubleDoubleFunction() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D B = new DenseDoubleMatrix1D(b);
		double sumMult = A.aggregate(B, DoubleFunctions.plus, DoubleFunctions.mult);
		double tmpSumMult = 0;
		for (int i = 0; i < a.length; i++) {
			tmpSumMult += a[i] * b[i];
		}
		assertEquals(tmpSumMult, sumMult, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		B = new DenseDoubleMatrix1D(b);
		sumMult = A.aggregate(B, DoubleFunctions.plus, DoubleFunctions.mult);
		tmpSumMult = 0;
		for (int i = 0; i < a.length; i++) {
			tmpSumMult += a[i] * b[i];
		}
		assertEquals(tmpSumMult, sumMult, tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D Av = A.viewFlip();
		B = new DenseDoubleMatrix1D(b);
		DoubleMatrix1D Bv = B.viewFlip();
		sumMult = Av.aggregate(Bv, DoubleFunctions.plus, DoubleFunctions.mult);
		tmpSumMult = 0;
		for (int i = 0; i < a.length; i++) {
			tmpSumMult += a[i] * b[i];
		}
		assertEquals(tmpSumMult, sumMult, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		Av = A.viewFlip();
		B = new DenseDoubleMatrix1D(b);
		Bv = B.viewFlip();
		sumMult = Av.aggregate(Bv, DoubleFunctions.plus, DoubleFunctions.mult);
		tmpSumMult = 0;
		for (int i = 0; i < a.length; i++) {
			tmpSumMult += a[i] * b[i];
		}
		assertEquals(tmpSumMult, sumMult, tol);

	}

	@Test
	public void testAssignDoubleProcedureDoubleFunction() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D B = A.copy();
		A.assign(new DoubleProcedure() {
			public boolean apply(double element) {
				if (Math.abs(element) > 0.1) {
					return true;
				} else {
					return false;
				}
			}
		}, DoubleFunctions.tan);
		for (int i = 0; i < size; i++) {
			if (Math.abs(B.getQuick(i)) > 0.1) {
				B.setQuick(i, Math.tan(B.getQuick(i)));
			}
		}
		Utils.assertArrayEquals((double[]) B.getElements(), (double[]) A.getElements(), tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		B = A.copy();
		A.assign(new DoubleProcedure() {
			public boolean apply(double element) {
				if (Math.abs(element) > 0.1) {
					return true;
				} else {
					return false;
				}
			}
		}, DoubleFunctions.tan);
		for (int i = 0; i < size; i++) {
			if (Math.abs(B.getQuick(i)) > 0.1) {
				B.setQuick(i, Math.tan(B.getQuick(i)));
			}
		}
		Utils.assertArrayEquals((double[]) B.getElements(), (double[]) A.getElements(), tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D Av = A.viewFlip();
		B = A.copy();
		Av.assign(new DoubleProcedure() {
			public boolean apply(double element) {
				if (Math.abs(element) > 0.1) {
					return true;
				} else {
					return false;
				}
			}
		}, DoubleFunctions.tan);
		for (int i = 0; i < size; i++) {
			if (Math.abs(B.getQuick(i)) > 0.1) {
				B.setQuick(i, Math.tan(B.getQuick(i)));
			}
		}
		for (int i = 0; i < size; i++) {
			assertEquals(B.getQuick(i), Av.getQuick(size - 1 - i), tol);
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		Av = A.viewFlip();
		B = A.copy();
		Av.assign(new DoubleProcedure() {
			public boolean apply(double element) {
				if (Math.abs(element) > 0.1) {
					return true;
				} else {
					return false;
				}
			}
		}, DoubleFunctions.tan);
		for (int i = 0; i < size; i++) {
			if (Math.abs(B.getQuick(i)) > 0.1) {
				B.setQuick(i, Math.tan(B.getQuick(i)));
			}
		}
		for (int i = 0; i < size; i++) {
			assertEquals(B.getQuick(i), Av.getQuick(size - 1 - i), tol);
		}
	}

	@Test
	public void testAssignDoubleProcedureDouble() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D B = A.copy();
		A.assign(new DoubleProcedure() {
			public boolean apply(double element) {
				if (Math.abs(element) > 0.1) {
					return true;
				} else {
					return false;
				}
			}
		}, -1);
		for (int i = 0; i < size; i++) {
			if (Math.abs(B.getQuick(i)) > 0.1) {
				B.setQuick(i, -1);
			}
		}
		Utils.assertArrayEquals((double[]) B.getElements(), (double[]) A.getElements(), tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		B = A.copy();
		A.assign(new DoubleProcedure() {
			public boolean apply(double element) {
				if (Math.abs(element) > 0.1) {
					return true;
				} else {
					return false;
				}
			}
		}, -1);
		for (int i = 0; i < size; i++) {
			if (Math.abs(B.getQuick(i)) > 0.1) {
				B.setQuick(i, -1);
			}
		}
		Utils.assertArrayEquals((double[]) B.getElements(), (double[]) A.getElements(), tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D Av = A.viewFlip();
		B = A.copy();
		Av.assign(new DoubleProcedure() {
			public boolean apply(double element) {
				if (Math.abs(element) > 0.1) {
					return true;
				} else {
					return false;
				}
			}
		}, -1);
		for (int i = 0; i < size; i++) {
			if (Math.abs(B.getQuick(i)) > 0.1) {
				B.setQuick(i, -1);
			}
		}
		for (int i = 0; i < size; i++) {
			assertEquals(B.getQuick(i), Av.getQuick(size - 1 - i), tol);
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		Av = A.viewFlip();
		B = A.copy();
		Av.assign(new DoubleProcedure() {
			public boolean apply(double element) {
				if (Math.abs(element) > 0.1) {
					return true;
				} else {
					return false;
				}
			}
		}, -1);
		for (int i = 0; i < size; i++) {
			if (Math.abs(B.getQuick(i)) > 0.1) {
				B.setQuick(i, -1);
			}
		}
		for (int i = 0; i < size; i++) {
			assertEquals(B.getQuick(i), Av.getQuick(size - 1 - i), tol);
		}
	}

	@Test
	public void testAssignDouble() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(size);
		double value = Math.random();
		A.assign(value);
		double[] aElts = (double[]) A.getElements();
		for (int i = 0; i < aElts.length; i++) {
			assertEquals(value, aElts[i], tol);
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(size);
		A.assign(value);
		aElts = (double[]) A.getElements();
		for (int i = 0; i < aElts.length; i++) {
			assertEquals(value, aElts[i], tol);
		}
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDoubleMatrix1D(size);
		DoubleMatrix1D Av = A.viewFlip();
		value = Math.random();
		Av.assign(value);
		for (int i = 0; i < size; i++) {
			assertEquals(value, Av.getQuick(i), tol);
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(size);
		Av = A.viewFlip();
		value = Math.random();
		Av.assign(value);
		for (int i = 0; i < size; i++) {
			assertEquals(value, Av.getQuick(i), tol);
		}
	}

	@Test
	public void testAssignDoubleArray() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(size);
		A.assign(a);
		double[] aElts = (double[]) A.getElements();
		Utils.assertArrayEquals(a, aElts, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(size);
		A.assign(a);
		aElts = (double[]) A.getElements();
		Utils.assertArrayEquals(a, aElts, tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDoubleMatrix1D(size);
		DoubleMatrix1D Av = A.viewFlip();
		Av.assign(a);
		for (int i = 0; i < size; i++) {
			assertEquals(a[i], Av.getQuick(i), tol);
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(size);
		Av = A.viewFlip();
		Av.assign(a);
		for (int i = 0; i < size; i++) {
			assertEquals(a[i], Av.getQuick(i), tol);
		}
	}

	@Test
	public void testAssignDoubleFunction() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(a);
		A.assign(DoubleFunctions.acos);
		double[] aElts = (double[]) A.getElements();
		double tmp;
		for (int i = 0; i < a.length; i++) {
			tmp = Math.acos(a[i]);
			assertEquals(tmp, aElts[i], tol);
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		A.assign(DoubleFunctions.acos);
		aElts = (double[]) A.getElements();
		for (int i = 0; i < a.length; i++) {
			tmp = Math.acos(a[i]);
			assertEquals(tmp, aElts[i], tol);
		}
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D Av = A.viewFlip();
		Av.assign(DoubleFunctions.acos);
		for (int i = 0; i < a.length; i++) {
			tmp = Math.acos(a[i]);
			assertEquals(tmp, Av.getQuick(size - 1 - i), tol);
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		Av = A.viewFlip();
		Av.assign(DoubleFunctions.acos);
		for (int i = 0; i < a.length; i++) {
			tmp = Math.acos(a[i]);
			assertEquals(tmp, Av.getQuick(size - 1 - i), tol);
		}
	}

	@Test
	public void testAssignDoubleMatrix1D() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(size);
		DoubleMatrix1D B = new DenseDoubleMatrix1D(a);
		A.assign(B);
		double[] aElts = (double[]) A.getElements();
		Utils.assertArrayEquals(a, aElts, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(size);
		B = new DenseDoubleMatrix1D(a);
		A.assign(B);
		aElts = (double[]) A.getElements();
		Utils.assertArrayEquals(a, aElts, tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDoubleMatrix1D(size);
		DoubleMatrix1D Av = A.viewFlip();
		B = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D Bv = B.viewFlip();
		Av.assign(Bv);
		for (int i = 0; i < a.length; i++) {
			assertEquals(a[size - 1 - i], Av.getQuick(i), tol);
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(size);
		Av = A.viewFlip();
		B = new DenseDoubleMatrix1D(a);
		Bv = B.viewFlip();
		Av.assign(Bv);
		for (int i = 0; i < a.length; i++) {
			assertEquals(a[size - 1 - i], Av.getQuick(i), tol);
		}
	}

	@Test
	public void testAssignDoubleMatrix1DDoubleDoubleFunction() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D B = new DenseDoubleMatrix1D(b);
		A.assign(B, DoubleFunctions.div);
		double[] aElts = (double[]) A.getElements();
		double tmp;
		for (int i = 0; i < aElts.length; i++) {
			tmp = a[i] / b[i];
			assertEquals(tmp, aElts[i], tol);
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		B = new DenseDoubleMatrix1D(b);
		A.assign(B, DoubleFunctions.div);
		aElts = (double[]) A.getElements();
		for (int i = 0; i < aElts.length; i++) {
			tmp = a[i] / b[i];
			assertEquals(tmp, aElts[i], tol);
		}
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D Av = A.viewFlip();
		B = new DenseDoubleMatrix1D(b);
		DoubleMatrix1D Bv = B.viewFlip();
		Av.assign(Bv, DoubleFunctions.div);
		for (int i = 0; i < aElts.length; i++) {
			tmp = a[i] / b[i];
			assertEquals(tmp, Av.getQuick(size - 1 - i), tol);
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		Av = A.viewFlip();
		B = new DenseDoubleMatrix1D(b);
		Bv = B.viewFlip();
		Av.assign(Bv, DoubleFunctions.div);
		for (int i = 0; i < aElts.length; i++) {
			tmp = a[i] / b[i];
			assertEquals(tmp, Av.getQuick(size - 1 - i), tol);
		}
	}

	@Test
	public void testCardinality() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(a);
		int card = A.cardinality();
		assertEquals(a.length, card);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		card = A.cardinality();
		assertEquals(a.length, card);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D Av = A.viewFlip();
		card = Av.cardinality();
		assertEquals(a.length, card);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		Av = A.viewFlip();
		card = Av.cardinality();
		assertEquals(a.length, card);
	}

	@Test
	public void testEqualsDouble() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(size);
		double value = 1;
		A.assign(1);
		boolean eq = A.equals(value);
		assertEquals(true, eq);
		eq = A.equals(2);
		assertEquals(false, eq);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(size);
		A.assign(1);
		eq = A.equals(value);
		assertEquals(true, eq);
		eq = A.equals(2);
		assertEquals(false, eq);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDoubleMatrix1D(size);
		DoubleMatrix1D Av = A.viewFlip();
		value = 1;
		Av.assign(1);
		eq = Av.equals(value);
		assertEquals(true, eq);
		eq = Av.equals(2);
		assertEquals(false, eq);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(size);
		Av = A.viewFlip();
		value = 1;
		Av.assign(1);
		eq = Av.equals(value);
		assertEquals(true, eq);
		eq = Av.equals(2);
		assertEquals(false, eq);
	}

	@Test
	public void testEqualsObject() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D B = new DenseDoubleMatrix1D(b);
		boolean eq = A.equals(A);
		assertEquals(true, eq);
		eq = A.equals(B);
		assertEquals(false, eq);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		B = new DenseDoubleMatrix1D(b);
		eq = A.equals(A);
		assertEquals(true, eq);
		eq = A.equals(B);
		assertEquals(false, eq);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D Av = A.viewFlip();
		B = new DenseDoubleMatrix1D(b);
		DoubleMatrix1D Bv = B.viewFlip();
		eq = Av.equals(Av);
		assertEquals(true, eq);
		eq = Av.equals(Bv);
		assertEquals(false, eq);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		Av = A.viewFlip();
		B = new DenseDoubleMatrix1D(b);
		Bv = B.viewFlip();
		eq = Av.equals(Av);
		assertEquals(true, eq);
		eq = Av.equals(Bv);
		assertEquals(false, eq);
	}

	@Test
	public void testGet() {
		DoubleMatrix1D A = new DenseDoubleMatrix1D(a);
		for (int i = 0; i < A.size(); i++) {
			assertEquals(a[i], A.getQuick(i), tol);
		}
	}

	@Test
	public void testGetMaxLocation() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(size);
		A.setQuick(size / 3, 0.7);
		A.setQuick(size / 2, 0.7);
		double[] maxAndLoc = A.getMaxLocation();
		assertEquals(0.7, maxAndLoc[0], tol);
		assertEquals(size / 3, (int) maxAndLoc[1]);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(size);
		A.setQuick(size / 3, 0.7);
		A.setQuick(size / 2, 0.7);
		maxAndLoc = A.getMaxLocation();
		assertEquals(0.7, maxAndLoc[0], tol);
		assertEquals(size / 3, (int) maxAndLoc[1]);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDoubleMatrix1D(size);
		DoubleMatrix1D Av = A.viewFlip();
		Av.setQuick(size / 3, 0.7);
		Av.setQuick(size / 2, 0.7);
		maxAndLoc = Av.getMaxLocation();
		assertEquals(0.7, maxAndLoc[0], tol);
		assertEquals(size / 3, (int) maxAndLoc[1]);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(size);
		Av = A.viewFlip();
		Av.setQuick(size / 3, 0.7);
		Av.setQuick(size / 2, 0.7);
		maxAndLoc = Av.getMaxLocation();
		assertEquals(0.7, maxAndLoc[0], tol);
		assertEquals(size / 3, (int) maxAndLoc[1]);

	}

	@Test
	public void testGetMinLocation() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(size);
		A.setQuick(size / 3, -0.7);
		A.setQuick(size / 2, -0.7);
		double[] minAndLoc = A.getMinLocation();
		assertEquals(-0.7, minAndLoc[0], tol);
		assertEquals(size / 3, (int) minAndLoc[1]);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(size);
		A.setQuick(size / 3, -0.7);
		A.setQuick(size / 2, -0.7);
		minAndLoc = A.getMinLocation();
		assertEquals(-0.7, minAndLoc[0], tol);
		assertEquals(size / 3, (int) minAndLoc[1]);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDoubleMatrix1D(size);
		DoubleMatrix1D Av = A.viewFlip();
		Av.setQuick(size / 3, -0.7);
		Av.setQuick(size / 2, -0.7);
		minAndLoc = Av.getMinLocation();
		assertEquals(-0.7, minAndLoc[0], tol);
		assertEquals(size / 3, (int) minAndLoc[1]);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(size);
		Av = A.viewFlip();
		Av.setQuick(size / 3, -0.7);
		Av.setQuick(size / 2, -0.7);
		minAndLoc = Av.getMinLocation();
		assertEquals(-0.7, minAndLoc[0], tol);
		assertEquals(size / 3, (int) minAndLoc[1]);
	}

	@Test
	public void testGetNonZerosIntArrayListDoubleArrayList() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(size);
		A.setQuick(size / 4, 1);
		A.setQuick(size / 2, 2);
		A.setQuick(size - 1, 3);
		IntArrayList indexList = new IntArrayList();
		DoubleArrayList valueList = new DoubleArrayList();
		A.getNonZeros(indexList, valueList);
		assertEquals(3, indexList.size());
		assertEquals(3, valueList.size());
		assertEquals(size / 4, indexList.get(0));
		assertEquals(size / 2, indexList.get(1));
		assertEquals(size - 1, indexList.get(2));
		assertEquals(1, valueList.get(0), tol);
		assertEquals(2, valueList.get(1), tol);
		assertEquals(3, valueList.get(2), tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(size);
		A.setQuick(size / 4, 1);
		A.setQuick(size / 2, 2);
		A.setQuick(size - 1, 3);
		indexList = new IntArrayList();
		valueList = new DoubleArrayList();
		A.getNonZeros(indexList, valueList);
		assertEquals(3, indexList.size());
		assertEquals(3, valueList.size());
		assertEquals(size / 4, indexList.get(0));
		assertEquals(size / 2, indexList.get(1));
		assertEquals(size - 1, indexList.get(2));
		assertEquals(1, valueList.get(0), tol);
		assertEquals(2, valueList.get(1), tol);
		assertEquals(3, valueList.get(2), tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDoubleMatrix1D(size);
		DoubleMatrix1D Av = A.viewFlip();
		Av.setQuick(size / 4, 1);
		Av.setQuick(size / 2, 2);
		Av.setQuick(size - 1, 3);
		indexList = new IntArrayList();
		valueList = new DoubleArrayList();
		Av.getNonZeros(indexList, valueList);
		assertEquals(3, indexList.size());
		assertEquals(3, valueList.size());
		assertEquals(size / 4, indexList.get(0));
		assertEquals(size / 2, indexList.get(1));
		assertEquals(size - 1, indexList.get(2));
		assertEquals(1, valueList.get(0), tol);
		assertEquals(2, valueList.get(1), tol);
		assertEquals(3, valueList.get(2), tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(size);
		Av = A.viewFlip();
		Av.setQuick(size / 4, 1);
		Av.setQuick(size / 2, 2);
		Av.setQuick(size - 1, 3);
		indexList = new IntArrayList();
		valueList = new DoubleArrayList();
		Av.getNonZeros(indexList, valueList);
		assertEquals(3, indexList.size());
		assertEquals(3, valueList.size());
		assertEquals(size / 4, indexList.get(0));
		assertEquals(size / 2, indexList.get(1));
		assertEquals(size - 1, indexList.get(2));
		assertEquals(1, valueList.get(0), tol);
		assertEquals(2, valueList.get(1), tol);
		assertEquals(3, valueList.get(2), tol);
	}

	@Test
	public void testGetNonZerosIntArrayListDoubleArrayListInt() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(size);
		A.setQuick(size / 4, 1);
		A.setQuick(size / 2, 2);
		A.setQuick(size - 1, 3);
		IntArrayList indexList = new IntArrayList();
		DoubleArrayList valueList = new DoubleArrayList();
		A.getNonZeros(indexList, valueList, 2);
		assertEquals(2, indexList.size());
		assertEquals(2, valueList.size());
		assertEquals(size / 4, indexList.get(0));
		assertEquals(size / 2, indexList.get(1));
		assertEquals(1, valueList.get(0), tol);
		assertEquals(2, valueList.get(1), tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(size);
		A.setQuick(size / 4, 1);
		A.setQuick(size / 2, 2);
		A.setQuick(size - 1, 3);
		indexList = new IntArrayList();
		valueList = new DoubleArrayList();
		A.getNonZeros(indexList, valueList, 2);
		assertEquals(2, indexList.size());
		assertEquals(2, valueList.size());
		assertEquals(size / 4, indexList.get(0));
		assertEquals(size / 2, indexList.get(1));
		assertEquals(1, valueList.get(0), tol);
		assertEquals(2, valueList.get(1), tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDoubleMatrix1D(size);
		DoubleMatrix1D Av = A.viewFlip();
		Av.setQuick(size / 4, 1);
		Av.setQuick(size / 2, 2);
		Av.setQuick(size - 1, 3);
		indexList = new IntArrayList();
		valueList = new DoubleArrayList();
		Av.getNonZeros(indexList, valueList, 2);
		assertEquals(2, indexList.size());
		assertEquals(2, valueList.size());
		assertEquals(size / 4, indexList.get(0));
		assertEquals(size / 2, indexList.get(1));
		assertEquals(1, valueList.get(0), tol);
		assertEquals(2, valueList.get(1), tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(size);
		Av = A.viewFlip();
		Av.setQuick(size / 4, 1);
		Av.setQuick(size / 2, 2);
		Av.setQuick(size - 1, 3);
		indexList = new IntArrayList();
		valueList = new DoubleArrayList();
		Av.getNonZeros(indexList, valueList, 2);
		assertEquals(2, indexList.size());
		assertEquals(2, valueList.size());
		assertEquals(size / 4, indexList.get(0));
		assertEquals(size / 2, indexList.get(1));
		assertEquals(1, valueList.get(0), tol);
		assertEquals(2, valueList.get(1), tol);
	}

	@Test
	public void testGetQuick() {
		DoubleMatrix1D A = new DenseDoubleMatrix1D(a);
		for (int i = 0; i < A.size(); i++) {
			assertEquals(a[i], A.getQuick(i), tol);
		}
	}

	@Test
	public void testReshapeIntInt() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(a);
		int rows = 10;
		int cols = 17;
		DoubleMatrix2D B = A.reshape(rows, cols);
		int idx = 0;
		for (int c = 0; c < cols; c++) {
			for (int r = 0; r < rows; r++) {
				assertEquals(A.getQuick(idx++), B.getQuick(r, c), tol);
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		B = A.reshape(rows, cols);
		idx = 0;
		for (int c = 0; c < cols; c++) {
			for (int r = 0; r < rows; r++) {
				assertEquals(A.getQuick(idx++), B.getQuick(r, c), tol);
			}
		}
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D Av = A.viewFlip();
		B = Av.reshape(rows, cols);
		idx = 0;
		for (int c = 0; c < cols; c++) {
			for (int r = 0; r < rows; r++) {
				assertEquals(Av.getQuick(idx++), B.getQuick(r, c), tol);
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		Av = A.viewFlip();
		B = Av.reshape(rows, cols);
		idx = 0;
		for (int c = 0; c < cols; c++) {
			for (int r = 0; r < rows; r++) {
				assertEquals(Av.getQuick(idx++), B.getQuick(r, c), tol);
			}
		}
	}

	@Test
	public void testReshapeIntIntInt() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(a);
		int slices = 2;
		int rows = 5;
		int cols = 17;
		DoubleMatrix3D B = A.reshape(slices, rows, cols);
		int idx = 0;
		for (int s = 0; s < slices; s++) {
			for (int c = 0; c < cols; c++) {
				for (int r = 0; r < rows; r++) {
					assertEquals(A.getQuick(idx++), B.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		B = A.reshape(slices, rows, cols);
		idx = 0;
		for (int s = 0; s < slices; s++) {
			for (int c = 0; c < cols; c++) {
				for (int r = 0; r < rows; r++) {
					assertEquals(A.getQuick(idx++), B.getQuick(s, r, c), tol);
				}
			}
		}
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D Av = A.viewFlip();
		B = Av.reshape(slices, rows, cols);
		idx = 0;
		for (int s = 0; s < slices; s++) {
			for (int c = 0; c < cols; c++) {
				for (int r = 0; r < rows; r++) {
					assertEquals(Av.getQuick(idx++), B.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		Av = A.viewFlip();
		B = Av.reshape(slices, rows, cols);
		idx = 0;
		for (int s = 0; s < slices; s++) {
			for (int c = 0; c < cols; c++) {
				for (int r = 0; r < rows; r++) {
					assertEquals(Av.getQuick(idx++), B.getQuick(s, r, c), tol);
				}
			}
		}
	}

	@Test
	public void testSet() {
		DoubleMatrix1D A = new DenseDoubleMatrix1D(size);
		double elem = Math.random();
		A.set(size / 2, elem);
		assertEquals(elem, A.getQuick(size / 2), tol);
	}

	@Test
	public void testSetQuick() {
		DoubleMatrix1D A = new DenseDoubleMatrix1D(size);
		double elem = Math.random();
		A.setQuick(size / 2, elem);
		assertEquals(elem, A.getQuick(size / 2), tol);
	}

	@Test
	public void testSwap() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D B = new DenseDoubleMatrix1D(b);
		A.swap(B);
		double[] aElts = (double[]) A.getElements();
		double[] bElts = (double[]) B.getElements();
		Utils.assertArrayEquals(b, aElts, tol);
		Utils.assertArrayEquals(a, bElts, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		B = new DenseDoubleMatrix1D(b);
		A.swap(B);
		aElts = (double[]) A.getElements();
		bElts = (double[]) B.getElements();
		Utils.assertArrayEquals(b, aElts, tol);
		Utils.assertArrayEquals(a, bElts, tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D Av = A.viewFlip();
		B = new DenseDoubleMatrix1D(b);
		DoubleMatrix1D Bv = B.viewFlip();
		Av.swap(Bv);
		for (int i = 0; i < size; i++) {
			assertEquals(a[i], Bv.getQuick(size - 1 - i), tol);
			assertEquals(b[i], Av.getQuick(size - 1 - i), tol);
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		Av = A.viewFlip();
		B = new DenseDoubleMatrix1D(b);
		Bv = B.viewFlip();
		Av.swap(Bv);
		for (int i = 0; i < size; i++) {
			assertEquals(a[i], Bv.getQuick(size - 1 - i), tol);
			assertEquals(b[i], Av.getQuick(size - 1 - i), tol);
		}
	}

	@Test
	public void testToArray() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(a);
		double[] array = A.toArray();
		Utils.assertArrayEquals(a, array, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		array = A.toArray();
		Utils.assertArrayEquals(a, array, tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D Av = A.viewFlip();
		array = Av.toArray();
		for (int i = 0; i < size; i++) {
			assertEquals(a[i], array[size - 1 - i], tol);
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		Av = A.viewFlip();
		array = Av.toArray();
		for (int i = 0; i < size; i++) {
			assertEquals(a[i], array[size - 1 - i], tol);
		}
	}

	@Test
	public void testToArrayDoubleArray() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(a);
		double[] b = new double[size];
		A.toArray(b);
		Utils.assertArrayEquals(a, b, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		A.toArray(b);
		Utils.assertArrayEquals(a, b, tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D Av = A.viewFlip();
		b = new double[size];
		Av.toArray(b);
		for (int i = 0; i < size; i++) {
			assertEquals(a[i], b[size - 1 - i], tol);
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		Av = A.viewFlip();
		b = new double[size];
		Av.toArray(b);
		for (int i = 0; i < size; i++) {
			assertEquals(a[i], b[size - 1 - i], tol);
		}
	}

	@Test
	public void testViewFlip() {
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D B = A.viewFlip();
		for (int i = 0; i < size; i++) {
			assertEquals(A.getQuick(size - 1 - i), B.getQuick(i), tol);
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		B = A.viewFlip();
		for (int i = 0; i < size; i++) {
			assertEquals(A.getQuick(size - 1 - i), B.getQuick(i), tol);
		}
	}

	@Test
	public void testViewPart() {
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D B = A.viewPart(15, 11);
		for (int i = 0; i < 11; i++) {
			assertEquals(a[15 + i], B.getQuick(i), tol);
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		B = A.viewPart(15, 11);
		for (int i = 0; i < 11; i++) {
			assertEquals(a[15 + i], B.getQuick(i), tol);
		}
	}

	@Test
	public void testViewSelectionDoubleProcedure() {
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D B = A.viewSelection(new DoubleProcedure() {
			public boolean apply(double element) {
				return element % 2 == 0;
			}
		});
		for (int i = 0; i < B.size(); i++) {
			double el = B.getQuick(i);
			if (el % 2 != 0) {
				fail();
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		B = A.viewSelection(new DoubleProcedure() {
			public boolean apply(double element) {
				return element % 2 == 0;
			}
		});
		for (int i = 0; i < B.size(); i++) {
			double el = B.getQuick(i);
			if (el % 2 != 0) {
				fail();
			}
		}
	}

	@Test
	public void testViewSelectionIntArray() {
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(a);
		int[] indexes = new int[] { 5, 11, 22, 37, 101 };
		DoubleMatrix1D B = A.viewSelection(indexes);
		for (int i = 0; i < indexes.length; i++) {
			assertEquals(A.getQuick(indexes[i]), B.getQuick(i), tol);
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		indexes = new int[] { 5, 11, 22, 37, 101 };
		B = A.viewSelection(indexes);
		for (int i = 0; i < indexes.length; i++) {
			assertEquals(A.getQuick(indexes[i]), B.getQuick(i), tol);
		}
	}

	@Test
	public void testViewSorted() {
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D B = A.viewSorted();
		double[] b = Arrays.copyOf(a, a.length);
		Arrays.sort(b);
		for (int i = 0; i < b.length; i++) {
			assertEquals(b[i], B.getQuick(i), tol);
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		B = A.viewSorted();
		b = Arrays.copyOf(a, a.length);
		Arrays.sort(b);
		for (int i = 0; i < b.length; i++) {
			assertEquals(b[i], B.getQuick(i), tol);
		}
	}

	@Test
	public void testViewStrides() {
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(a);
		int stride = 3;
		DoubleMatrix1D B = A.viewStrides(stride);
		for (int i = 0; i < B.size(); i++) {
			assertEquals(A.getQuick(i * stride), B.getQuick(i), tol);
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		B = A.viewStrides(stride);
		for (int i = 0; i < B.size(); i++) {
			assertEquals(A.getQuick(i * stride), B.getQuick(i), tol);
		}
	}

	@Test
	public void testZDotProductDoubleMatrix1D() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D B = new DenseDoubleMatrix1D(b);
		double product = A.zDotProduct(B);
		double tmpSumMult = 0;
		for (int i = 0; i < a.length; i++) {
			tmpSumMult += a[i] * b[i];
		}
		assertEquals(tmpSumMult, product, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		B = new DenseDoubleMatrix1D(b);
		product = A.zDotProduct(B);
		tmpSumMult = 0;
		for (int i = 0; i < a.length; i++) {
			tmpSumMult += a[i] * b[i];
		}
		assertEquals(tmpSumMult, product, tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D Av = A.viewFlip();
		B = new DenseDoubleMatrix1D(b);
		DoubleMatrix1D Bv = B.viewFlip();
		product = Av.zDotProduct(Bv);
		tmpSumMult = 0;
		for (int i = 0; i < a.length; i++) {
			tmpSumMult += a[i] * b[i];
		}
		assertEquals(tmpSumMult, product, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		Av = A.viewFlip();
		B = new DenseDoubleMatrix1D(b);
		Bv = B.viewFlip();
		product = Av.zDotProduct(Bv);
		tmpSumMult = 0;
		for (int i = 0; i < a.length; i++) {
			tmpSumMult += a[i] * b[i];
		}
		assertEquals(tmpSumMult, product, tol);
	}

	@Test
	public void testZDotProductDoubleMatrix1DIntInt() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D B = new DenseDoubleMatrix1D(b);
		double product = A.zDotProduct(B, 5, B.size() - 10);
		double tmpSumMult = 0;
		for (int i = 5; i < a.length - 5; i++) {
			tmpSumMult += a[i] * b[i];
		}
		assertEquals(tmpSumMult, product, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		B = new DenseDoubleMatrix1D(b);
		product = A.zDotProduct(B, 5, B.size() - 10);
		tmpSumMult = 0;
		for (int i = 5; i < a.length - 5; i++) {
			tmpSumMult += a[i] * b[i];
		}
		assertEquals(tmpSumMult, product, tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D Av = A.viewFlip();
		B = new DenseDoubleMatrix1D(b);
		DoubleMatrix1D Bv = B.viewFlip();
		product = Av.zDotProduct(Bv, 5, Bv.size() - 10);
		tmpSumMult = 0;
		for (int i = 5; i < a.length - 5; i++) {
			tmpSumMult += a[i] * b[i];
		}
		assertEquals(tmpSumMult, product, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		Av = A.viewFlip();
		B = new DenseDoubleMatrix1D(b);
		Bv = B.viewFlip();
		product = Av.zDotProduct(Bv, 5, Bv.size() - 10);
		tmpSumMult = 0;
		for (int i = 5; i < a.length - 5; i++) {
			tmpSumMult += a[i] * b[i];
		}
	}

	@Test
	public void testZDotProductDoubleMatrix1DIntIntIntArrayList() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D B = new DenseDoubleMatrix1D(b);
		IntArrayList indexList = new IntArrayList();
		DoubleArrayList valueList = new DoubleArrayList();
		B.getNonZeros(indexList, valueList);
		double product = A.zDotProduct(B, 5, B.size() - 10, indexList);
		double tmpSumMult = 0;
		for (int i = 5; i < a.length - 5; i++) {
			tmpSumMult += a[i] * b[i];
		}
		assertEquals(tmpSumMult, product, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		B = new DenseDoubleMatrix1D(b);
		indexList = new IntArrayList();
		valueList = new DoubleArrayList();
		B.getNonZeros(indexList, valueList);
		product = A.zDotProduct(B, 5, B.size() - 10, indexList);
		tmpSumMult = 0;
		for (int i = 5; i < a.length - 5; i++) {
			tmpSumMult += a[i] * b[i];
		}
		assertEquals(tmpSumMult, product, tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D Av = A.viewFlip();
		B = new DenseDoubleMatrix1D(b);
		DoubleMatrix1D Bv = B.viewFlip();
		indexList = new IntArrayList();
		valueList = new DoubleArrayList();
		Bv.getNonZeros(indexList, valueList);
		product = Av.zDotProduct(Bv, 5, Bv.size() - 10, indexList);
		tmpSumMult = 0;
		for (int i = 5; i < a.length - 5; i++) {
			tmpSumMult += a[i] * b[i];
		}
		assertEquals(tmpSumMult, product, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		Av = A.viewFlip();
		B = new DenseDoubleMatrix1D(b);
		Bv = B.viewFlip();
		indexList = new IntArrayList();
		valueList = new DoubleArrayList();
		Bv.getNonZeros(indexList, valueList);
		product = Av.zDotProduct(Bv, 5, Bv.size() - 10, indexList);
		tmpSumMult = 0;
		for (int i = 5; i < a.length - 5; i++) {
			tmpSumMult += a[i] * b[i];
		}
		assertEquals(tmpSumMult, product, tol);
	}

	@Test
	public void testZSum() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DoubleMatrix1D A = new DenseDoubleMatrix1D(a);
		double aSum = A.zSum();
		double tmpSum = 0;
		for (int i = 0; i < a.length; i++) {
			tmpSum += a[i];
		}
		assertEquals(tmpSum, aSum, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		tmpSum = 0;
		for (int i = 0; i < a.length; i++) {
			tmpSum += a[i];
		}
		assertEquals(tmpSum, aSum, tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDoubleMatrix1D(a);
		DoubleMatrix1D Av = A.viewFlip();
		aSum = Av.zSum();
		tmpSum = 0;
		for (int i = 0; i < a.length; i++) {
			tmpSum += a[i];
		}
		assertEquals(tmpSum, aSum, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_1D(nThreadsBegin);
		A = new DenseDoubleMatrix1D(a);
		Av = A.viewFlip();
		aSum = Av.zSum();
		tmpSum = 0;
		for (int i = 0; i < a.length; i++) {
			tmpSum += a[i];
		}
		assertEquals(tmpSum, aSum, tol);
	}

}
