package com.cacheserverdeploy.deploy;

import java.util.HashMap;
import java.util.Map;

public class Node implements Comparable<Node> {
	public static Map<Integer, Node> idToNode = new HashMap<Integer, Node>();
	int vertexId;
	int demands;
	int cost;
	public Node(int vertexId, int demands, int cost) {
		this.vertexId = vertexId;
		this.demands = demands;
		this.cost = cost;
		idToNode.put(vertexId, this);
	}
	
	public static Node getNode(int vertexId) {
		return idToNode.get(vertexId);
	}
	
	public int getOutput() {
		int output = 0;
		for (Edge e : Graph.adj[vertexId]) {
			output += e.bandwidth;
		}
		return output + demands;
	}
	
	public int getDeployCost() {
		return cost + Graph.diffServerCost.get(Search.initialTypes.get(vertexId));
	}
	
	@Override
	public int compareTo(Node that) {
//		return that.cost - this.cost;
		return this.demands - that.demands;
	}
	
}
