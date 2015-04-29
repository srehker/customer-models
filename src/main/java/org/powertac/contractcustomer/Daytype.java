package org.powertac.contractcustomer;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

public enum Daytype {
	MONDAY,
	TUESDAY,
	WEDNESDAY,
	THURSDAY,
	FRIDAY,
	SATURDAY,
	SUNDAY,
	HOLIDAY;
	
	public static Daytype getDaytypeFromDate(DateTime d){
		
		// for now just christmas
		if(d.getDayOfMonth() >= 24 && d.getDayOfMonth() <= 26 && d.getMonthOfYear()==DateTimeConstants.DECEMBER){
			return HOLIDAY;
		}
		
		switch (d.getDayOfWeek()){
		
		case DateTimeConstants.MONDAY:
			return MONDAY;
			
		case DateTimeConstants.TUESDAY:
			return TUESDAY;
			
		case DateTimeConstants.WEDNESDAY:
			return WEDNESDAY;
			
		case DateTimeConstants.THURSDAY:
			return THURSDAY;
			
		case DateTimeConstants.FRIDAY:
			return FRIDAY;
			
		case DateTimeConstants.SATURDAY:
			return SATURDAY;
			
		case DateTimeConstants.SUNDAY:
			return SUNDAY;			
			
		}
		return null;
		
	}

}