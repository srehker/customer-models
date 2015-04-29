package org.powertac.contractcustomer;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDateTime;

public class TimeSeries {
	private List<TimeSeriesDay> days;
	private DateTime fromDate;
	private DateTime toDate;
	

	
	public TimeSeries(List<TimeSeriesDay> days, DateTime fromDate,
			DateTime toDate) {
		super();
		this.days = days;
		this.fromDate = fromDate;
		this.toDate = toDate;
	}

	public List<TimeSeriesDay> getDays() {
		return days;
	}

	public void setDays(List<TimeSeriesDay> days) {
		this.days = days;
	} 
	
	public void outputFile(String filename){
		try
		{
		    FileWriter writer = new FileWriter(filename);
	 
		    writer.append("Date");
		    writer.append(';');
		    writer.append("Load");		    
		    writer.append('\n');
	 
		    for(TimeSeriesDay d:days){
		    	for(int i=0; i<d.getHourvalues().size(); i++){
		    		LocalDateTime tmp=new LocalDateTime(d.getDate()).withHourOfDay(i+1!=24?i+1:0);
		    		//DateTime tmp= new DateTime(d.getDate()).withHourOfDay(i+1!=24?i+1:0);
		    	writer.append(tmp.toString("dd.MM.yyyy HH:mm"));
			    writer.append(';');
			    writer.append(d.getHourvalues().get(i).toString());		    
			    writer.append('\n');}	
		    }
		    	 
		    writer.flush();
		    writer.close();
		}
		catch(IOException e)
		{
		     e.printStackTrace();
		} 
	    
	}

}
