package ar.edu.itba.pod.legajo50272;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Set;

import ar.edu.itba.balance.api.AgentsBalancer;
import ar.edu.itba.balance.api.AgentsTransfer;
import ar.edu.itba.event.RemoteEventDispatcher;
import ar.edu.itba.node.Node;
import ar.edu.itba.node.NodeInformation;
import ar.edu.itba.node.api.ClusterAdministration;
import ar.edu.itba.pod.time.TimeMapper;

// -D java.rmi.server.hostname=IP

public class NodeImpl implements Node {

	private NodeInformation nodeInformation;
	private TimeMapper timeMapper;
	private ClusterAdministration clusterAdministration;
	private RemoteEventDispatcher remoteEventDispatcher;
	private AgentsTransfer agentsTransfer;
	private AgentsBalancer agentsBalancer;

	public NodeImpl(String host, int port, String id, TimeMapper timeMapper) {
		try {
			nodeInformation = new NodeInformation(host, port, id);
			clusterAdministration = new ClusterAdministrationImpl(this);
			remoteEventDispatcher = new RemoteEventDispatcherImpl(this);
			agentsTransfer = new AgentsTransferImpl(this);
			agentsBalancer = new AgentsBalancerImpl(this);
			this.timeMapper = timeMapper;
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void startServices() {
		try {
			Registry registry = LocateRegistry.createRegistry(nodeInformation.port());
			registry.bind(CLUSTER_COMUNICATION, clusterAdministration);
			registry.bind(DISTRIBUTED_EVENT_DISPATCHER, remoteEventDispatcher);
			registry.bind(AGENTS_TRANSFER, agentsTransfer);
			registry.bind(AGENTS_BALANCER, agentsBalancer);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public NodeInformation getNodeInformation() {
		return nodeInformation;
	}
	
	public TimeMapper getTimeMapper() {
		return timeMapper;
	}

	public ClusterAdministration getClusterAdministration() {
		return clusterAdministration;
	}

	public RemoteEventDispatcher getRemoteEventDispatcher() {
		return remoteEventDispatcher;
	}

	public AgentsTransfer getAgentsTransfer() {
		return agentsTransfer;
	}

	public AgentsBalancer getAgentsBalancer() {
		return agentsBalancer;
	}	
	
	public Set<NodeInformation> getConnectedNodes() throws RemoteException{
		return clusterAdministration.connectedNodes();
	}
}
