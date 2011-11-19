package ar.edu.itba.pod.legajo50272;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import ar.edu.itba.balance.api.AgentsTransfer;
import ar.edu.itba.balance.api.NodeAgent;
import ar.edu.itba.node.Node;
import ar.edu.itba.node.NodeInformation;

public class BalanceTask implements Runnable {

	private RemoteSimulation node;
	private PriorityQueue<EnhancedNodeInformation> agentsQuantPerNode = new PriorityQueue<EnhancedNodeInformation>(10, new DescendantSort());
	private List<NodeAgent> agentsForNode = new ArrayList<NodeAgent>();
	private List<EnhancedNodeInformation> nodesToReceiveAgents = new ArrayList<EnhancedNodeInformation>();
	private Queue<NodeAgent> agentsToMove = new LinkedList<NodeAgent>();
	
	public BalanceTask(RemoteSimulation node){
		super();
		this.node = node;
	}
	
	@Override
	public void run() {
		try {
			synchronized (this.node.getAgentsBalancer()) {			
				double totalAgents = 0;
				for(NodeInformation connectedNode: node.getConnectedNodes()){
					Registry registry = LocateRegistry.getRegistry(connectedNode.host(), connectedNode.port());
					AgentsTransfer agentsTransfer = (AgentsTransfer) registry.lookup(Node.AGENTS_TRANSFER);
					EnhancedNodeInformation enhancedNode = new EnhancedNodeInformation(connectedNode, agentsTransfer.getNumberOfAgents());
					agentsQuantPerNode.add(enhancedNode);		
					totalAgents += enhancedNode.getAgentsQuant();
				}
				if(totalAgents > 1) {
					int n = (int)Math.ceil(totalAgents/agentsQuantPerNode.size());
					// Quantity of nodes that must have n agents after the balancing
					int a = (int)totalAgents - agentsQuantPerNode.size() * (n - 1);
					System.out.println("A: "+ a);	
					
					while(!agentsQuantPerNode.isEmpty() && agentsQuantPerNode.peek().getAgentsQuant() >= n) {
						EnhancedNodeInformation enhancedNode = agentsQuantPerNode.remove();
						NodeInformation nodeInformation = enhancedNode.getNodeInformation();
						int agentsQuant = enhancedNode.getAgentsQuant();
							
						Registry registry = LocateRegistry.getRegistry(nodeInformation.host(), nodeInformation.port());
						AgentsTransfer agentsTransfer = (AgentsTransfer) registry.lookup(Node.AGENTS_TRANSFER);
						if(a > 0) {
							a--;
							agentsToMove.addAll(agentsTransfer.stopAndGet(agentsQuant - n));
						} else {
							agentsToMove.addAll(agentsTransfer.stopAndGet(agentsQuant - (n - 1)));
						}
					}
					
					while(!agentsQuantPerNode.isEmpty())
						nodesToReceiveAgents.add(agentsQuantPerNode.remove());
					
					for(int i = nodesToReceiveAgents.size() - 1; i >= 0; i--) {
						EnhancedNodeInformation enhancedNode = nodesToReceiveAgents.get(i);
						NodeInformation nodeInformation = enhancedNode.getNodeInformation();
						int agentsQuant = enhancedNode.getAgentsQuant();
						
						Registry registry = LocateRegistry.getRegistry(nodeInformation.host(), nodeInformation.port());
						AgentsTransfer agentsTransfer = (AgentsTransfer) registry.lookup(Node.AGENTS_TRANSFER);
						int min = Math.min(agentsToMove.size(), n - agentsQuant);
						for(int j = 0; j < min; j++)
							agentsForNode.add(agentsToMove.remove());
						agentsTransfer.runAgentsOnNode(agentsForNode);
						agentsForNode.clear();
					}				
				}
				System.out.println("BALANCED");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}				
	}
	
	
	public class DescendantSort implements Comparator<EnhancedNodeInformation> {

		@Override
		public int compare(EnhancedNodeInformation o1,
				EnhancedNodeInformation o2) {
			return o2.getAgentsQuant() - o1.getAgentsQuant();
		}
		
	}
}
