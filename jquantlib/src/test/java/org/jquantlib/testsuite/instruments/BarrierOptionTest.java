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

package org.jquantlib.testsuite.instruments;

import junit.framework.TestCase;

import org.jquantlib.Configuration;
import org.jquantlib.Settings;
import org.jquantlib.daycounters.Actual360;
import org.jquantlib.daycounters.DayCounter;
import org.jquantlib.exercise.EuropeanExercise;
import org.jquantlib.exercise.Exercise;
import org.jquantlib.instruments.BarrierOption;
import org.jquantlib.instruments.BarrierType;
import org.jquantlib.instruments.Option;
import org.jquantlib.instruments.PlainVanillaPayoff;
import org.jquantlib.instruments.StrikedTypePayoff;
import org.jquantlib.pricingengines.PricingEngine;
import org.jquantlib.pricingengines.barrier.AnalyticBarrierOptionEngine;
import org.jquantlib.processes.BlackScholesMertonProcess;
import org.jquantlib.processes.StochasticProcess;
import org.jquantlib.quotes.Handle;
import org.jquantlib.quotes.Quote;
import org.jquantlib.quotes.SimpleQuote;
import org.jquantlib.termstructures.BlackVolTermStructure;
import org.jquantlib.termstructures.YieldTermStructure;
import org.jquantlib.testsuite.util.Utilities;
import org.jquantlib.time.Period;
import org.jquantlib.util.Date;
import org.jquantlib.util.DateFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BarrierOptionTest {

	private final static Logger logger = LoggerFactory.getLogger(BarrierOptionTest.class);

    private final Settings settings;
    private final Date today;      
    

    public BarrierOptionTest() {
        logger.info("\n\n::::: "+this.getClass().getSimpleName()+" :::::");
        this.settings = Configuration.getSystemConfiguration(null).getGlobalSettings();
        this.today = settings.getEvaluationDate();      
    }

    
    @Test
	public void testHaugValues() {

	    logger.info("Testing barrier options against Haug's values...");

	    NewBarrierOptionData values[] = {
	        /* The data below are from
	          "Option pricing formulas", E.G. Haug, McGraw-Hill 1998 pag. 72
	        */
	        //     barrierType, barrier, rebate,         type, strike,     s,    q,    r,    t,    v,  result, tol
	        new NewBarrierOptionData( BarrierType.DownOut,    95.0,    3.0, Option.Type.CALL,     90, 100.0, 0.04, 0.08, 0.50, 0.25,  9.0246, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.DownOut,    95.0,    3.0, Option.Type.CALL,    100, 100.0, 0.04, 0.08, 0.50, 0.25,  6.7924, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.DownOut,    95.0,    3.0, Option.Type.CALL,    110, 100.0, 0.04, 0.08, 0.50, 0.25,  4.8759, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.DownOut,   100.0,    3.0, Option.Type.CALL,     90, 100.0, 0.04, 0.08, 0.50, 0.25,  3.0000, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.DownOut,   100.0,    3.0, Option.Type.CALL,    100, 100.0, 0.04, 0.08, 0.50, 0.25,  3.0000, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.DownOut,   100.0,    3.0, Option.Type.CALL,    110, 100.0, 0.04, 0.08, 0.50, 0.25,  3.0000, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.UpOut,     105.0,    3.0, Option.Type.CALL,     90, 100.0, 0.04, 0.08, 0.50, 0.25,  2.6789, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.UpOut,     105.0,    3.0, Option.Type.CALL,    100, 100.0, 0.04, 0.08, 0.50, 0.25,  2.3580, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.UpOut,     105.0,    3.0, Option.Type.CALL,    110, 100.0, 0.04, 0.08, 0.50, 0.25,  2.3453, 1.0e-4),

	        new NewBarrierOptionData( BarrierType.DownIn,     95.0,    3.0, Option.Type.CALL,    90, 100.0, 0.04, 0.08, 0.50, 0.25,  7.7627, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.DownIn,     95.0,    3.0, Option.Type.CALL,   100, 100.0, 0.04, 0.08, 0.50, 0.25,  4.0109, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.DownIn,     95.0,    3.0, Option.Type.CALL,   110, 100.0, 0.04, 0.08, 0.50, 0.25,  2.0576, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.DownIn,    100.0,    3.0, Option.Type.CALL,    90, 100.0, 0.04, 0.08, 0.50, 0.25, 13.8333, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.DownIn,    100.0,    3.0, Option.Type.CALL,   100, 100.0, 0.04, 0.08, 0.50, 0.25,  7.8494, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.DownIn,    100.0,    3.0, Option.Type.CALL,   110, 100.0, 0.04, 0.08, 0.50, 0.25,  3.9795, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.UpIn,      105.0,    3.0, Option.Type.CALL,    90, 100.0, 0.04, 0.08, 0.50, 0.25, 14.1112, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.UpIn,      105.0,    3.0, Option.Type.CALL,   100, 100.0, 0.04, 0.08, 0.50, 0.25,  8.4482, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.UpIn,      105.0,    3.0, Option.Type.CALL,   110, 100.0, 0.04, 0.08, 0.50, 0.25,  4.5910, 1.0e-4),

	        new NewBarrierOptionData( BarrierType.DownOut,    95.0,    3.0, Option.Type.CALL,    90, 100.0, 0.04, 0.08, 0.50, 0.30,  8.8334, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.DownOut,    95.0,    3.0, Option.Type.CALL,   100, 100.0, 0.04, 0.08, 0.50, 0.30,  7.0285, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.DownOut,    95.0,    3.0, Option.Type.CALL,   110, 100.0, 0.04, 0.08, 0.50, 0.30,  5.4137, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.DownOut,   100.0,    3.0, Option.Type.CALL,    90, 100.0, 0.04, 0.08, 0.50, 0.30,  3.0000, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.DownOut,   100.0,    3.0, Option.Type.CALL,   100, 100.0, 0.04, 0.08, 0.50, 0.30,  3.0000, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.DownOut,   100.0,    3.0, Option.Type.CALL,   110, 100.0, 0.04, 0.08, 0.50, 0.30,  3.0000, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.UpOut,     105.0,    3.0, Option.Type.CALL,    90, 100.0, 0.04, 0.08, 0.50, 0.30,  2.6341, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.UpOut,     105.0,    3.0, Option.Type.CALL,   100, 100.0, 0.04, 0.08, 0.50, 0.30,  2.4389, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.UpOut,     105.0,    3.0, Option.Type.CALL,   110, 100.0, 0.04, 0.08, 0.50, 0.30,  2.4315, 1.0e-4),

	        new NewBarrierOptionData( BarrierType.DownIn,     95.0,    3.0, Option.Type.CALL,    90, 100.0, 0.04, 0.08, 0.50, 0.30,  9.0093, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.DownIn,     95.0,    3.0, Option.Type.CALL,   100, 100.0, 0.04, 0.08, 0.50, 0.30,  5.1370, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.DownIn,     95.0,    3.0, Option.Type.CALL,   110, 100.0, 0.04, 0.08, 0.50, 0.30,  2.8517, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.DownIn,    100.0,    3.0, Option.Type.CALL,    90, 100.0, 0.04, 0.08, 0.50, 0.30, 14.8816, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.DownIn,    100.0,    3.0, Option.Type.CALL,   100, 100.0, 0.04, 0.08, 0.50, 0.30,  9.2045, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.DownIn,    100.0,    3.0, Option.Type.CALL,   110, 100.0, 0.04, 0.08, 0.50, 0.30,  5.3043, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.UpIn,      105.0,    3.0, Option.Type.CALL,    90, 100.0, 0.04, 0.08, 0.50, 0.30, 15.2098, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.UpIn,      105.0,    3.0, Option.Type.CALL,   100, 100.0, 0.04, 0.08, 0.50, 0.30,  9.7278, 1.0e-4),
	        new NewBarrierOptionData( BarrierType.UpIn,      105.0,    3.0, Option.Type.CALL,   110, 100.0, 0.04, 0.08, 0.50, 0.30,  5.8350, 1.0e-4),



	        new NewBarrierOptionData( BarrierType.DownOut,    95.0,    3.0,  Option.Type.PUT,    90, 100.0, 0.04, 0.08, 0.50, 0.25,  2.2798, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.DownOut,    95.0,    3.0,  Option.Type.PUT,   100, 100.0, 0.04, 0.08, 0.50, 0.25,  2.2947, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.DownOut,    95.0,    3.0,  Option.Type.PUT,   110, 100.0, 0.04, 0.08, 0.50, 0.25,  2.6252, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.DownOut,   100.0,    3.0,  Option.Type.PUT,    90, 100.0, 0.04, 0.08, 0.50, 0.25,  3.0000, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.DownOut,   100.0,    3.0,  Option.Type.PUT,   100, 100.0, 0.04, 0.08, 0.50, 0.25,  3.0000, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.DownOut,   100.0,    3.0,  Option.Type.PUT,   110, 100.0, 0.04, 0.08, 0.50, 0.25,  3.0000, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.UpOut,     105.0,    3.0,  Option.Type.PUT,    90, 100.0, 0.04, 0.08, 0.50, 0.25,  3.7760, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.UpOut,     105.0,    3.0,  Option.Type.PUT,   100, 100.0, 0.04, 0.08, 0.50, 0.25,  5.4932, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.UpOut,     105.0,    3.0,  Option.Type.PUT,   110, 100.0, 0.04, 0.08, 0.50, 0.25,  7.5187, 1.0e-4 ),

	        new NewBarrierOptionData( BarrierType.DownIn,     95.0,    3.0,  Option.Type.PUT,    90, 100.0, 0.04, 0.08, 0.50, 0.25,  2.9586, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.DownIn,     95.0,    3.0,  Option.Type.PUT,   100, 100.0, 0.04, 0.08, 0.50, 0.25,  6.5677, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.DownIn,     95.0,    3.0,  Option.Type.PUT,   110, 100.0, 0.04, 0.08, 0.50, 0.25, 11.9752, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.DownIn,    100.0,    3.0,  Option.Type.PUT,    90, 100.0, 0.04, 0.08, 0.50, 0.25,  2.2845, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.DownIn,    100.0,    3.0,  Option.Type.PUT,   100, 100.0, 0.04, 0.08, 0.50, 0.25,  5.9085, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.DownIn,    100.0,    3.0,  Option.Type.PUT,   110, 100.0, 0.04, 0.08, 0.50, 0.25, 11.6465, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.UpIn,      105.0,    3.0,  Option.Type.PUT,    90, 100.0, 0.04, 0.08, 0.50, 0.25,  1.4653, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.UpIn,      105.0,    3.0,  Option.Type.PUT,   100, 100.0, 0.04, 0.08, 0.50, 0.25,  3.3721, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.UpIn,      105.0,    3.0,  Option.Type.PUT,   110, 100.0, 0.04, 0.08, 0.50, 0.25,  7.0846, 1.0e-4 ),

	        new NewBarrierOptionData( BarrierType.DownOut,    95.0,    3.0,  Option.Type.PUT,    90, 100.0, 0.04, 0.08, 0.50, 0.30,  2.4170, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.DownOut,    95.0,    3.0,  Option.Type.PUT,   100, 100.0, 0.04, 0.08, 0.50, 0.30,  2.4258, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.DownOut,    95.0,    3.0,  Option.Type.PUT,   110, 100.0, 0.04, 0.08, 0.50, 0.30,  2.6246, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.DownOut,   100.0,    3.0,  Option.Type.PUT,    90, 100.0, 0.04, 0.08, 0.50, 0.30,  3.0000, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.DownOut,   100.0,    3.0,  Option.Type.PUT,   100, 100.0, 0.04, 0.08, 0.50, 0.30,  3.0000, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.DownOut,   100.0,    3.0,  Option.Type.PUT,   110, 100.0, 0.04, 0.08, 0.50, 0.30,  3.0000, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.UpOut,     105.0,    3.0,  Option.Type.PUT,    90, 100.0, 0.04, 0.08, 0.50, 0.30,  4.2293, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.UpOut,     105.0,    3.0,  Option.Type.PUT,   100, 100.0, 0.04, 0.08, 0.50, 0.30,  5.8032, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.UpOut,     105.0,    3.0,  Option.Type.PUT,   110, 100.0, 0.04, 0.08, 0.50, 0.30,  7.5649, 1.0e-4 ),

	        new NewBarrierOptionData( BarrierType.DownIn,     95.0,    3.0,  Option.Type.PUT,    90, 100.0, 0.04, 0.08, 0.50, 0.30,  3.8769, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.DownIn,     95.0,    3.0,  Option.Type.PUT,   100, 100.0, 0.04, 0.08, 0.50, 0.30,  7.7989, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.DownIn,     95.0,    3.0,  Option.Type.PUT,   110, 100.0, 0.04, 0.08, 0.50, 0.30, 13.3078, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.DownIn,    100.0,    3.0,  Option.Type.PUT,    90, 100.0, 0.04, 0.08, 0.50, 0.30,  3.3328, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.DownIn,    100.0,    3.0,  Option.Type.PUT,   100, 100.0, 0.04, 0.08, 0.50, 0.30,  7.2636, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.DownIn,    100.0,    3.0,  Option.Type.PUT,   110, 100.0, 0.04, 0.08, 0.50, 0.30, 12.9713, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.UpIn,      105.0,    3.0,  Option.Type.PUT,    90, 100.0, 0.04, 0.08, 0.50, 0.30,  2.0658, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.UpIn,      105.0,    3.0,  Option.Type.PUT,   100, 100.0, 0.04, 0.08, 0.50, 0.30,  4.4226, 1.0e-4 ),
	        new NewBarrierOptionData( BarrierType.UpIn,      105.0,    3.0,  Option.Type.PUT,   110, 100.0, 0.04, 0.08, 0.50, 0.30,  8.3686, 1.0e-4 )

	        /*
	            Data from "Going to Extreme: Correcting Simulation Bias in Exotic Option Valuation"
	            D.R. Beaglehole, P.H. Dybvig and G. Zhou
	            Financial Analysts Journal; Jan / Feb 1997; 53, 1
	        */
	        //    barrierType, barrier, rebate,         type, strike,     s,    q,    r,    t,    v,  result, tol
	        // { BarrierType.DownOut,    45.0,    0.0,  Option.Type.PUT,     50,  50.0,-0.05, 0.10, 0.25, 0.50,   4.032, 1.0e-3 },
	        // { BarrierType.DownOut,    45.0,    0.0,  Option.Type.PUT,     50,  50.0,-0.05, 0.10, 1.00, 0.50,   5.477, 1.0e-3 }

	    };


	    DayCounter dc = Actual360.getDayCounter();
	    SimpleQuote spot = new SimpleQuote(0.0);
	    SimpleQuote qRate = new SimpleQuote(0.0);
	    YieldTermStructure qTS = Utilities.flatRate(today, new Handle<Quote>(qRate), dc);
	    SimpleQuote rRate = new SimpleQuote(0.0);
	    YieldTermStructure rTS = Utilities.flatRate(today, new Handle<Quote>(rRate), dc);
	    SimpleQuote vol = new SimpleQuote(0.0);
	    BlackVolTermStructure volTS = Utilities.flatVol(today, new Handle<Quote>(vol), dc);

	    
	    for (int i=0; i<values.length; i++) {
	        Date exDate = today.getDateAfter( timeToDays(values[i].t) );

	        Exercise exercise = new EuropeanExercise(exDate);

	        spot.setValue(values[i].s);
	        qRate.setValue(values[i].q);
	        rRate.setValue(values[i].r);
	        vol.setValue(values[i].v);

	        StrikedTypePayoff payoff = new PlainVanillaPayoff(values[i].type, values[i].strike);

	        StochasticProcess stochProcess = new 
	            BlackScholesMertonProcess(new Handle<Quote>(spot),
	                                      new Handle<YieldTermStructure>(qTS),
	                                      new Handle<YieldTermStructure>(rTS),
	                                      new Handle<BlackVolTermStructure>(volTS));

	        BarrierOption barrierOption = new 
	            BarrierOption(values[i].barrierType,
	                          values[i].barrier,
	                          values[i].rebate,
	                          stochProcess,
	                          payoff,
	                          exercise,
	                          new AnalyticBarrierOptionEngine());
	        
	        double calculated = barrierOption.getNPV();
	        double expected = values[i].result;
	        double error = Math.abs(calculated-expected);
	        if (error>values[i].tol) {
	            REPORT_FAILURE("value", values[i].barrierType, values[i].barrier,
	                           values[i].rebate, payoff, exercise, values[i].s,
	                           values[i].q, values[i].r, today, values[i].v,
	                           expected, calculated, error, values[i].tol);
	        }

	    }
	}

	private int timeToDays(/*@Time*/ double t) {
	    return (int) (t*360+0.5);
	}
	
	//TODO: fix barrier option - getNPV() gets 0
    @Ignore
    @Test
    public void testBabsiriValues(){
        logger.info("Testing barrier options against Babsiri's values...");
        /*
        Data from
        "Simulating Path-Dependent Options: A New Approach"
          - M. El Babsiri and G. Noel
            Journal of Derivatives; Winter 1998; 6, 2
        */
        
    BarrierOptionData values[] = {
        new BarrierOptionData( BarrierType.DownIn,   0.10,   100,  90,   0.07187,  0.0),
        new BarrierOptionData( BarrierType.DownIn,   0.15,   100,  90,   0.60638,  0.0),
        new BarrierOptionData( BarrierType.DownIn,   0.20,   100,  90,   1.64005,  0.0),
        new BarrierOptionData( BarrierType.DownIn,   0.25,   100,  90,   2.98495,  0.0),
        new BarrierOptionData( BarrierType.DownIn,   0.30,   100,  90,   4.50952,  0.0),
        new BarrierOptionData( BarrierType.UpIn,     0.10,   100,  110,  4.79148,  0.0),
        new BarrierOptionData( BarrierType.UpIn,     0.15,   100,  110,   7.08268,  0.0 ),
        new BarrierOptionData( BarrierType.UpIn,     0.20,   100,  110,   9.11008,  0.0 ),
        new BarrierOptionData( BarrierType.UpIn,     0.25,   100,  110,  11.06148,  0.0 ),
        new BarrierOptionData( BarrierType.UpIn,     0.30,   100,  110,  12.98351,  0.0 )
    };
        

        double underlyingPrice = 100.0;
        double rebate = 0.0;
        double r = 0.05;
        double q = 0.02;

        DayCounter dc = Actual360.getDayCounter();
        Date today = DateFactory.getFactory().getTodaysDate();
        Handle<Quote> underlying = new Handle<Quote>(new SimpleQuote(underlyingPrice));
        
        Handle<Quote> qH_SME = new Handle<Quote>(new SimpleQuote(q));
        YieldTermStructure qTS = Utilities.flatRate(today, qH_SME, dc);

        Handle<Quote> rH_SME = new Handle<Quote>(new SimpleQuote(r));
        YieldTermStructure rTS = Utilities.flatRate(today, rH_SME, dc);
        
        Handle<SimpleQuote> volatility = new Handle<SimpleQuote>(new SimpleQuote(0.10));
        BlackVolTermStructure volTS = Utilities.flatVol(today, volatility, dc);
        
        PricingEngine engine = new AnalyticBarrierOptionEngine();
        
        Date exDate = today.increment(Period.ONE_YEAR_FORWARD);
        
        Exercise exercise = new EuropeanExercise(exDate);
        
        for(int i = 0; i<values.length; i++){
            volatility.getLink().setValue(values[i].volatility);
            StrikedTypePayoff callPayoff = new PlainVanillaPayoff(Option.Type.CALL, values[i].strike);
            StochasticProcess stochProcess = new BlackScholesMertonProcess(new Handle<Quote>(underlying),
                    new Handle<YieldTermStructure>(qTS), new Handle<YieldTermStructure>(rTS), 
                            new Handle<BlackVolTermStructure>(volTS));
            
           BarrierOption barrierCallOption = new BarrierOption(values[i].barrierType,
                   values[i].barrier, rebate, stochProcess, callPayoff, exercise, engine);
           
           double calculated = barrierCallOption.getNPV();
           double expected = values[i].callValue;
           double error = Math.abs(calculated - expected);
           double maxErrorAllowed = 1.0e-5;
           if(error>maxErrorAllowed){
               REPORT_FAILURE("value", values[i].barrierType, values[i].barrier,
                       rebate, callPayoff, exercise, underlyingPrice,
                       q, r, today, values[i].volatility,
                       expected, calculated, error, maxErrorAllowed);
           }
           }
    }
        
    //TODO: fix barrier option - getNPV() gets 0
    @Ignore
    @Test
    public void testBeagleholeValues() {

        logger.info("Testing barrier options against Beaglehole's values...");
        /*
            Data from
            "Going to Extreme: Correcting Simulation Bias in Exotic
             Option Valuation"
              - D.R. Beaglehole, P.H. Dybvig and G. Zhou
                Financial Analysts Journal; Jan / Feb 1997; 53, 1
        */
        BarrierOptionData values[] = {
             new BarrierOptionData(BarrierType.DownOut, 0.50,   50,      45,  5.477,  0.0)
        };
        
        double underlyingPrice = 50.0;
        double rebate = 0.0;
        double r = Math.log(1.1);
        double q = 0.00;
        
        DayCounter dc = Actual360.getDayCounter();
        Date today = DateFactory.getFactory().getTodaysDate();
        Handle<Quote> underlying = new Handle<Quote>(new SimpleQuote(underlyingPrice));
        
        Handle<Quote> qH_SME = new Handle<Quote>(new SimpleQuote(q));
        YieldTermStructure qTS = Utilities.flatRate(today, qH_SME, dc);

        Handle<Quote> rH_SME = new Handle<Quote>(new SimpleQuote(r));
        YieldTermStructure rTS = Utilities.flatRate(today, rH_SME, dc);
        
        Handle<SimpleQuote> volatility = new Handle<SimpleQuote>(new SimpleQuote(0.10));
        BlackVolTermStructure volTS = Utilities.flatVol(today, volatility, dc);
        
        PricingEngine engine = new AnalyticBarrierOptionEngine();
        
        Date exDate = today.increment(Period.ONE_YEAR_FORWARD);
        
        Exercise exercise = new EuropeanExercise(exDate);
        
        for(int i = 0; i<values.length; i++){
            volatility.getLink().setValue(values[i].volatility);
            StrikedTypePayoff callPayoff = new PlainVanillaPayoff(Option.Type.CALL, values[i].strike);
            StochasticProcess stochProcess = new BlackScholesMertonProcess(new Handle<Quote>(underlying),
                    new Handle<YieldTermStructure>(qTS), new Handle<YieldTermStructure>(rTS), 
                            new Handle<BlackVolTermStructure>(volTS));
            
           BarrierOption barrierCallOption = new BarrierOption(values[i].barrierType,
                   values[i].barrier, rebate, stochProcess, callPayoff, exercise, engine);
           
           double calculated = barrierCallOption.getNPV();
           double expected = values[i].callValue;
           double error = Math.abs(calculated - expected);
           double maxErrorAllowed = 1.0e-5;
           if(error>maxErrorAllowed){
               REPORT_FAILURE("value", values[i].barrierType, values[i].barrier,
                       rebate, callPayoff, exercise, underlyingPrice,
                       q, r, today, values[i].volatility,
                       expected, calculated, error, maxErrorAllowed);
           }
           }
        
        double maxMcRelativeErrorAllowed = 0.01;
        int timeSteps = 1;
        boolean brownianBridge = true;
        boolean antitheticVariate = false;
        boolean controlVariate = false;
        int requiredSamples = 131071; //2^17-1
        double requiredTolerance;
        int maxSamples = 1048575; // 2^20-1
        boolean isBiased = false;
        double seed = 10;
        
        //TODO: MC Barrier engine not implemented yet.
        /*
        boost::shared_ptr<PricingEngine> mcEngine(
                new MCBarrierEngine<LowDiscrepancy>(timeSteps, brownianBridge,
                                                antitheticVariate, controlVariate,
                                                requiredSamples, requiredTolerance,
                                                maxSamples, isBiased, seed));

            barrierCallOption.setPricingEngine(mcEngine);
            calculated = barrierCallOption.NPV();
            error = std::fabs(calculated-expected)/expected;
            if (error>maxMcRelativeErrorAllowed) {
                REPORT_FAILURE("value", values[i].type, values[i].barrier,
                               rebate, callPayoff, exercise, underlyingPrice,
                               q, r, today, values[i].volatility,
                               expected, calculated, error,
                               maxMcRelativeErrorAllowed);
            }
    */
    }
	
	


    private void REPORT_FAILURE(String greekName, BarrierType barrierType, 
            double barrier, double rebate, StrikedTypePayoff payoff, 
            Exercise exercise, double s, double q, double r, Date today, 
            double v, double expected, double calculated, 
            double error, double tolerance) {
        TestCase.fail("\n" + barrierType + " " + exercise  
                + payoff.optionType() + " option with " 
                + payoff.getClass().getSimpleName() + " payoff:\n"
                + "    underlying value: " +  s + "\n" 
                + "    strike:           " + payoff.strike() + "\n" 
                + "    barrier:          " + barrier + "\n" 
                + "    rebate:           " + rebate + "\n" 
                + "    dividend yield:   " + q + "\n" 
                + "    risk-free rate:   " + r + "\n" 
                + "    reference date:   " + today + "\n" 
                + "    maturity:         " + exercise.lastDate() + "\n" 
                + "    volatility:       " + v  + "\n\n" 
                + "    expected   " + greekName + ": " + expected + "\n" 
                + "    calculated " + greekName + ": " + calculated + "\n"
                + "    error:            " + error + "\n" 
                + "    tolerance:        " + tolerance);
    }
    
    
    private static class NewBarrierOptionData {
        
        private BarrierType barrierType;
        private double barrier;
        private double rebate;
        private Option.Type type;
        private double strike;
        private double s;        // spot
        private double q;        // dividend
        private double r;        // risk-free rate
        private double t;        // time to maturity
        private double v;  // volatility
        private double result;   // result
        private double tol;      // tolerance

        NewBarrierOptionData(   BarrierType barrierType,
                                double barrier,
                                double rebate,
                                Option.Type type,
                                double strike,
                                double s,        // spot
                                double q,        // dividend
                                double r,        // risk-free rate
                                double t,        // time to maturity
                                double v,  // volatility
                                double result,   // result
                                double tol      // tolerance
        ) {
            this.barrierType = barrierType;
            this.barrier = barrier;
            this.rebate = rebate;
            this.type = type;
            this.strike = strike;
            this.s = s;
            this.q = q;
            this.r = r;
            this.t = t;
            this.v = v;
            this.result = result;
            this.tol = tol;
        }
    }
    
    private static class BarrierOptionData {

        BarrierOptionData(      BarrierType barrierType,
                                double volatility,   
                                double strike,        
                                double barrier,        
                                double callValue,        
                                double putValue
        ) {
            this.barrierType = barrierType;
            this.volatility = volatility;
            this.strike = strike;
            this.barrier = barrier;
            this.callValue = callValue;
            this.putValue = putValue;
        }
        BarrierType barrierType;
        double volatility;
        double strike;
        double barrier;
        double callValue;
        double putValue;
    };

}
