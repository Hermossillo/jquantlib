/*
 Copyright (C) 2007 Richard Gomes

 This file is part of JQuantLib, a free-software/open-source library
 for financial quantitative analysts and developers - http://jquantlib.org/

 JQuantLib is free software: you can redistribute it and/or modify it
 under the terms of the QuantLib license.  You should have received a
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
Copyright (C) 2003, 2004, 2005, 2006 StatPro Italia srl

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

package org.jquantlib.termstructures.yieldcurves;

import org.jquantlib.math.interpolation.Interpolation;
import org.jquantlib.math.interpolation.Interpolator;
import org.jquantlib.util.Date;
import org.jquantlib.util.Pair;

public final class InterpolatedZeroCurve<I extends Interpolator> extends ZeroYieldStructure implements YieldCurve {

	//
	// private fields
	//
	// In the original C++ implementation these fields are protected and, doing so, accessible by
	// PiecewiseCurve, which *optionally* extends this class, depending on template metaprogramming.
	//
	private Date[]				dates;
	private /* @Time */double[]	times;
	private /* @Rate */double[]	data; // forwards
	
	//
	// protected fields
	//
	
	protected Interpolation			interpolation;
	protected boolean isNegativeRates;

	
	//
	// implements PiecewiseYieldCurve.YieldCurve
	//
	
	@Override
	public final Date[] getDates() /* @ReadOnly */ {
		return dates;
	}

	@Override
	public final double[] getData() /* @ReadOnly */ {
		return data;
	}

	@Override
	public final Date getMaxDate() /* @ReadOnly */ {
		return dates[dates.length-1];
	}
	
	@Override
	public final Pair<Date, Double>[] getNodes() /* @ReadOnly */ {
      Pair<Date, /*@Rate*/ Double>[] results = new Pair /* <Date, @Rate Double> */ [dates.length];
      for (int i=0; i<dates.length; ++i)
          results[i] = new Pair<Date, Double>(dates[i], data[i]);
      return results;
	}

	@Override
	public final double[] getTimes() /* @ReadOnly */ {
		return times;
	}

	// In particular, these methods should not exist in the interface.
	// In the original C++ implementation the related fields are protected and, doing so, accessible by
	// PiecewiseCurve, which *optionally* extends ancestor classes using template metaprogramming.
	
	@Override 
	public final void setMaxDate(final Date maxDate) {
		dates[dates.length-1] = maxDate;
	}

	@Override
	public void setDates(Date[] dates) {
		this.dates = dates;
	}

	@Override
	public void setTimes(double[] times) {
		this.times = times;
	}

	@Override
	public void setData(double[] data) {
		this.data = data;
	}

	@Override
	public final void setNodes(Pair<Date, Double>[] pairs) /* @ReadOnly */ {
		this.dates = new Date[pairs.length];
		this.data = new double[pairs.length];
	    for (int i=0; i<dates.length; ++i) {
	        dates[i] = pairs[i].getFirst();
	        data[i]  = pairs[i].getSecond();
	    }
	}

}


///*! \file zerocurve.hpp
//   \brief interpolated zero-rates structure
//*/
//
//#ifndef quantlib_zero_curve_hpp
//#define quantlib_zero_curve_hpp
//
//#include <ql/termstructures/yieldcurves/zeroyieldstructure.hpp>
//#include <ql/math/interpolations/linearinterpolation.hpp>
//#include <vector>
//#include <utility>
//
//namespace QuantLib {
//
//   //! Term structure based on interpolation of zero yields
//   /*! \ingroup yieldtermstructures */
//   template <class Interpolator>
//   class InterpolatedZeroCurve : public ZeroYieldStructure {
//     public:
//       // constructor
//       InterpolatedZeroCurve(const std::vector<Date>& dates,
//                             const std::vector<Rate>& yields,
//                             const DayCounter& dayCounter,
//                             const Interpolator& interpolator
//                                                           = Interpolator());
//       //! \name Inspectors
//       //@{
//       Date maxDate() const;
//       const std::vector<Time>& times() const;
//       const std::vector<Date>& dates() const;
//       const std::vector<Rate>& zeroRates() const;
//       std::vector<std::pair<Date,Rate> > nodes() const;
//     protected:
//       InterpolatedZeroCurve(const DayCounter&,
//                             const Interpolator& interpolator
//                                                           = Interpolator());
//       InterpolatedZeroCurve(const Date& referenceDate,
//                             const DayCounter&,
//                             const Interpolator& interpolator
//                                                           = Interpolator());
//       InterpolatedZeroCurve(Natural settlementDays,
//                             const Calendar&,
//                             const DayCounter&,
//                             const Interpolator& interpolator
//                                                           = Interpolator());
//       Rate zeroYieldImpl(Time t) const;
//       mutable std::vector<Date> dates_;
//       mutable std::vector<Time> times_;
//       mutable std::vector<Rate> data_;
//       mutable Interpolation interpolation_;
//       Interpolator interpolator_;
//   };
//
//   //! Term structure based on linear interpolation of zero yields
//   /*! \ingroup yieldtermstructures */
//   typedef InterpolatedZeroCurve<Linear> ZeroCurve;
//
//
//   // inline definitions
//
//   template <class T>
//   inline Date InterpolatedZeroCurve<T>::maxDate() const {
//       return dates_.back();
//   }
//
//   template <class T>
//   inline const std::vector<Time>& InterpolatedZeroCurve<T>::times() const {
//       return times_;
//   }
//
//   template <class T>
//   inline const std::vector<Date>& InterpolatedZeroCurve<T>::dates() const {
//       return dates_;
//   }
//
//   template <class T>
//   inline const std::vector<Rate>&
//   InterpolatedZeroCurve<T>::zeroRates() const {
//       return data_;
//   }
//
//   template <class T>
//   inline std::vector<std::pair<Date,Rate> >
//   InterpolatedZeroCurve<T>::nodes() const {
//       std::vector<std::pair<Date,Rate> > results(dates_.size());
//       for (Size i=0; i<dates_.size(); ++i)
//           results[i] = std::make_pair(dates_[i],data_[i]);
//       return results;
//   }
//
//   template <class T>
//   inline InterpolatedZeroCurve<T>::InterpolatedZeroCurve(
//                                                const DayCounter& dayCounter,
//                                                const T& interpolator)
//   : interpolator_(interpolator) {}
//
//   template <class T>
//   inline InterpolatedZeroCurve<T>::InterpolatedZeroCurve(
//                                                const Date& referenceDate,
//                                                const DayCounter& dayCounter,
//                                                const T& interpolator)
//   : ZeroYieldStructure(referenceDate, Calendar(), dayCounter),
//     interpolator_(interpolator) {}
//
//   template <class T>
//   inline InterpolatedZeroCurve<T>::InterpolatedZeroCurve(
//                                                Natural settlementDays,
//                                                const Calendar& calendar,
//                                                const DayCounter& dayCounter,
//                                                const T& interpolator)
//   : ZeroYieldStructure(settlementDays,calendar, dayCounter),
//     interpolator_(interpolator) {}
//
//   template <class T>
//   Rate InterpolatedZeroCurve<T>::zeroYieldImpl(Time t) const {
//       return interpolation_(t, true);
//   }
//
//   // template definitions
//
//   template <class T>
//   InterpolatedZeroCurve<T>::InterpolatedZeroCurve(
//                                             const std::vector<Date>& dates,
//                                             const std::vector<Rate>& yields,
//                                             const DayCounter& dayCounter,
//                                             const T& interpolator)
//   : ZeroYieldStructure(dates[0], Calendar(), dayCounter),
//     dates_(dates), data_(yields), interpolator_(interpolator) {
//
//       QL_REQUIRE(dates_.size()>1, "too few dates");
//       QL_REQUIRE(data_.size()==dates_.size(),
//                  "dates/yields count mismatch");
//
//       times_.resize(dates_.size());
//       times_[0]=0.0;
//       for (Size i = 1; i < dates_.size(); i++) {
//           QL_REQUIRE(dates_[i] > dates_[i-1],
//                      "invalid date (" << dates_[i] << ", vs "
//                      << dates_[i-1] << ")");
//           #if !defined(QL_NEGATIVE_RATES)
//           QL_REQUIRE(data_[i] >= 0.0, "negative yield");
//           #endif
//           times_[i] = dayCounter.yearFraction(dates_[0], dates_[i]);
//       }
//
//       interpolation_ = interpolator_.interpolate(times_.begin(),
//                                                  times_.end(),
//                                                  data_.begin());
//       interpolation_.update();
//
//   }
//
//}