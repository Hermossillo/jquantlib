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
 Copyright (C) 2003 Neil Firth
 Copyright (C) 2002, 2003 Ferdinando Ametrano
 Copyright (C) 2002, 2003 Sadruddin Rejeb
 Copyright (C) 2000, 2001, 2002, 2003 RiskMap srl

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

package org.jquantlib.pricingengines.barrier;

import org.jquantlib.instruments.BarrierType;
import org.jquantlib.instruments.PlainVanillaPayoff;
import org.jquantlib.math.distributions.CumulativeNormalDistribution;
import org.jquantlib.processes.GeneralizedBlackScholesProcess;
import org.jquantlib.termstructures.Compounding;
import org.jquantlib.termstructures.InterestRate;
import org.jquantlib.time.Frequency;

/**
 * Pricing engine for barrier options using analytical formulae
 * <p>
 * The formulas are taken from "Option pricing formulas", E.G. Haug, McGraw-Hill, p.69 and following.
 * 
 * @category barrierengines
 * 
 * @author <Richard Gomes>
 */
@SuppressWarnings("PMD.TooManyMethods")
public class AnalyticBarrierEngine extends BarrierOptionEngine {

    // TODO: refactor messages
    private static final String BS_PROCESS_REQUIRED = "Black-Scholes process required";
    private static final String NON_PLAIN_PAYOFF_GIVEN = "non-plain payoff given";
    private static final String STRIKE_MUST_BE_POSITIVE = "strike must be positive";
    private static final String UNKNOWN_TYPE = "unknown type";


    //
    // private fields
    //

    private final CumulativeNormalDistribution f;

    // these fields are initialised every time calculate() is called
    private transient GeneralizedBlackScholesProcess process;
    private transient PlainVanillaPayoff payoff;


    //
    // public constructors
    //

    public AnalyticBarrierEngine() {
        this.f = new CumulativeNormalDistribution();
    }


    //
    // implements PricingEngine
    //

    @Override
    public void calculate() {
        assert getArguments().payoff instanceof PlainVanillaPayoff : NON_PLAIN_PAYOFF_GIVEN;
        this.payoff = (PlainVanillaPayoff)getArguments().payoff;
        assert payoff.strike()>0.0 : STRIKE_MUST_BE_POSITIVE;
        assert arguments.stochasticProcess instanceof GeneralizedBlackScholesProcess : BS_PROCESS_REQUIRED;
        this.process = (GeneralizedBlackScholesProcess)arguments.stochasticProcess;

        final double strike = payoff.strike();
        final BarrierType barrierType = arguments.barrierType;

        switch (payoff.optionType()) {
        case CALL:
            switch (barrierType) {
            case DownIn:
                if (strike >= barrier())
                    results.value = C(1,1) + E(1);
                else
                    results.value = A(1) - B(1) + D(1,1) + E(1);
                break;
            case  UpIn:
                if (strike >= barrier())
                    results.value = A(1) + E(-1);
                else
                    results.value = B(1) - C(-1,1) + D(-1,1) + E(-1);
                break;
            case  DownOut:
                if (strike >= barrier())
                    results.value = A(1) - C(1,1) + F(1);
                else
                    results.value = B(1) - D(1,1) + F(1);
                break;
            case  UpOut:
                if (strike >= barrier())
                    results.value = F(-1);
                else
                    results.value = A(1) - B(1) + C(-1,1) - D(-1,1) + F(-1);
                break;
            }
            break;
        case PUT:
            switch (barrierType) {
            case  DownIn:
                if (strike >= barrier())
                    results.value = B(-1) - C(1,-1) + D(1,-1) + E(1);
                else
                    results.value = A(-1) + E(1);
                break;
            case  UpIn:
                if (strike >= barrier())
                    results.value = A(-1) - B(-1) + D(-1,-1) + E(-1);
                else
                    results.value = C(-1,-1) + E(-1);
                break;
            case  DownOut:
                if (strike >= barrier())
                    results.value = A(-1) - B(-1) + C(1,-1) - D(1,-1) + F(1);
                else
                    results.value = F(1);
                break;
            case  UpOut:
                if (strike >= barrier())
                    results.value = B(-1) - D(-1,-1) + F(-1);
                else
                    results.value = A(-1) - C(-1,-1) + F(-1);
                break;
            }
            break;
        default:
            throw new AssertionError(UNKNOWN_TYPE);
        }

    }


    //
    // private methods
    //

    private double  underlying()  {
        return this.process.initialValues(). first();
    }

    private double strike()  {
        return this.payoff.strike();
    }

    private double /*@Time*/  residualTime()  {
        return this.process.getTime(arguments.exercise.lastDate());
    }

    private double /*@Volatility*/  volatility()  {
        return this.process.blackVolatility().getLink().blackVol(residualTime(), strike());
    }

    private double  stdDeviation()  {
        return volatility() * Math.sqrt(residualTime());
    }

    private double  barrier()  {
        return arguments.barrier;
    }

    private double  rebate()  {
        return arguments.rebate;
    }

    private double /*@Rate*/  riskFreeRate()  {
        final InterestRate rate =  this.process.riskFreeRate().getLink().zeroRate(residualTime(), Compounding.CONTINUOUS,
                Frequency.NO_FREQUENCY, false);
        return rate.rate();
    }

    private double /*@DiscountFactor*/  riskFreeDiscount()  {
        return this.process.riskFreeRate().getLink().discount(residualTime());
    }

    private double /*@Rate*/  dividendYield()  {
        final InterestRate yield = this.process.dividendYield().getLink().zeroRate(
                residualTime(), Compounding.CONTINUOUS, Frequency.NO_FREQUENCY, false);
        return yield.rate();
    }

    private double /*@DiscountFactor*/  dividendDiscount()  {
        return this.process.dividendYield().getLink().discount(residualTime());
    }

    private double /*@Rate*/  mu()  {
        final double /*@Volatility*/ vol = volatility();
        return (riskFreeRate() - dividendYield())/(vol * vol) - 0.5;
    }

    private double  muSigma()  {
        return (1 + mu()) * stdDeviation();
    }

    //TODO: consider change method name to lowercase
    @SuppressWarnings("PMD.MethodNamingConventions")
    private double  A(final double phi)  {
        final double x1 = Math.log(underlying()/strike())/stdDeviation() + muSigma();
        final double N1 = f.op(phi*x1);
        final double N2 = f.op(phi*(x1-stdDeviation()));
        return phi*(underlying() * dividendDiscount() * N1 - strike() * riskFreeDiscount() * N2);
    }

    //TODO: consider change method name to lowercase
    @SuppressWarnings("PMD.MethodNamingConventions")
    private double  B(final double phi)  {
        final double x2 = Math.log(underlying()/barrier())/stdDeviation() + muSigma();
        final double N1 = f.op(phi*x2);
        final double N2 = f.op(phi*(x2-stdDeviation()));
        return phi*(underlying() * dividendDiscount() * N1 - strike() * riskFreeDiscount() * N2);
    }

    //TODO: consider change method name to lowercase
    @SuppressWarnings("PMD.MethodNamingConventions")
    private double  C(final double eta, final double phi)  {
        final double HS = barrier()/underlying();
        final double powHS0 = Math.pow(HS, 2 * mu());
        final double powHS1 = powHS0 * HS * HS;
        final double y1 = Math.log(barrier()*HS/strike())/stdDeviation() + muSigma();
        final double N1 = f.op(eta*y1);
        final double N2 = f.op(eta*(y1-stdDeviation()));
        return phi*(underlying() * dividendDiscount() * powHS1 * N1 - strike() * riskFreeDiscount() * powHS0 * N2);
    }

    //TODO: consider change method name to lowercase
    @SuppressWarnings("PMD.MethodNamingConventions")
    private double  D(final double eta, final double phi)  {
        final double HS = barrier()/underlying();
        final double powHS0 = Math.pow(HS, 2 * mu());
        final double powHS1 = powHS0 * HS * HS;
        final double y2 = Math.log(barrier()/underlying())/stdDeviation() + muSigma();
        final double N1 = f.op(eta*y2);
        final double N2 = f.op(eta*(y2-stdDeviation()));
        return phi*(underlying() * dividendDiscount() * powHS1 * N1 - strike() * riskFreeDiscount() * powHS0 * N2);
    }

    //TODO: consider change method name to lowercase
    @SuppressWarnings("PMD.MethodNamingConventions")
    private double  E(final double eta)  {
        if (rebate() > 0) {
            final double powHS0 = Math.pow(barrier()/underlying(), 2 * mu());
            final double x2 = Math.log(underlying()/barrier())/stdDeviation() + muSigma();
            final double y2 = Math.log(barrier()/underlying())/stdDeviation() + muSigma();
            final double N1 = f.op(eta*(x2 - stdDeviation()));
            final double N2 = f.op(eta*(y2 - stdDeviation()));
            return rebate() * riskFreeDiscount() * (N1 - powHS0 * N2);
        } else
            return 0.0;
    }

    //TODO: consider change method name to lowercase
    @SuppressWarnings("PMD.MethodNamingConventions")
    private double  F(final double eta)  {
        if (rebate() > 0) {
            final double /*@Rate*/ m = mu();
            final double /*@Volatility*/ vol = volatility();
            final double lambda = Math.sqrt(m*m + 2.0*riskFreeRate()/(vol * vol));
            final double HS = barrier()/underlying();
            final double powHSplus = Math.pow(HS, m + lambda);
            final double powHSminus = Math.pow(HS, m - lambda);

            final double sigmaSqrtT = stdDeviation();
            final double z = Math.log(barrier()/underlying())/sigmaSqrtT + lambda*sigmaSqrtT;

            final double N1 = f.op(eta * z);
            final double N2 = f.op(eta * (z - 2.0 * lambda * sigmaSqrtT));
            return rebate() * (powHSplus * N1 + powHSminus * N2);
        } else
            return 0.0;
    }

}
