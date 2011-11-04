package ar.edu.itba.pod.legajo50272;

import org.joda.time.Duration;

import ar.edu.itba.pod.agent.market.Consumer;
import ar.edu.itba.pod.agent.market.Market;
import ar.edu.itba.pod.agent.market.Producer;
import ar.edu.itba.pod.agent.market.Resource;
import ar.edu.itba.pod.time.TimeMapper;
import ar.edu.itba.pod.time.TimeMappers;

public class SimulationAppClient {

	private static String host = "10.6.0.101";
	private static int port = 1099;
	private static String serverHost = "10.6.0.44";
	private static int serverPort = 1100;
	private static String id = host + port;

	public static void main(String[] args) {
		TimeMapper timeMapper = TimeMappers.oneSecondEach(Duration
				.standardHours(6));
		RemoteSimulation remoteSimulation = new RemoteSimulation(host, port,
				id, serverHost, serverPort, timeMapper);

		Resource gold = new Resource("Mineral", "Gold");
		Resource copper = new Resource("Mineral", "Copper");
		Resource steel = new Resource("Alloy", "Steel");

		for (int i = 0; i < 1; i++) {
			remoteSimulation.add(new Producer("steel mine" + i, steel, Duration
					.standardDays(1), 5));
			remoteSimulation.add(new Producer("copper mine" + i, copper,
					Duration.standardDays(1), 10));
			remoteSimulation.add(new Producer("gold mine" + i, gold, Duration
					.standardDays(1), 1));

			remoteSimulation.add(new Consumer("steel consumer" + i, steel,
					Duration.standardDays(3), 2));
			remoteSimulation.add(new Consumer("copper consumer" + i, copper,
					Duration.standardHours(8), 2));
			remoteSimulation.add(new Consumer("gold consumer" + i, gold,
					Duration.standardDays(2), 4));
		}

		remoteSimulation.add(new Market("gold market", gold));
		remoteSimulation.add(new Market("cooper market", copper));
		remoteSimulation.add(new Market("steel market", steel));

		remoteSimulation.start(Duration.standardMinutes(10));

		System.out.println("Node started");
	}
}
