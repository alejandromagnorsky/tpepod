package ar.edu.itba.pod.legajo50272;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import ar.edu.itba.balance.api.AgentsBalancer;

public class AgentsBalancerServer {

	public static void main(String[] args) {
		try {
			AgentsBalancer obj = new AgentsBalancerImpl();
			Registry registry = LocateRegistry.createRegistry(1099);
			registry.bind("AgentsBalancer", obj);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
