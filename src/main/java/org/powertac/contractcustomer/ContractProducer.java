package org.powertac.contractcustomer;

import java.util.Observable;
import java.util.Observer;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.powertac.common.Broker;
import org.powertac.common.Contract;
import org.powertac.common.timeseries.LoadForecast;
import org.powertac.common.timeseries.LoadTimeSeries;
import org.powertac.common.timeseries.TimeSeriesGenerator;
import org.powertac.contractcustomer.negotiation.NegotiationMessage;
import org.powertac.contractcustomer.negotiation.NegotiationStrategy;

public class ContractProducer extends Broker implements NegotiationStrategy{

	
	public static int idCount=0;
	public int id;
	private LoadTimeSeries historicLoad;
	private TimeSeriesGenerator generator;
	private LoadForecast forecast;	
	
	
	public ContractProducer(DateTime startTimeSlot) {
		super("ContractProducer"+idCount);
		this.id=idCount;
		idCount++;
		this.historicLoad = generator.generateLoadTimeSeries(startTimeSlot.minusYears(1), startTimeSlot, id%3);
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


	@Override
	public void respondMessage(NegotiationMessage m) {
		// TODO Auto-generated method stub
		
	}

}
