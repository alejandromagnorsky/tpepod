package ar.edu.itba.pod.legajo50272;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ar.edu.itba.balance.api.AgentsTransfer;
import ar.edu.itba.balance.api.NodeAgent;
import ar.edu.itba.pod.multithread.AgentThread;
import ar.edu.itba.pod.multithread.EventDispatcher;

public class AgentsTransferImpl extends UnicastRemoteObject implements AgentsTransfer {

	private BlockingQueue<AgentThread> agents = new LinkedBlockingQueue<AgentThread>();
	// The current node
	private NodeImpl node;
	
	public AgentsTransferImpl(NodeImpl node) throws RemoteException {
		super();
		this.node = node;
	}

	@Override
	public void runAgentsOnNode(List<NodeAgent> agents) throws RemoteException {
		for(NodeAgent nodeAgent: agents){
			AgentThread agentThread = new AgentThread(node.getTimeMapper(), (EventDispatcher) node.getRemoteEventDispatcher(), nodeAgent.agent());
			this.agents.add(agentThread);
			agentThread.start();
		}
	}


	@Override
	public int getNumberOfAgents() throws RemoteException {
		return this.agents.size();
	}


	@Override
	public List<NodeAgent> stopAndGet(int numberOfAgents)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	
}
