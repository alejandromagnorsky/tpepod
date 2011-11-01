package ar.edu.itba.pod.legajo50272;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ar.edu.itba.event.EventInformation;
import ar.edu.itba.event.RemoteEventDispatcher;
import ar.edu.itba.node.Node;
import ar.edu.itba.node.NodeInformation;
import ar.edu.itba.pod.agent.runner.Agent;
import ar.edu.itba.pod.multithread.EventDispatcher;

public class RemoteEventDispatcherImpl extends UnicastRemoteObject implements
		EventDispatcher, RemoteEventDispatcher {

	// The events for the current node that must be broadcasted
	private BlockingQueue<EventInformation> eventsToSend = new LinkedBlockingQueue<EventInformation>();
	// The events for the current node that have been broadcasted
	private BlockingQueue<EventInformation> eventsSended = new LinkedBlockingQueue<EventInformation>();
	private Map<NodeInformation, BlockingQueue<EventInformation>> eventsToSendPerNode = new HashMap<NodeInformation, BlockingQueue<EventInformation>>();
	// The current node information
	private final NodeImpl node;

	private class DispatcherThread extends Thread {

		@Override
		public void run() {
			try {
				EventInformation event = eventsToSend.take();
				for(NodeInformation nodeInformation: node.getClusterAdministration().connectedNodes()){
					Registry registry = LocateRegistry.getRegistry(nodeInformation.host(), nodeInformation.port());
					RemoteEventDispatcher remoteEventDispatcher = (RemoteEventDispatcher) registry.lookup(Node.AGENTS_TRANSFER);
					remoteEventDispatcher.publish(event);
				}
				
				eventsSended.add(event);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

	public RemoteEventDispatcherImpl(NodeImpl node)
			throws RemoteException {
		super();
		this.node = node;
		new DispatcherThread().start();
	}

	@Override
	public void publish(EventInformation event) throws RemoteException,
			InterruptedException {
		this.eventsToSend.offer(event);
	}

	@Override
	public Set<EventInformation> newEventsFor(NodeInformation nodeInformation)
			throws RemoteException {
		Set<EventInformation> events = new HashSet<EventInformation>(eventsToSendPerNode.get(nodeInformation));
		eventsToSendPerNode.get(nodeInformation).clear();
		return events;
	}

	@Override
	public BlockingQueue<Object> moveQueueFor(Agent agent)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deregister(Agent agent, Class<? extends Serializable> eventType) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void publish(Agent source, Serializable event)
			throws InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void register(Agent agent, Class<? extends Serializable> eventType) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <T extends Serializable> T waitFor(Agent agent, Class<T> baseType)
			throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

}
