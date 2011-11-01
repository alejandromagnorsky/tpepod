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
	// The current node
	private final NodeImpl node;

	private class DispatcherThread extends Thread {

		@Override
		public void run() {
			try {
				while(true){
					EventInformation event = eventsToSend.take();
					int quant = 0;
					synchronized (node.getClusterAdministration().connectedNodes()) {
						int total = node.getClusterAdministration().connectedNodes().size();					
						for(NodeInformation nodeInformation: node.getClusterAdministration().connectedNodes()){
							Registry registry = LocateRegistry.getRegistry(nodeInformation.host(), nodeInformation.port());
							RemoteEventDispatcher remoteEventDispatcher = (RemoteEventDispatcher) registry.lookup(Node.AGENTS_TRANSFER);
							if(remoteEventDispatcher.publish(event))
								quant++;
							if(quant > total/2)
								break;
						}
					}
					eventsSended.add(event);
				}
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

	// ¿Solo se puede procesar los de eventsSended?
	// ¿Las que ya fueron procesadas deben borrarse de eventsSended?
	// ¿En que momento se carga eventsToSendPerNode?
	// ¿Los metodos de la interfaz EventDispatcher tienen que usar los metodos de la interfaz RemoteEventDispatcher?
	@Override
	public boolean publish(EventInformation event) throws RemoteException,
			InterruptedException {
		if(!eventsToSend.contains(event) && !eventsSended.contains(event)){
			this.eventsToSend.offer(event);
			return true;
		}
		return false;
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
