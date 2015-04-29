package org.powertac.contractcustomer;

import org.joda.time.DateTime;

public interface LoadForecast {

	public TimeSeries calculateLoadForecast(TimeSeries historicLoad, DateTime fromDate, DateTime toDate);
}
