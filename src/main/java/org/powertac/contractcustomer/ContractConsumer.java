package org.powertac.contractcustomer;

import java.util.Observable;
import java.util.Observer;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.powertac.common.Broker;
import org.powertac.common.Contract;
import org.powertac.contractcustomer.negotiation.NegotiationMessage;
import org.powertac.contractcustomer.negotiation.NegotiationStrategy;
import org.powertac.contractcustomer.timeseries.LoadForecast;
import org.powertac.contractcustomer.timeseries.LoadTimeSeries;
import org.powertac.contractcustomer.timeseries.TimeSeriesGenerator;

public class ContractConsumer extends Broker implements Observer, NegotiationStrategy {
	
	public static int idCount=0;
	private int id;
	private LoadTimeSeries historicLoad;
	private TimeSeriesGenerator generator;
	private LoadForecast forecast;
	
	
	public ContractConsumer() {		
		super("ContractConsumer"+idCount);
		this.id=idCount;
		idCount++;
		DateTimeFormatter df=DateTimeFormat.forPattern("dd.MM.yyyy");
		this.historicLoad = generator.generateLoadTimeSeries(df.parseDateTime("01.01.2013"), df.parseDateTime("31.12.2013"), id%3);
		
	}


	@Override
	public void respondMessage(NegotiationMessage m) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void update(Observable o, Object arg) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public double computeUtility(Contract offer) {
		double utility = 0;
		
		LoadTimeSeries loadForecastTS= forecast.calculateLoadForecast(historicLoad, offer.getStartDate(), offer.getEndDate());
		utility += loadForecastTS.getTotalLoad()*offer.getEnergyPrice(); // total expected energy cost
		
		for(int month =1; month<= 12;month++){
			utility += loadForecastTS.getMaxLoad(month)*offer.getPeakLoadPrice(); // total expected peak load fee
		}
		
		if(activeContract(offer.getStartDate())){
			utility += offer.getEarlyExitFee();
		}
		
		//TODO utility for negotiation rounds
		
		return utility;
	}


	private boolean activeContract(DateTime startDate) {
		// TODO Auto-generated method stub
		return false;
	}	
	

}
