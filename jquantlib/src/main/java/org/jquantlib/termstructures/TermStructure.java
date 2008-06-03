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
 Copyright (C) 2004, 2005, 2006 StatPro Italia srl

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

package org.jquantlib.termstructures;

import java.util.List;

import org.jquantlib.Configuration;
import org.jquantlib.Settings;
import org.jquantlib.daycounters.Actual365Fixed;
import org.jquantlib.daycounters.DayCounter;
import org.jquantlib.math.interpolation.DefaultExtrapolator;
import org.jquantlib.math.interpolation.Extrapolator;
import org.jquantlib.time.Calendar;
import org.jquantlib.time.TimeUnit;
import org.jquantlib.util.Date;
import org.jquantlib.util.DefaultObservable;
import org.jquantlib.util.Observable;
import org.jquantlib.util.Observer;



/**
 * Basic term-structure functionality.
 * 
 * <p><b>More Details about constructors:</b>
 * <p>There are three ways in which a term structure can keep
 * track of its reference date:
 * <li>such date is fixed;</li>
 * <li>such date is determined by advancing the current date of a given number of business days;</li>
 * <li>such date is based on the reference date of some other structure.</li>
 * 
 * <p>Case 1: The constructor taking a date is to be used.
 * The default implementation of {@link TermStructure#getReferenceDate()} will
 * then return such date.
 * 
 * <p>Case 2: The constructor taking a number of days and a calendar is to be used 
 * so that {@link TermStructure#getReferenceDate()} will return a date calculated based on the
 * current evaluation date and the term structure and observers will be notified when the
 * evaluation date changes.
 * 
 * <p>Case 3: The {@link TermStructure#getReferenceDate()} method must
 * be overridden in derived classes so that it fetches and
 * return the appropriate date.
 *  
 * @author Richard Gomes
 */
public abstract class TermStructure implements Observer, Observable {

	static private final String THIS_METHOD_MUST_BE_OVERRIDDEN = "This method must be overridden";
	
	
	//
	// protected fields
	//
	protected final Settings settings;
	
	
	//
	// private fields
	//
	
	/**
	 * <p>Case 1: The constructor taking a date is to be used.
	 * The default implementation of {@link TermStructure#getReferenceDate()} will
	 * then return such date.
	 * 
	 * <p>Case 2: The constructor taking a number of days and a calendar is to be used 
	 * so that {@link TermStructure#getReferenceDate()} will return a date calculated based on the
	 * current evaluation date and the term structure and observers will be notified when the
	 * evaluation date changes.
	 * 
	 * <p>Case 3: The {@link TermStructure#getReferenceDate()} method must
	 * be overridden in derived classes so that it fetches and
	 * return the appropriate date.
	 */
	private Date referenceDate;
	
	/**
	 * Beware that this variable must always be accessed via {@link #getCalendar()} method.
	 * Extended classes have the option to redefine semantics of a calendar by keeping their own private
	 * calendar variable and providing their own version of {@link #getCalendar()} method. When extended 
	 * classes fail to provide their version of {@link #getCalendar()} method, <i><b>this</b>.getCalendar</i> 
	 * must throw an {@link IllegalStateException} because the private variable calendar was never initialised.
	 * 
	 * @see #getCalendar
	 */
	private Calendar calendar;
	
	/**
	 * Beware that this variable must always be accessed via {@link #getDayCounter()} method.
	 * Extended classes have the option to redefine semantics of a day counter by keeping their own private
	 * dayCounter variable and providing their own version of {@link #getDayCounter()} method. When extended 
	 * classes fail to provide their version of {@link #getDayCounter()} method, <i><b>this</b>.getDayCounter</i> 
	 * must throw an {@link IllegalStateException} because the private variable dayCounter was never initialised.
	 * 
	 * @see #getDayCounter
	 */
	private final DayCounter dayCounter;
	
	/**
	 * This variable must be <i>false</i> when Case 2; <i>true</i> otherwise
	 */
	private boolean updated;


	//
	// private final fields
	//
	
	private final int settlementDays;
	
	/**
	 * This variable must be <i>true</i> when Case 2; <i>false</i> otherwise
	 */
	private final boolean moving;

	/**
	 * This private field is automatically initialized by constructors.
	 * In the specific case of Case 2, the corresponding constructor
	 * picks up it's value from {@link Settings} singleton. This procedure
	 * caches values from the singleton, intending to avoid contention in
	 * heavily multi-threaded environments. In this specific case this class
	 * observes date changes in order to update this variable.
	 */
	private Date today;

	
	//
	// constructors
	//
	
	/**
	 * <p>This constructor requires an override of method {@link TermStructure#getReferenceDate()} in 
	 * derived classes so that it fetches and return the appropriate reference date.
	 * This is the <i>Case 3</i> described on the top of this class.
	 * 
     * @see TermStructure documentation for more details about constructors.
	 */
	public TermStructure() {
		this(new Actual365Fixed());
	}

	/**
	 * <p>This constructor requires an override of method {@link TermStructure#getReferenceDate()} in 
	 * derived classes so that it fetches and return the appropriate reference date.
	 * This is the <i>Case 3</i> described on the top of this class.
	 * 
     * @see TermStructure documentation for more details about constructors.
	 */
	public TermStructure(final DayCounter dc) {
		if (dc==null) throw new NullPointerException(); // TODO: message
		this.referenceDate = null;
		this.settlementDays = 0;
		this.dayCounter = dc;
		this.settings = Configuration.getSystemConfiguration(null).getGlobalSettings();

		// When Case 1 or Case 3
		this.moving = false;
		this.updated = true;
		this.today = null;
	}

	/**
	 * Initialize with a fixed reference date
	 * 
	 * <p>This constructor takes a date to be used. 
	 * The default implementation of {@link TermStructure#getReferenceDate()} will
	 * then return such date.
	 * This is the <i>Case 1</i> described on the top of this class.
	 * 
     * @see TermStructure documentation for more details about constructors.
	 */ 
	public TermStructure(final Date referenceDate, final Calendar calendar) {
		this(referenceDate, calendar, new Actual365Fixed());
	}

	/**
	 * Initialize with a fixed reference date
	 * 
	 * <p>This constructor takes a date to be used. 
	 * The default implementation of {@link TermStructure#getReferenceDate()} will
	 * then return such date.
	 * This is the <i>Case 1</i> described on the top of this class.
	 * 
     * @see TermStructure documentation for more details about constructors.
	 */ 
	public TermStructure(final Date referenceDate, final Calendar calendar, final DayCounter dc) {
		if (referenceDate==null) throw new NullPointerException(); // TODO: message
		if (calendar==null) throw new NullPointerException(); // TODO: message
		if (dc==null) throw new NullPointerException(); // TODO: message
		this.referenceDate = referenceDate;
		this.settlementDays = 0;
		this.calendar = calendar;
		this.dayCounter = dc;
		this.settings = Configuration.getSystemConfiguration(null).getGlobalSettings();

		// When Case 1 or Case 3
		this.moving = false;
		this.updated = true;
		this.today = null;
	}
	
	/**
	 * Calculate the reference date based on the global evaluation date
	 * 
	 * <p>This constructor takes a number of days and a calendar to be used 
	 * so that {@link TermStructure#getReferenceDate()} will return a date calculated based on the
	 * current evaluation date and the term structure. This class will be notified when the
	 * evaluation date changes.
	 * This is the <i>Case 2</i> described on the top of this class.
	 * 
     * @see TermStructure documentation for more details about constructors.
	 */ 
	public TermStructure(final int settlementDays, final Calendar calendar) {
		this(settlementDays, calendar, new Actual365Fixed());
	}

	
	/**
	 * Calculate the reference date based on the global evaluation date
	 * 
	 * <p>This constructor takes a number of days and a calendar to be used 
	 * so that {@link TermStructure#getReferenceDate()} will return a date calculated based on the
	 * current evaluation date and the term structure. This class will be notified when the
	 * evaluation date changes.
	 * This is the <i>Case 2</i> described on the top of this class.
	 * 
     * @see TermStructure documentation for more details about constructors.
	 */ 
	public TermStructure(final int settlementDays, final Calendar calendar, final DayCounter dc) {
		this.referenceDate = null;
		this.settlementDays = settlementDays;
		this.calendar = calendar;
		this.dayCounter = dc;
		this.settings = Configuration.getSystemConfiguration(null).getGlobalSettings();

		// When Case 2
		this.moving = true;
		this.updated = false;
		this.today = settings.getEvaluationDate();
		today.addObserver(this);
	}
	
	
	//
	// abstract methods
	//
	
	/**
	 * @return the latest date for which the curve can return values
	 */
	public abstract Date getMaxDate();

	
	//
	// public methods
	//
	
	/**
	 * @return the calendar used for reference date calculation
	 */
	public Calendar getCalendar() /* @ReadOnly */ {
		if (this.calendar == null) throw new IllegalStateException(THIS_METHOD_MUST_BE_OVERRIDDEN);
		return calendar;
	}

	/**
	 * This method performs a date to double conversion which represents
	 * the fraction of the year between the reference date and 
	 * the date passed as parameter.
	 *  type filter text
	 * @param date
	 * @return the fraction of the year as a double
	 */
	public final /*@Time*/ double getTimeFromReference(final Date date) {
		return getDayCounter().getYearFraction(getReferenceDate(), date);
	}

	
	//
	// protected methods
	//
	
	/**
	 * This method performs date-range check
	 */ 
	protected final void checkRange(final Date date, boolean extrapolate) {
		checkRange(getTimeFromReference(date), extrapolate);
	}

	/**
	 * This method performs date-range check
	 */ 
	protected final void checkRange(final /*@Time*/ double time, boolean extrapolate) {
		/*@Time*/ double t = time;
		if (t<0.0) throw new IllegalArgumentException("negative double given");
		if (! (extrapolate || allowsExtrapolation() || (t<=getMaxTime())) ) 
			throw new IllegalArgumentException("double ("+time+") is past max curve double ("+getMaxTime()+")");
	}
	
	/**
	 * @return the day counter used for date/double conversion
	 * 
	 * @see #dayCounter
	 */
	public DayCounter getDayCounter() {
		if (this.dayCounter == null) throw new IllegalStateException(THIS_METHOD_MUST_BE_OVERRIDDEN);
		return dayCounter;
	}

	/**
	 * @return the latest double for which the curve can return values
	 */
	public final /*@Time*/ double getMaxTime(){
		return getTimeFromReference(getMaxDate());
	}

	/**
	 * Returns the Date at which discount = 1.0 and/or variance = 0.0
	 * 
	 * @note Term structures initialized by means of this
	 * constructor must manage their own reference date 
	 * by overriding the getReferenceDate() method.
	 *  
	 * @returns the Date at which discount = 1.0 and/or variance = 0.0
	 */
	public Date getReferenceDate() {
		if (moving) {
			if (!updated) {
				referenceDate = getCalendar().advance(today, settlementDays, TimeUnit.DAYS);
				updated = true;
			}
		}
		
		if (referenceDate==null) // i.e: Case 3
			throw new IllegalStateException(THIS_METHOD_MUST_BE_OVERRIDDEN); 

		return referenceDate;
	}

	
	//
	// implements Extrapolator
	//
	
	/**
	 * Implements multiple inheritance via delegate pattern to a inner class
	 * 
	 * @see Extrapolator
	 */
	private DefaultExtrapolator delegatedExtrapolator = new DefaultExtrapolator();
	
	public final boolean allowsExtrapolation() {
		return delegatedExtrapolator.allowsExtrapolation();
	}

	public void disableExtrapolation() {
		delegatedExtrapolator.disableExtrapolation();
	}

	public void enableExtrapolation() {
		delegatedExtrapolator.enableExtrapolation();
	}

	
	//
	// implements Observer
	//
	
	public void update(Observable o, Object arg) {
		if (moving) {
			updated = false;
		}
		notifyObservers();
	}

	
	//
	// implements Observable
	//
	
	/**
	 * Implements multiple inheritance via delegate pattern to an inner class
	 * 
	 * @see Observable
	 * @see DefaultObservable
	 */
    private Observable delegatedObservable = new DefaultObservable(this);

	public void addObserver(Observer observer) {
		delegatedObservable.addObserver(observer);
	}

	public int countObservers() {
		return delegatedObservable.countObservers();
	}

	public void deleteObserver(Observer observer) {
		delegatedObservable.deleteObserver(observer);
	}

	public void notifyObservers() {
		delegatedObservable.notifyObservers();
	}

	public void notifyObservers(Object arg) {
		delegatedObservable.notifyObservers(arg);
	}

	public void deleteObservers() {
		delegatedObservable.deleteObservers();
	}

	public List<Observer> getObservers() {
		return delegatedObservable.getObservers();
	}

}
