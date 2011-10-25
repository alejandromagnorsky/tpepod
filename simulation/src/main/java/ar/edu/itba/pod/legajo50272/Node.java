package ar.edu.itba.pod.legajo50272;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import ar.edu.itba.balance.api.AgentsTransfer;

public class Node {
	
	public static void main(String[] args) {
		try {
			AgentsTransfer obj = new AgentsTransferImpl();
			Registry registry = LocateRegistry.createRegistry(1099);
			registry.bind("AgentsTransfer", obj);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
