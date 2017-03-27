package com.cacheserverdeploy.deploy;

public class Client implements Comparable<Client> {
	int vertexId;
	int demands;
	public Client(int vertexId, int demands) {
		this.vertexId = vertexId;
		this.demands = demands;
	}
	
	@Override
	public int compareTo(Client that) {
		return this.demands - that.demands;
	}
	
}
