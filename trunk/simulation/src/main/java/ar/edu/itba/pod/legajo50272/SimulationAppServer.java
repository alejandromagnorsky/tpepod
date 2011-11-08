package ar.edu.itba.pod.legajo50272;

import org.joda.time.Duration;

import ar.edu.itba.pod.time.TimeMapper;
import ar.edu.itba.pod.time.TimeMappers;

public class SimulationAppServer {

	private static String host = "localhost";
	private static int port = 1100;
	private static String id = host + port;
	
	public static void main(String[] args) {
		TimeMapper timeMapper = TimeMappers.oneSecondEach(Duration.standardHours(6));
		RemoteSimulation remoteSimulation = new RemoteSimulation(host, port, id, timeMapper);
		System.out.println("Server started");
	}
}
