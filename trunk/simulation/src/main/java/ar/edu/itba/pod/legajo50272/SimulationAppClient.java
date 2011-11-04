package ar.edu.itba.pod.legajo50272;

import org.joda.time.Duration;

import ar.edu.itba.pod.agent.market.AgentState;
import ar.edu.itba.pod.agent.runner.Agent;
import ar.edu.itba.pod.agent.runner.Environment;
import ar.edu.itba.pod.time.TimeMapper;
import ar.edu.itba.pod.time.TimeMappers;

public class SimulationAppClient {

	private static String host = "localhost";
	private static int port = 1099;
	private static String serverHost = "localhost";
	private static int serverPort = 1100;
	private static String id = host + port;
	
	public static void main(String[] args) {
		TimeMapper timeMapper = TimeMappers.oneSecondEach(Duration.standardHours(6));
		NodeImpl node = new NodeImpl(host, port, id, timeMapper);
		node.startServices();
		try {
			node.getClusterAdministration().connectToGroup(serverHost, serverPort);
			RemoteSimulation remoteSimulation = new RemoteSimulation(node);
			for(int i = 0; i < 10; i++){
				final String name = "Agent"+i;
				remoteSimulation.add(new Agent(name){
					@Override
					public void execute(Environment env) {
						System.out.println(super.name());
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

					@Override
					public AgentState state() {
						return null;
					}		
				});
			}			
			remoteSimulation.start(Duration.standardMinutes(10));
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Node started");
	}
}
