package ar.edu.itba.pod.legajo50272;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import ar.edu.itba.balance.api.AgentsBalancer;
import ar.edu.itba.balance.api.AgentsTransfer;
import ar.edu.itba.event.RemoteEventDispatcher;
import ar.edu.itba.node.Node;
import ar.edu.itba.node.NodeInformation;
import ar.edu.itba.node.api.ClusterAdministration;

public class NodeImpl implements Node {
	
	private static String host = "localhost";
	private static int port = 1099;
	private static String id = host+port;
	
	public static void main(String[] args) {
		try {
			NodeInformation nodeInformation = new NodeInformation(host, port, id);
			ClusterAdministration clusterAdministration = new ClusterAdministrationImpl(nodeInformation);
			RemoteEventDispatcher remoteEventDispatcher = new RemoteEventDispatcherImpl(nodeInformation);
			AgentsTransfer agentsTransfer = new AgentsTransferImpl();			 
			AgentsBalancer agentsBalancer = new AgentsBalancerImpl();
			Registry registry = LocateRegistry.createRegistry(port);
			registry.bind(CLUSTER_COMUNICATION, clusterAdministration);
			registry.bind(DISTRIBUTED_EVENT_DISPATCHER, remoteEventDispatcher);
			registry.bind(AGENTS_TRANSFER, agentsTransfer);
			registry.bind(AGENTS_BALANCER, agentsBalancer);			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
