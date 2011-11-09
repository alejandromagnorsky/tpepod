package ar.edu.itba.pod.legajo50272;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import ar.edu.itba.balance.api.AgentsTransfer;
import ar.edu.itba.balance.api.NodeAgent;
import ar.edu.itba.node.Node;
import ar.edu.itba.node.NodeInformation;

public class BalancerTask implements Runnable {

	private RemoteSimulation node;
	private PriorityQueue<EnhancedNodeInformation> agentsQuantPerNode = new PriorityQueue<EnhancedNodeInformation>();
	private List<NodeAgent> agentsForNode = new ArrayList<NodeAgent>();
	private Queue<NodeAgent> agentsToMove = new LinkedList<NodeAgent>();
	
	public class EnhancedNodeInformation implements Comparable<EnhancedNodeInformation> {
		private NodeInformation nodeInformation;
		private int agentsQuant;
		
		public EnhancedNodeInformation(NodeInformation nodeInformation, int agentsQuant) {
			this.nodeInformation = nodeInformation;
			this.agentsQuant = agentsQuant;
		}

		public NodeInformation getNodeInformation() {
			return nodeInformation;
		}

		public Integer getAgentsQuant() {
			return agentsQuant;
		}

		@Override
		public int compareTo(EnhancedNodeInformation o) {
			return o.getAgentsQuant() - this.agentsQuant;
		}
	}
	
	public BalancerTask(RemoteSimulation node){
		super();
		this.node = node;
	}
	
	@Override
	public void run() {
		try {
			while(true){
				Thread.sleep(8000);
				if(node.isCoordinator()){
					for(NodeInformation connectedNode: node.getConnectedNodes()){
						Registry registry = LocateRegistry.getRegistry(connectedNode.host(), connectedNode.port());
						AgentsTransfer agentsTransfer = (AgentsTransfer) registry.lookup(Node.AGENTS_TRANSFER);
						agentsQuantPerNode.add(new EnhancedNodeInformation(connectedNode, agentsTransfer.getNumberOfAgents()));						
					}
					double totalAgents = 0;
					for(EnhancedNodeInformation enhancedNode: agentsQuantPerNode){
						totalAgents += enhancedNode.getAgentsQuant();
					}
					int n = (int)Math.ceil(totalAgents/agentsQuantPerNode.size());
					System.out.println("N: "+ n);
					
					while(!agentsQuantPerNode.isEmpty()) {
						EnhancedNodeInformation enhancedNode = agentsQuantPerNode.remove();
						NodeInformation nodeInformation = enhancedNode.getNodeInformation();
						int agentsQuant = enhancedNode.getAgentsQuant();
						
						if(agentsQuant > n){
							Registry registry = LocateRegistry.getRegistry(nodeInformation.host(), nodeInformation.port());
							AgentsTransfer agentsTransfer = (AgentsTransfer) registry.lookup(Node.AGENTS_TRANSFER);
							agentsToMove.addAll(agentsTransfer.stopAndGet(agentsQuant - n));						
						} else if(agentsQuant < n){
							Registry registry = LocateRegistry.getRegistry(nodeInformation.host(), nodeInformation.port());
							AgentsTransfer agentsTransfer = (AgentsTransfer) registry.lookup(Node.AGENTS_TRANSFER);
							int min = Math.min(agentsToMove.size(), n - agentsQuant);
							for(int i = 0; i < min; i++)
								agentsForNode.add(agentsToMove.remove());
							agentsTransfer.runAgentsOnNode(agentsForNode);
							agentsForNode.clear();
						}
					}
					agentsToMove.clear();
					System.out.println("BALANCED");
				}				
			}
		} catch (InterruptedException e) {
			return;
		} catch (Exception e) {
				e.printStackTrace();
		}				
	}
	
}
