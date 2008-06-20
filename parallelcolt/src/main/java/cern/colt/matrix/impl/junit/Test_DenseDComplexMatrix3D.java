package cern.colt.matrix.impl.junit;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cern.colt.Utils;
import cern.colt.function.DComplexProcedure;
import cern.colt.list.IntArrayList;
import cern.colt.matrix.DComplexMatrix1D;
import cern.colt.matrix.DComplexMatrix2D;
import cern.colt.matrix.DComplexMatrix2DProcedure;
import cern.colt.matrix.DComplexMatrix3D;
import cern.colt.matrix.DoubleFactory3D;
import cern.colt.matrix.DoubleMatrix3D;
import cern.colt.matrix.impl.DenseDComplexMatrix3D;
import cern.jet.math.DComplex;
import cern.jet.math.DComplexFunctions;

public class Test_DenseDComplexMatrix3D {
	private static final int slices = 5;

	private static final int rows = 53;

	private static final int cols = 57;

	private static final double tol = 1e-10;

	private static final int nThreads = 3;

	private static final int nThreadsBegin = 1;

	private double[][][] a_3d, b_3d, a_3dt, b_3dt;

	private double[] a_1d, b_1d, a_1dt;

	private Random rand;

	private static final DoubleFactory3D factory = DoubleFactory3D.dense;

	@Before
	public void setUp() throws Exception {
		rand = new Random(0);
		a_1d = new double[slices * rows * 2 * cols];
		a_3d = new double[slices][rows][2 * cols];
		int idx = 0;
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < 2 * cols; c++) {
					a_3d[s][r][c] = rand.nextDouble();
					a_1d[idx++] = a_3d[s][r][c];
				}
			}
		}
		b_1d = new double[slices * rows * 2 * cols];
		b_3d = new double[slices][rows][2 * cols];
		idx = 0;
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < 2 * cols; c++) {
					b_3d[s][r][c] = rand.nextDouble();
					b_1d[idx++] = b_3d[s][r][c];
				}
			}
		}
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		a_3dt = A.viewDice(2, 1, 0).toArray();
		a_1dt = (double[]) A.viewDice(2, 1, 0).copy().getElements();
		DComplexMatrix3D B = new DenseDComplexMatrix3D(b_3d);
		b_3dt = B.viewDice(2, 1, 0).toArray();
	}

	@After
	public void tearDown() throws Exception {
		a_1d = null;
		a_1dt = null;
		a_3d = null;
		a_3dt = null;
		b_1d = null;
		b_3d = null;
		b_3dt = null;
		System.gc();
	}

	@Test
	public void testAggregateComplexComplexComplexFunctionComplexComplexFunction() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		double[] aSum = A.aggregate(DComplexFunctions.plus, DComplexFunctions.sqrt);
		double[] tmpSum = new double[2];
		double[] tmpEl = new double[2];
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					tmpEl[0] = a_3d[s][r][2 * c];
					tmpEl[1] = a_3d[s][r][2 * c + 1];
					tmpEl = DComplex.sqrt(tmpEl);
					tmpSum[0] += tmpEl[0];
					tmpSum[1] += tmpEl[1];
				}
			}
		}
		Utils.assertArrayEquals(tmpSum, aSum, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		aSum = A.aggregate(DComplexFunctions.plus, DComplexFunctions.sqrt);
		tmpSum = new double[2];
		tmpEl = new double[2];
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					tmpEl[0] = a_3d[s][r][2 * c];
					tmpEl[1] = a_3d[s][r][2 * c + 1];
					tmpEl = DComplex.sqrt(tmpEl);
					tmpSum[0] += tmpEl[0];
					tmpSum[1] += tmpEl[1];
				}
			}
		}
		Utils.assertArrayEquals(tmpSum, aSum, tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix3D Av = A.viewDice(2, 1, 0);
		aSum = Av.aggregate(DComplexFunctions.plus, DComplexFunctions.sqrt);
		tmpSum = new double[2];
		tmpEl = new double[2];
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					tmpEl[0] = a_3dt[s][r][2 * c];
					tmpEl[1] = a_3dt[s][r][2 * c + 1];
					tmpEl = DComplex.sqrt(tmpEl);
					tmpSum[0] += tmpEl[0];
					tmpSum[1] += tmpEl[1];
				}
			}
		}
		Utils.assertArrayEquals(tmpSum, aSum, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		Av = A.viewDice(2, 1, 0);
		aSum = Av.aggregate(DComplexFunctions.plus, DComplexFunctions.sqrt);
		tmpSum = new double[2];
		tmpEl = new double[2];
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					tmpEl[0] = a_3dt[s][r][2 * c];
					tmpEl[1] = a_3dt[s][r][2 * c + 1];
					tmpEl = DComplex.sqrt(tmpEl);
					tmpSum[0] += tmpEl[0];
					tmpSum[1] += tmpEl[1];
				}
			}
		}
		Utils.assertArrayEquals(tmpSum, aSum, tol);

	}

	@Test
	public void testAggregateComplexMatrix3DComplexComplexComplexFunctionComplexComplexComplexFunction() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix3D B = new DenseDComplexMatrix3D(b_3d);
		double[] sumMult = A.aggregate(B, DComplexFunctions.plus, DComplexFunctions.mult);
		double[] tmpSumMult = new double[2];
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					tmpSumMult = DComplex.plus(tmpSumMult, DComplex.mult(new double[] { a_3d[s][r][2 * c], a_3d[s][r][2 * c + 1] }, new double[] { b_3d[s][r][2 * c], b_3d[s][r][2 * c + 1] }));
				}
			}
		}
		Utils.assertArrayEquals(tmpSumMult, sumMult, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		B = new DenseDComplexMatrix3D(b_3d);
		sumMult = A.aggregate(B, DComplexFunctions.plus, DComplexFunctions.mult);
		tmpSumMult = new double[2];
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					tmpSumMult = DComplex.plus(tmpSumMult, DComplex.mult(new double[] { a_3d[s][r][2 * c], a_3d[s][r][2 * c + 1] }, new double[] { b_3d[s][r][2 * c], b_3d[s][r][2 * c + 1] }));
				}
			}
		}
		Utils.assertArrayEquals(tmpSumMult, sumMult, tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix3D Av = A.viewDice(2, 1, 0);
		B = new DenseDComplexMatrix3D(b_3d);
		DComplexMatrix3D Bv = B.viewDice(2, 1, 0);
		sumMult = Av.aggregate(Bv, DComplexFunctions.plus, DComplexFunctions.mult);
		tmpSumMult = new double[2];
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					tmpSumMult = DComplex.plus(tmpSumMult, DComplex.mult(new double[] { a_3dt[s][r][2 * c], a_3dt[s][r][2 * c + 1] }, new double[] { b_3dt[s][r][2 * c], b_3dt[s][r][2 * c + 1] }));
				}
			}
		}
		Utils.assertArrayEquals(tmpSumMult, sumMult, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		Av = A.viewDice(2, 1, 0);
		B = new DenseDComplexMatrix3D(b_3d);
		Bv = B.viewDice(2, 1, 0);
		sumMult = Av.aggregate(Bv, DComplexFunctions.plus, DComplexFunctions.mult);
		tmpSumMult = new double[2];
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					tmpSumMult = DComplex.plus(tmpSumMult, DComplex.mult(new double[] { a_3dt[s][r][2 * c], a_3dt[s][r][2 * c + 1] }, new double[] { b_3dt[s][r][2 * c], b_3dt[s][r][2 * c + 1] }));
				}
			}
		}
		Utils.assertArrayEquals(tmpSumMult, sumMult, tol);
	}

	@Test
	public void testAssignComplexComplexFunction() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		A.assign(DComplexFunctions.acos);
		double[] tmp = new double[2];
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					tmp[0] = a_3d[s][r][2 * c];
					tmp[1] = a_3d[s][r][2 * c + 1];
					tmp = DComplex.acos(tmp);
					Utils.assertArrayEquals(tmp, A.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		A.assign(DComplexFunctions.acos);
		tmp = new double[2];
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					tmp[0] = a_3d[s][r][2 * c];
					tmp[1] = a_3d[s][r][2 * c + 1];
					tmp = DComplex.acos(tmp);
					Utils.assertArrayEquals(tmp, A.getQuick(s, r, c), tol);
				}
			}
		}
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix3D Av = A.viewDice(2, 1, 0);
		Av.assign(DComplexFunctions.acos);
		tmp = new double[2];
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					tmp[0] = a_3dt[s][r][2 * c];
					tmp[1] = a_3dt[s][r][2 * c + 1];
					tmp = DComplex.acos(tmp);
					Utils.assertArrayEquals(tmp, Av.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		Av = A.viewDice(2, 1, 0);
		Av.assign(DComplexFunctions.acos);
		tmp = new double[2];
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					tmp[0] = a_3dt[s][r][2 * c];
					tmp[1] = a_3dt[s][r][2 * c + 1];
					tmp = DComplex.acos(tmp);
					Utils.assertArrayEquals(tmp, Av.getQuick(s, r, c), tol);
				}
			}
		}
	}

	@Test
	public void testAssignComplexMatrix3D() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(slices, rows, cols);
		DComplexMatrix3D B = new DenseDComplexMatrix3D(a_3d);
		A.assign(B);
		double[] aElts = (double[]) A.getElements();
		Utils.assertArrayEquals(a_1d, aElts, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(slices, rows, cols);
		B = new DenseDComplexMatrix3D(a_3d);
		A.assign(B);
		aElts = (double[]) A.getElements();
		Utils.assertArrayEquals(a_1d, aElts, tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDComplexMatrix3D(slices, rows, cols);
		DComplexMatrix3D Av = A.viewDice(2, 1, 0);
		B = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix3D Bv = B.viewDice(2, 1, 0);
		Av.assign(Bv);
		double[] tmp = new double[2];
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					tmp[0] = a_3dt[s][r][2 * c];
					tmp[1] = a_3dt[s][r][2 * c + 1];
					Utils.assertArrayEquals(tmp, Av.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(slices, rows, cols);
		Av = A.viewDice(2, 1, 0);
		B = new DenseDComplexMatrix3D(a_3d);
		Bv = B.viewDice(2, 1, 0);
		Av.assign(Bv);
		tmp = new double[2];
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					tmp[0] = a_3dt[s][r][2 * c];
					tmp[1] = a_3dt[s][r][2 * c + 1];
					Utils.assertArrayEquals(tmp, Av.getQuick(s, r, c), tol);
				}
			}
		}
	}

	@Test
	public void testAssignComplexMatrix3DComplexComplexComplexFunction() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix3D B = new DenseDComplexMatrix3D(b_3d);
		A.assign(B, DComplexFunctions.div);
		double[] tmp1 = new double[2];
		double[] tmp2 = new double[2];
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					tmp1[0] = a_3d[s][r][2 * c];
					tmp1[1] = a_3d[s][r][2 * c + 1];
					tmp2[0] = b_3d[s][r][2 * c];
					tmp2[1] = b_3d[s][r][2 * c + 1];
					tmp1 = DComplex.div(tmp1, tmp2);
					Utils.assertArrayEquals(tmp1, A.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		B = new DenseDComplexMatrix3D(b_3d);
		A.assign(B, DComplexFunctions.div);
		tmp1 = new double[2];
		tmp2 = new double[2];
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					tmp1[0] = a_3d[s][r][2 * c];
					tmp1[1] = a_3d[s][r][2 * c + 1];
					tmp2[0] = b_3d[s][r][2 * c];
					tmp2[1] = b_3d[s][r][2 * c + 1];
					tmp1 = DComplex.div(tmp1, tmp2);
					Utils.assertArrayEquals(tmp1, A.getQuick(s, r, c), tol);
				}
			}
		}
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix3D Av = A.viewDice(2, 1, 0);
		B = new DenseDComplexMatrix3D(b_3d);
		DComplexMatrix3D Bv = B.viewDice(2, 1, 0);
		Av.assign(Bv, DComplexFunctions.div);
		tmp1 = new double[2];
		tmp2 = new double[2];
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					tmp1[0] = a_3dt[s][r][2 * c];
					tmp1[1] = a_3dt[s][r][2 * c + 1];
					tmp2[0] = b_3dt[s][r][2 * c];
					tmp2[1] = b_3dt[s][r][2 * c + 1];
					tmp1 = DComplex.div(tmp1, tmp2);
					Utils.assertArrayEquals(tmp1, Av.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		Av = A.viewDice(2, 1, 0);
		B = new DenseDComplexMatrix3D(b_3d);
		Bv = B.viewDice(2, 1, 0);
		Av.assign(Bv, DComplexFunctions.div);
		tmp1 = new double[2];
		tmp2 = new double[2];
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					tmp1[0] = a_3dt[s][r][2 * c];
					tmp1[1] = a_3dt[s][r][2 * c + 1];
					tmp2[0] = b_3dt[s][r][2 * c];
					tmp2[1] = b_3dt[s][r][2 * c + 1];
					tmp1 = DComplex.div(tmp1, tmp2);
					Utils.assertArrayEquals(tmp1, Av.getQuick(s, r, c), tol);
				}
			}
		}
	}

	@Test
	public void testAssignComplexProcedureComplexComplexFunction() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix3D B = A.copy();
		A.assign(new DComplexProcedure() {
			public boolean apply(double[] element) {
				if (DComplex.abs(element) > 3) {
					return true;
				} else {
					return false;
				}
			}
		}, DComplexFunctions.tan);
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					if (DComplex.abs(B.getQuick(s, r, c)) > 3) {
						B.setQuick(s, r, c, DComplex.tan(B.getQuick(s, r, c)));
					}
				}
			}
		}
		Utils.assertArrayEquals((double[]) B.getElements(), (double[]) A.getElements(), tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		B = A.copy();
		A.assign(new DComplexProcedure() {
			public boolean apply(double[] element) {
				if (DComplex.abs(element) > 3) {
					return true;
				} else {
					return false;
				}
			}
		}, DComplexFunctions.tan);
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					if (DComplex.abs(B.getQuick(s, r, c)) > 3) {
						B.setQuick(s, r, c, DComplex.tan(B.getQuick(s, r, c)));
					}
				}
			}
		}
		Utils.assertArrayEquals((double[]) B.getElements(), (double[]) A.getElements(), tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix3D Av = A.viewDice(2, 1, 0);
		B = A.copy();
		DComplexMatrix3D Bv = B.viewDice(2, 1, 0);
		Av.assign(new DComplexProcedure() {
			public boolean apply(double[] element) {
				if (DComplex.abs(element) > 3) {
					return true;
				} else {
					return false;
				}
			}
		}, DComplexFunctions.tan);
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					if (DComplex.abs(Bv.getQuick(s, r, c)) > 3) {
						Bv.setQuick(s, r, c, DComplex.tan(Bv.getQuick(s, r, c)));
					}
				}
			}
		}
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					Utils.assertArrayEquals(Bv.getQuick(s, r, c), Av.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		Av = A.viewDice(2, 1, 0);
		B = A.copy();
		Bv = B.viewDice(2, 1, 0);
		Av.assign(new DComplexProcedure() {
			public boolean apply(double[] element) {
				if (DComplex.abs(element) > 3) {
					return true;
				} else {
					return false;
				}
			}
		}, DComplexFunctions.tan);
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					if (DComplex.abs(Bv.getQuick(s, r, c)) > 3) {
						Bv.setQuick(s, r, c, DComplex.tan(Bv.getQuick(s, r, c)));
					}
				}
			}
		}
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					Utils.assertArrayEquals(Bv.getQuick(s, r, c), Av.getQuick(s, r, c), tol);
				}
			}
		}
	}

	@Test
	public void testAssignComplexProcedureDoubleArray() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix3D B = A.copy();
		A.assign(new DComplexProcedure() {
			public boolean apply(double[] element) {
				if (DComplex.abs(element) > 3) {
					return true;
				} else {
					return false;
				}
			}
		}, new double[] { -1, -1 });
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					if (DComplex.abs(B.getQuick(s, r, c)) > 3) {
						B.setQuick(s, r, c, new double[] { -1, -1 });
					}
				}
			}
		}
		Utils.assertArrayEquals((double[]) B.getElements(), (double[]) A.getElements(), tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		B = A.copy();
		A.assign(new DComplexProcedure() {
			public boolean apply(double[] element) {
				if (DComplex.abs(element) > 3) {
					return true;
				} else {
					return false;
				}
			}
		}, new double[] { -1, -1 });
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					if (DComplex.abs(B.getQuick(s, r, c)) > 3) {
						B.setQuick(s, r, c, new double[] { -1, -1 });
					}
				}
			}
		}
		Utils.assertArrayEquals((double[]) B.getElements(), (double[]) A.getElements(), tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix3D Av = A.viewDice(2, 1, 0);
		B = A.copy();
		DComplexMatrix3D Bv = B.viewDice(2, 1, 0);
		Av.assign(new DComplexProcedure() {
			public boolean apply(double[] element) {
				if (DComplex.abs(element) > 3) {
					return true;
				} else {
					return false;
				}
			}
		}, new double[] { -1, -1 });
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					if (DComplex.abs(Bv.getQuick(s, r, c)) > 3) {
						Bv.setQuick(s, r, c, new double[] { -1, -1 });
					}
				}
			}
		}
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					Utils.assertArrayEquals(Bv.getQuick(s, r, c), Av.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		Av = A.viewDice(2, 1, 0);
		B = A.copy();
		Bv = B.viewDice(2, 1, 0);
		Av.assign(new DComplexProcedure() {
			public boolean apply(double[] element) {
				if (DComplex.abs(element) > 3) {
					return true;
				} else {
					return false;
				}
			}
		}, new double[] { -1, -1 });
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					if (DComplex.abs(Bv.getQuick(s, r, c)) > 3) {
						Bv.setQuick(s, r, c, new double[] { -1, -1 });
					}
				}
			}
		}
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					Utils.assertArrayEquals(Bv.getQuick(s, r, c), Av.getQuick(s, r, c), tol);
				}
			}
		}
	}

	@Test
	public void testAssignComplexRealFunction() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		A.assign(DComplexFunctions.abs);
		double[] tmp = new double[2];
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					tmp[0] = a_3d[s][r][2 * c];
					tmp[1] = a_3d[s][r][2 * c + 1];
					tmp[0] = DComplex.abs(tmp);
					tmp[1] = 0;
					Utils.assertArrayEquals(tmp, A.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		A.assign(DComplexFunctions.abs);
		tmp = new double[2];
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					tmp[0] = a_3d[s][r][2 * c];
					tmp[1] = a_3d[s][r][2 * c + 1];
					tmp[0] = DComplex.abs(tmp);
					tmp[1] = 0;
					Utils.assertArrayEquals(tmp, A.getQuick(s, r, c), tol);
				}
			}
		}
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix3D Av = A.viewDice(2, 1, 0);
		Av.assign(DComplexFunctions.abs);
		tmp = new double[2];
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					tmp[0] = a_3dt[s][r][2 * c];
					tmp[1] = a_3dt[s][r][2 * c + 1];
					tmp[0] = DComplex.abs(tmp);
					tmp[1] = 0;
					Utils.assertArrayEquals(tmp, Av.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		Av = A.viewDice(2, 1, 0);
		Av.assign(DComplexFunctions.abs);
		tmp = new double[2];
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					tmp[0] = a_3dt[s][r][2 * c];
					tmp[1] = a_3dt[s][r][2 * c + 1];
					tmp[0] = DComplex.abs(tmp);
					tmp[1] = 0;
					Utils.assertArrayEquals(tmp, Av.getQuick(s, r, c), tol);
				}
			}
		}
	}

	@Test
	public void testAssignDoubleArray() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(slices, rows, cols);
		A.assign(a_1d);
		double[] aElts = (double[]) A.getElements();
		Utils.assertArrayEquals(a_1d, aElts, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(slices, rows, cols);
		A.assign(a_1d);
		aElts = (double[]) A.getElements();
		Utils.assertArrayEquals(a_1d, aElts, tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDComplexMatrix3D(slices, rows, cols);
		DComplexMatrix3D Av = A.viewDice(2, 1, 0);
		Av.assign(a_1dt);
		double[] tmp = new double[2];
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					tmp[0] = a_1dt[s * 2 * rows * slices + r * 2 * slices + 2 * c];
					tmp[1] = a_1dt[s * 2 * rows * slices + r * 2 * slices + 2 * c + 1];
					Utils.assertArrayEquals(tmp, Av.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(slices, rows, cols);
		Av = A.viewDice(2, 1, 0);
		Av.assign(a_1dt);
		tmp = new double[2];
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					tmp[0] = a_1dt[s * 2 * rows * slices + r * 2 * slices + 2 * c];
					tmp[1] = a_1dt[s * 2 * rows * slices + r * 2 * slices + 2 * c + 1];
					Utils.assertArrayEquals(tmp, Av.getQuick(s, r, c), tol);
				}
			}
		}

	}

	@Test
	public void testAssignDoubleArrayArrayArray() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(slices, rows, cols);
		A.assign(a_3d);
		double[] aElts = (double[]) A.getElements();
		Utils.assertArrayEquals(a_1d, aElts, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(slices, rows, cols);
		A.assign(a_3d);
		aElts = (double[]) A.getElements();
		Utils.assertArrayEquals(a_1d, aElts, tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDComplexMatrix3D(slices, rows, cols);
		DComplexMatrix3D Av = A.viewDice(2, 1, 0);
		Av.assign(a_3dt);
		double[] tmp = new double[2];
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					tmp[0] = a_3dt[s][r][2 * c];
					tmp[1] = a_3dt[s][r][2 * c + 1];
					Utils.assertArrayEquals(tmp, Av.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(slices, rows, cols);
		Av = A.viewDice(2, 1, 0);
		Av.assign(a_3dt);
		tmp = new double[2];
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					tmp[0] = a_3dt[s][r][2 * c];
					tmp[1] = a_3dt[s][r][2 * c + 1];
					Utils.assertArrayEquals(tmp, Av.getQuick(s, r, c), tol);
				}
			}
		}
	}

	@Test
	public void testAssignDoubleDouble() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(slices, rows, cols);
		double[] value = new double[] { Math.random(), Math.random() };
		A.assign(value[0], value[1]);
		double[] aElt = null;
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					aElt = A.getQuick(s, r, c);
					Utils.assertArrayEquals(value, aElt, tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(slices, rows, cols);
		value = new double[] { Math.random(), Math.random() };
		A.assign(value[0], value[1]);
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					aElt = A.getQuick(s, r, c);
					Utils.assertArrayEquals(value, aElt, tol);
				}
			}
		}
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDComplexMatrix3D(slices, rows, cols);
		DComplexMatrix3D Av = A.viewDice(2, 1, 0);
		value = new double[] { Math.random(), Math.random() };
		Av.assign(value[0], value[1]);
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					aElt = Av.getQuick(s, r, c);
					Utils.assertArrayEquals(value, aElt, tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(slices, rows, cols);
		Av = A.viewDice(2, 1, 0);
		value = new double[] { Math.random(), Math.random() };
		Av.assign(value[0], value[1]);
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					aElt = Av.getQuick(s, r, c);
					Utils.assertArrayEquals(value, aElt, tol);
				}
			}
		}
	}

	@Test
	public void testAssignImaginary() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(slices, rows, cols);
		DoubleMatrix3D Im = factory.random(slices, rows, cols);
		A.assignImaginary(Im);
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					Utils.assertArrayEquals(new double[] { 0, Im.getQuick(s, r, c) }, A.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(slices, rows, cols);
		Im = factory.random(slices, rows, cols);
		A.assignImaginary(Im);
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					Utils.assertArrayEquals(new double[] { 0, Im.getQuick(s, r, c) }, A.getQuick(s, r, c), tol);
				}
			}
		}
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDComplexMatrix3D(slices, rows, cols);
		DComplexMatrix3D Av = A.viewDice(2, 1, 0);
		Im = factory.random(slices, rows, cols);
		DoubleMatrix3D Imv = Im.viewDice(2, 1, 0);
		Av.assignImaginary(Imv);
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					Utils.assertArrayEquals(new double[] { 0, Imv.getQuick(s, r, c) }, Av.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(slices, rows, cols);
		Av = A.viewDice(2, 1, 0);
		Im = factory.random(slices, rows, cols);
		Imv = Im.viewDice(2, 1, 0);
		Av.assignImaginary(Imv);
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					Utils.assertArrayEquals(new double[] { 0, Imv.getQuick(s, r, c) }, Av.getQuick(s, r, c), tol);
				}
			}
		}
	}

	@Test
	public void testAssignReal() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(slices, rows, cols);
		DoubleMatrix3D Re = factory.random(slices, rows, cols);
		A.assignReal(Re);
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					Utils.assertArrayEquals(new double[] { Re.getQuick(s, r, c), 0 }, A.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(slices, rows, cols);
		Re = factory.random(slices, rows, cols);
		A.assignReal(Re);
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					Utils.assertArrayEquals(new double[] { Re.getQuick(s, r, c), 0 }, A.getQuick(s, r, c), tol);
				}
			}
		}
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDComplexMatrix3D(slices, rows, cols);
		DComplexMatrix3D Av = A.viewDice(2, 1, 0);
		Re = factory.random(slices, rows, cols);
		DoubleMatrix3D Rev = Re.viewDice(2, 1, 0);
		Av.assignReal(Rev);
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					Utils.assertArrayEquals(new double[] { Rev.getQuick(s, r, c), 0 }, Av.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(slices, rows, cols);
		Av = A.viewDice(2, 1, 0);
		Re = factory.random(slices, rows, cols);
		Rev = Re.viewDice(2, 1, 0);
		Av.assignReal(Rev);
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					Utils.assertArrayEquals(new double[] { Rev.getQuick(s, r, c), 0 }, Av.getQuick(s, r, c), tol);
				}
			}
		}
	}

	@Test
	public void testCardinality() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		int card = A.cardinality();
		assertEquals(slices * rows * cols, card);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		card = A.cardinality();
		assertEquals(slices * rows * cols, card);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix3D Av = A.viewDice(2, 1, 0);
		card = Av.cardinality();
		assertEquals(slices * rows * cols, card);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		Av = A.viewDice(2, 1, 0);
		card = Av.cardinality();
		assertEquals(slices * rows * cols, card);
	}

	@Test
	public void testCopy() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix3D B = A.copy();
		double[] bElts = (double[]) B.getElements();
		Utils.assertArrayEquals(a_1d, bElts, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		B = A.copy();
		bElts = (double[]) B.getElements();
		Utils.assertArrayEquals(a_1d, bElts, tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix3D Av = A.viewDice(2, 1, 0);
		B = Av.copy();
		bElts = (double[]) B.getElements();
		Utils.assertArrayEquals(a_1dt, bElts, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		Av = A.viewDice(2, 1, 0);
		B = Av.copy();
		bElts = (double[]) B.getElements();
		Utils.assertArrayEquals(a_1dt, bElts, tol);

	}

	@Test
	public void testEqualsDoubleArray() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(slices, rows, cols);
		double[] value = new double[] { 1, 1 };
		A.assign(1, 1);
		boolean eq = A.equals(value);
		assertEquals(true, eq);
		eq = A.equals(new double[] { 2, 1 });
		assertEquals(false, eq);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(slices, rows, cols);
		A.assign(1, 1);
		eq = A.equals(value);
		assertEquals(true, eq);
		eq = A.equals(new double[] { 2, 1 });
		assertEquals(false, eq);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDComplexMatrix3D(slices, rows, cols);
		DComplexMatrix3D Av = A.viewDice(2, 1, 0);
		value = new double[] { 1, 1 };
		Av.assign(1, 1);
		eq = Av.equals(value);
		assertEquals(true, eq);
		eq = Av.equals(new double[] { 2, 1 });
		assertEquals(false, eq);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(slices, rows, cols);
		Av = A.viewDice(2, 1, 0);
		value = new double[] { 1, 1 };
		Av.assign(1, 1);
		eq = Av.equals(value);
		assertEquals(true, eq);
		eq = Av.equals(new double[] { 2, 1 });
		assertEquals(false, eq);
	}

	@Test
	public void testEqualsObject() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix3D B = new DenseDComplexMatrix3D(b_3d);
		boolean eq = A.equals(A);
		assertEquals(true, eq);
		eq = A.equals(B);
		assertEquals(false, eq);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		B = new DenseDComplexMatrix3D(b_3d);
		eq = A.equals(A);
		assertEquals(true, eq);
		eq = A.equals(B);
		assertEquals(false, eq);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix3D Av = A.viewDice(2, 1, 0);
		B = new DenseDComplexMatrix3D(b_3d);
		DComplexMatrix3D Bv = B.viewDice(2, 1, 0);
		eq = Av.equals(Av);
		assertEquals(true, eq);
		eq = Av.equals(Bv);
		assertEquals(false, eq);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		Av = A.viewDice(2, 1, 0);
		B = new DenseDComplexMatrix3D(b_3d);
		Bv = B.viewDice(2, 1, 0);
		eq = Av.equals(Av);
		assertEquals(true, eq);
		eq = Av.equals(Bv);
		assertEquals(false, eq);

	}

	@Test
	public void testGet() {
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		double[] elem;
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					elem = A.get(s, r, c);
					assertEquals(a_3d[s][r][2 * c], elem[0], tol);
					assertEquals(a_3d[s][r][2 * c + 1], elem[1], tol);
				}
			}
		}
	}

	@Test
	public void testGetImaginaryPart() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		DoubleMatrix3D Im = A.getImaginaryPart();
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					assertEquals(a_3d[s][r][2 * c + 1], Im.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		Im = A.getImaginaryPart();
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					assertEquals(a_3d[s][r][2 * c + 1], Im.getQuick(s, r, c), tol);
				}
			}
		}
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix3D Av = A.viewDice(2, 1, 0);
		Im = Av.getImaginaryPart();
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					assertEquals(a_3dt[s][r][2 * c + 1], Im.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		Av = A.viewDice(2, 1, 0);
		Im = Av.getImaginaryPart();
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					assertEquals(a_3dt[s][r][2 * c + 1], Im.getQuick(s, r, c), tol);
				}
			}
		}
	}

	@Test
	public void testGetNonZeros() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(slices, rows, cols);
		A.setQuick(slices / 4, rows / 4, cols / 4, 1, 1);
		A.setQuick(slices / 2, rows / 2, cols / 2, 0, 2);
		A.setQuick(slices - 1, rows - 1, cols - 1, 3, 0);
		IntArrayList sliceList = new IntArrayList();
		IntArrayList rowList = new IntArrayList();
		IntArrayList colList = new IntArrayList();
		ArrayList<double[]> valueList = new ArrayList<double[]>();
		A.getNonZeros(sliceList, rowList, colList, valueList);
		assertEquals(3, sliceList.size());
		assertEquals(3, rowList.size());
		assertEquals(3, colList.size());
		assertEquals(3, valueList.size());
		assertEquals(slices / 4, sliceList.get(0));
		assertEquals(rows / 4, rowList.get(0));
		assertEquals(cols / 4, colList.get(0));
		assertEquals(slices / 2, sliceList.get(1));
		assertEquals(rows / 2, rowList.get(1));
		assertEquals(cols / 2, colList.get(1));
		assertEquals(slices - 1, sliceList.get(2));
		assertEquals(rows - 1, rowList.get(2));
		assertEquals(cols - 1, colList.get(2));
		Utils.assertArrayEquals(new double[] { 1, 1 }, valueList.get(0), tol);
		Utils.assertArrayEquals(new double[] { 0, 2 }, valueList.get(1), tol);
		Utils.assertArrayEquals(new double[] { 3, 0 }, valueList.get(2), tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(slices, rows, cols);
		A.setQuick(slices / 4, rows / 4, cols / 4, 1, 1);
		A.setQuick(slices / 2, rows / 2, cols / 2, 0, 2);
		A.setQuick(slices - 1, rows - 1, cols - 1, 3, 0);
		sliceList = new IntArrayList();
		rowList = new IntArrayList();
		colList = new IntArrayList();
		valueList = new ArrayList<double[]>();
		A.getNonZeros(sliceList, rowList, colList, valueList);
		assertEquals(3, sliceList.size());
		assertEquals(3, rowList.size());
		assertEquals(3, colList.size());
		assertEquals(3, valueList.size());
		assertEquals(slices / 4, sliceList.get(0));
		assertEquals(rows / 4, rowList.get(0));
		assertEquals(cols / 4, colList.get(0));
		assertEquals(slices / 2, sliceList.get(1));
		assertEquals(rows / 2, rowList.get(1));
		assertEquals(cols / 2, colList.get(1));
		assertEquals(slices - 1, sliceList.get(2));
		assertEquals(rows - 1, rowList.get(2));
		assertEquals(cols - 1, colList.get(2));
		Utils.assertArrayEquals(new double[] { 1, 1 }, valueList.get(0), tol);
		Utils.assertArrayEquals(new double[] { 0, 2 }, valueList.get(1), tol);
		Utils.assertArrayEquals(new double[] { 3, 0 }, valueList.get(2), tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDComplexMatrix3D(cols, rows, slices);
		DComplexMatrix3D Av = A.viewDice(2, 1, 0);
		Av.setQuick(slices / 4, rows / 4, cols / 4, 1, 1);
		Av.setQuick(slices / 2, rows / 2, cols / 2, 0, 2);
		Av.setQuick(slices - 1, rows - 1, cols - 1, 3, 0);
		sliceList = new IntArrayList();
		rowList = new IntArrayList();
		colList = new IntArrayList();
		valueList = new ArrayList<double[]>();
		Av.getNonZeros(sliceList, rowList, colList, valueList);
		assertEquals(3, sliceList.size());
		assertEquals(3, rowList.size());
		assertEquals(3, colList.size());
		assertEquals(3, valueList.size());
		assertEquals(slices / 4, sliceList.get(0));
		assertEquals(rows / 4, rowList.get(0));
		assertEquals(cols / 4, colList.get(0));
		assertEquals(slices / 2, sliceList.get(1));
		assertEquals(rows / 2, rowList.get(1));
		assertEquals(cols / 2, colList.get(1));
		assertEquals(slices - 1, sliceList.get(2));
		assertEquals(rows - 1, rowList.get(2));
		assertEquals(cols - 1, colList.get(2));
		Utils.assertArrayEquals(new double[] { 1, 1 }, valueList.get(0), tol);
		Utils.assertArrayEquals(new double[] { 0, 2 }, valueList.get(1), tol);
		Utils.assertArrayEquals(new double[] { 3, 0 }, valueList.get(2), tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(cols, rows, slices);
		Av = A.viewDice(2, 1, 0);
		Av.setQuick(slices / 4, rows / 4, cols / 4, 1, 1);
		Av.setQuick(slices / 2, rows / 2, cols / 2, 0, 2);
		Av.setQuick(slices - 1, rows - 1, cols - 1, 3, 0);
		sliceList = new IntArrayList();
		rowList = new IntArrayList();
		colList = new IntArrayList();
		valueList = new ArrayList<double[]>();
		Av.getNonZeros(sliceList, rowList, colList, valueList);
		assertEquals(3, sliceList.size());
		assertEquals(3, rowList.size());
		assertEquals(3, colList.size());
		assertEquals(3, valueList.size());
		assertEquals(slices / 4, sliceList.get(0));
		assertEquals(rows / 4, rowList.get(0));
		assertEquals(cols / 4, colList.get(0));
		assertEquals(slices / 2, sliceList.get(1));
		assertEquals(rows / 2, rowList.get(1));
		assertEquals(cols / 2, colList.get(1));
		assertEquals(slices - 1, sliceList.get(2));
		assertEquals(rows - 1, rowList.get(2));
		assertEquals(cols - 1, colList.get(2));
		Utils.assertArrayEquals(new double[] { 1, 1 }, valueList.get(0), tol);
		Utils.assertArrayEquals(new double[] { 0, 2 }, valueList.get(1), tol);
		Utils.assertArrayEquals(new double[] { 3, 0 }, valueList.get(2), tol);
	}

	@Test
	public void testGetQuick() {
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		double[] elem;
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					elem = A.getQuick(s, r, c);
					assertEquals(a_3d[s][r][2 * c], elem[0], tol);
					assertEquals(a_3d[s][r][2 * c + 1], elem[1], tol);
				}
			}
		}
	}

	@Test
	public void testGetRealPart() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		DoubleMatrix3D R = A.getRealPart();
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					assertEquals(a_3d[s][r][2 * c], R.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		R = A.getRealPart();
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					assertEquals(a_3d[s][r][2 * c], R.getQuick(s, r, c), tol);
				}
			}
		}
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix3D Av = A.viewDice(2, 1, 0);
		R = Av.getRealPart();
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					assertEquals(a_3d[c][r][2 * s], R.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		Av = A.viewDice(2, 1, 0);
		R = Av.getRealPart();
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					assertEquals(a_3d[c][r][2 * s], R.getQuick(s, r, c), tol);
				}
			}
		}

	}

	@Test
	public void testSet() {
		DComplexMatrix3D A = new DenseDComplexMatrix3D(slices, rows, cols);
		double[] elem = new double[] { Math.random(), Math.random() };
		A.set(slices / 2, rows / 2, cols / 2, elem);
		double[] aElem = A.getQuick(slices / 2, rows / 2, cols / 2);
		Utils.assertArrayEquals(elem, aElem, tol);
	}

	@Test
	public void testSetQuickIntIntIntDoubleArray() {
		DComplexMatrix3D A = new DenseDComplexMatrix3D(slices, rows, cols);
		double[] elem = new double[] { Math.random(), Math.random() };
		A.setQuick(slices / 2, rows / 2, cols / 2, elem);
		double[] aElem = A.getQuick(slices / 2, rows / 2, cols / 2);
		Utils.assertArrayEquals(elem, aElem, tol);
	}

	@Test
	public void testSetQuickIntIntIntDoubleDouble() {
		DComplexMatrix3D A = new DenseDComplexMatrix3D(slices, rows, cols);
		double[] elem = new double[] { Math.random(), Math.random() };
		A.setQuick(slices / 2, rows / 2, cols / 2, elem[0], elem[1]);
		double[] aElem = A.getQuick(slices / 2, rows / 2, cols / 2);
		Utils.assertArrayEquals(elem, aElem, tol);
	}

	@Test
	public void testToArray() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		double[][][] array = A.toArray();
		Utils.assertArrayEquals(a_3d, array, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		array = A.toArray();
		Utils.assertArrayEquals(a_3d, array, tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix3D Av = A.viewDice(2, 1, 0);
		array = Av.toArray();
		Utils.assertArrayEquals(a_3dt, array, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		Av = A.viewDice(2, 1, 0);
		array = Av.toArray();
		Utils.assertArrayEquals(a_3dt, array, tol);

	}

	@Test
	public void testToString() {
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		String s = A.toString();
		System.out.println(s);
	}

	@Test
	public void testVectorize() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix1D B = A.vectorize();
		int idx = 0;
		for (int s = 0; s < slices; s++) {
			for (int c = 0; c < cols; c++) {
				for (int r = 0; r < rows; r++) {
					Utils.assertArrayEquals(A.getQuick(s, r, c), B.getQuick(idx++), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		B = A.vectorize();
		idx = 0;
		for (int s = 0; s < slices; s++) {
			for (int c = 0; c < cols; c++) {
				for (int r = 0; r < rows; r++) {
					Utils.assertArrayEquals(A.getQuick(s, r, c), B.getQuick(idx++), tol);
				}
			}
		}
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix3D Av = A.viewDice(2, 1, 0);
		B = Av.vectorize();
		idx = 0;
		for (int s = 0; s < cols; s++) {
			for (int c = 0; c < slices; c++) {
				for (int r = 0; r < rows; r++) {
					Utils.assertArrayEquals(Av.getQuick(s, r, c), B.getQuick(idx++), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		Av = A.viewDice(2, 1, 0);
		B = Av.vectorize();
		idx = 0;
		for (int s = 0; s < cols; s++) {
			for (int c = 0; c < slices; c++) {
				for (int r = 0; r < rows; r++) {
					Utils.assertArrayEquals(Av.getQuick(s, r, c), B.getQuick(idx++), tol);
				}
			}
		}

	}

	@Test
	public void testViewColumn() {
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix2D B = A.viewColumn(cols / 2);
		assertEquals(slices, B.rows());
		assertEquals(rows, B.columns());
		double[] tmp;
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				tmp = B.getQuick(s, r);
				assertEquals(a_3d[s][r][2 * (cols / 2)], tmp[0], tol);
				assertEquals(a_3d[s][r][2 * (cols / 2) + 1], tmp[1], tol);
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		B = A.viewColumn(cols / 2);
		assertEquals(slices, B.rows());
		assertEquals(rows, B.columns());
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				tmp = B.getQuick(s, r);
				assertEquals(a_3d[s][r][2 * (cols / 2)], tmp[0], tol);
				assertEquals(a_3d[s][r][2 * (cols / 2) + 1], tmp[1], tol);
			}
		}
	}

	@Test
	public void testViewColumnFlip() {
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix3D B = A.viewColumnFlip();
		assertEquals(A.size(), B.size());
		double[] tmp;
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					tmp = B.getQuick(s, r, c);
					assertEquals(a_3d[s][r][2 * (cols - 1 - c)], tmp[0], tol);
					assertEquals(a_3d[s][r][2 * (cols - 1 - c) + 1], tmp[1], tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		B = A.viewColumnFlip();
		assertEquals(A.size(), B.size());
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					tmp = B.getQuick(s, r, c);
					assertEquals(a_3d[s][r][2 * (cols - 1 - c)], tmp[0], tol);
					assertEquals(a_3d[s][r][2 * (cols - 1 - c) + 1], tmp[1], tol);
				}
			}
		}
	}

	@Test
	public void testViewDice() {
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix3D B = A.viewDice(2, 1, 0);
		assertEquals(A.slices(), B.columns());
		assertEquals(A.rows(), B.rows());
		assertEquals(A.columns(), B.slices());
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					Utils.assertArrayEquals(A.getQuick(s, r, c), B.getQuick(c, r, s), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		B = A.viewDice(2, 1, 0);
		assertEquals(A.slices(), B.columns());
		assertEquals(A.rows(), B.rows());
		assertEquals(A.columns(), B.slices());
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					Utils.assertArrayEquals(A.getQuick(s, r, c), B.getQuick(c, r, s), tol);
				}
			}
		}
	}

	@Test
	public void testViewPart() {
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix3D B = A.viewPart(2, 15, 11, 2, 21, 27);
		for (int s = 0; s < 2; s++) {
			for (int r = 0; r < 21; r++) {
				for (int c = 0; c < 27; c++) {
					Utils.assertArrayEquals(A.getQuick(2 + s, 15 + r, 11 + c), B.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		B = A.viewPart(2, 15, 11, 2, 21, 27);
		for (int s = 0; s < 2; s++) {
			for (int r = 0; r < 21; r++) {
				for (int c = 0; c < 27; c++) {
					Utils.assertArrayEquals(A.getQuick(2 + s, 15 + r, 11 + c), B.getQuick(s, r, c), tol);
				}
			}
		}
	}

	@Test
	public void testViewRow() {
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix2D B = A.viewRow(rows / 2);
		assertEquals(slices, B.rows());
		assertEquals(cols, B.columns());
		double[] tmp;
		for (int s = 0; s < slices; s++) {
			for (int c = 0; c < cols; c++) {
				tmp = B.getQuick(s, c);
				assertEquals(a_3d[s][rows / 2][2 * c], tmp[0], tol);
				assertEquals(a_3d[s][rows / 2][2 * c + 1], tmp[1], tol);
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		B = A.viewRow(rows / 2);
		assertEquals(slices, B.rows());
		assertEquals(cols, B.columns());
		for (int s = 0; s < slices; s++) {
			for (int c = 0; c < cols; c++) {
				tmp = B.getQuick(s, c);
				assertEquals(a_3d[s][rows / 2][2 * c], tmp[0], tol);
				assertEquals(a_3d[s][rows / 2][2 * c + 1], tmp[1], tol);
			}
		}
	}

	@Test
	public void testViewRowFlip() {
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix3D B = A.viewRowFlip();
		assertEquals(A.size(), B.size());
		double[] tmp;
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					tmp = B.getQuick(s, r, c);
					assertEquals(a_3d[s][rows - 1 - r][2 * c], tmp[0], tol);
					assertEquals(a_3d[s][rows - 1 - r][2 * c + 1], tmp[1], tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		B = A.viewRowFlip();
		assertEquals(A.size(), B.size());
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					tmp = B.getQuick(s, r, c);
					assertEquals(a_3d[s][rows - 1 - r][2 * c], tmp[0], tol);
					assertEquals(a_3d[s][rows - 1 - r][2 * c + 1], tmp[1], tol);
				}
			}
		}
	}

	@Test
	public void testViewSelectionComplexMatrix2DProcedure() {
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		final double[] value = new double[] { 2, 3 };
		A.setQuick(3, rows / 4, 0, value);
		DComplexMatrix3D B = A.viewSelection(new DComplexMatrix2DProcedure() {
			public boolean apply(DComplexMatrix2D element) {
				return DComplex.isEqual(element.getQuick(rows / 4, 0), value, tol);

			}
		});
		assertEquals(1, B.slices());
		assertEquals(A.rows(), B.rows());
		assertEquals(A.columns(), B.columns());
		Utils.assertArrayEquals(A.getQuick(3, rows / 4, 0), B.getQuick(0, rows / 4, 0), tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		A.setQuick(3, rows / 4, 0, value);
		B = A.viewSelection(new DComplexMatrix2DProcedure() {
			public boolean apply(DComplexMatrix2D element) {
				return DComplex.isEqual(element.getQuick(rows / 4, 0), value, tol);

			}
		});
		assertEquals(1, B.slices());
		assertEquals(A.rows(), B.rows());
		assertEquals(A.columns(), B.columns());
		Utils.assertArrayEquals(A.getQuick(3, rows / 4, 0), B.getQuick(0, rows / 4, 0), tol);
	}

	@Test
	public void testViewSelectionIntArrayIntArrayIntArray() {
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		int[] sliceIndexes = new int[] { 2, 3 };
		int[] rowIndexes = new int[] { 5, 11, 22, 37 };
		int[] colIndexes = new int[] { 2, 17, 32, 47, 51 };
		DComplexMatrix3D B = A.viewSelection(sliceIndexes, rowIndexes, colIndexes);
		assertEquals(sliceIndexes.length, B.slices());
		assertEquals(rowIndexes.length, B.rows());
		assertEquals(colIndexes.length, B.columns());
		for (int s = 0; s < sliceIndexes.length; s++) {
			for (int r = 0; r < rowIndexes.length; r++) {
				for (int c = 0; c < colIndexes.length; c++) {
					Utils.assertArrayEquals(A.getQuick(sliceIndexes[s], rowIndexes[r], colIndexes[c]), B.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		sliceIndexes = new int[] { 2, 3 };
		rowIndexes = new int[] { 5, 11, 22, 37 };
		colIndexes = new int[] { 2, 17, 32, 47, 51 };
		B = A.viewSelection(sliceIndexes, rowIndexes, colIndexes);
		assertEquals(sliceIndexes.length, B.slices());
		assertEquals(rowIndexes.length, B.rows());
		assertEquals(colIndexes.length, B.columns());
		for (int s = 0; s < sliceIndexes.length; s++) {
			for (int r = 0; r < rowIndexes.length; r++) {
				for (int c = 0; c < colIndexes.length; c++) {
					Utils.assertArrayEquals(A.getQuick(sliceIndexes[s], rowIndexes[r], colIndexes[c]), B.getQuick(s, r, c), tol);
				}
			}
		}
	}

	@Test
	public void testViewSlice() {
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix2D B = A.viewSlice(slices / 2);
		assertEquals(rows, B.rows());
		assertEquals(cols, B.columns());
		double[] tmp;
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				tmp = B.getQuick(r, c);
				assertEquals(a_3d[slices / 2][r][2 * c], tmp[0], tol);
				assertEquals(a_3d[slices / 2][r][2 * c + 1], tmp[1], tol);
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		B = A.viewSlice(slices / 2);
		assertEquals(rows, B.rows());
		assertEquals(cols, B.columns());
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				tmp = B.getQuick(r, c);
				assertEquals(a_3d[slices / 2][r][2 * c], tmp[0], tol);
				assertEquals(a_3d[slices / 2][r][2 * c + 1], tmp[1], tol);
			}
		}
	}

	@Test
	public void testViewSliceFlip() {
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix3D B = A.viewSliceFlip();
		assertEquals(A.size(), B.size());
		double[] tmp;
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					tmp = B.getQuick(s, r, c);
					assertEquals(a_3d[slices - 1 - s][r][2 * c], tmp[0], tol);
					assertEquals(a_3d[slices - 1 - s][r][2 * c + 1], tmp[1], tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		B = A.viewSliceFlip();
		assertEquals(A.size(), B.size());
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					tmp = B.getQuick(s, r, c);
					assertEquals(a_3d[slices - 1 - s][r][2 * c], tmp[0], tol);
					assertEquals(a_3d[slices - 1 - s][r][2 * c + 1], tmp[1], tol);
				}
			}
		}
	}

	@Test
	public void testViewStrides() {
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		int sliceStride = 2;
		int rowStride = 3;
		int colStride = 5;
		DComplexMatrix3D B = A.viewStrides(sliceStride, rowStride, colStride);
		for (int s = 0; s < B.slices(); s++) {
			for (int r = 0; r < B.rows(); r++) {
				for (int c = 0; c < B.columns(); c++) {
					Utils.assertArrayEquals(A.getQuick(s * sliceStride, r * rowStride, c * colStride), B.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		B = A.viewStrides(sliceStride, rowStride, colStride);
		for (int s = 0; s < B.slices(); s++) {
			for (int r = 0; r < B.rows(); r++) {
				for (int c = 0; c < B.columns(); c++) {
					Utils.assertArrayEquals(A.getQuick(s * sliceStride, r * rowStride, c * colStride), B.getQuick(s, r, c), tol);
				}
			}
		}
	}

	@Test
	public void testZSum() {
		/* No view */
		// single thread
		Utils.setNP(1);
		DComplexMatrix3D A = new DenseDComplexMatrix3D(a_3d);
		double[] aSum = A.zSum();
		double[] tmpSum = new double[2];
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					tmpSum[0] += a_3d[s][r][2 * c];
					tmpSum[1] += a_3d[s][r][2 * c + 1];
				}
			}
		}
		Utils.assertArrayEquals(tmpSum, aSum, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		aSum = A.zSum();
		tmpSum = new double[2];
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					tmpSum[0] += a_3d[s][r][2 * c];
					tmpSum[1] += a_3d[s][r][2 * c + 1];
				}
			}
		}
		Utils.assertArrayEquals(tmpSum, aSum, tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseDComplexMatrix3D(a_3d);
		DComplexMatrix3D Av = A.viewDice(2, 1, 0);
		aSum = Av.zSum();
		tmpSum = new double[2];
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					tmpSum[0] += a_3dt[s][r][2 * c];
					tmpSum[1] += a_3dt[s][r][2 * c + 1];
				}
			}
		}
		Utils.assertArrayEquals(tmpSum, aSum, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseDComplexMatrix3D(a_3d);
		Av = A.viewDice(2, 1, 0);
		aSum = Av.zSum();
		tmpSum = new double[2];
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					tmpSum[0] += a_3dt[s][r][2 * c];
					tmpSum[1] += a_3dt[s][r][2 * c + 1];
				}
			}
		}
		Utils.assertArrayEquals(tmpSum, aSum, tol);
	}

}
