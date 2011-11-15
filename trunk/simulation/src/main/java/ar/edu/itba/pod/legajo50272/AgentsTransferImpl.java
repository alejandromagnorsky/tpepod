package ar.edu.itba.pod.legajo50272;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import ar.edu.itba.balance.api.AgentsTransfer;
import ar.edu.itba.balance.api.NodeAgent;
import ar.edu.itba.event.RemoteEventDispatcher;
import ar.edu.itba.node.Node;
import ar.edu.itba.node.api.NodeStatistics;
import ar.edu.itba.node.api.StatisticReports;
import ar.edu.itba.pod.agent.market.AgentState;
import ar.edu.itba.pod.agent.runner.Agent;
import ar.edu.itba.pod.multithread.MultiThreadEventDispatcher;

public class AgentsTransferImpl extends UnicastRemoteObject implements AgentsTransfer, StatisticReports {

	// The current node
	private RemoteSimulation node;
	
	public AgentsTransferImpl(RemoteSimulation node) throws RemoteException{
		super();
		this.node = node;
	}
	
	// ¿Coordinacion entre nodos?
	@Override
	public void runAgentsOnNode(List<NodeAgent> agents) throws RemoteException {
		for(NodeAgent nodeAgent : agents){
			if(nodeAgent.node() != null) {
				Registry registry = LocateRegistry.getRegistry(nodeAgent.node().host(), nodeAgent.node().port());
				try {
					RemoteEventDispatcher remoteEventDispatcher = (RemoteEventDispatcher) registry.lookup(Node.DISTRIBUTED_EVENT_DISPATCHER);
					BlockingQueue<Object> agentEvents = remoteEventDispatcher.moveQueueFor(nodeAgent.agent());
					((MultiThreadEventDispatcher)node.dispatcher()).setAgentQueue(nodeAgent.agent(), agentEvents);
				} catch (NotBoundException e) {
					e.printStackTrace();
				}			
			}
		}
		
		for (NodeAgent nodeAgent : agents)
			node.add(nodeAgent.agent());
	}

	@Override
	public int getNumberOfAgents() throws RemoteException {
		return node.agentsRunning();
	}

	@Override
	public List<NodeAgent> stopAndGet(int numberOfAgents)
			throws RemoteException {
		List<NodeAgent> ans = new ArrayList<NodeAgent>();
		synchronized (this.node) {
			List<Agent> agents = node.getAgentsRunning();
			for (int i = 0; i < numberOfAgents; i++) {
				ans.add(new NodeAgent(node.getNodeInformation(), agents.get(i)));
				node.remove(agents.get(i));
			}
		}
		return ans;
	}
	
	@Override
	public NodeStatistics getNodeStatistics() throws RemoteException {
		List<AgentState> agentsStates = new ArrayList<AgentState>();
		for (Agent agent : node.getAgentsRunning())
			agentsStates.add(agent.state());
		return new NodeStatistics(node.agentsRunning(), agentsStates);
	}
}
