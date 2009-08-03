

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
 Copyright (C) 2005 Gary Kennedy

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

package org.jquantlib.pricingengines.asian;


import org.jquantlib.daycounters.DayCounter;
import org.jquantlib.exercise.Exercise;
import org.jquantlib.instruments.AverageType;
import org.jquantlib.instruments.PlainVanillaPayoff;
import org.jquantlib.pricingengines.BlackCalculator;
import org.jquantlib.processes.GeneralizedBlackScholesProcess;
import org.jquantlib.termstructures.Compounding;
import org.jquantlib.time.Frequency;
import org.jquantlib.util.Date;


/**
 * @author <Richard Gomes>
 */
//TODO class comments
//TODO add reference to original paper, clewlow strickland
public class AnalyticContinuousGeometricAveragePriceasianEngine extends ContinuousAveragingAsianOptionEngine{

    //
    // implements PricingEngine
    //

    @Override
    public void calculate() /*@ReadOnly*/ {
        assert arguments.averageType==AverageType.Geometric : "not a geometric average option";
        assert arguments.exercise.type()==Exercise.Type.EUROPEAN : "not an European Option";
        final Date exercise = arguments.exercise.lastDate();
        assert arguments.payoff instanceof PlainVanillaPayoff : "non-plain payoff given";
        final PlainVanillaPayoff payoff = (PlainVanillaPayoff)arguments.payoff;
        assert arguments.stochasticProcess instanceof GeneralizedBlackScholesProcess : "Black-Scholes process required";

        final GeneralizedBlackScholesProcess process = (GeneralizedBlackScholesProcess)arguments.stochasticProcess;
        /*@Volatility*/ final double volatility = process.blackVolatility().getLink().blackVol(exercise, payoff.strike());
        /*@Real*/ final double variance = process.blackVolatility().getLink().blackVariance(exercise, payoff.strike());
        /*@DiscountFactor*/ final double  riskFreeDiscount = process.riskFreeRate().getLink().discount(exercise);
        final DayCounter rfdc  = process.riskFreeRate().getLink().dayCounter();
        final DayCounter divdc = process.dividendYield().getLink().dayCounter();
        final DayCounter voldc = process.blackVolatility().getLink().dayCounter();

        /*@Spread*/ final double dividendYield = 0.5 * (
                process.riskFreeRate().getLink().zeroRate(
                        exercise,
                        rfdc,
                        Compounding.CONTINUOUS,
                        Frequency.NO_FREQUENCY).rate() + process.dividendYield().getLink().zeroRate(
                                exercise,
                                divdc,
                                Compounding.CONTINUOUS,
                                Frequency.NO_FREQUENCY).rate() + volatility*volatility/6.0);

        /*@Time*/ final double t_q = divdc.yearFraction(
                process.dividendYield().getLink().referenceDate(), exercise);
        /*@DiscountFactor*/ final double dividendDiscount = Math.exp(-dividendYield*t_q);
        /*@Real*/ final double spot = process.stateVariable().getLink().evaluate();
        /*@Real*/ final double forward = spot * dividendDiscount / riskFreeDiscount;

        final BlackCalculator black = new BlackCalculator(payoff, forward, Math.sqrt(variance/3.0),riskFreeDiscount);
        results.value = black.value();
        results.delta = black.delta(spot);
        results.gamma = black.gamma(spot);
        results.dividendRho = black.dividendRho(t_q)/2.0;

        /*@Time*/ final double t_r = rfdc.yearFraction(process.riskFreeRate().getLink().referenceDate(),
                arguments.exercise.lastDate());
        results.rho = black.rho(t_r) + 0.5 * black.dividendRho(t_q);

        /*@Time*/ final double t_v = voldc.yearFraction(
                process.blackVolatility().getLink().referenceDate(),
                arguments.exercise.lastDate());
        results.vega = black.vega(t_v)/Math.sqrt(3.0) +
        black.dividendRho(t_q)*volatility/6.0;
        results.theta = black.theta(spot, t_v);
        //results_.theta = Null<Real>();
    }

}
