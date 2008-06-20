package cern.colt.matrix.impl.junit;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cern.colt.Utils;
import cern.colt.function.FComplexProcedure;
import cern.colt.list.IntArrayList;
import cern.colt.matrix.FComplexMatrix1D;
import cern.colt.matrix.FComplexMatrix2D;
import cern.colt.matrix.FComplexMatrix2DProcedure;
import cern.colt.matrix.FComplexMatrix3D;
import cern.colt.matrix.FloatFactory3D;
import cern.colt.matrix.FloatMatrix3D;
import cern.colt.matrix.impl.DenseFComplexMatrix3D;
import cern.jet.math.FComplex;
import cern.jet.math.FComplexFunctions;

public class Test_DenseFComplexMatrix3D {
	private static final int slices = 5;

	private static final int rows = 53;

	private static final int cols = 57;

	private static final float tol = 1e-1f;

	private static final int nThreads = 3;

	private static final int nThreadsBegin = 1;

	private float[][][] a_3d, b_3d, a_3dt, b_3dt;

	private float[] a_1d, b_1d, a_1dt;

	private Random rand;

	private static final FloatFactory3D factory = FloatFactory3D.dense;

	@Before
	public void setUp() throws Exception {
		rand = new Random(0);
		a_1d = new float[slices * rows * 2 * cols];
		a_3d = new float[slices][rows][2 * cols];
		int idx = 0;
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < 2 * cols; c++) {
					a_3d[s][r][c] = rand.nextFloat();
					a_1d[idx++] = a_3d[s][r][c];
				}
			}
		}
		b_1d = new float[slices * rows * 2 * cols];
		b_3d = new float[slices][rows][2 * cols];
		idx = 0;
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < 2 * cols; c++) {
					b_3d[s][r][c] = rand.nextFloat();
					b_1d[idx++] = b_3d[s][r][c];
				}
			}
		}
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		a_3dt = A.viewDice(2, 1, 0).toArray();
		a_1dt = (float[]) A.viewDice(2, 1, 0).copy().getElements();
		FComplexMatrix3D B = new DenseFComplexMatrix3D(b_3d);
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
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		float[] aSum = A.aggregate(FComplexFunctions.plus, FComplexFunctions.sqrt);
		float[] tmpSum = new float[2];
		float[] tmpEl = new float[2];
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					tmpEl[0] = a_3d[s][r][2 * c];
					tmpEl[1] = a_3d[s][r][2 * c + 1];
					tmpEl = FComplex.sqrt(tmpEl);
					tmpSum[0] += tmpEl[0];
					tmpSum[1] += tmpEl[1];
				}
			}
		}
		Utils.assertArrayEquals(tmpSum, aSum, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(a_3d);
		aSum = A.aggregate(FComplexFunctions.plus, FComplexFunctions.sqrt);
		tmpSum = new float[2];
		tmpEl = new float[2];
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					tmpEl[0] = a_3d[s][r][2 * c];
					tmpEl[1] = a_3d[s][r][2 * c + 1];
					tmpEl = FComplex.sqrt(tmpEl);
					tmpSum[0] += tmpEl[0];
					tmpSum[1] += tmpEl[1];
				}
			}
		}
		Utils.assertArrayEquals(tmpSum, aSum, tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix3D Av = A.viewDice(2, 1, 0);
		aSum = Av.aggregate(FComplexFunctions.plus, FComplexFunctions.sqrt);
		tmpSum = new float[2];
		tmpEl = new float[2];
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					tmpEl[0] = a_3dt[s][r][2 * c];
					tmpEl[1] = a_3dt[s][r][2 * c + 1];
					tmpEl = FComplex.sqrt(tmpEl);
					tmpSum[0] += tmpEl[0];
					tmpSum[1] += tmpEl[1];
				}
			}
		}
		Utils.assertArrayEquals(tmpSum, aSum, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(a_3d);
		Av = A.viewDice(2, 1, 0);
		aSum = Av.aggregate(FComplexFunctions.plus, FComplexFunctions.sqrt);
		tmpSum = new float[2];
		tmpEl = new float[2];
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					tmpEl[0] = a_3dt[s][r][2 * c];
					tmpEl[1] = a_3dt[s][r][2 * c + 1];
					tmpEl = FComplex.sqrt(tmpEl);
					tmpSum[0] += tmpEl[0];
					tmpSum[1] += tmpEl[1];
				}
			}
		}
		Utils.assertArrayEquals(tmpSum, aSum, tol);

	}

	@Test
	public void testAggregateComplexMatrix3FComplexComplexComplexFunctionComplexComplexComplexFunction() {
		/* No view */
		// single thread
		Utils.setNP(1);
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix3D B = new DenseFComplexMatrix3D(b_3d);
		float[] sumMult = A.aggregate(B, FComplexFunctions.plus, FComplexFunctions.mult);
		float[] tmpSumMult = new float[2];
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					tmpSumMult = FComplex.plus(tmpSumMult, FComplex.mult(new float[] { a_3d[s][r][2 * c], a_3d[s][r][2 * c + 1] }, new float[] { b_3d[s][r][2 * c], b_3d[s][r][2 * c + 1] }));
				}
			}
		}
		Utils.assertArrayEquals(tmpSumMult, sumMult, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(a_3d);
		B = new DenseFComplexMatrix3D(b_3d);
		sumMult = A.aggregate(B, FComplexFunctions.plus, FComplexFunctions.mult);
		tmpSumMult = new float[2];
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					tmpSumMult = FComplex.plus(tmpSumMult, FComplex.mult(new float[] { a_3d[s][r][2 * c], a_3d[s][r][2 * c + 1] }, new float[] { b_3d[s][r][2 * c], b_3d[s][r][2 * c + 1] }));
				}
			}
		}
		Utils.assertArrayEquals(tmpSumMult, sumMult, tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix3D Av = A.viewDice(2, 1, 0);
		B = new DenseFComplexMatrix3D(b_3d);
		FComplexMatrix3D Bv = B.viewDice(2, 1, 0);
		sumMult = Av.aggregate(Bv, FComplexFunctions.plus, FComplexFunctions.mult);
		tmpSumMult = new float[2];
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					tmpSumMult = FComplex.plus(tmpSumMult, FComplex.mult(new float[] { a_3dt[s][r][2 * c], a_3dt[s][r][2 * c + 1] }, new float[] { b_3dt[s][r][2 * c], b_3dt[s][r][2 * c + 1] }));
				}
			}
		}
		Utils.assertArrayEquals(tmpSumMult, sumMult, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(a_3d);
		Av = A.viewDice(2, 1, 0);
		B = new DenseFComplexMatrix3D(b_3d);
		Bv = B.viewDice(2, 1, 0);
		sumMult = Av.aggregate(Bv, FComplexFunctions.plus, FComplexFunctions.mult);
		tmpSumMult = new float[2];
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					tmpSumMult = FComplex.plus(tmpSumMult, FComplex.mult(new float[] { a_3dt[s][r][2 * c], a_3dt[s][r][2 * c + 1] }, new float[] { b_3dt[s][r][2 * c], b_3dt[s][r][2 * c + 1] }));
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
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		A.assign(FComplexFunctions.acos);
		float[] tmp = new float[2];
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					tmp[0] = a_3d[s][r][2 * c];
					tmp[1] = a_3d[s][r][2 * c + 1];
					tmp = FComplex.acos(tmp);
					Utils.assertArrayEquals(tmp, A.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(a_3d);
		A.assign(FComplexFunctions.acos);
		tmp = new float[2];
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					tmp[0] = a_3d[s][r][2 * c];
					tmp[1] = a_3d[s][r][2 * c + 1];
					tmp = FComplex.acos(tmp);
					Utils.assertArrayEquals(tmp, A.getQuick(s, r, c), tol);
				}
			}
		}
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix3D Av = A.viewDice(2, 1, 0);
		Av.assign(FComplexFunctions.acos);
		tmp = new float[2];
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					tmp[0] = a_3dt[s][r][2 * c];
					tmp[1] = a_3dt[s][r][2 * c + 1];
					tmp = FComplex.acos(tmp);
					Utils.assertArrayEquals(tmp, Av.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(a_3d);
		Av = A.viewDice(2, 1, 0);
		Av.assign(FComplexFunctions.acos);
		tmp = new float[2];
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					tmp[0] = a_3dt[s][r][2 * c];
					tmp[1] = a_3dt[s][r][2 * c + 1];
					tmp = FComplex.acos(tmp);
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
		FComplexMatrix3D A = new DenseFComplexMatrix3D(slices, rows, cols);
		FComplexMatrix3D B = new DenseFComplexMatrix3D(a_3d);
		A.assign(B);
		float[] aElts = (float[]) A.getElements();
		Utils.assertArrayEquals(a_1d, aElts, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(slices, rows, cols);
		B = new DenseFComplexMatrix3D(a_3d);
		A.assign(B);
		aElts = (float[]) A.getElements();
		Utils.assertArrayEquals(a_1d, aElts, tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseFComplexMatrix3D(slices, rows, cols);
		FComplexMatrix3D Av = A.viewDice(2, 1, 0);
		B = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix3D Bv = B.viewDice(2, 1, 0);
		Av.assign(Bv);
		float[] tmp = new float[2];
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
		A = new DenseFComplexMatrix3D(slices, rows, cols);
		Av = A.viewDice(2, 1, 0);
		B = new DenseFComplexMatrix3D(a_3d);
		Bv = B.viewDice(2, 1, 0);
		Av.assign(Bv);
		tmp = new float[2];
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
	public void testAssignComplexMatrix3FComplexComplexComplexFunction() {
		/* No view */
		// single thread
		Utils.setNP(1);
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix3D B = new DenseFComplexMatrix3D(b_3d);
		A.assign(B, FComplexFunctions.div);
		float[] tmp1 = new float[2];
		float[] tmp2 = new float[2];
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					tmp1[0] = a_3d[s][r][2 * c];
					tmp1[1] = a_3d[s][r][2 * c + 1];
					tmp2[0] = b_3d[s][r][2 * c];
					tmp2[1] = b_3d[s][r][2 * c + 1];
					tmp1 = FComplex.div(tmp1, tmp2);
					Utils.assertArrayEquals(tmp1, A.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(a_3d);
		B = new DenseFComplexMatrix3D(b_3d);
		A.assign(B, FComplexFunctions.div);
		tmp1 = new float[2];
		tmp2 = new float[2];
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					tmp1[0] = a_3d[s][r][2 * c];
					tmp1[1] = a_3d[s][r][2 * c + 1];
					tmp2[0] = b_3d[s][r][2 * c];
					tmp2[1] = b_3d[s][r][2 * c + 1];
					tmp1 = FComplex.div(tmp1, tmp2);
					Utils.assertArrayEquals(tmp1, A.getQuick(s, r, c), tol);
				}
			}
		}
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix3D Av = A.viewDice(2, 1, 0);
		B = new DenseFComplexMatrix3D(b_3d);
		FComplexMatrix3D Bv = B.viewDice(2, 1, 0);
		Av.assign(Bv, FComplexFunctions.div);
		tmp1 = new float[2];
		tmp2 = new float[2];
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					tmp1[0] = a_3dt[s][r][2 * c];
					tmp1[1] = a_3dt[s][r][2 * c + 1];
					tmp2[0] = b_3dt[s][r][2 * c];
					tmp2[1] = b_3dt[s][r][2 * c + 1];
					tmp1 = FComplex.div(tmp1, tmp2);
					Utils.assertArrayEquals(tmp1, Av.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(a_3d);
		Av = A.viewDice(2, 1, 0);
		B = new DenseFComplexMatrix3D(b_3d);
		Bv = B.viewDice(2, 1, 0);
		Av.assign(Bv, FComplexFunctions.div);
		tmp1 = new float[2];
		tmp2 = new float[2];
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					tmp1[0] = a_3dt[s][r][2 * c];
					tmp1[1] = a_3dt[s][r][2 * c + 1];
					tmp2[0] = b_3dt[s][r][2 * c];
					tmp2[1] = b_3dt[s][r][2 * c + 1];
					tmp1 = FComplex.div(tmp1, tmp2);
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
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix3D B = A.copy();
		A.assign(new FComplexProcedure() {
			public boolean apply(float[] element) {
				if (FComplex.abs(element) > 3) {
					return true;
				} else {
					return false;
				}
			}
		}, FComplexFunctions.tan);
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					if (FComplex.abs(B.getQuick(s, r, c)) > 3) {
						B.setQuick(s, r, c, FComplex.tan(B.getQuick(s, r, c)));
					}
				}
			}
		}
		Utils.assertArrayEquals((float[]) B.getElements(), (float[]) A.getElements(), tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(a_3d);
		B = A.copy();
		A.assign(new FComplexProcedure() {
			public boolean apply(float[] element) {
				if (FComplex.abs(element) > 3) {
					return true;
				} else {
					return false;
				}
			}
		}, FComplexFunctions.tan);
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					if (FComplex.abs(B.getQuick(s, r, c)) > 3) {
						B.setQuick(s, r, c, FComplex.tan(B.getQuick(s, r, c)));
					}
				}
			}
		}
		Utils.assertArrayEquals((float[]) B.getElements(), (float[]) A.getElements(), tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix3D Av = A.viewDice(2, 1, 0);
		B = A.copy();
		FComplexMatrix3D Bv = B.viewDice(2, 1, 0);
		Av.assign(new FComplexProcedure() {
			public boolean apply(float[] element) {
				if (FComplex.abs(element) > 3) {
					return true;
				} else {
					return false;
				}
			}
		}, FComplexFunctions.tan);
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					if (FComplex.abs(Bv.getQuick(s, r, c)) > 3) {
						Bv.setQuick(s, r, c, FComplex.tan(Bv.getQuick(s, r, c)));
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
		A = new DenseFComplexMatrix3D(a_3d);
		Av = A.viewDice(2, 1, 0);
		B = A.copy();
		Bv = B.viewDice(2, 1, 0);
		Av.assign(new FComplexProcedure() {
			public boolean apply(float[] element) {
				if (FComplex.abs(element) > 3) {
					return true;
				} else {
					return false;
				}
			}
		}, FComplexFunctions.tan);
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					if (FComplex.abs(Bv.getQuick(s, r, c)) > 3) {
						Bv.setQuick(s, r, c, FComplex.tan(Bv.getQuick(s, r, c)));
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
	public void testAssignComplexProcedureFloatArray() {
		/* No view */
		// single thread
		Utils.setNP(1);
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix3D B = A.copy();
		A.assign(new FComplexProcedure() {
			public boolean apply(float[] element) {
				if (FComplex.abs(element) > 3) {
					return true;
				} else {
					return false;
				}
			}
		}, new float[] { -1, -1 });
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					if (FComplex.abs(B.getQuick(s, r, c)) > 3) {
						B.setQuick(s, r, c, new float[] { -1, -1 });
					}
				}
			}
		}
		Utils.assertArrayEquals((float[]) B.getElements(), (float[]) A.getElements(), tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(a_3d);
		B = A.copy();
		A.assign(new FComplexProcedure() {
			public boolean apply(float[] element) {
				if (FComplex.abs(element) > 3) {
					return true;
				} else {
					return false;
				}
			}
		}, new float[] { -1, -1 });
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					if (FComplex.abs(B.getQuick(s, r, c)) > 3) {
						B.setQuick(s, r, c, new float[] { -1, -1 });
					}
				}
			}
		}
		Utils.assertArrayEquals((float[]) B.getElements(), (float[]) A.getElements(), tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix3D Av = A.viewDice(2, 1, 0);
		B = A.copy();
		FComplexMatrix3D Bv = B.viewDice(2, 1, 0);
		Av.assign(new FComplexProcedure() {
			public boolean apply(float[] element) {
				if (FComplex.abs(element) > 3) {
					return true;
				} else {
					return false;
				}
			}
		}, new float[] { -1, -1 });
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					if (FComplex.abs(Bv.getQuick(s, r, c)) > 3) {
						Bv.setQuick(s, r, c, new float[] { -1, -1 });
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
		A = new DenseFComplexMatrix3D(a_3d);
		Av = A.viewDice(2, 1, 0);
		B = A.copy();
		Bv = B.viewDice(2, 1, 0);
		Av.assign(new FComplexProcedure() {
			public boolean apply(float[] element) {
				if (FComplex.abs(element) > 3) {
					return true;
				} else {
					return false;
				}
			}
		}, new float[] { -1, -1 });
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					if (FComplex.abs(Bv.getQuick(s, r, c)) > 3) {
						Bv.setQuick(s, r, c, new float[] { -1, -1 });
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
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		A.assign(FComplexFunctions.abs);
		float[] tmp = new float[2];
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					tmp[0] = a_3d[s][r][2 * c];
					tmp[1] = a_3d[s][r][2 * c + 1];
					tmp[0] = FComplex.abs(tmp);
					tmp[1] = 0;
					Utils.assertArrayEquals(tmp, A.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(a_3d);
		A.assign(FComplexFunctions.abs);
		tmp = new float[2];
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					tmp[0] = a_3d[s][r][2 * c];
					tmp[1] = a_3d[s][r][2 * c + 1];
					tmp[0] = FComplex.abs(tmp);
					tmp[1] = 0;
					Utils.assertArrayEquals(tmp, A.getQuick(s, r, c), tol);
				}
			}
		}
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix3D Av = A.viewDice(2, 1, 0);
		Av.assign(FComplexFunctions.abs);
		tmp = new float[2];
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					tmp[0] = a_3dt[s][r][2 * c];
					tmp[1] = a_3dt[s][r][2 * c + 1];
					tmp[0] = FComplex.abs(tmp);
					tmp[1] = 0;
					Utils.assertArrayEquals(tmp, Av.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(a_3d);
		Av = A.viewDice(2, 1, 0);
		Av.assign(FComplexFunctions.abs);
		tmp = new float[2];
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					tmp[0] = a_3dt[s][r][2 * c];
					tmp[1] = a_3dt[s][r][2 * c + 1];
					tmp[0] = FComplex.abs(tmp);
					tmp[1] = 0;
					Utils.assertArrayEquals(tmp, Av.getQuick(s, r, c), tol);
				}
			}
		}
	}

	@Test
	public void testAssignFloatArray() {
		/* No view */
		// single thread
		Utils.setNP(1);
		FComplexMatrix3D A = new DenseFComplexMatrix3D(slices, rows, cols);
		A.assign(a_1d);
		float[] aElts = (float[]) A.getElements();
		Utils.assertArrayEquals(a_1d, aElts, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(slices, rows, cols);
		A.assign(a_1d);
		aElts = (float[]) A.getElements();
		Utils.assertArrayEquals(a_1d, aElts, tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseFComplexMatrix3D(slices, rows, cols);
		FComplexMatrix3D Av = A.viewDice(2, 1, 0);
		Av.assign(a_1dt);
		float[] tmp = new float[2];
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
		A = new DenseFComplexMatrix3D(slices, rows, cols);
		Av = A.viewDice(2, 1, 0);
		Av.assign(a_1dt);
		tmp = new float[2];
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
	public void testAssignFloatArrayArrayArray() {
		/* No view */
		// single thread
		Utils.setNP(1);
		FComplexMatrix3D A = new DenseFComplexMatrix3D(slices, rows, cols);
		A.assign(a_3d);
		float[] aElts = (float[]) A.getElements();
		Utils.assertArrayEquals(a_1d, aElts, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(slices, rows, cols);
		A.assign(a_3d);
		aElts = (float[]) A.getElements();
		Utils.assertArrayEquals(a_1d, aElts, tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseFComplexMatrix3D(slices, rows, cols);
		FComplexMatrix3D Av = A.viewDice(2, 1, 0);
		Av.assign(a_3dt);
		float[] tmp = new float[2];
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
		A = new DenseFComplexMatrix3D(slices, rows, cols);
		Av = A.viewDice(2, 1, 0);
		Av.assign(a_3dt);
		tmp = new float[2];
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
	public void testAssignFloatFloat() {
		/* No view */
		// single thread
		Utils.setNP(1);
		FComplexMatrix3D A = new DenseFComplexMatrix3D(slices, rows, cols);
		float[] value = new float[] { (float) Math.random(), (float) Math.random() };
		A.assign(value[0], value[1]);
		float[] aElt = null;
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
		A = new DenseFComplexMatrix3D(slices, rows, cols);
		value = new float[] { (float) Math.random(), (float) Math.random() };
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
		A = new DenseFComplexMatrix3D(slices, rows, cols);
		FComplexMatrix3D Av = A.viewDice(2, 1, 0);
		value = new float[] { (float) Math.random(), (float) Math.random() };
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
		A = new DenseFComplexMatrix3D(slices, rows, cols);
		Av = A.viewDice(2, 1, 0);
		value = new float[] { (float) Math.random(), (float) Math.random() };
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
		FComplexMatrix3D A = new DenseFComplexMatrix3D(slices, rows, cols);
		FloatMatrix3D Im = factory.random(slices, rows, cols);
		A.assignImaginary(Im);
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					Utils.assertArrayEquals(new float[] { 0, Im.getQuick(s, r, c) }, A.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(slices, rows, cols);
		Im = factory.random(slices, rows, cols);
		A.assignImaginary(Im);
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					Utils.assertArrayEquals(new float[] { 0, Im.getQuick(s, r, c) }, A.getQuick(s, r, c), tol);
				}
			}
		}
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseFComplexMatrix3D(slices, rows, cols);
		FComplexMatrix3D Av = A.viewDice(2, 1, 0);
		Im = factory.random(slices, rows, cols);
		FloatMatrix3D Imv = Im.viewDice(2, 1, 0);
		Av.assignImaginary(Imv);
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					Utils.assertArrayEquals(new float[] { 0, Imv.getQuick(s, r, c) }, Av.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(slices, rows, cols);
		Av = A.viewDice(2, 1, 0);
		Im = factory.random(slices, rows, cols);
		Imv = Im.viewDice(2, 1, 0);
		Av.assignImaginary(Imv);
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					Utils.assertArrayEquals(new float[] { 0, Imv.getQuick(s, r, c) }, Av.getQuick(s, r, c), tol);
				}
			}
		}
	}

	@Test
	public void testAssignReal() {
		/* No view */
		// single thread
		Utils.setNP(1);
		FComplexMatrix3D A = new DenseFComplexMatrix3D(slices, rows, cols);
		FloatMatrix3D Re = factory.random(slices, rows, cols);
		A.assignReal(Re);
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					Utils.assertArrayEquals(new float[] { Re.getQuick(s, r, c), 0 }, A.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(slices, rows, cols);
		Re = factory.random(slices, rows, cols);
		A.assignReal(Re);
		for (int s = 0; s < slices; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					Utils.assertArrayEquals(new float[] { Re.getQuick(s, r, c), 0 }, A.getQuick(s, r, c), tol);
				}
			}
		}
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseFComplexMatrix3D(slices, rows, cols);
		FComplexMatrix3D Av = A.viewDice(2, 1, 0);
		Re = factory.random(slices, rows, cols);
		FloatMatrix3D Rev = Re.viewDice(2, 1, 0);
		Av.assignReal(Rev);
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					Utils.assertArrayEquals(new float[] { Rev.getQuick(s, r, c), 0 }, Av.getQuick(s, r, c), tol);
				}
			}
		}
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(slices, rows, cols);
		Av = A.viewDice(2, 1, 0);
		Re = factory.random(slices, rows, cols);
		Rev = Re.viewDice(2, 1, 0);
		Av.assignReal(Rev);
		for (int s = 0; s < cols; s++) {
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < slices; c++) {
					Utils.assertArrayEquals(new float[] { Rev.getQuick(s, r, c), 0 }, Av.getQuick(s, r, c), tol);
				}
			}
		}
	}

	@Test
	public void testCardinality() {
		/* No view */
		// single thread
		Utils.setNP(1);
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		int card = A.cardinality();
		assertEquals(slices * rows * cols, card);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(a_3d);
		card = A.cardinality();
		assertEquals(slices * rows * cols, card);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix3D Av = A.viewDice(2, 1, 0);
		card = Av.cardinality();
		assertEquals(slices * rows * cols, card);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(a_3d);
		Av = A.viewDice(2, 1, 0);
		card = Av.cardinality();
		assertEquals(slices * rows * cols, card);
	}

	@Test
	public void testCopy() {
		/* No view */
		// single thread
		Utils.setNP(1);
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix3D B = A.copy();
		float[] bElts = (float[]) B.getElements();
		Utils.assertArrayEquals(a_1d, bElts, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(a_3d);
		B = A.copy();
		bElts = (float[]) B.getElements();
		Utils.assertArrayEquals(a_1d, bElts, tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix3D Av = A.viewDice(2, 1, 0);
		B = Av.copy();
		bElts = (float[]) B.getElements();
		Utils.assertArrayEquals(a_1dt, bElts, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(a_3d);
		Av = A.viewDice(2, 1, 0);
		B = Av.copy();
		bElts = (float[]) B.getElements();
		Utils.assertArrayEquals(a_1dt, bElts, tol);

	}

	@Test
	public void testEqualsFloatArray() {
		/* No view */
		// single thread
		Utils.setNP(1);
		FComplexMatrix3D A = new DenseFComplexMatrix3D(slices, rows, cols);
		float[] value = new float[] { 1, 1 };
		A.assign(1, 1);
		boolean eq = A.equals(value);
		assertEquals(true, eq);
		eq = A.equals(new float[] { 2, 1 });
		assertEquals(false, eq);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(slices, rows, cols);
		A.assign(1, 1);
		eq = A.equals(value);
		assertEquals(true, eq);
		eq = A.equals(new float[] { 2, 1 });
		assertEquals(false, eq);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseFComplexMatrix3D(slices, rows, cols);
		FComplexMatrix3D Av = A.viewDice(2, 1, 0);
		value = new float[] { 1, 1 };
		Av.assign(1, 1);
		eq = Av.equals(value);
		assertEquals(true, eq);
		eq = Av.equals(new float[] { 2, 1 });
		assertEquals(false, eq);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(slices, rows, cols);
		Av = A.viewDice(2, 1, 0);
		value = new float[] { 1, 1 };
		Av.assign(1, 1);
		eq = Av.equals(value);
		assertEquals(true, eq);
		eq = Av.equals(new float[] { 2, 1 });
		assertEquals(false, eq);
	}

	@Test
	public void testEqualsObject() {
		/* No view */
		// single thread
		Utils.setNP(1);
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix3D B = new DenseFComplexMatrix3D(b_3d);
		boolean eq = A.equals(A);
		assertEquals(true, eq);
		eq = A.equals(B);
		assertEquals(false, eq);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(a_3d);
		B = new DenseFComplexMatrix3D(b_3d);
		eq = A.equals(A);
		assertEquals(true, eq);
		eq = A.equals(B);
		assertEquals(false, eq);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix3D Av = A.viewDice(2, 1, 0);
		B = new DenseFComplexMatrix3D(b_3d);
		FComplexMatrix3D Bv = B.viewDice(2, 1, 0);
		eq = Av.equals(Av);
		assertEquals(true, eq);
		eq = Av.equals(Bv);
		assertEquals(false, eq);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(a_3d);
		Av = A.viewDice(2, 1, 0);
		B = new DenseFComplexMatrix3D(b_3d);
		Bv = B.viewDice(2, 1, 0);
		eq = Av.equals(Av);
		assertEquals(true, eq);
		eq = Av.equals(Bv);
		assertEquals(false, eq);

	}

	@Test
	public void testGet() {
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		float[] elem;
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
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		FloatMatrix3D Im = A.getImaginaryPart();
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
		A = new DenseFComplexMatrix3D(a_3d);
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
		A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix3D Av = A.viewDice(2, 1, 0);
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
		A = new DenseFComplexMatrix3D(a_3d);
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
		FComplexMatrix3D A = new DenseFComplexMatrix3D(slices, rows, cols);
		A.setQuick(slices / 4, rows / 4, cols / 4, 1, 1);
		A.setQuick(slices / 2, rows / 2, cols / 2, 0, 2);
		A.setQuick(slices - 1, rows - 1, cols - 1, 3, 0);
		IntArrayList sliceList = new IntArrayList();
		IntArrayList rowList = new IntArrayList();
		IntArrayList colList = new IntArrayList();
		ArrayList<float[]> valueList = new ArrayList<float[]>();
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
		Utils.assertArrayEquals(new float[] { 1, 1 }, valueList.get(0), tol);
		Utils.assertArrayEquals(new float[] { 0, 2 }, valueList.get(1), tol);
		Utils.assertArrayEquals(new float[] { 3, 0 }, valueList.get(2), tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(slices, rows, cols);
		A.setQuick(slices / 4, rows / 4, cols / 4, 1, 1);
		A.setQuick(slices / 2, rows / 2, cols / 2, 0, 2);
		A.setQuick(slices - 1, rows - 1, cols - 1, 3, 0);
		sliceList = new IntArrayList();
		rowList = new IntArrayList();
		colList = new IntArrayList();
		valueList = new ArrayList<float[]>();
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
		Utils.assertArrayEquals(new float[] { 1, 1 }, valueList.get(0), tol);
		Utils.assertArrayEquals(new float[] { 0, 2 }, valueList.get(1), tol);
		Utils.assertArrayEquals(new float[] { 3, 0 }, valueList.get(2), tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseFComplexMatrix3D(cols, rows, slices);
		FComplexMatrix3D Av = A.viewDice(2, 1, 0);
		Av.setQuick(slices / 4, rows / 4, cols / 4, 1, 1);
		Av.setQuick(slices / 2, rows / 2, cols / 2, 0, 2);
		Av.setQuick(slices - 1, rows - 1, cols - 1, 3, 0);
		sliceList = new IntArrayList();
		rowList = new IntArrayList();
		colList = new IntArrayList();
		valueList = new ArrayList<float[]>();
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
		Utils.assertArrayEquals(new float[] { 1, 1 }, valueList.get(0), tol);
		Utils.assertArrayEquals(new float[] { 0, 2 }, valueList.get(1), tol);
		Utils.assertArrayEquals(new float[] { 3, 0 }, valueList.get(2), tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(cols, rows, slices);
		Av = A.viewDice(2, 1, 0);
		Av.setQuick(slices / 4, rows / 4, cols / 4, 1, 1);
		Av.setQuick(slices / 2, rows / 2, cols / 2, 0, 2);
		Av.setQuick(slices - 1, rows - 1, cols - 1, 3, 0);
		sliceList = new IntArrayList();
		rowList = new IntArrayList();
		colList = new IntArrayList();
		valueList = new ArrayList<float[]>();
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
		Utils.assertArrayEquals(new float[] { 1, 1 }, valueList.get(0), tol);
		Utils.assertArrayEquals(new float[] { 0, 2 }, valueList.get(1), tol);
		Utils.assertArrayEquals(new float[] { 3, 0 }, valueList.get(2), tol);
	}

	@Test
	public void testGetQuick() {
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		float[] elem;
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
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		FloatMatrix3D R = A.getRealPart();
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
		A = new DenseFComplexMatrix3D(a_3d);
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
		A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix3D Av = A.viewDice(2, 1, 0);
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
		A = new DenseFComplexMatrix3D(a_3d);
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
		FComplexMatrix3D A = new DenseFComplexMatrix3D(slices, rows, cols);
		float[] elem = new float[] { (float) Math.random(), (float) Math.random() };
		A.set(slices / 2, rows / 2, cols / 2, elem);
		float[] aElem = A.getQuick(slices / 2, rows / 2, cols / 2);
		Utils.assertArrayEquals(elem, aElem, tol);
	}

	@Test
	public void testSetQuickIntIntIntFloatArray() {
		FComplexMatrix3D A = new DenseFComplexMatrix3D(slices, rows, cols);
		float[] elem = new float[] { (float) Math.random(), (float) Math.random() };
		A.setQuick(slices / 2, rows / 2, cols / 2, elem);
		float[] aElem = A.getQuick(slices / 2, rows / 2, cols / 2);
		Utils.assertArrayEquals(elem, aElem, tol);
	}

	@Test
	public void testSetQuickIntIntIntFloatFloat() {
		FComplexMatrix3D A = new DenseFComplexMatrix3D(slices, rows, cols);
		float[] elem = new float[] { (float) Math.random(), (float) Math.random() };
		A.setQuick(slices / 2, rows / 2, cols / 2, elem[0], elem[1]);
		float[] aElem = A.getQuick(slices / 2, rows / 2, cols / 2);
		Utils.assertArrayEquals(elem, aElem, tol);
	}

	@Test
	public void testToArray() {
		/* No view */
		// single thread
		Utils.setNP(1);
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		float[][][] array = A.toArray();
		Utils.assertArrayEquals(a_3d, array, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(a_3d);
		array = A.toArray();
		Utils.assertArrayEquals(a_3d, array, tol);
		/* View */
		// single thread
		Utils.setNP(1);
		A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix3D Av = A.viewDice(2, 1, 0);
		array = Av.toArray();
		Utils.assertArrayEquals(a_3dt, array, tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(a_3d);
		Av = A.viewDice(2, 1, 0);
		array = Av.toArray();
		Utils.assertArrayEquals(a_3dt, array, tol);

	}

	@Test
	public void testToString() {
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		String s = A.toString();
		System.out.println(s);
	}

	@Test
	public void testVectorize() {
		/* No view */
		// single thread
		Utils.setNP(1);
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix1D B = A.vectorize();
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
		A = new DenseFComplexMatrix3D(a_3d);
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
		A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix3D Av = A.viewDice(2, 1, 0);
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
		A = new DenseFComplexMatrix3D(a_3d);
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
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix2D B = A.viewColumn(cols / 2);
		assertEquals(slices, B.rows());
		assertEquals(rows, B.columns());
		float[] tmp;
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
		A = new DenseFComplexMatrix3D(a_3d);
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
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix3D B = A.viewColumnFlip();
		assertEquals(A.size(), B.size());
		float[] tmp;
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
		A = new DenseFComplexMatrix3D(a_3d);
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
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix3D B = A.viewDice(2, 1, 0);
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
		A = new DenseFComplexMatrix3D(a_3d);
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
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix3D B = A.viewPart(2, 15, 11, 2, 21, 27);
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
		A = new DenseFComplexMatrix3D(a_3d);
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
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix2D B = A.viewRow(rows / 2);
		assertEquals(slices, B.rows());
		assertEquals(cols, B.columns());
		float[] tmp;
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
		A = new DenseFComplexMatrix3D(a_3d);
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
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix3D B = A.viewRowFlip();
		assertEquals(A.size(), B.size());
		float[] tmp;
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
		A = new DenseFComplexMatrix3D(a_3d);
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
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		final float[] value = new float[] { 2, 3 };
		A.setQuick(3, rows / 4, 0, value);
		FComplexMatrix3D B = A.viewSelection(new FComplexMatrix2DProcedure() {
			public boolean apply(FComplexMatrix2D element) {
				return FComplex.isEqual(element.getQuick(rows / 4, 0), value, tol);

			}
		});
		assertEquals(1, B.slices());
		assertEquals(A.rows(), B.rows());
		assertEquals(A.columns(), B.columns());
		Utils.assertArrayEquals(A.getQuick(3, rows / 4, 0), B.getQuick(0, rows / 4, 0), tol);
		// multiple threads
		Utils.setNP(nThreads);
		Utils.setThreadsBeginN_3D(nThreadsBegin);
		A = new DenseFComplexMatrix3D(a_3d);
		A.setQuick(3, rows / 4, 0, value);
		B = A.viewSelection(new FComplexMatrix2DProcedure() {
			public boolean apply(FComplexMatrix2D element) {
				return FComplex.isEqual(element.getQuick(rows / 4, 0), value, tol);

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
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		int[] sliceIndexes = new int[] { 2, 3 };
		int[] rowIndexes = new int[] { 5, 11, 22, 37 };
		int[] colIndexes = new int[] { 2, 17, 32, 47, 51 };
		FComplexMatrix3D B = A.viewSelection(sliceIndexes, rowIndexes, colIndexes);
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
		A = new DenseFComplexMatrix3D(a_3d);
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
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix2D B = A.viewSlice(slices / 2);
		assertEquals(rows, B.rows());
		assertEquals(cols, B.columns());
		float[] tmp;
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
		A = new DenseFComplexMatrix3D(a_3d);
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
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix3D B = A.viewSliceFlip();
		assertEquals(A.size(), B.size());
		float[] tmp;
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
		A = new DenseFComplexMatrix3D(a_3d);
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
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		int sliceStride = 2;
		int rowStride = 3;
		int colStride = 5;
		FComplexMatrix3D B = A.viewStrides(sliceStride, rowStride, colStride);
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
		A = new DenseFComplexMatrix3D(a_3d);
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
		FComplexMatrix3D A = new DenseFComplexMatrix3D(a_3d);
		float[] aSum = A.zSum();
		float[] tmpSum = new float[2];
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
		A = new DenseFComplexMatrix3D(a_3d);
		aSum = A.zSum();
		tmpSum = new float[2];
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
		A = new DenseFComplexMatrix3D(a_3d);
		FComplexMatrix3D Av = A.viewDice(2, 1, 0);
		aSum = Av.zSum();
		tmpSum = new float[2];
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
		A = new DenseFComplexMatrix3D(a_3d);
		Av = A.viewDice(2, 1, 0);
		aSum = Av.zSum();
		tmpSum = new float[2];
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
