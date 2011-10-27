package ar.edu.itba.pod.legajo50272;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ar.edu.itba.event.EventInformation;
import ar.edu.itba.event.RemoteEventDispatcher;
import ar.edu.itba.node.NodeInformation;
import ar.edu.itba.pod.agent.runner.Agent;

public class RemoteEventDispatcherImpl extends UnicastRemoteObject implements RemoteEventDispatcher {

	// The events for the current node
	private BlockingQueue<EventInformation> events = new LinkedBlockingQueue<EventInformation>();
	// The current node information
	private NodeInformation nodeInformation;

	public RemoteEventDispatcherImpl(NodeInformation nodeInformation) throws RemoteException {
		super();
		this.nodeInformation = nodeInformation;
	}

	@Override
	public void publish(EventInformation event) throws RemoteException,
			InterruptedException {
		this.events.add(event);
	}

	@Override
	public Set<EventInformation> newEventsFor(NodeInformation nodeInformation)
			throws RemoteException {
		if (this.nodeInformation.equals(nodeInformation)) {
			Set<EventInformation> ans = new HashSet<EventInformation>(events);
			events.clear();
			return ans;
		} else {
			try {
				Registry registry = LocateRegistry.getRegistry(
						nodeInformation.host(), nodeInformation.port());
				RemoteEventDispatcher remoteEventDispatcher = (RemoteEventDispatcher) registry
						.lookup("RemoteEventDispatcher");
				return remoteEventDispatcher.newEventsFor(nodeInformation);
			} catch (NotBoundException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	@Override
	public BlockingQueue<Object> moveQueueFor(Agent agent)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

}
