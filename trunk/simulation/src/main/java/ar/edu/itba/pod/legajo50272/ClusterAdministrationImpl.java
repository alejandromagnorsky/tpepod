package ar.edu.itba.pod.legajo50272;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Set;

import ar.edu.itba.node.NodeInformation;
import ar.edu.itba.node.api.ClusterAdministration;

public class ClusterAdministrationImpl extends UnicastRemoteObject implements
		ClusterAdministration {

	// The information of the current node
	private NodeInformation nodeInformation;
	// The rest of the nodes that the current node is connected to
	private Set<NodeInformation> connectedNodes = new HashSet<NodeInformation>();
	private String groupId = null;


	public ClusterAdministrationImpl(NodeInformation nodeInformation)
			throws RemoteException {
		super();
		this.nodeInformation = nodeInformation;
	}

	@Override
	public void createGroup() throws RemoteException {
		if (isConnectedToGroup())
			throw new IllegalStateException();
		groupId = nodeInformation.id();
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
		try {
			Registry registry = LocateRegistry.getRegistry(host, port);
			ClusterAdministration cluster = (ClusterAdministration) registry.lookup("ClusterAdministration");
			this.connectedNodes = cluster.addNewNode(nodeInformation);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void disconnectFromGroup(NodeInformation node)
			throws RemoteException, NotBoundException {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<NodeInformation> addNewNode(NodeInformation node)
			throws RemoteException, NotBoundException {
		connectedNodes().add(node);
		return connectedNodes();
	}

	@Override
	public Set<NodeInformation> connectedNodes() throws RemoteException {
		return this.connectedNodes;
	}

}
