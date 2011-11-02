package ar.edu.itba.pod.legajo50272;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import ar.edu.itba.node.Node;
import ar.edu.itba.node.NodeInformation;
import ar.edu.itba.node.api.ClusterAdministration;

public class ClusterAdministrationImpl extends UnicastRemoteObject implements
		ClusterAdministration {

	// The current node
	private NodeImpl node;
	// The rest of the nodes that the current node is connected to
	private Set<NodeInformation> connectedNodes = Collections.synchronizedSet(new HashSet<NodeInformation>());
	private String groupId = null;

	public ClusterAdministrationImpl(NodeImpl node) throws RemoteException {
		super();
		this.node = node;
	}

	@Override
	public void createGroup() throws RemoteException {
		if (isConnectedToGroup())
			throw new IllegalStateException();
		groupId = node.getNodeInformation().id();
	}

	@Override
	public String getGroupId() throws RemoteException {
		return groupId;
	}

	@Override
	public boolean isConnectedToGroup() throws RemoteException {
		return groupId != null;
	}

	
	// ¿Como se obtiene el groupId?
	@Override
	public void connectToGroup(String host, int port) throws RemoteException,
			NotBoundException {
		Registry registry = LocateRegistry.getRegistry(host, port);
		ClusterAdministration cluster = (ClusterAdministration) registry.lookup(Node.CLUSTER_COMUNICATION);
		this.connectedNodes = Collections.synchronizedSet(cluster.addNewNode(node.getNodeInformation()));
	}

	@Override
	public void disconnectFromGroup(NodeInformation node)
			throws RemoteException, NotBoundException {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<NodeInformation> addNewNode(NodeInformation nodeInformation)
			throws RemoteException, NotBoundException {
		if (!node.getNodeInformation().equals(nodeInformation)
				&& !connectedNodes.contains(nodeInformation)) {
			synchronized (connectedNodes) {
				connectedNodes().add(nodeInformation);
				for (NodeInformation connectedNode : connectedNodes) {
					Registry registry = LocateRegistry.getRegistry(connectedNode.host(), connectedNode.port());
					ClusterAdministration cluster = (ClusterAdministration) registry.lookup(Node.CLUSTER_COMUNICATION);
					cluster.addNewNode(nodeInformation);
				}
			}
			return connectedNodes();
		}
		return null;
	}

	@Override
	public Set<NodeInformation> connectedNodes() throws RemoteException {
		return this.connectedNodes;
	}

}
