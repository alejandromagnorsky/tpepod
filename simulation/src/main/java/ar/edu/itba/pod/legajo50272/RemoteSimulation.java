package ar.edu.itba.pod.legajo50272;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ar.edu.itba.balance.api.AgentsBalancer;
import ar.edu.itba.balance.api.AgentsTransfer;
import ar.edu.itba.balance.api.NodeAgent;
import ar.edu.itba.event.RemoteEventDispatcher;
import ar.edu.itba.node.Node;
import ar.edu.itba.node.NodeInformation;
import ar.edu.itba.node.api.ClusterAdministration;
import ar.edu.itba.node.api.NodeStatistics;
import ar.edu.itba.node.api.StatisticReports;
import ar.edu.itba.pod.agent.market.AgentState;
import ar.edu.itba.pod.agent.runner.Agent;
import ar.edu.itba.pod.agent.runner.Simulation;
import ar.edu.itba.pod.multithread.EventDispatcher;
import ar.edu.itba.pod.multithread.LocalSimulation;
import ar.edu.itba.pod.time.TimeMapper;

public class RemoteSimulation extends LocalSimulation implements Simulation, Node, AgentsTransfer, StatisticReports {

	// The current node information
	private NodeInformation nodeInformation;
	private ClusterAdministration clusterAdministration;
	private RemoteEventDispatcher remoteEventDispatcher;
	private AgentsBalancer agentsBalancer;
	
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
	
	private void initServices(String host, int port, String id){
		nodeInformation = new NodeInformation(host, port, id);
		try {
			clusterAdministration = new ClusterAdministrationImpl(this);
			remoteEventDispatcher = new RemoteEventDispatcherImpl(this);
			agentsBalancer = new AgentsBalancerImpl(this);
			Registry registry = LocateRegistry.createRegistry(nodeInformation.port());		
			registry.bind(CLUSTER_COMUNICATION, clusterAdministration);
			registry.bind(DISTRIBUTED_EVENT_DISPATCHER, remoteEventDispatcher);
			registry.bind(AGENTS_TRANSFER, this);
			registry.bind(AGENTS_BALANCER, agentsBalancer);
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	@Override
	public EventDispatcher dispatcher() {
		return (EventDispatcher) remoteEventDispatcher;
	}

	public void chooseCoordinator(){
		try {
			((AgentsBalancerImpl) agentsBalancer).chooseCoordinator();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void runAgentsOnNode(List<NodeAgent> agents) throws RemoteException {
		for(NodeAgent nodeAgent: agents)
			add(nodeAgent.agent());		
	}

	@Override
	public int getNumberOfAgents() throws RemoteException {
		return agentsRunning();
	}

	@Override
	public List<NodeAgent> stopAndGet(int numberOfAgents)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}
	
	public Set<NodeInformation> getConnectedNodes() throws RemoteException{
		return clusterAdministration.connectedNodes();
	}
	
	public NodeInformation getNodeInformation(){
		return nodeInformation;
	}

	@Override
	public NodeStatistics getNodeStatistics() throws RemoteException {
		List<AgentState> agentsStates = new ArrayList<AgentState>();
		for(Agent agent: this.getAgentsRunning())
			agentsStates.add(agent.state());
		return new NodeStatistics(this.agentsRunning(), agentsStates);
	}
}
