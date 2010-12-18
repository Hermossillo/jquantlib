/*
 Copyright (C) 2008 Daniel Kong

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
package org.jquantlib.instruments.bonds;

import java.util.ArrayList;
import java.util.List;

import org.jquantlib.QL;
import org.jquantlib.cashflow.Callability;
import org.jquantlib.cashflow.CashFlow;
import org.jquantlib.cashflow.Dividend;
import org.jquantlib.cashflow.Leg;
import org.jquantlib.daycounters.DayCounter;
import org.jquantlib.exercise.Exercise;
import org.jquantlib.instruments.CallabilitySchedule;
import org.jquantlib.instruments.DividendSchedule;
import org.jquantlib.instruments.Instrument;
import org.jquantlib.instruments.OneAssetOption;
import org.jquantlib.instruments.Option;
import org.jquantlib.instruments.PlainVanillaPayoff;
import org.jquantlib.instruments.Instrument.Results;
import org.jquantlib.instruments.OneAssetOption.Arguments;
import org.jquantlib.instruments.OneAssetOption.ArgumentsImpl;
import org.jquantlib.instruments.OneAssetOption.ResultsImpl;
import org.jquantlib.instruments.Option.Greeks;
import org.jquantlib.instruments.Option.MoreGreeks;
import org.jquantlib.instruments.Option.Type;
import org.jquantlib.lang.reflect.ReflectConstants;
import org.jquantlib.math.Constants;
import org.jquantlib.methods.lattices.BinomialTree;
import org.jquantlib.methods.lattices.Tree;
import org.jquantlib.pricingengines.GenericEngine;
import org.jquantlib.pricingengines.PricingEngine;
import org.jquantlib.quotes.Handle;
import org.jquantlib.quotes.Quote;
import org.jquantlib.time.Date;
import org.jquantlib.time.Schedule;

/**
 *
 * @author Daniel Kong
 * @author Zahid Hussain
 */
//TODO: Work in progress
public class ConvertibleBondOption extends OneAssetOption {

    private final ConvertibleBond bond_;
    private double conversionRatio_;
    private CallabilitySchedule callability_;
    private DividendSchedule  dividends_;
    private Handle<Quote> creditSpread_;
    private Leg cashflows_;
    private DayCounter dayCounter_;
    private Date issueDate_;
    private Schedule schedule_;
    private int settlementDays_;
    private double redemption_;

    public ConvertibleBondOption(final ConvertibleBond bond,
                                 final Exercise exercise,
                                 double conversionRatio,
                                 final DividendSchedule dividends,
                                 final CallabilitySchedule callability,
                                 final Handle<Quote> creditSpread,
                                 final Leg cashflows,
                                 final DayCounter dayCounter,
                                 final Schedule schedule,
                                 final Date issueDate,
                                 int  settlementDays,
                                 double redemption) {
        super(new PlainVanillaPayoff(Option.Type.Call,bond.faceAmount()/100.0 * redemption/conversionRatio), exercise);
        this.bond_ = bond;
        this.conversionRatio_ = conversionRatio;
        this.callability_ = callability;
        this.dividends_ = dividends;
        this.creditSpread_ = creditSpread;
        this.cashflows_ = cashflows;
        this.dayCounter_ = dayCounter;
        this.issueDate_ = issueDate;
        this.schedule_ = schedule;
        this.settlementDays_ = settlementDays;
        this.redemption_ = redemption;
    }

    public void setupArguments(PricingEngine.Arguments args) {

        super.setupArguments(args);
        
        QL.require(ConvertibleBondOption.ArgumentsImpl.class.isAssignableFrom(args.getClass()), ReflectConstants.WRONG_ARGUMENT_TYPE); // QA:[RG]::verified
        ConvertibleBondOption.ArgumentsImpl moreArgs = args instanceof ConvertibleBondOption.ArgumentsImpl 
                                                        ? (ConvertibleBondOption.ArgumentsImpl)args
                                                        : null;
        QL.require(moreArgs != null, "wrong argument type");

        moreArgs.conversionRatio = conversionRatio_;

        Date settlement = bond_.settlementDate().clone();

        int n = callability_.size();
        moreArgs.callabilityDates.clear();
        moreArgs.callabilityTypes.clear();
        moreArgs.callabilityPrices.clear();
        moreArgs.callabilityTriggers.clear();

        //Not required in Java:
        //moreArgs.callabilityDates.reserve(n);
        //moreArgs.callabilityTypes.reserve(n);
        //moreArgs.callabilityPrices.reserve(n);
        //moreArgs.callabilityTriggers.reserve(n);
        
        for (int i=0; i<n; i++) {
            if (!callability_.get(i).hasOccurred(settlement)) {
                
                moreArgs.callabilityTypes.add(callability_.get(i).type());
                moreArgs.callabilityDates.add(callability_.get(i).date());
                moreArgs.callabilityPrices.add(callability_.get(i).price().amount());
                
                if (callability_.get(i).price().type() == Callability.Price.Type.Clean ) {
                    int lastIdx = moreArgs.callabilityPrices.size()-1;
                    double d = moreArgs.callabilityPrices.get(lastIdx) +
                                bond_.accruedAmount(callability_.get(i).date());
                    moreArgs.callabilityPrices.set(lastIdx, d);
                }
                Object obj = callability_.get(i);
                SoftCallability softCall = obj instanceof SoftCallability ? (SoftCallability)obj : null;
                if (softCall != null)
                    moreArgs.callabilityTriggers.add(softCall.trigger());
                else
                    moreArgs.callabilityTriggers.add(Constants.NULL_REAL);
            }
        }

        final Leg cashflows = bond_.cashflows();

        moreArgs.couponDates.clear();
        moreArgs.couponAmounts.clear();
        for (int i=0; i<cashflows.size()-1; i++) {
            if (!cashflows.get(i).hasOccurred(settlement)) {
                moreArgs.couponDates.add(cashflows.get(i).date());
                moreArgs.couponAmounts.add(cashflows.get(i).amount());
            }
        }

        moreArgs.dividends.clear();
        moreArgs.dividendDates.clear();
        for (int i=0; i<dividends_.size(); i++) {
            if (!dividends_.get(i).hasOccurred(settlement)) {
                moreArgs.dividends.add(dividends_.get(i));
                moreArgs.dividendDates.add(dividends_.get(i).date());
            }
        }

        moreArgs.creditSpread = creditSpread_;
        moreArgs.issueDate = issueDate_.clone();
        moreArgs.settlementDate = settlement.clone();
        moreArgs.settlementDays = settlementDays_;
        moreArgs.redemption = redemption_;
    }

//    @Override
//    public void setupArguments(final PricingEngine.Arguments arguments) /* @ReadOnly */ {
//      super.setupArguments(arguments);
//
//      QL.require(ConvertibleBondOption.ArgumentsImpl.class.isAssignableFrom(arguments.getClass()), ReflectConstants.WRONG_ARGUMENT_TYPE); // QA:[RG]::verified
//      final ConvertibleBondOption.ArgumentsImpl moreArgs = (ConvertibleBondOption.ArgumentsImpl)arguments;
//        QL_REQUIRE(moreArgs != 0, "wrong argument type");
//
//      
//      moreArgs.conversionRatio = conversionRatio_;
//        final Date settlement = bond_.settlementDate();
//        final int n = callability_.size();
//        moreArgs.callabilityTimes.clear();
//        moreArgs.callabilityTypes.clear();
//        moreArgs.callabilityPrices.clear();
//        moreArgs.callabilityTriggers.clear();
//
//        for (int i=0; i<n; i++) {
//            if (!callability_.get(i).hasOccurred(settlement)) {
//                moreArgs.callabilityTypes.add(callability_.get(i).getType());
//                moreArgs.callabilityTimes.add(dayCounter_.yearFraction(settlement, callability_.get(i).date()));
//
//                double d = callability_.get(i).getPrice().amount();
//                if (callability_.get(i).getPrice().type() == Callability.Price.Type.Clean){
//                  d += bond_.accruedAmount(callability_.get(i).date());
//                }
//                moreArgs.callabilityPrices.add(d);
//                Object obj = callability_.get(i);
//                final SoftCallability softCall = obj instanceof SoftCallability ? (SoftCallability)obj : null;
//              if(softCall != null){
//                    moreArgs.callabilityTriggers.add(softCall.getTrigger());
//              }else{
//                  moreArgs.callabilityTriggers.add(0.0);
//              }
//            }
//        }
//
//        final List<CashFlow> cashFlows = bond_.cashflows();
//
//        moreArgs.couponTimes.clear();
//        moreArgs.couponAmounts.clear();
//        for (int i=0; i<cashFlows.size()-1; i++) {
//            if (!cashFlows.get(i).hasOccurred(settlement)) {
//                moreArgs.couponTimes.add(dayCounter_.yearFraction(settlement,cashFlows.get(i).date()));
//                moreArgs.couponAmounts.add(cashFlows.get(i).amount());
//            }
//        }
//
//        moreArgs.dividends.clear();
//        moreArgs.dividendTimes.clear();
//        for (int i=0; i<dividends_.size(); i++) {
//            if (!dividends_.get(i).hasOccurred(settlement)) {
//                moreArgs.dividends.add(dividends_.get(i));
//                moreArgs.dividendTimes.add(dayCounter_.yearFraction(settlement,dividends_.get(i).date()));
//            }
//        }
//
//        moreArgs.creditSpread = creditSpread_;
//        moreArgs.dayCounter = dayCounter_;
//        moreArgs.issueDate = issueDate_;
//        moreArgs.settlementDate = settlement;
//        moreArgs.settlementDays = settlementDays_;
//        moreArgs.redemption = redemption_;
//  }



    //
    // ????? inner interfaces
    //

    public interface Arguments extends OneAssetOption.Arguments { }

    public interface Results extends Instrument.Results, Option.Greeks, Option.MoreGreeks { }



    //
    // ????? inner classes
    //


    static public class ArgumentsImpl extends OneAssetOption.ArgumentsImpl implements Arguments {

        public double conversionRatio;
        public Handle<Quote> creditSpread;
        public DividendSchedule dividends;
        public List<Date> dividendDates;
        public List<Date> callabilityDates;
        public List<Callability.Type> callabilityTypes;
        public List<Double> callabilityPrices;
        public List<Double> callabilityTriggers;
        public List<Date> couponDates;
        public List<Double> couponAmounts;
        public Date issueDate;
        public Date settlementDate;
        public int settlementDays;
        public double redemption;

        public ArgumentsImpl() {
            conversionRatio = Constants.NULL_REAL;
            settlementDays = Constants.NULL_INTEGER;
            redemption = Constants.NULL_REAL;

            dividends = new DividendSchedule();
            dividendDates = new ArrayList<Date>();
            callabilityDates = new ArrayList<Date>();
            callabilityTypes = new ArrayList<Callability.Type>();
            callabilityPrices = new ArrayList<Double>();
            callabilityTriggers = new ArrayList<Double>();
            couponDates = new ArrayList<Date>();
            couponAmounts = new ArrayList<Double>();
        }

        public void validate() {

            QL.require(conversionRatio != Constants.NULL_REAL, "null conversion ratio");
            QL.require(conversionRatio > 0.0,
                   "positive conversion ratio required: "
                   + conversionRatio + " not allowed");

            QL.require(redemption != Constants.NULL_REAL, "null redemption");
            QL.require(redemption >= 0.0, "positive redemption required: "
                                        + redemption + " not allowed");

            QL.require(!settlementDate.isNull(), "null settlement date");

            QL.require(settlementDays != Constants.NULL_NATURAL, "null settlement days");

            QL.require(callabilityDates.size() == callabilityTypes.size(),
                   "different number of callability dates and types");
            QL.require(callabilityDates.size() == callabilityPrices.size(),
                   "different number of callability dates and prices");
            QL.require(callabilityDates.size() == callabilityTriggers.size(),
                   "different number of callability dates and triggers");

            QL.require(couponDates.size() == couponAmounts.size(),
                   "different number of coupon dates and amounts");
    }
//        @Override
//        public void validate() /*@ReadOnly*/ {
//            super.validate();
//
//            // TODO: message
//            QL.require(!Double.isNaN(conversionRatio), "null conversion ratio");
//            QL.require(conversionRatio > 0.0, "positive conversion ratio required");
//            QL.require(!Double.isNaN(redemption), "null redemption");
//            QL.require(redemption >= 0.0, "positive redemption required");
//            QL.require(!settlementDate.isNull(), "null settlement date");
//            QL.require(settlementDays != Constants.NULL_INTEGER, "null settlement days");
//            QL.require(callabilityDates.size() == callabilityTypes.size(),    "different number of callability dates and types");
//            QL.require(callabilityDates.size() == callabilityPrices.size(),   "different number of callability dates and prices");
//            QL.require(callabilityDates.size() == callabilityTriggers.size(), "different number of callability dates and triggers");
//            QL.require(couponDates.size() == couponAmounts.size(), "different number of coupon dates and amounts");
//        }

    }


    static public class ResultsImpl extends OneAssetOption.ResultsImpl {

        @Override
        public void reset() /* @ReadOnly */ {
            super.reset();
        }

    }


    static public abstract class EngineImpl
            extends GenericEngine<ConvertibleBondOption.ArgumentsImpl, ConvertibleBondOption.ResultsImpl> {

        protected EngineImpl() {
            super(new ArgumentsImpl(), new ResultsImpl());
        }

    }

}
