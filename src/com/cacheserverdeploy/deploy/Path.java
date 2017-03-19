/**
 * 
 */
package com.cacheserverdeploy.deploy;

import java.util.List;

/**
 * @author JwZhou
 *
 */
public class Path {
	
	public List<Integer> nodes;
	public int flow;
	public int clientId;
	
	public Path(List<Integer> nodes, int flow) {
		this.nodes = nodes;
		this.flow = flow;
		this.clientId = Graph.vertexToClient.get(nodes.get(nodes.size() - 1));
	}
	
	@Override
	public String toString() {
		return nodes.toString().replace("[", "").replace("]", "").replace(",", "") + " " + clientId + " " + flow;
	}
}
