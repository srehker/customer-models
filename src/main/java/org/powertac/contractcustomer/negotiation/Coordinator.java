package org.powertac.contractcustomer.negotiation;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

public class Coordinator extends Observable implements Observer{

	ArrayList<Coordinator> partners;

	
	@Override
	public void update(Observable o, Object arg) {
		setChanged();
		notifyObservers(arg);
		
	}
	
	public void answerMessage(NegotiationMessage message, NegotiationMessage answer){
		
	}
	
	

	
}
