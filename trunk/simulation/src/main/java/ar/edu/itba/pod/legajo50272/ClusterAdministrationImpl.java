package ar.edu.itba.pod.legajo50272;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import ar.edu.itba.node.Node;
import ar.edu.itba.node.NodeInformation;
import ar.edu.itba.node.api.ClusterAdministration;

public class ClusterAdministrationImpl extends UnicastRemoteObject implements
		ClusterAdministration {

	// The current node
	private RemoteSimulation node;
	// The rest of the nodes that the current node is connected to (include itself)
	private Set<NodeInformation> connectedNodes = new CopyOnWriteArraySet<NodeInformation>();
	private String groupId = null;

	public ClusterAdministrationImpl(RemoteSimulation node) throws RemoteException {
		super();
		this.node = node;
	}

	@Override
	public void createGroup() throws RemoteException {
		if (isConnectedToGroup())
			throw new IllegalStateException();
		groupId = node.getNodeInformation().id();
		connectedNodes.add(node.getNodeInformation());
	}

	@Override
	public String getGroupId() throws RemoteException {
		return groupId;
	}

	@Override
	public boolean isConnectedToGroup() throws RemoteException {
		return groupId != null;
	}

	
	@Override
	public void connectToGroup(String host, int port) throws RemoteException,
			NotBoundException {
		Registry registry = LocateRegistry.getRegistry(host, port);
		ClusterAdministration cluster = (ClusterAdministration) registry.lookup(Node.CLUSTER_COMUNICATION);
		this.connectedNodes = new CopyOnWriteArraySet<NodeInformation>(cluster.addNewNode(node.getNodeInformation()));
		this.groupId = cluster.getGroupId();
	}

	@Override
	public void disconnectFromGroup(NodeInformation node)
			throws RemoteException, NotBoundException {
		if (connectedNodes.remove(node)) {
			for (NodeInformation connectedNode : connectedNodes) {
				Registry registry = LocateRegistry.getRegistry(connectedNode.host(), connectedNode.port());
				ClusterAdministration cluster = (ClusterAdministration) registry.lookup(Node.CLUSTER_COMUNICATION);
				cluster.disconnectFromGroup(node);
			}
		}
	}
	
	@Override
	public Set<NodeInformation> addNewNode(NodeInformation nodeInformation)
			throws RemoteException, NotBoundException {
		if (!node.getNodeInformation().equals(nodeInformation)
				&& connectedNodes.add(nodeInformation)) {
			if(this.node.isCoordinator())
				this.node.balanceAgents();
			for (NodeInformation connectedNode : connectedNodes) {
				Registry registry = LocateRegistry.getRegistry(connectedNode.host(), connectedNode.port());
				ClusterAdministration cluster = (ClusterAdministration) registry.lookup(Node.CLUSTER_COMUNICATION);
				cluster.addNewNode(nodeInformation);
			}
		}
		return connectedNodes();
	}

	@Override
	public Set<NodeInformation> connectedNodes() throws RemoteException {
		return this.connectedNodes;
	}

}
