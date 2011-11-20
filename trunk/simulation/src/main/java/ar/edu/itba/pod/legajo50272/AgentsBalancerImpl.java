package ar.edu.itba.pod.legajo50272;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
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
	private CountDownLatch chooseCoordinatorLatch = new CountDownLatch(1);
	
	private volatile boolean electionLive;
	
	private BlockingQueue<BullyEvent> eventsForElection = new LinkedBlockingQueue<BullyEvent>();
	private Set<BullyEvent> electionEvents = Collections.synchronizedSet(new HashSet<BullyEvent>());
	private BlockingQueue<BullyEvent> eventsForCoordinator = new LinkedBlockingQueue<BullyEvent>();
	private Set<BullyEvent> coordinatorEvents = Collections.synchronizedSet(new HashSet<BullyEvent>());
	
	
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
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((node == null) ? 0 : node.hashCode());
			result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
			return result;
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
	
	private class ElectionBroadcastTask implements Runnable {

		@Override
		public void run() {
			try {
				while(true){
					BullyEvent electionEvent = eventsForElection.take();											
					for (NodeInformation dest : node.getConnectedNodes()) 
						if(!dest.equals(node.getNodeInformation())){
							Registry registry = LocateRegistry.getRegistry(dest.host(), dest.port());
							AgentsBalancer agentsBalancer = (AgentsBalancer) registry.lookup(Node.AGENTS_BALANCER);
							agentsBalancer.bullyElection(electionEvent.getNode(), electionEvent.getTimestamp());
						}					
				}
			} catch (InterruptedException e) {
				return;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
	
	private class CoordinatorBroadcastTask implements Runnable {

		@Override
		public void run() {
			try {
				while(true){
					BullyEvent coordinatorEvent = eventsForCoordinator.take();											
					for (NodeInformation dest : node.getConnectedNodes())
						if(!dest.equals(node.getNodeInformation())){
							Registry registry = LocateRegistry.getRegistry(dest.host(), dest.port());
							AgentsBalancer agentsBalancer = (AgentsBalancer) registry.lookup(Node.AGENTS_BALANCER);
							agentsBalancer.bullyCoordinator(coordinatorEvent.getNode(), coordinatorEvent.getTimestamp());
						}
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
		
		node.execute(new ElectionBroadcastTask());
		node.execute(new CoordinatorBroadcastTask());
	}

	@Override
	public void bullyElection(NodeInformation node, long timestamp)
			throws RemoteException {
		BullyEvent electionEvent = new BullyEvent(node, timestamp);
		if(electionEvents.add(electionEvent)){
			if (this.node.getNodeInformation().id().compareTo(node.id()) > 0) {
				Registry registry = LocateRegistry.getRegistry(node.host(),	node.port());
				try {
					AgentsBalancer agentsBalancer = (AgentsBalancer) registry.lookup(Node.AGENTS_BALANCER);
					agentsBalancer.bullyOk(this.node.getNodeInformation());
				} catch (NotBoundException e) {
					e.printStackTrace();
				}
				if(!electionLive)
					chooseCoordinator();
			} else {
				eventsForElection.add(electionEvent);
			}
		}
	}
	
	@Override
	public void bullyOk(NodeInformation node) throws RemoteException {
		electionLive = false;
	}

	@Override
	public void bullyCoordinator(NodeInformation node, long timestamp)
			throws RemoteException {
		BullyEvent coordinatorEvent = new BullyEvent(node, timestamp);
		if(coordinatorEvents.add(coordinatorEvent)){
			this.coordinator = node;
			System.out.println("COORDINATOR: "+this.coordinator);			
			eventsForCoordinator.add(coordinatorEvent);
			
			chooseCoordinatorLatch.countDown();
			chooseCoordinatorLatch = new CountDownLatch(1);
		}
	}

	@Override
	public void shutdown(List<NodeAgent> agents) throws RemoteException,
			NotCoordinatorException {
		synchronized (this) {
			if(!node.isCoordinator())
				throw new NotCoordinatorException(chooseAndGetCoordinator());
				
			if(!agents.isEmpty() && !this.node.getConnectedNodes().isEmpty()) {
				PriorityQueue<EnhancedNodeInformation> agentsQuantPerNode = new PriorityQueue<EnhancedNodeInformation>(10, new AscendantSort());
				for(NodeInformation connectedNode: node.getConnectedNodes()){
					if(!agents.get(0).node().equals(connectedNode)) {
						Registry registry = LocateRegistry.getRegistry(connectedNode.host(), connectedNode.port());
						try {
							AgentsTransfer agentsTransfer = (AgentsTransfer) registry.lookup(Node.AGENTS_TRANSFER);
							EnhancedNodeInformation enhancedNode = new EnhancedNodeInformation(connectedNode, agentsTransfer.getNumberOfAgents());
							agentsQuantPerNode.add(enhancedNode);
						} catch (NotBoundException e) {
							e.printStackTrace();
						}				
					}
				}
				
				if(!agentsQuantPerNode.isEmpty()){
					int assigned = 0;			
					while(assigned < agents.size()) {
						EnhancedNodeInformation enhancedNode = agentsQuantPerNode.remove();
						NodeInformation nodeInformation = enhancedNode.getNodeInformation();
						int agentsQuant = enhancedNode.getAgentsQuant();
						
						Registry registry = LocateRegistry.getRegistry(nodeInformation.host(), nodeInformation.port());
						try {
							AgentsTransfer agentsTransfer = (AgentsTransfer) registry.lookup(Node.AGENTS_TRANSFER);
							agentsTransfer.runAgentsOnNode(Arrays.asList(agents.get(assigned)));
							assigned++;
							agentsQuant++;
							enhancedNode.setAgentsQuant(agentsQuant);
							agentsQuantPerNode.add(enhancedNode);
						} catch (NotBoundException e) {
								e.printStackTrace();
						}
					}
				}
			}
		}
	}

	@Override
	public void addAgentToCluster(NodeAgent agent) throws RemoteException,
			NotCoordinatorException {
		synchronized (this) {		
			if(!node.isCoordinator())
				throw new NotCoordinatorException(chooseAndGetCoordinator());
			
			int min = 0;
			AgentsTransfer randomNodeAgentsTransfer = null;
			try {
				NodeInformation randomNode = node.getConnectedNodes().iterator().next();
				Registry registry = LocateRegistry.getRegistry(randomNode.host(), randomNode.port());
				randomNodeAgentsTransfer = (AgentsTransfer) registry.lookup(Node.AGENTS_TRANSFER);
				min = randomNodeAgentsTransfer.getNumberOfAgents();
			} catch (NotBoundException e) {
				e.printStackTrace();
			}	
			
			boolean assigned = false;
			for(NodeInformation connectedNode: node.getConnectedNodes()){
				Registry registry = LocateRegistry.getRegistry(connectedNode.host(), connectedNode.port());
				try {
					AgentsTransfer agentsTransfer = (AgentsTransfer) registry.lookup(Node.AGENTS_TRANSFER);
					if(agentsTransfer.getNumberOfAgents() < min){
						agentsTransfer.runAgentsOnNode(Arrays.asList(agent));
						assigned = true;
						break;
					}
				} catch (NotBoundException e) {
					e.printStackTrace();
				}
			}
			if(!assigned)
				randomNodeAgentsTransfer.runAgentsOnNode(Arrays.asList(agent));
		}
	}
	
	public void balanceAgents(){
		node.execute(new BalanceTask(this.node));				
	}
	
	private class ChooseCoordinatorTask implements Runnable {
		public void run(){
			try {
				electionLive = true;
				bullyElection(node.getNodeInformation(), System.nanoTime());
				// Wait until all the nodes received the election message
				Thread.sleep(10000);
				// Check if any node has bullied this node
				if(electionLive){
					bullyCoordinator(node.getNodeInformation(), System.nanoTime());
					electionLive = false;
					balanceAgents();
				}
			} catch (InterruptedException e) {
				return;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void chooseCoordinator(){
		this.node.execute(new ChooseCoordinatorTask());
	}	
	
	public synchronized NodeInformation chooseAndGetCoordinator(){
		try {
			if(coordinator == null || !this.node.getConnectedNodes().contains(coordinator)) {
				chooseCoordinator();
				chooseCoordinatorLatch.await();				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return coordinator;
	}
	
	public boolean isCoordinator(){
		return coordinator != null && coordinator.equals(this.node.getNodeInformation());
	}
	
	public class AscendantSort implements Comparator<EnhancedNodeInformation> {

		@Override
		public int compare(EnhancedNodeInformation o1,
				EnhancedNodeInformation o2) {
			return o1.getAgentsQuant() - o2.getAgentsQuant();
		}
		
	}
}
