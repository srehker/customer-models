package org.powertac.contractcustomer;

public class ContractConsumer {
	
	public static int idCount=0;
	public int id;
	
	
	public ContractConsumer() {
		this.id=idCount;
		idCount++;
	}
	
	

}
