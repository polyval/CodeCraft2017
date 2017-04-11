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
	public int serverType;
	
	public Path(List<Integer> nodes, int flow) {
		this.nodes = nodes;
		this.flow = flow;
		this.clientId = Graph.vertexToClient.get(nodes.get(nodes.size() - 1));
		this.serverType = Search.bestServerTypes.get(Search.bestServers.indexOf((Integer.valueOf(nodes.get(0)))));
	}
	
	@Override
	public String toString() {
		return nodes.toString().replace("[", "").replace("]", "").replace(",", "") + " " + clientId + " " + flow + " " + serverType;
	}
}
