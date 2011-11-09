package ar.edu.itba.pod.legajo50272;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ar.edu.itba.balance.api.AgentsBalancer;
import ar.edu.itba.balance.api.AgentsTransfer;
import ar.edu.itba.event.RemoteEventDispatcher;
import ar.edu.itba.node.Node;
import ar.edu.itba.node.NodeInformation;
import ar.edu.itba.node.api.ClusterAdministration;
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
	private ExecutorService executor = Executors.newFixedThreadPool(4);

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
			Registry registry = LocateRegistry.createRegistry(nodeInformation
					.port());
			registry.bind(CLUSTER_COMUNICATION, clusterAdministration);
			registry.bind(DISTRIBUTED_EVENT_DISPATCHER, remoteEventDispatcher);
			registry.bind(AGENTS_BALANCER, agentsBalancer);
			registry.bind(AGENTS_TRANSFER, agentsTransfer);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public EventDispatcher dispatcher() {
		return (EventDispatcher) remoteEventDispatcher;
	}

	@Override
	protected void addAgentThread(AgentThread thread){
		synchronized (this) {
			super.addAgentThread(thread);
		}
	}	
		
	public Set<NodeInformation> getConnectedNodes() throws RemoteException {
		return clusterAdministration.connectedNodes();
	}

	public NodeInformation getNodeInformation() {
		return nodeInformation;
	}
	
	public AgentsTransfer getAgentsTransfer(){
		return agentsTransfer;
	}
	
	public AgentsBalancer getAgentsBalancer(){
		return agentsBalancer;
	}

	public void execute(Runnable task) {
		executor.execute(task);
	}
}
