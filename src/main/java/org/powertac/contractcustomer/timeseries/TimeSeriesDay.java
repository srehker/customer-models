package org.powertac.contractcustomer.timeseries;

import java.util.ArrayList;

import org.joda.time.DateTime;

public class TimeSeriesDay {
	private Daytype daytype;
	private DateTime date;
	private ArrayList<Double> hourvalues; // 1-24
	
		
	public TimeSeriesDay(Daytype daytype, DateTime date,
			ArrayList<Double> hourvalues) {
		super();
		this.daytype = daytype;
		this.setDate(date);
		this.hourvalues = hourvalues;
	}
	public Daytype getDaytype() {
		return daytype;
	}
	public void setDaytype(Daytype daytype) {
		this.daytype = daytype;
	}
	public ArrayList<Double> getHourvalues() {
		return hourvalues;
	}
	public void setHourvalues(ArrayList<Double> hourvalues) {
		this.hourvalues = hourvalues;
	}
	public DateTime getDate() {
		return date;
	}
	public void setDate(DateTime date) {
		this.date = date;
	}

}
