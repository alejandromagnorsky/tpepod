package ar.edu.itba.pod.legajo50272;

import ar.edu.itba.node.NodeInformation;

public class EnhancedNodeInformation {

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

	public void setAgentsQuant(int agentQuant) {
		this.agentsQuant = agentQuant;
	}
}
