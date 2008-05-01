/*
 Copyright (C) 2007 Richard Gomes

 This file is part of JQuantLib, a free-software/open-source library
 for financial quantitative analysts and developers - http://jquantlib.org/

 JQuantLib is free software: you can redistribute it and/or modify it
 under the terms of the QuantLib license.  You should have received a
 copy of the license along with this program; if not, please email
 <jquantlib-dev@lists.sf.net>. The license is also available online at
 <http://jquantlib.org/license.shtml>.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE.  See the license for more details.
 
 JQuantLib is based on QuantLib. http://quantlib.org/
 When applicable, the originating copyright notice follows below.
 */

/*
 Copyright (C) 2003 RiskMap srl

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

package org.jquantlib.testsuite.instruments;

import static org.junit.Assert.assertFalse;

import org.jquantlib.instruments.Instrument;
import org.jquantlib.instruments.Stock;
import org.jquantlib.quotes.Quote;
import org.jquantlib.quotes.RelinkableHandle;
import org.jquantlib.quotes.SimpleQuote;
import org.jquantlib.util.Utilities.Flag;
import org.junit.Test;

public class IntrumentsTest {

//	#include "instruments.hpp"
//	#include "utilities.hpp"
//	#include <ql/instruments/stock.hpp>
//	#include <ql/quotes/simplequote.hpp>
//
//	using namespace QuantLib;
//	using namespace boost::unit_test_framework;

	@Test
	public void testObservable() {

	    System.out.println("Testing observability of instruments...");


	    SimpleQuote me1 = new SimpleQuote(0.0);
	    RelinkableHandle<Quote>  h = new RelinkableHandle<Quote>(me1);
	    Instrument s = new Stock(h);

	    Flag f = new Flag();
	    s.addObserver(f); //f.registerWith(s);
	    
	    s.getNPV();
	    me1.setValue(3.14);
	    assertFalse("Observer was not notified of instrument change", !f.isUp());
	    
	    s.getNPV();
	    f.lower();
	    SimpleQuote me2 = new SimpleQuote(0.0);

	    h.setLink(me2);
	    assertFalse("Observer was not notified of instrument change", !f.isUp());

	    f.lower();
	    s.freeze();
	    s.getNPV();
	    me2.setValue(2.71);
	    assertFalse("Observer was notified of frozen instrument change", f.isUp());
	    s.getNPV();
	    s.unfreeze();
	    assertFalse("Observer was not notified of instrument change", !f.isUp());
	}

}
