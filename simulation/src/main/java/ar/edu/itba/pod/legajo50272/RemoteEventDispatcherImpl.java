package ar.edu.itba.pod.legajo50272;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ar.edu.itba.event.EventInformation;
import ar.edu.itba.event.RemoteEventDispatcher;
import ar.edu.itba.node.Node;
import ar.edu.itba.node.NodeInformation;
import ar.edu.itba.pod.agent.runner.Agent;
import ar.edu.itba.pod.multithread.MultiThreadEventDispatcher;

public class RemoteEventDispatcherImpl extends MultiThreadEventDispatcher implements RemoteEventDispatcher {

	// The events to send
	private BlockingQueue<EventInformation> eventsToSend = new LinkedBlockingQueue<EventInformation>();
	// The history of events
	private List<EventInformation> events = Collections.synchronizedList(new ArrayList<EventInformation>());
	// Current position in the history of events that has to be sent
	private Map<NodeInformation, Integer> indexPerNode = new HashMap<NodeInformation, Integer>();
	// The current node
	private final NodeImpl node;

	private class DispatcherThread extends Thread {

		@Override
		public void run() {
			try {
				while(true){
					EventInformation event = eventsToSend.take();
					synchronized (node.getClusterAdministration().connectedNodes()) {
						for(NodeInformation dest: node.getClusterAdministration().connectedNodes())
							synchronized (indexPerNode) {
								if(indexPerNode.get(dest) == null)
									indexPerNode.put(dest, 0);
								int index = indexPerNode.get(dest);
								if(event.equals(events.get(index))){
									Registry registry = LocateRegistry.getRegistry(dest.host(), dest.port());
									RemoteEventDispatcher remoteEventDispatcher = (RemoteEventDispatcher) registry.lookup(Node.DISTRIBUTED_EVENT_DISPATCHER);
									boolean received = remoteEventDispatcher.publish(event);
									indexPerNode.put(dest, index+1);
									if(!received && Math.random() > 0.6)
										break;
								}
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

	@Override
	public synchronized boolean publish(EventInformation event) throws RemoteException,
			InterruptedException {
		 if(!events.contains(event)){
			// Append the event to the history of events
			this.events.add(event);
			// Add to the queue of events to broadcast
			this.eventsToSend.offer(event);			
			return true;
		}		
		return false;
	}

	@Override
	public Set<EventInformation> newEventsFor(NodeInformation nodeInformation)
			throws RemoteException {
		Set<EventInformation> ans = new HashSet<EventInformation>();
		int length = events.size();
		synchronized (indexPerNode) {
			Integer index = indexPerNode.get(nodeInformation);
			if(index == null || index >= length - 1)
				return null;
			for(int i = index; i < length; i++)
				ans.add(events.get(i));
			indexPerNode.put(nodeInformation, length - 1);
		}
		return ans;
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
