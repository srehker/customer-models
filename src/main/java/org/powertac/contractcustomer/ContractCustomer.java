package org.powertac.contractcustomer;

import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.powertac.common.CustomerInfo;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.exceptions.PowerTacException;
import org.powertac.common.msg.ContractAnnounce;
import org.powertac.common.msg.ContractDecommit;
import org.powertac.common.timeseries.LoadTimeSeries;
import org.powertac.customer.AbstractContractCustomer;
import org.springframework.stereotype.Service;

public class ContractCustomer extends AbstractContractCustomer {
	
	public ContractCustomer(){
		super();
	}
	
	public ContractCustomer(PowerType powertype, Instant baseTime) {
		super(baseTime.toDateTime());
		if (powertype == PowerType.CONSUMPTION)
			name = "contractConsumer" + custId;
		else if (powertype == PowerType.PRODUCTION)
			name = "contractProducer" + custId;
		else
			throw new PowerTacException(
					"Attempted to create Contract Customer with invalid Powertype. only PRODUCTION & CONSUMPTION is valid.");

		CustomerInfo ci = new CustomerInfo(name, 1)
				.withCanNegotiate(true).withPowerType(powertype);
		addCustomerInfo(ci);
		
	}

	@Override
	public void step() {
		// no active contract then announce that it needs one
		if(!activeContract(service.getTimeslotRepo().currentTimeslot().getStartTime())){
			for (CustomerInfo ci : service.getCustomerRepo().findByName(
					getName())) {
				ContractAnnounce cann = new ContractAnnounce(ci.getId());// has to be CustomerInfo ID
				service.getBrokerProxyService().broadcastMessage(cann);
			}
		}
		
		historicLoad = service.getTimeSeriesRepo()
				.findHistoricLoadByCustomerId(custId);
		DateTime now = service.getTimeslotRepo().currentTimeslot()
				.getStartTime();
		LoadTimeSeries l = generator.generateLoadTimeSeries(now,
				now.plusHours(1), (int) (custId % 3));
		historicLoad.addValue(now, l.getValue(now));

		
	}

}
