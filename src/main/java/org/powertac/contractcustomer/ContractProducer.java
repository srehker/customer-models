package org.powertac.contractcustomer;

public class ContractProducer {

	
	public static int idCount=0;
	public int id;
	
	
	public ContractProducer() {
		this.id=idCount;
		idCount++;
	}
}
