package org.powertac.contractcustomer.negotiation;

import java.util.ArrayList;
import java.util.Observable;

public class NegotiationThread extends Observable implements Runnable{

	private Coordinator coordinator;
	private Coordinator partner;
	private NegotiationMessage latestMessage;
	private ArrayList<NegotiationMessage> history;
	private boolean newMessage =false;
	private boolean activeNegotiation = true;
	
	
	@Override
	public void run() {
		while(!activeNegotiation ){
			if(newMessage){
				setChanged();
				notifyObservers(latestMessage);
				newMessage = false;
			}
		}
	}
	
	public void sendMessage(NegotiationMessage m){
		if(isValidMessage(m)){
			latestMessage = m;
			history.add(m);
			newMessage = true;
		}
	}

	private boolean isValidMessage(NegotiationMessage m) {
		// TODO Auto-generated method stub
		return false;
	}


}
