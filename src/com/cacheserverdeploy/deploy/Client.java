package com.cacheserverdeploy.deploy;

import java.util.HashMap;
import java.util.Map;

public class Client implements Comparable<Client> {
	public static Map<Integer, Client> idToClient = new HashMap<Integer, Client>();
	int vertexId;
	int demands;
	public Client(int vertexId, int demands) {
		this.vertexId = vertexId;
		this.demands = demands;
		idToClient.put(vertexId, this);
	}
	
	public static Client getClient(int vertexId) {
		return idToClient.get(vertexId);
	}
	
	public int getOutput() {
		int output = 0;
		for (Edge e : Graph.adj[vertexId]) {
			output += e.bandwidth;
		}
		return output;
	}
	
	@Override
	public int compareTo(Client that) {
		return this.demands - that.demands;
	}
	
}
