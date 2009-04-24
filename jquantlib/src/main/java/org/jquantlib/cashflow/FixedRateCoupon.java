/*
 Copyright (C) 2009 Ueli Hofstetter

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


package org.jquantlib.cashflow;

import org.jquantlib.daycounters.DayCounter;
import org.jquantlib.util.Date;
import org.jquantlib.util.TypedVisitor;
import org.jquantlib.util.Visitor;

public class FixedRateCoupon extends Coupon{
    
    private double rate_;
    private DayCounter dayCounter_;

    public FixedRateCoupon(double nominal, 
            Date paymentDate, 
            double rate,
            DayCounter dayCounter,
            Date accrualStartDate, 
            Date accrualEndDate, 
            Date refPeriodStart,
            Date refPeriodEnd) {
        super(nominal, paymentDate, accrualStartDate, accrualEndDate, refPeriodStart, refPeriodEnd);
        this.rate_ = rate;
        this.dayCounter_ = dayCounter;
    }

    @Override
    public DayCounter dayCounter() {
       return dayCounter_;
    }

    @Override
    public double getAmount() {
        return nominal()*rate_*accrualPeriod();
    }
    
    public double accruedAmount(Date d){
        if(d.le(accrualStartDate_) || d.gt(paymentDate_)){
            return 0.0;
        }
        else{
            return nominal()*rate_*dayCounter_.yearFraction(accrualStartDate_, 
                    /* FIXME: nasty......*/
                    d.le(accrualEndDate_)?d:accrualEndDate_, 
                            refPeriodStart_, refPeriodEnd_);
        }
    }
    
    @Override
    public void accept(final TypedVisitor<Event> v) {
        Visitor<Event> v1 = (v!=null) ? v.getVisitor(this.getClass()) : null;
        if (v1 != null) {
            v1.visit(this);
        } else {
            super.accept(v);
        }
    }

}