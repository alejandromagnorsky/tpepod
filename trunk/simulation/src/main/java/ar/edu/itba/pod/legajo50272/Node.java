package ar.edu.itba.pod.legajo50272;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import ar.edu.itba.balance.api.AgentsBalancer;
import ar.edu.itba.balance.api.AgentsTransfer;
import ar.edu.itba.node.NodeInformation;
import ar.edu.itba.node.api.ClusterAdministration;

public class Node {
	
	public static void main(String[] args) {
		try {
			ClusterAdministration clusterAdministration = new ClusterAdministrationImpl(new NodeInformation("localhost", 1099, "node1"));
			AgentsTransfer agentsTransfer = new AgentsTransferImpl();			 
			AgentsBalancer agentsBalancer = new AgentsBalancerImpl();
			Registry registry = LocateRegistry.createRegistry(1099);
			registry.bind("ClusterAdministration", clusterAdministration);
			registry.bind("AgentsTransfer", agentsTransfer);
			registry.bind("AgentsBalancer", agentsBalancer);			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
