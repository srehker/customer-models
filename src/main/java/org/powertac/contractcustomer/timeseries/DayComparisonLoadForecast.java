package org.powertac.contractcustomer.timeseries;

import java.util.ArrayList;
import java.util.HashMap;

import org.joda.time.DateTime;

public class DayComparisonLoadForecast implements LoadForecast {

	HashMap<String, ArrayList<Double>> averageLoads=new HashMap<String, ArrayList<Double>>();
	
	@Override
	public LoadTimeSeries calculateLoadForecast(LoadTimeSeries historicLoad,
			DateTime fromDate, DateTime toDate) {

		for(Daytype daytype: Daytype.values()){
			for(int m=1; m<=12;m++){
				averageLoads.put(m+"-"+daytype, getAverageLoadPerDaytypeAndMonth(historicLoad, daytype, m));
			}
		}
		
		ArrayList<TimeSeriesDay> predictedDays=new ArrayList<TimeSeriesDay>();
		
		for (DateTime date = fromDate; date.isBefore(toDate.plus(1)); date = date.plusDays(1))
		{
			String key=date.getMonthOfYear()+"-"+Daytype.getDaytypeFromDate(date);			
		    predictedDays.add(new TimeSeriesDay(Daytype.getDaytypeFromDate(date), date, averageLoads.get(key)));
		}
		
		LoadTimeSeries predictionTimeSeries=new LoadTimeSeries(predictedDays, fromDate, toDate);
		
		return predictionTimeSeries;
	}

	public ArrayList<Double> getAverageLoadPerDaytypeAndMonth(
			LoadTimeSeries historicLoad, Daytype daytype, int month) {
		ArrayList<TimeSeriesDay> matchingDays = new ArrayList<TimeSeriesDay>();
		for (TimeSeriesDay d : historicLoad.getDays()) {			
			if (d.getDaytype().equals(daytype)
					&& d.getDate().getMonthOfYear()==month) {
				matchingDays.add(d);
			}			
		}
		ArrayList<Double> ret=new ArrayList<Double>();
		
		for(int h=0;h<24;h++){
			double averageLoad=0;
			for(TimeSeriesDay d: matchingDays){
				averageLoad+=d.getHourvalues().get(h);
			}
			ret.add(Math.round(averageLoad*100/matchingDays.size())/100.);
		}
		
		return ret;
	}

}
