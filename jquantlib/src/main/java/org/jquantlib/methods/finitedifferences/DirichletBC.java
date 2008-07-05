/*
 Copyright (C) 2008 Srinivas Hasti

 This source code is release under the BSD License.
 
 This file is part of JQuantLib, a free-software/open-source library
 for financial quantitative analysts and developers - http://jquantlib.org/

 JQuantLib is free software: you can redistribute it and/or modify it
 under the terms of the JQuantLib license.  You should have received a
 copy of the license along with this program; if not, please email
 <jquant-devel@lists.sourceforge.net>. The license is also available online at
 <http://www.jquantlib.org/index.php/LICENSE.TXT>.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE.  See the license for more details.
 
 JQuantLib is based on QuantLib. http://quantlib.org/
 When applicable, the original copyright notice follows this notice.
 */
package org.jquantlib.methods.finitedifferences;

import java.util.List;

public class DirichletBC implements
		BoundaryCondition<TridiagonalOperator, List<Double>> {

	private/* @Real */double value;
	private Side side;

	public DirichletBC(double value, Side side) {
		this.value = value;
		this.side = side;
	}

	@Override
	public void applyAfterApplying(List<Double> u) {
		switch (side) {
		case LOWER:
			u.set(0, value);
			break;
		case UPPER:
			u.set(u.size() - 1, value);
			break;
		default:
			throw new IllegalStateException(
					"unknown side for Neumann boundary condition");
		}
	}

	@Override
	public void applyAfterSolving(List<Double> arrayType) {

	}

	@Override
	public void applyBeforeApplying(TridiagonalOperator operator) {
		switch (side) {
		case LOWER:
			operator.setFirstRow(1.0, 0.0);
			break;
		case UPPER:
			operator.setLastRow(0.0, 1.0);
			break;
		default:
			throw new IllegalStateException(
					"unknown side for Neumann boundary condition");
		}
	}

	@Override
	public void applyBeforeSolving(TridiagonalOperator operator,
			List<Double> rhs) {
		switch (side) {
		case LOWER:
			operator.setFirstRow(1.0, 0.0);
			rhs.set(0, value);
			break;
		case UPPER:
			operator.setLastRow(0.0, 1.0);
			rhs.set(rhs.size() - 1, value);
			break;
		default:
			throw new IllegalStateException(
					"unknown side for Neumann boundary condition");
		}
	}

	@Override
	public void setTime(double t) {
	}
}