package ar.edu.itba.pod.legajo50272;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.util.Properties;

import org.joda.time.Duration;

import ar.edu.itba.node.NodeInformation;
import ar.edu.itba.pod.agent.market.Consumer;
import ar.edu.itba.pod.agent.market.Market;
import ar.edu.itba.pod.agent.market.Producer;
import ar.edu.itba.pod.agent.market.Resource;
import ar.edu.itba.pod.agent.runner.Agent;
import ar.edu.itba.pod.time.TimeMapper;
import ar.edu.itba.pod.time.TimeMappers;

public class SimulationAppNode {

	private static BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
	private static String host;
	private static Integer port;
	private static String serverHost;
	private static Integer serverPort;
	private static RemoteSimulation remoteSimulation;
	private static Resource gold;
	private static Resource steel;
	private static Resource copper;

	public static void main(String[] args) {
		TimeMapper timeMapper = TimeMappers.oneSecondEach(Duration.standardHours(24));
		String line, values[];

		gold = new Resource("Mineral", "Gold");
		copper = new Resource("Mineral", "Copper");
		steel = new Resource("Alloy", "Steel");
		System.out.println("Enter the path of the file wich contains the network configuration");
		loadFile(readLine());

		if (host == null || port == null) {
			System.out.println("Enter your host and your port (host:port)");
			line = readLine();
			values = line.split(":");
			host = values[0];
			port = Integer.valueOf(values[1]);
		}
		String id = host + ":" + port;

		System.out.println("Node id: "+ id);
		System.out.println("Choose between create a group or connect to group (s/c)");
		line = readLine();
		if (line.equals("s")) 
			remoteSimulation = new RemoteSimulation(host, port, id, timeMapper);
		else {
			if (serverHost == null || serverPort == null) {
				System.out.println("Enter host and port from the entry node (host:port)");
				line = readLine();
				values = line.split(":");
				serverHost = values[0];
				serverPort = Integer.valueOf(values[1]);
			}
			remoteSimulation = new RemoteSimulation(host, port, id, serverHost, serverPort, timeMapper);
		}

		remoteSimulation.start(Duration.standardMinutes(100));

		System.out.println("Type help to see the instructions");
		
		Thread parserThread = new Thread() {
			
			@Override
			public void run() {
				String line, values[];
				while (true) {
					try {
						line = readLine();
						values = line.split(" ");
						if (line.equals("status"))
							displayStatistics();
						else if (line.equals("help"))
							displayInstructions();
						else if(line.equals("shutdown")){
							shutdown();
							System.exit(0);
						}
						else if (values[0].equals("add"))
							addAgent(values[1].split(","));			
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		};
		parserThread.setDaemon(true);
		parserThread.start();
	}
	
	
	private static void displayStatistics(){
		System.out.println("----------------------------------------------------");
		System.out.println("QUANTITY OF AGENTS RUNNING IN THIS NODE");
		System.out.println(remoteSimulation.agentsRunning());
		System.out.println("AGENTS RUNNING IN THIS NODE");
		for(Agent agent: remoteSimulation.getAgentsRunning())
			System.out.println(agent);
		System.out.println("CONNECTED NODES");
		try {
			for(NodeInformation connectedNode: remoteSimulation.getConnectedNodes())
				System.out.println(connectedNode);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		System.out.println("----------------------------------------------------");
	}
	
	private static void addAgent(String values[]) {
		String agentName = null;
		Agent agent = null;
		int agentNumber = (int) Math.floor(Math.random() * 100);
		String agentType = values[0];
		
		if(values.length == 1){
			if (agentType.equals("cm")) {
				agentName = "copper market" + agentNumber;
				agent = new Market(agentName, copper);
			} else if (agentType.equals("sm")) {
				agentName = "steel market" + agentNumber;
				agent = new Market(agentName, steel);
			} else if (agentType.equals("gm")) {
				agentName = "gold market" + agentNumber;
				agent = new Market(agentName, gold);
			}
		} else {
			Duration rate = Duration.standardDays(Integer.valueOf(values[1]));
			int amount = Integer.valueOf(values[2]);				
			if (agentType.equals("cc")) {
				agentName = "copper consumer" + agentNumber;
				agent = new Consumer(agentName, copper, rate, amount);
			} else if (agentType.equals("sc")) {
				agentName = "steel consumer" + agentNumber;
				agent = new Consumer(agentName, steel, rate, amount);
			} else if (agentType.equals("gc")) {
				agentName = "gold consumer" + agentNumber;
				agent = new Consumer(agentName, gold, rate, amount);
			} else if (agentType.equals("cp")) {
				agentName = "copper mine" + agentNumber;
				agent = new Producer(agentName, copper, rate, amount);
			} else if (agentType.equals("sp")) {
				agentName = "steel mine" + agentNumber;
				agent = new Producer(agentName, steel, rate, amount);
			} else if (agentType.equals("gp")) {
				agentName = "gold mine" + agentNumber;
				agent = new Producer(agentName, gold, rate, amount);
			}
		}
		if(agent != null){
			remoteSimulation.addAgentToCluster(agent);
			System.out.println("Added " + agentName);
		}
	}
	
	private static void shutdown() {
		System.out.println("SHUTTING DOWN");
		remoteSimulation.stop();
		System.out.println("FINISHED");		
	}
	
	private static String readLine() {
		try {
			return stdin.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static void loadFile(String line) {
		Properties properties = new Properties();
		if(line == null || line.isEmpty())
			line = "src/main/resources/config.properties";
		try {
			properties.load(new FileInputStream(line));
			host = properties.getProperty("host");
			port = Integer.valueOf(properties.getProperty("port"));
			serverHost = properties.getProperty("serverHost");
			serverPort = Integer.valueOf(properties.getProperty("serverPort"));
		} catch (Exception e) {
			System.out.println("Error loading configuration file");
		}
	}

	private static void displayInstructions() {
		System.out.println("Commands");
		System.out.println("- status");
		System.out.println("	Display the agents running in this node and the nodes in the cluster");
		System.out.println("- add");
		System.out.println("	Add agent to the simulation specifying type");
		System.out.println("	If the agent is a consumer or a producer, include rate in days and the amount of resources");
		System.out.println("	Type of agents:");
		System.out.println("		Consumers: cc:CooperConsumer / sc:SteelConsumer / gc:GoldConsumer");
		System.out.println("		Producers: cp:CopperMine / sp:SteelMine / gp:GoldMine");
		System.out.println("		Markets: cm:CopperMarket / sm:SteelMarket / gm:GoldMarket");
		System.out.println("	Example: add cc,1,5");
		System.out.println("- shutdown");
		System.out.println("    Stop and redistribute the agents running in this node");
	}
}
