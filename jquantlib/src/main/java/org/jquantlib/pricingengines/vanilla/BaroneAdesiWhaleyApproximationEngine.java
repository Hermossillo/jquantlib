/*
 Copyright (C) 2008 Richard Gomes

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

/*
 Copyright (C) 2003, 2004 Ferdinando Ametrano

 This file is part of QuantLib, a free-software/open-source library
 for financial quantitative analysts and developers - http://quantlib.org/

 QuantLib is free software: you can redistribute it and/or modify it
 under the terms of the QuantLib license.  You should have received a
 copy of the license along with this program; if not, please email
 <quantlib-dev@lists.sf.net>. The license is also available online at
 <http://quantlib.org/license.shtml>.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE.  See the license for more details.
*/


package org.jquantlib.pricingengines.vanilla;

import org.jquantlib.daycounters.DayCounter;
import org.jquantlib.exercise.AmericanExercise;
import org.jquantlib.exercise.Exercise;
import org.jquantlib.instruments.StrikedTypePayoff;
import org.jquantlib.math.distributions.CumulativeNormalDistribution;
import org.jquantlib.pricingengines.BlackCalculator;
import org.jquantlib.pricingengines.BlackFormula;
import org.jquantlib.pricingengines.VanillaOptionEngine;
import org.jquantlib.processes.GeneralizedBlackScholesProcess;
import org.jquantlib.instruments.Option;

/**
 * Barone-Adesi and Whaley pricing engine for American options (1987)
 * <p>
 * Ported from 
 * <ul>
 * <li>ql/pricingengines/vanilla/baroneadesiwhaleyengine.cpp</li>
 * <li>ql/pricingengines/vanilla/baroneadesiwhaleyengine.hpp</li>
 * </ul>
 * 
 * @author <Richard Gomes>
 *
 */
public class BaroneAdesiWhaleyApproximationEngine extends VanillaOptionEngine {

	@Override
	public void calculate() {

		if (!(arguments.exercise.type()==Exercise.Type.AMERICAN)){
			throw new ArithmeticException("not an American Option");
		}

		if (!(arguments.exercise instanceof AmericanExercise)){
			throw new ArithmeticException("non-American exercise given");
		}
		AmericanExercise ex = (AmericanExercise)arguments.exercise;
		if (ex.payoffAtExpiry()){
			throw new ArithmeticException("payoff at expiry not handled");
		}

		if (!(arguments.payoff instanceof StrikedTypePayoff)){
			throw new ArithmeticException("non-striked payoff given");
		}
		StrikedTypePayoff payoff = (StrikedTypePayoff)arguments.payoff;

		if (!(arguments.stochasticProcess instanceof GeneralizedBlackScholesProcess)){
			throw new ArithmeticException("Black-Scholes process required");
		}
		GeneralizedBlackScholesProcess process = (GeneralizedBlackScholesProcess)arguments.stochasticProcess;

        
        double /*@Real*/ variance = process.blackVolatility().getLink().blackVariance(
            ex.lastDate(), payoff.getStrike());
        double /*@DiscountFactor*/ dividendDiscount = process.dividendYield().getLink().discount(
            ex.lastDate());
        double /*@DiscountFactor*/ riskFreeDiscount = process.riskFreeRate().getLink().discount(
            ex.lastDate());
        double /*@Real*/ spot = process.stateVariable().getLink().evaluate();
        double /*@Real*/ forwardPrice = spot * dividendDiscount / riskFreeDiscount;
        BlackCalculator black = new BlackCalculator(payoff, forwardPrice, Math.sqrt(variance),
                              riskFreeDiscount);

        if (dividendDiscount>=1.0 && payoff.getOptionType()==Option.Type.CALL) {
            // early exercise never optimal
            results.value        = black.value();
            results.delta        = black.delta(spot);
            results.deltaForward = black.deltaForward();
            results.elasticity   = black.elasticity(spot);
            results.gamma        = black.gamma(spot);

            DayCounter rfdc  = process.riskFreeRate().getLink().dayCounter();
            DayCounter divdc = process.dividendYield().getLink().dayCounter();
            DayCounter voldc = process.blackVolatility().getLink().dayCounter();
            double /*@Time*/ t = rfdc.yearFraction(process.riskFreeRate().getLink().referenceDate(),
                                       arguments.exercise.lastDate());
            results.rho = black.rho(t);

            t = divdc.yearFraction(process.dividendYield().getLink().referenceDate(),
                                   arguments.exercise.lastDate());
            results.dividendRho = black.dividendRho(t);

            t = voldc.yearFraction(process.blackVolatility().getLink().referenceDate(),
                                   arguments.exercise.lastDate());
            results.vega        = black.vega(t);
            results.theta       = black.theta(spot, t);
            results.thetaPerDay = black.thetaPerDay(spot, t);

            results.strikeSensitivity  = black.strikeSensitivity();
            results.itmCashProbability = black.itmCashProbability();
        } else {
            // early exercise can be optimal
            CumulativeNormalDistribution cumNormalDist = new CumulativeNormalDistribution();
            double /*@Real*/ tolerance = 1e-6;
            double /*@Real*/ Sk = criticalPrice(payoff, riskFreeDiscount,
                dividendDiscount, variance, tolerance);
            double /*@Real*/ forwardSk = Sk * dividendDiscount / riskFreeDiscount;
            double /*@Real*/ d1 = (Math.log(forwardSk/payoff.getStrike()) + 0.5*variance)
                /Math.sqrt(variance);
            double /*@Real*/ n = 2.0*Math.log(dividendDiscount/riskFreeDiscount)/variance;
            double /*@Real*/ K = -2.0*Math.log(riskFreeDiscount)/
                (variance*(1.0-riskFreeDiscount));
            double /*@Real*/ Q, a;
            switch (payoff.getOptionType()) {
                case CALL:
                    Q = (-(n-1.0) + Math.sqrt(((n-1.0)*(n-1.0))+4.0*K))/2.0;
                    a =  (Sk/Q) * (1.0 - dividendDiscount * cumNormalDist.evaluate(d1));
                    if (spot<Sk) {
                        results.value = black.value() +
                            a * Math.pow((spot/Sk), Q);
                    } else {
                        results.value = spot - payoff.getStrike();
                    }
                    break;
                case PUT:
                    Q = (-(n-1.0) - Math.sqrt(((n-1.0)*(n-1.0))+4.0*K))/2.0;
                    a = -(Sk/Q) *
                        (1.0 - dividendDiscount * cumNormalDist.evaluate(-d1));
                    if (spot>Sk) {
                        results.value = black.value() +
                            a * Math.pow((spot/Sk), Q);
                    } else {
                        results.value = payoff.getStrike() - spot;
                    }
                    break;
                default:
                  throw new ArithmeticException("unknown option type");
            }
        } // end of "early exercise can be optimal"
		
	}
	
    static double  criticalPrice(
            StrikedTypePayoff payoff,
            double /*@DiscountFactor*/ riskFreeDiscount,
            double /*@DiscountFactor*/ dividendDiscount,
            double variance){
    	return criticalPrice(payoff, riskFreeDiscount, dividendDiscount, variance, 1.0e-6);
    }
    static double  criticalPrice(
            StrikedTypePayoff payoff,
            double /*@DiscountFactor*/ riskFreeDiscount,
            double /*@DiscountFactor*/ dividendDiscount,
            double variance,
            double tolerance){
    	
        // Calculation of seed value, Si
        double /*@Real*/ n= 2.0*Math.log(dividendDiscount/riskFreeDiscount)/(variance);
        double /*@Real*/ m=-2.0*Math.log(riskFreeDiscount)/(variance);
        double /*@Real*/ bT = Math.log(dividendDiscount/riskFreeDiscount);

        double /*@Real*/ qu, Su, h, Si;
        switch (payoff.getOptionType()) {
          case CALL:
            qu = (-(n-1.0) + Math.sqrt(((n-1.0)*(n-1.0)) + 4.0*m))/2.0;
            Su = payoff.getStrike() / (1.0 - 1.0/qu);
            h = -(bT + 2.0*Math.sqrt(variance)) * payoff.getStrike() /
                (Su - payoff.getStrike());
            Si = payoff.getStrike() + (Su - payoff.getStrike()) *
                (1.0 - Math.exp(h));
            break;
          case PUT:
            qu = (-(n-1.0) - Math.sqrt(((n-1.0)*(n-1.0)) + 4.0*m))/2.0;
            Su = payoff.getStrike() / (1.0 - 1.0/qu);
            h = (bT - 2.0*Math.sqrt(variance)) * payoff.getStrike() /
                (payoff.getStrike() - Su);
            Si = Su + (payoff.getStrike() - Su) * Math.exp(h);
            break;
          default:
            throw new ArithmeticException("unknown option type");
        }


        // Newton Raphson algorithm for finding critical price Si
        double /*@Real*/ Q, LHS, RHS, bi;
        double /*@Real*/ forwardSi = Si * dividendDiscount / riskFreeDiscount;
        double /*@Real*/ d1 = (Math.log(forwardSi/payoff.getStrike()) + 0.5*variance) /
            Math.sqrt(variance);
        CumulativeNormalDistribution cumNormalDist = new CumulativeNormalDistribution();
        double /*@Real*/ K = (riskFreeDiscount!=1.0 ? -2.0*Math.log(riskFreeDiscount)/
            (variance*(1.0-riskFreeDiscount)) : 0.0);
        double /*@Real*/ temp = BlackFormula.blackFormula(payoff.getOptionType(), payoff.getStrike(),
                forwardSi, Math.sqrt(variance))*riskFreeDiscount;
        switch (payoff.getOptionType()) {
          case CALL:
            Q = (-(n-1.0) + Math.sqrt(((n-1.0)*(n-1.0)) + 4 * K)) / 2;
            LHS = Si - payoff.getStrike();
            RHS = temp + (1 - dividendDiscount * cumNormalDist.evaluate(d1)) * Si / Q;
            bi =  dividendDiscount * cumNormalDist.evaluate(d1) * (1 - 1/Q) +
                (1 - dividendDiscount *
                 cumNormalDist.derivative(d1) / Math.sqrt(variance)) / Q;
            while (Math.abs(LHS - RHS)/payoff.getStrike() > tolerance) {
                Si = (payoff.getStrike() + RHS - bi * Si) / (1 - bi);
                forwardSi = Si * dividendDiscount / riskFreeDiscount;
                d1 = (Math.log(forwardSi/payoff.getStrike())+0.5*variance)
                    /Math.sqrt(variance);
                LHS = Si - payoff.getStrike();
                double /*@Real*/ temp2 = BlackFormula.blackFormula(payoff.getOptionType(), payoff.getStrike(),
                    forwardSi, Math.sqrt(variance))*riskFreeDiscount;
                RHS = temp2 + (1 - dividendDiscount * cumNormalDist.evaluate(d1)) * Si / Q;
                bi = dividendDiscount * cumNormalDist.evaluate(d1) * (1 - 1 / Q)
                    + (1 - dividendDiscount *
                       cumNormalDist.derivative(d1) / Math.sqrt(variance))
                    / Q;
            }
            break;
          case PUT:
            Q = (-(n-1.0) - Math.sqrt(((n-1.0)*(n-1.0)) + 4 * K)) / 2;
            LHS = payoff.getStrike() - Si;
            RHS = temp - (1 - dividendDiscount * cumNormalDist.evaluate(-d1)) * Si / Q;
            bi = -dividendDiscount * cumNormalDist.evaluate(-d1) * (1 - 1/Q)
                - (1 + dividendDiscount * cumNormalDist.derivative(-d1)
                   / Math.sqrt(variance)) / Q;
            while (Math.abs(LHS - RHS)/payoff.getStrike() > tolerance) {
                Si = (payoff.getStrike() - RHS + bi * Si) / (1 + bi);
                forwardSi = Si * dividendDiscount / riskFreeDiscount;
                d1 = (Math.log(forwardSi/payoff.getStrike())+0.5*variance)
                    /Math.sqrt(variance);
                LHS = payoff.getStrike() - Si;
                double /*@Real*/ temp2 = BlackFormula.blackFormula(payoff.getOptionType(), payoff.getStrike(),
                    forwardSi, Math.sqrt(variance))*riskFreeDiscount;
                RHS = temp2 - (1 - dividendDiscount * cumNormalDist.evaluate(-d1)) * Si / Q;
                bi = -dividendDiscount * cumNormalDist.evaluate(-d1) * (1 - 1 / Q)
                    - (1 + dividendDiscount * cumNormalDist.derivative(-d1)
                       / Math.sqrt(variance)) / Q;
            }
            break;
          default:
            throw new ArithmeticException("unknown option type");
        }

        return Si;
    }

}