package ar.edu.itba.pod.legajo50272;

import java.rmi.RemoteException;

import org.joda.time.Duration;

import ar.edu.itba.pod.time.TimeMapper;
import ar.edu.itba.pod.time.TimeMappers;

public class SimulationAppServer {

	private static String host = "localhost";
	private static int port = 1100;
	private static String id = host + port;
	
	public static void main(String[] args) {
		TimeMapper timeMapper = TimeMappers.oneSecondEach(Duration.standardHours(6));
		NodeImpl node = new NodeImpl(host, port, id, timeMapper);
		node.startServices();
		try {
			node.getClusterAdministration().createGroup();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		System.out.println("Server started");
	}
}
