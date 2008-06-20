package cern.jet.math;

/**
 * Only for performance tuning of compute intensive linear algebraic
 * computations. Constructs functions that return one of
 * <ul>
 * <li><tt>a + b*constant</tt>
 * <li><tt>a - b*constant</tt>
 * <li><tt>a + b/constant</tt>
 * <li><tt>a - b/constant</tt>
 * </ul>
 * <tt>a</tt> and <tt>b</tt> are variables, <tt>constant</tt> is fixed,
 * but for performance reasons publicly accessible. Intended to be passed to
 * <tt>matrix.assign(otherMatrix,function)</tt> methods.
 */

public class DComplexPlusMult implements cern.colt.function.DComplexDComplexDComplexFunction {
	/**
	 * Public read/write access to avoid frequent object construction.
	 */
	public double[] multiplicator;

	/**
	 * Insert the method's description here. Creation date: (8/10/99 19:12:09)
	 */
	protected DComplexPlusMult(final double[] multiplicator) {
		this.multiplicator = multiplicator;
	}

	/**
	 * Returns the result of the function evaluation.
	 */
	public final double[] apply(double[] a, double[] b) {
		double[] z = new double[2];
		z[0] = b[0] * multiplicator[0] - b[1] * multiplicator[1];
		z[1] = b[1] * multiplicator[0] + b[0] * multiplicator[1];
		z[0] += a[0];
		z[1] += a[1];
		return z;
	}

	/**
	 * <tt>a - b/constant</tt>.
	 */
	public static DComplexPlusMult minusDiv(final double[] constant) {
		return new DComplexPlusMult(DComplex.neg(DComplex.inv(constant)));
	}

	/**
	 * <tt>a - b*constant</tt>.
	 */
	public static DComplexPlusMult minusMult(final double[] constant) {
		return new DComplexPlusMult(DComplex.neg(constant));
	}

	/**
	 * <tt>a + b/constant</tt>.
	 */
	public static DComplexPlusMult plusDiv(final double[] constant) {
		return new DComplexPlusMult(DComplex.inv(constant));
	}

	/**
	 * <tt>a + b*constant</tt>.
	 */
	public static DComplexPlusMult plusMult(final double[] constant) {
		return new DComplexPlusMult(constant);
	}
}
