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
	private NodeInformation coordinator;
	private boolean electionLive;
		
	protected AgentsBalancerImpl(NodeImpl node) throws RemoteException {
		super();
		this.node = node;
	}

	
	// ¿Cual es el flujo para la seleccion de un coordinador?
	// ¿Para que se usa el timestamp?
	// ¿Condicion para terminar de broadcastear?
	@Override
	public void bullyElection(NodeInformation node, long timestamp)
			throws RemoteException {
		NodeInformation coordinatorCandidate = node;
		if(this.node.getNodeInformation().id().compareTo(coordinatorCandidate.id()) > 0){
			Registry registry = LocateRegistry.getRegistry(node.host(), node.port());
			try {
				AgentsBalancer agentsBalancer = (AgentsBalancer) registry.lookup(Node.AGENTS_BALANCER);			
				agentsBalancer.bullyOk(this.node.getNodeInformation());
			} catch (NotBoundException e) {
				e.printStackTrace();
			}
			electionLive = true;
			coordinatorCandidate = this.node.getNodeInformation();
		}
		
		for(NodeInformation dest: this.node.getConnectedNodes()){
			if(coordinatorCandidate.equals(this.node.getNodeInformation()) && !electionLive)
				break;
			Registry registry = LocateRegistry.getRegistry(dest.host(), dest.port());
			try {
				AgentsBalancer agentsBalancer = (AgentsBalancer) registry.lookup(Node.AGENTS_BALANCER);			
				agentsBalancer.bullyElection(coordinatorCandidate, System.currentTimeMillis());
			} catch (NotBoundException e) {
				e.printStackTrace();
			}
		}
		
		this.bullyCoordinator(coordinatorCandidate, System.currentTimeMillis());		
	}

	@Override
	public void bullyOk(NodeInformation node) throws RemoteException {
		electionLive = false;
	}

	@Override
	public void bullyCoordinator(NodeInformation node, long timestamp)
			throws RemoteException {
		this.coordinator = node;
	}

	@Override
	public void shutdown(List<NodeAgent> agents) throws RemoteException,
			NotCoordinatorException {
		// TODO Auto-generated method stub

	}

	// ¿Falta un setter de NodeAgent para el NodeInformation?
	@Override
	public void addAgentToCluster(NodeAgent agent) throws RemoteException,
			NotCoordinatorException {
		if(!this.node.equals(coordinator))
			throw new NotCoordinatorException(coordinator);
		
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
