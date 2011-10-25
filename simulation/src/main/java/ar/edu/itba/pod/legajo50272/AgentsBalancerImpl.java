package ar.edu.itba.pod.legajo50272;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.List;

import ar.edu.itba.balance.api.AgentsBalancer;
import ar.edu.itba.balance.api.AgentsTransfer;
import ar.edu.itba.balance.api.NodeAgent;
import ar.edu.itba.balance.api.NotCoordinatorException;
import ar.edu.itba.node.NodeInformation;

public class AgentsBalancerImpl extends UnicastRemoteObject implements
		AgentsBalancer {

	private AgentsTransfer agentsTransfer;

	protected AgentsBalancerImpl() throws RemoteException {
		super();
		try {
			Registry registry = LocateRegistry.getRegistry("localhost");
			agentsTransfer = (AgentsTransfer) registry.lookup("AgentsTransfer");
		} catch (Exception e) {
			e.printStackTrace();
		}
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

	@Override
	public void addAgentToCluster(NodeAgent agent) throws RemoteException,
			NotCoordinatorException {
		agentsTransfer.runAgentsOnNode(Arrays.asList(agent));
	}

}
