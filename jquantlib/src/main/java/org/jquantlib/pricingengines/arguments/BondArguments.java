package org.jquantlib.pricingengines.arguments;

import org.jquantlib.cashflow.Leg;
import org.jquantlib.time.Calendar;
import org.jquantlib.util.Date;

public class BondArguments extends Arguments {
	public Date settlementDate;
    public Leg cashflows;
    public Calendar calendar;


	@Override
	public void validate() {
		assert (settlementDate != Date.NULL_DATE): ("no settlement date provided");
		assert(!cashflows.isEmpty()): ("no cash flow provided");
        for (int i=0; i<cashflows.size(); ++i)
        	assert(cashflows.get(i)!=null): ("null cash flow provided");
		
	}

}