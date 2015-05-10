package org.powertac.contractcustomer.negotiation;

import org.powertac.common.Contract;

@Deprecated
public class NegotiationMessage {
	
	private Action action;
	private Contract offer;
	private Coordinator sender;
	public NegotiationMessage(Action action, Contract offer) {
		super();
		this.action = action;
		this.offer = offer;
	}
	public Action getAction() {
		return action;
	}
	public void setAction(Action action) {
		this.action = action;
	}
	public Contract getOffer() {
		return offer;
	}
	public void setOffer(Contract offer) {
		this.offer = offer;
	}
	
	

}
