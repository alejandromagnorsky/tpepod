package ar.edu.itba.pod.legajo50272;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.joda.time.Duration;

import ar.edu.itba.balance.api.NodeAgent;
import ar.edu.itba.pod.agent.runner.Agent;
import ar.edu.itba.pod.agent.runner.Simulation;
import ar.edu.itba.pod.multithread.EventDispatcher;
import ar.edu.itba.pod.multithread.LocalSimulation;

public class RemoteSimulation extends LocalSimulation implements Simulation {

	// The current node
	private NodeImpl node;
	private BlockingQueue<NodeAgent> agents;

	public RemoteSimulation(NodeImpl node) {
		super(node.getTimeMapper());
		this.node = node;
		this.agents = new LinkedBlockingQueue<NodeAgent>();
	}

	@Override
	public EventDispatcher dispatcher() {
		return (EventDispatcher) node.getRemoteEventDispatcher();
	}

	@Override
	public void add(Agent agent) {
		if (agent == null) {
			System.out.println("Agent cannot be null");
			return;
		}
		agents.add(new NodeAgent(null, agent));
	}

	@Override
	public void remove(Agent agent) {
		// TODO Auto-generated method stub

	}

	// ¿Getters para los variables de LocalSimulation?
	@Override
	public void start(Duration duration) {
		synchronized (agents) {
			for (NodeAgent agent : agents)
				try {
					node.getAgentsBalancer().addAgentToCluster(agent);
				} catch (Exception e) {
					e.printStackTrace();
				}
		}
	}

	@Override
	public void startAndWait(Duration duration) throws InterruptedException {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop() throws InterruptedException {
		// TODO Auto-generated method stub

	}

	@Override
	public int agentsRunning() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<Agent> getAgentsRunning() {
		// TODO Auto-generated method stub
		return null;
	}

}
