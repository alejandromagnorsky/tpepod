package ar.edu.itba.pod.legajo50272;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.joda.time.Duration;

import ar.edu.itba.pod.agent.market.Consumer;
import ar.edu.itba.pod.agent.market.Market;
import ar.edu.itba.pod.agent.market.Producer;
import ar.edu.itba.pod.agent.market.Resource;
import ar.edu.itba.pod.agent.runner.Agent;
import ar.edu.itba.pod.time.TimeMapper;
import ar.edu.itba.pod.time.TimeMappers;

public class SimulationAppNode {

	private static BufferedReader stdin = new BufferedReader(
			new InputStreamReader(System.in));

	public static void main(String[] args) {
		TimeMapper timeMapper = TimeMappers.oneSecondEach(Duration
				.standardHours(6));
		RemoteSimulation remoteSimulation;
		String line, values[], agentName;
		Agent agent;
		int agentNumber;
		Resource gold = new Resource("Mineral", "Gold");
		Resource copper = new Resource("Mineral", "Copper");
		Resource steel = new Resource("Alloy", "Steel");

		System.out.println("Enter your host and your port (host:port)");
		line = readLine();
		values = line.split(":");
		String host = values[0];
		int port = Integer.valueOf(values[1]);
		String id = host + port;

		System.out
				.println("Choose between create a group or connect to group (s/c)");
		line = readLine();
		if (line.equals("s")) {
			remoteSimulation = new RemoteSimulation(host, port, id, timeMapper);
			remoteSimulation.add(new Market("gold market", gold));
			remoteSimulation.add(new Market("cooper market", copper));
			remoteSimulation.add(new Market("steel market", steel));
		} else {
			System.out
					.println("Enter host and port from the entry node (host:port)");
			line = readLine();
			values = line.split(":");
			String serverHost = values[0];
			int serverPort = Integer.valueOf(values[1]);
			remoteSimulation = new RemoteSimulation(host, port, id, serverHost,
					serverPort, timeMapper);
		}

		remoteSimulation.start(Duration.standardMinutes(10));

		System.out
				.println("Available type of agents: cc:CooperConsumer / sc:SteelConsumer / gc:GoldConsumer / cm:CopperMine / sm:SilverMine / gm:GoldMine");
		System.out.println("Add agents: add type,rate,amount");
		while (true) {
			try {
				line = readLine();
				values = line.split(" ");
				if (values[0].equals("add")) {
					values = values[1].split(",");
					String agentType = values[0];
					Duration rate = Duration.standardDays(Integer
							.valueOf(values[1]));
					int amount = Integer.valueOf(values[2]);
					agentNumber = (int) Math.floor(Math.random() * 100);
					if (agentType.equals("cc")) {
						agentName = "copper consumer" + agentNumber;
						agent = new Consumer(agentName, copper, rate, amount);
					} else if (agentType.equals("sc")) {
						agentName = "steel consumer" + agentNumber;
						agent = new Consumer(agentName, steel, rate, amount);
					} else if (agentType.equals("gc")) {
						agentName = "gold consumer" + agentNumber;
						agent = new Consumer(agentName, gold, rate, amount);
					} else if (agentType.equals("cm")) {
						agentName = "copper mine" + agentNumber;
						agent = new Producer(agentName, copper, rate, amount);
					} else if (agentType.equals("sm")) {
						agentName = "silver mine" + agentNumber;
						agent = new Producer(agentName, steel, rate, amount);
					} else {
						agentName = "gold mine" + agentNumber;
						agent = new Producer(agentName, gold, rate, amount);
					}
					remoteSimulation.add(agent);
					System.out.println("Added " + agentName);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	private static String readLine() {
		try {
			return stdin.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
