package com.cacheserverdeploy.deploy;

import java.util.HashMap;
import java.util.Map;

public class Node implements Comparable<Node> {
	public static Map<Integer, Node> idToNode = new HashMap<Integer, Node>();
	int vertexId;
	int demands;
	public Node(int vertexId, int demands) {
		this.vertexId = vertexId;
		this.demands = demands;
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
		return output;
	}
	
	@Override
	public int compareTo(Node that) {
		return this.demands - that.demands;
	}
	
}
