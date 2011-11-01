package ar.edu.itba.pod.legajo50272;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

import org.joda.time.Duration;

import ar.edu.itba.balance.api.AgentsBalancer;
import ar.edu.itba.balance.api.NodeAgent;
import ar.edu.itba.node.Node;
import ar.edu.itba.pod.agent.runner.Agent;
import ar.edu.itba.pod.agent.runner.Simulation;
import ar.edu.itba.pod.multithread.EventDispatcher;
import ar.edu.itba.pod.multithread.LocalSimulation;
import ar.edu.itba.pod.time.TimeMapper;

public class RemoteSimulation extends LocalSimulation implements Simulation {

	private AgentsBalancer agentsBalancer;
	private RemoteEventDispatcherImpl remoteEventDispatcher;
	
	
	public RemoteSimulation(TimeMapper timeMapper){
		super(timeMapper);
		try {
			Registry registry = LocateRegistry.getRegistry("localhost");
			agentsBalancer = (AgentsBalancer) registry.lookup(Node.AGENTS_BALANCER);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public EventDispatcher dispatcher() {
		return this.remoteEventDispatcher;
	}
	
	@Override
	public void add(Agent agent) {
		if(agent == null){
			System.out.println("Agent cannot be null");
			return;
		}
		NodeAgent nodeAgent = new NodeAgent(null, agent);
		try {
			agentsBalancer.addAgentToCluster(nodeAgent);
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

	@Override
	public void remove(Agent agent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void start(Duration duration) {
		// TODO Auto-generated method stub
		
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
