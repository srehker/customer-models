package org.powertac.contractcustomer.negotiation;

import org.joda.time.DateTime;

public class Contract {
	
	private double energyPrice; //per kWh
	private double peakLoadPrice; //per Month per kWh
	private DateTime startDate;
	private DateTime endDate;
	private double earlyExitFee; // when DECOMMIT is send (also before startDate)
	
	public Contract(double energyPrice, double peakLoadPrice,
			DateTime startDate, DateTime endDate, double earlyExitFee) {
		super();
		this.energyPrice = energyPrice;
		this.peakLoadPrice = peakLoadPrice;
		this.startDate = startDate;
		this.endDate = endDate;
		this.earlyExitFee = earlyExitFee;
	}

	public double getEnergyPrice() {
		return energyPrice;
	}

	public void setEnergyPrice(double energyPrice) {
		this.energyPrice = energyPrice;
	}

	public double getPeakLoadPrice() {
		return peakLoadPrice;
	}

	public void setPeakLoadPrice(double peakLoadPrice) {
		this.peakLoadPrice = peakLoadPrice;
	}

	public DateTime getStartDate() {
		return startDate;
	}

	public void setStartDate(DateTime startDate) {
		this.startDate = startDate;
	}

	public DateTime getEndDate() {
		return endDate;
	}

	public void setEndDate(DateTime endDate) {
		this.endDate = endDate;
	}

	public double getEarlyExitFee() {
		return earlyExitFee;
	}

	public void setEarlyExitFee(double earlyExitFee) {
		this.earlyExitFee = earlyExitFee;
	}
	
	
	
	

}
