package ar.edu.itba.pod.legajo50272;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
    private List<EventInformation> events = Collections.synchronizedList(new ArrayList<EventInformation>());
	// Current position in the history of events that has to be sent
    private Map<NodeInformation, Integer> indexPerNode = new ConcurrentHashMap<NodeInformation, Integer>();
	// The current node
	private final RemoteSimulation node;
	
	private class DispatcherTask implements Runnable {

		@Override
		public void run() {
			try {
				while(true){
					EventInformation event = eventsToSend.take();
					for(NodeInformation dest: indexPerNode.keySet())
						synchronized (indexPerNode) {
							int index = indexPerNode.get(dest);
							if(event.equals(events.get(index))){
								Registry registry = LocateRegistry.getRegistry(dest.host(), dest.port());
								RemoteEventDispatcher remoteEventDispatcher = (RemoteEventDispatcher) registry.lookup(Node.DISTRIBUTED_EVENT_DISPATCHER);
								boolean received = remoteEventDispatcher.publish(event);
								indexPerNode.put(dest, index+1);
								if(!received && Math.random() > 0.5)
									break;
							}
						}
				}
			} catch (InterruptedException e) {
				return;
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private class CheckerTask implements Runnable {

		@Override
		public void run() {
			try {
				while(true){
					Thread.sleep(1000);
					synchronized (node.getConnectedNodes()) {
						List<NodeInformation> connectedNodes = new ArrayList<NodeInformation>(node.getConnectedNodes());
						if(connectedNodes.size() > 1){
							connectedNodes.remove(node.getNodeInformation());						
							NodeInformation dest = connectedNodes.get((int)Math.floor(Math.random()*connectedNodes.size()));
							Registry registry = LocateRegistry.getRegistry(dest.host(), dest.port());
							RemoteEventDispatcher remoteEventDispatcher = (RemoteEventDispatcher) registry.lookup(Node.DISTRIBUTED_EVENT_DISPATCHER);
							for(EventInformation newEvent: remoteEventDispatcher.newEventsFor(node.getNodeInformation()))
								publish(newEvent);
						}
					}
				}
			} catch (InterruptedException e) {
				return;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	

	public RemoteEventDispatcherImpl(RemoteSimulation node)
			throws RemoteException {
		super();
		UnicastRemoteObject.exportObject(this, 0);
		this.node = node;
		node.execute(new DispatcherTask());
		node.execute(new CheckerTask());
	}

	@Override
	public synchronized boolean publish(EventInformation event) throws RemoteException,
			InterruptedException {
		 if(!events.contains(event)){
			// Append the event to the history of events
			this.events.add(event);
			// Add to the queue of events to broadcast
			this.eventsToSend.offer(event);
			// Publish the event locally
			super.publish(event.source(), event.event());
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
            if(index == null){
            	indexPerNode.put(nodeInformation, 0);
            	index = 0;
            }
            if(index > length - 1)
            	return ans;
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
		EventInformation eventInformation = new EventInformation(event, this.node.getNodeInformation().id(), source);
		eventInformation.setReceivedTime(System.nanoTime());
		try {
			publish(eventInformation);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
}
