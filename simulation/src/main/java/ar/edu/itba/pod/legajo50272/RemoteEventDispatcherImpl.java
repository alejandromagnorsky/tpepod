package ar.edu.itba.pod.legajo50272;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import ar.edu.itba.event.EventInformation;
import ar.edu.itba.event.RemoteEventDispatcher;
import ar.edu.itba.node.Node;
import ar.edu.itba.node.NodeInformation;
import ar.edu.itba.pod.agent.runner.Agent;
import ar.edu.itba.pod.multithread.MultiThreadEventDispatcher;

public class RemoteEventDispatcherImpl extends MultiThreadEventDispatcher implements RemoteEventDispatcher {

	// The events to broadcast
	private BlockingQueue<EventInformation> eventsToSend = new LinkedBlockingQueue<EventInformation>();
	// The history of events
	private BlockingQueue<EventInformation> events = new LinkedBlockingQueue<EventInformation>();
	// Events to broadcast per node
	private Map<NodeInformation, BlockingQueue<EventInformation>> eventsPerNode = new ConcurrentHashMap<NodeInformation, BlockingQueue<EventInformation>>();
	// The current node
	private final NodeImpl node;

	private class DispatcherThread extends Thread {

		@Override
		public void run() {
			try {
				while(true){
					EventInformation event = eventsToSend.take();
					for(NodeInformation dest: eventsPerNode.keySet())
						synchronized (eventsPerNode) {
							if(eventsPerNode.get(dest).peek().equals(event)){
								Registry registry = LocateRegistry.getRegistry(dest.host(), dest.port());
								RemoteEventDispatcher remoteEventDispatcher = (RemoteEventDispatcher) registry.lookup(Node.DISTRIBUTED_EVENT_DISPATCHER);
								boolean received = remoteEventDispatcher.publish(event);
								eventsPerNode.get(dest).poll();
								if(!received && Math.random() > 0.5)
									break;
							}
						}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	public RemoteEventDispatcherImpl(NodeImpl node)
			throws RemoteException {
		super();
		UnicastRemoteObject.exportObject(this, 0);
		this.node = node;
		new DispatcherThread().start();
	}

	// ¿Los nodos agregados deben recibir los eventos anteriores?
	@Override
	public synchronized boolean publish(EventInformation event) throws RemoteException,
			InterruptedException {
		 if(!events.contains(event)){
			synchronized (node.getConnectedNodes()) {
				for(NodeInformation connectedNode: node.getConnectedNodes()){
					if(eventsPerNode.get(connectedNode) == null)
						eventsPerNode.put(connectedNode, new LinkedBlockingQueue<EventInformation>());
					eventsPerNode.get(connectedNode).add(event);
				}				
			}
			// Append the event to the history of events
			this.events.add(event);
			// Add to the queue of events to broadcast
			this.eventsToSend.offer(event);
			return true;
		}		
		return false;
	}

	// ¿Si no tiene nuevos eventos devuelve null?
	@Override
	public Set<EventInformation> newEventsFor(NodeInformation nodeInformation)
			throws RemoteException {
		synchronized (eventsPerNode) {
			if(eventsPerNode.get(nodeInformation) == null || eventsPerNode.get(nodeInformation).isEmpty())
				return null;
			Set<EventInformation> ans = new HashSet<EventInformation>(eventsPerNode.get(nodeInformation));
			eventsPerNode.get(nodeInformation).clear();
			return ans;
		}
	}

	@Override
	public BlockingQueue<Object> moveQueueFor(Agent agent)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void publish(Agent source, Serializable event)
			throws InterruptedException {
		// TODO Auto-generated method stub
		
	}
}
