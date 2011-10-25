package ar.edu.itba.pod.legajo50272;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ar.edu.itba.balance.api.AgentsTransfer;
import ar.edu.itba.balance.api.NodeAgent;
import ar.edu.itba.pod.agent.runner.Agent;

public class AgentsTransferImpl extends UnicastRemoteObject implements AgentsTransfer {

	private class AgentThread extends Thread {
		private final Agent agent;
		
		public AgentThread(Agent agent) {
			super(agent.name());
			this.agent = agent;
		}
		
		@Override
		public void run() {
			agent.beforeStart(null);				
			while(true) {
				agent.execute(null);
			}			
		}

		public boolean isRunning(Agent agent) {
			return this.agent.equals(agent);
		}
		
		public Agent getAgent() {
			return agent;
		}
	}
	
	private BlockingQueue<AgentThread> agents = new LinkedBlockingQueue<AgentThread>();
		
	protected AgentsTransferImpl() throws RemoteException {
		super();
	}

	@Override
	public void runAgentsOnNode(List<NodeAgent> agents) throws RemoteException {
		for(NodeAgent nodeAgent: agents){
			AgentThread agentThread = new AgentThread(nodeAgent.agent());
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
