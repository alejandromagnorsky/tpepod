package ar.edu.itba.pod.legajo50272;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ar.edu.itba.balance.api.AgentsBalancer;
import ar.edu.itba.balance.api.AgentsTransfer;
import ar.edu.itba.balance.api.NodeAgent;
import ar.edu.itba.balance.api.NotCoordinatorException;
import ar.edu.itba.node.Node;
import ar.edu.itba.node.NodeInformation;

public class AgentsBalancerImpl extends UnicastRemoteObject implements
		AgentsBalancer {

	// The current node
	private NodeImpl node;
		
	protected AgentsBalancerImpl(NodeImpl node) throws RemoteException {
		super();
		this.node = node;
	}

	@Override
	public void bullyElection(NodeInformation node, long timestamp)
			throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void bullyOk(NodeInformation node) throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void bullyCoordinator(NodeInformation node, long timestamp)
			throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void shutdown(List<NodeAgent> agents) throws RemoteException,
			NotCoordinatorException {
		// TODO Auto-generated method stub

	}

	// �Falta un setter de NodeAgent para el NodeInformation?
	// �ConnectedNodes incluye al nodo local?
	@Override
	public void addAgentToCluster(NodeAgent agent) throws RemoteException,
			NotCoordinatorException {
		List<NodeInformation> clusterNodes = new ArrayList<NodeInformation>(node.getConnectedNodes());
		NodeInformation selectedNode = clusterNodes.get((int)Math.floor(Math.random()*clusterNodes.size()));
		
		Registry registry = LocateRegistry.getRegistry(selectedNode.host(), selectedNode.port());
		try {
			AgentsTransfer agentsTransfer = (AgentsTransfer) registry.lookup(Node.AGENTS_TRANSFER);
			agentsTransfer.runAgentsOnNode(Arrays.asList(agent));
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
	}

}