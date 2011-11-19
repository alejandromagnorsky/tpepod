package ar.edu.itba.pod.legajo50272;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.joda.time.Duration;

import ar.edu.itba.balance.api.AgentsBalancer;
import ar.edu.itba.balance.api.AgentsTransfer;
import ar.edu.itba.balance.api.NodeAgent;
import ar.edu.itba.balance.api.NotCoordinatorException;
import ar.edu.itba.event.EventInformation;
import ar.edu.itba.event.RemoteEventDispatcher;
import ar.edu.itba.node.Node;
import ar.edu.itba.node.NodeInformation;
import ar.edu.itba.node.api.ClusterAdministration;
import ar.edu.itba.pod.agent.runner.Agent;
import ar.edu.itba.pod.agent.runner.Simulation;
import ar.edu.itba.pod.multithread.AgentThread;
import ar.edu.itba.pod.multithread.EventDispatcher;
import ar.edu.itba.pod.multithread.LocalSimulation;
import ar.edu.itba.pod.time.TimeMapper;

public class RemoteSimulation extends LocalSimulation implements Simulation,
		Node {

	// The current node information
	private NodeInformation nodeInformation;
	private ClusterAdministration clusterAdministration;
	private RemoteEventDispatcher remoteEventDispatcher;
	private AgentsBalancer agentsBalancer;
	private AgentsTransfer agentsTransfer;
	private ExecutorService executor = Executors.newCachedThreadPool();

	// Creates a server node
	public RemoteSimulation(String host, int port, String id,
			TimeMapper timeMapper) {
		super(timeMapper);
		initServices(host, port, id);
		try {
			clusterAdministration.createGroup();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	// Creates a client node
	public RemoteSimulation(String host, int port, String id,
			String serverHost, int serverPort, TimeMapper timeMapper) {
		super(timeMapper);
		initServices(host, port, id);
		try {
			clusterAdministration.connectToGroup(serverHost, serverPort);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initServices(String host, int port, String id) {
		nodeInformation = new NodeInformation(host, port, id);
		try {
			clusterAdministration = new ClusterAdministrationImpl(this);
			remoteEventDispatcher = new RemoteEventDispatcherImpl(this);
			agentsBalancer = new AgentsBalancerImpl(this);
			agentsTransfer = new AgentsTransferImpl(this);
			Registry registry = LocateRegistry.createRegistry(nodeInformation.port());
			registry.bind(CLUSTER_COMUNICATION, clusterAdministration);
			registry.bind(DISTRIBUTED_EVENT_DISPATCHER, remoteEventDispatcher);
			registry.bind(AGENTS_BALANCER, agentsBalancer);
			registry.bind(AGENTS_TRANSFER, agentsTransfer);
			registry.bind(STATISTIC_REPORTS, agentsTransfer);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public EventDispatcher dispatcher() {
		return (EventDispatcher) remoteEventDispatcher;
	}
	
	public void addAgentToCluster(Agent agent) {
		addAgentToCluster(agent, this.chooseAndGetCoordinator());
	}
	
	private void addAgentToCluster(Agent agent, NodeInformation coordinator) {
		try {
			Registry registry = LocateRegistry.getRegistry(coordinator.host(), coordinator.port());
			AgentsBalancer agentsBalancer = (AgentsBalancer) registry.lookup(Node.AGENTS_BALANCER);
			agentsBalancer.addAgentToCluster(new NodeAgent(null, agent));
		} catch (NotCoordinatorException e) {
			e.printStackTrace();
			this.addAgentToCluster(agent, e.getNewCoordinator());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void addAgentThread(AgentThread thread) {
		synchronized (this) {
			super.addAgentThread(thread);
		}
	}

	@Override
	public void start(final Duration duration) {
		synchronized (this) {
			super.start(duration);
		}
	}
	
	@Override
	public void stop() {
		try {
			shutdown(this.chooseAndGetCoordinator());
			clusterAdministration.disconnectFromGroup(this.getNodeInformation());
			executor.shutdownNow();
			executor.awaitTermination(5, TimeUnit.SECONDS);
			super.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void shutdown(NodeInformation coordinator) {
		try {
			Registry registry = LocateRegistry.getRegistry(coordinator.host(), coordinator.port());
			AgentsBalancer agentsBalancer = (AgentsBalancer) registry.lookup(Node.AGENTS_BALANCER);
			List<NodeAgent> nodeAgents = new ArrayList<NodeAgent>();
			for(Agent agent: super.getAgentsRunning())
				nodeAgents.add(new NodeAgent(this.getNodeInformation(), agent));
			agentsBalancer.shutdown(nodeAgents);
		} catch (NotCoordinatorException e) {
			e.printStackTrace();
			this.shutdown(e.getNewCoordinator());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}	
	
	public void execute(Runnable task) {
		executor.execute(task);
	}
	
	public NodeInformation getNodeInformation() {
		return nodeInformation;
	}
	
	public Set<NodeInformation> getConnectedNodes() throws RemoteException {
		return clusterAdministration.connectedNodes();
	}

	public List<NodeAgent> stopAndGet(int numberOfAgents) throws RemoteException {
		return agentsTransfer.stopAndGet(numberOfAgents);
	}
	
	public RemoteEventDispatcher getRemoteEventDispatcher() {
		return remoteEventDispatcher;
	}

	public void balanceAgents() {
		((AgentsBalancerImpl)agentsBalancer).balanceAgents();	
	}
	
	public NodeInformation chooseAndGetCoordinator() {
		return ((AgentsBalancerImpl)agentsBalancer).chooseAndGetCoordinator();
	}
	
	public boolean isCoordinator() {
		return ((AgentsBalancerImpl)agentsBalancer).isCoordinator();
	}
}
