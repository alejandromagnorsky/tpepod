package ar.edu.itba.pod.legajo50272;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ar.edu.itba.balance.api.AgentsBalancer;
import ar.edu.itba.balance.api.AgentsTransfer;
import ar.edu.itba.balance.api.NodeAgent;
import ar.edu.itba.balance.api.NotCoordinatorException;
import ar.edu.itba.node.Node;
import ar.edu.itba.node.NodeInformation;

public class AgentsBalancerImpl extends UnicastRemoteObject implements
		AgentsBalancer {

	// The current node
	private final RemoteSimulation node;
	// The coordinator of the cluster
	private NodeInformation coordinator;
		
	private volatile boolean electionLive;
	private BlockingQueue<BullyEvent> eventsForElection = new LinkedBlockingQueue<BullyEvent>();
	private BlockingQueue<BullyEvent> electionEvents = new LinkedBlockingQueue<BullyEvent>();
	
	
	private class BullyEvent {
		private NodeInformation node;
		private long timestamp;
		
		public BullyEvent(NodeInformation node, long timestamp){
			this.node = node;
			this.timestamp = timestamp;
		}

		public NodeInformation getNode() {
			return node;
		}

		public long getTimestamp() {
			return timestamp;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			BullyEvent other = (BullyEvent) obj;
			if (node == null) {
				if (other.node != null)
					return false;
			} else if (!node.equals(other.node))
				return false;
			if (timestamp != other.timestamp)
				return false;
			return true;
		}
	}
	
	
	private class ElectionTask implements Runnable {

		@Override
		public void run() {
			try {
				while(true){
					BullyEvent electionEvent = eventsForElection.take();
											
					for (NodeInformation dest : node.getConnectedNodes()) {
						if(!dest.equals(node.getNodeInformation())){
							Registry registry = LocateRegistry.getRegistry(dest.host(), dest.port());
							AgentsBalancer agentsBalancer = (AgentsBalancer) registry.lookup(Node.AGENTS_BALANCER);
							agentsBalancer.bullyElection(electionEvent.getNode(), electionEvent.getTimestamp());
						}
					}
				}
			} catch (InterruptedException e) {
				return;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	private class ChooseCoordinatorTask implements Runnable {

		@Override
		public void run() {
			try {
				System.out.println("CHOOSING");
				electionLive = true;
				bullyElection(node.getNodeInformation(), System.nanoTime());
				// Wait until all the nodes received the election message
				Thread.sleep(2000);
				// Check if any node has bullied this node
				if(electionLive){
					bullyCoordinator(node.getNodeInformation(), System.nanoTime());
					electionLive = false;
				}
			} catch (InterruptedException e) {
				return;
			} catch (Exception e) {
					e.printStackTrace();
			}
		}
	}

	public AgentsBalancerImpl(RemoteSimulation node) throws RemoteException {
		super();
		this.node = node;
		node.execute(new ElectionTask());
	}

	// ¿Cual es el flujo para la seleccion de un coordinador?
	@Override
	public synchronized void bullyElection(NodeInformation node, long timestamp)
			throws RemoteException {
		BullyEvent electionEvent = new BullyEvent(node, timestamp);
		if(!electionEvents.contains(electionEvent)){
			electionEvents.add(electionEvent);			
			if (this.node.getNodeInformation().id().compareTo(node.id()) > 0) {
				Registry registry = LocateRegistry.getRegistry(node.host(),	node.port());
				try {
					AgentsBalancer agentsBalancer = (AgentsBalancer) registry.lookup(Node.AGENTS_BALANCER);
					agentsBalancer.bullyOk(this.node.getNodeInformation());
				} catch (NotBoundException e) {
					e.printStackTrace();
				}
				chooseCoordinator();
			} else {
				System.out.println("BULLY EVENT: "+ electionEvent.getNode()+","+electionEvent.getTimestamp());
				eventsForElection.add(electionEvent);
			}			
		}
	}
	
	public void chooseCoordinator(){
		node.execute(new ChooseCoordinatorTask());
	}

	@Override
	public void bullyOk(NodeInformation node) throws RemoteException {
		electionLive = false;
		System.out.println("BULLY OK: " + node);
	}

	@Override
	public void bullyCoordinator(NodeInformation node, long timestamp)
			throws RemoteException {
		System.out.println("COORDINADOR: "+node);
		this.coordinator = node;
	}

	@Override
	public void shutdown(List<NodeAgent> agents) throws RemoteException,
			NotCoordinatorException {
		// TODO Auto-generated method stub

	}

	@Override
	public void addAgentToCluster(NodeAgent agent) throws RemoteException,
			NotCoordinatorException {
		// if(!this.node.equals(coordinator))
		// throw new NotCoordinatorException(coordinator);

		List<NodeInformation> clusterNodes = new ArrayList<NodeInformation>(node.getConnectedNodes());
		NodeInformation selectedNode = clusterNodes.get((int) Math.floor(Math.random() * clusterNodes.size()));

		Registry registry = LocateRegistry.getRegistry(selectedNode.host(), selectedNode.port());
		try {
			AgentsTransfer agentsTransfer = (AgentsTransfer) registry.lookup(Node.AGENTS_TRANSFER);
			agentsTransfer.runAgentsOnNode(Arrays.asList(agent));
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
	}

}
