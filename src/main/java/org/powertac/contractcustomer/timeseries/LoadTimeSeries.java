package org.powertac.contractcustomer.timeseries;

import java.util.List;

import org.joda.time.DateTime;

public class LoadTimeSeries extends TimeSeries{

	public LoadTimeSeries(List<TimeSeriesDay> days, DateTime fromDate,
			DateTime toDate) {
		super(days, fromDate, toDate);
	}
	
	public double getTotalLoad() {
		return getTotalLoad(getFromDate(), getToDate());
	}

	public double getTotalLoad(DateTime fromDate, DateTime toDate) {
		double totalLoad = 0;
		for (TimeSeriesDay d : getDays()) {
			if (d.getDate().isAfter(fromDate) && d.getDate().isBefore(toDate)
					|| d.getDate().equals(fromDate)
					|| d.getDate().equals(toDate))
				for (Double value : d.getHourvalues()) {
					totalLoad += value;
				}
		}
		return Math.round(totalLoad * 100) / 100.;
	}

	public double getMaxLoad(DateTime fromDate, DateTime toDate) {
		double maxLoad = 0;
		for (TimeSeriesDay d : getDays()) {
			if (d.getDate().isAfter(fromDate) && d.getDate().isBefore(toDate)
					|| d.getDate().equals(fromDate)
					|| d.getDate().equals(toDate))
				for (Double value : d.getHourvalues()) {
					if (maxLoad < value)
						maxLoad = value;
				}
		}
		return maxLoad;
	}

	public double getMaxLoad(int month) {
		double maxLoad = 0;
		for (TimeSeriesDay d : getDays()) {
			if (d.getDate().getMonthOfYear() == month)
				for (Double value : d.getHourvalues()) {
					if (maxLoad < value)
						maxLoad = value;
				}
		}
		return maxLoad;
	}

}
