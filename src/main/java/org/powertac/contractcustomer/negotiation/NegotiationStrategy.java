package org.powertac.contractcustomer.negotiation;

import org.powertac.common.Contract;

public interface NegotiationStrategy {
	
	public void respondMessage(NegotiationMessage m);
	
	public double computeUtility(Contract offer);

}
