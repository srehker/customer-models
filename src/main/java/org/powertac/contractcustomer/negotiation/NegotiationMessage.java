package org.powertac.contractcustomer.negotiation;

public class NegotiationMessage {
	
	private Action action;
	private Contract offer;
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
