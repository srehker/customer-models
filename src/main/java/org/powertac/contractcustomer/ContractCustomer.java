package org.powertac.contractcustomer;

import org.joda.time.DateTime;
import org.powertac.common.CustomerInfo;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.exceptions.PowerTacException;
import org.powertac.common.timeseries.LoadTimeSeries;
import org.powertac.customer.AbstractContractCustomer;

public class ContractCustomer extends AbstractContractCustomer {

	public ContractCustomer(PowerType powertype) {
		super();
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
		historicLoad = service.getTimeSeriesRepo()
				.findHistoricLoadByCustomerId(custId);
		DateTime now = service.getTimeslotRepo().currentTimeslot()
				.getStartTime();
		LoadTimeSeries l = generator.generateLoadTimeSeries(now,
				now.plusHours(1), (int) (custId % 3));
		historicLoad.addValue(now, l.getValue(now));

		// TODO vertrag läuft aus? neu announcen
	}
}
