/**
 * 
 */
package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author JwZhou
 *
 */
public class VNS {
	
	
	public static void changeServerOrder() {
		
		for (int i = 0; i < 15; i++) {
			
			List<Node> newServers = new ArrayList<>(Search.servers);
			for (int k = 0; k < Search.servers.size(); k++) {
				for (Edge neighborEdge : Graph.adj[Search.servers.get(k).vertexId]) {
					if (!Graph.nodes[neighborEdge.target].isServer) {
						// Get new servers.
						newServers.set(k, Graph.nodes[neighborEdge.target]);
						newServers.get(k).isServer = true;
						
						Route.removeAllPaths();
						List<Route> newSolution = new ArrayList<>();
						for (Node server : Search.servers) {
							List<Route> allPaths = new ArrayList<>();
							for (Node clientNode : Graph.clientNodes) {
								// Client node that still has demands.
								allPaths.addAll(Route.getShortestPaths(server.vertexId, clientNode.vertexId));
							}
							Collections.sort(allPaths);
							int newCost = 0;
							for (Route path : allPaths) {
								if (Graph.nodes[path.client].demands > 0 && newCost < Graph.serverCost) {
									newCost += Constructive.addPath(path);
									newSolution.add(path);
								}
							}
						}
						if (Search.isFeasible(newSolution) && Search.computerCost(newSolution) < Search.cost) {
							Search.solution = newSolution;
							Search.servers = newServers;
							Search.cost = Search.computerCost(newSolution);
						}
						else {
							Route.removeAllPaths();
							Route.addPaths(Search.solution);
						}
					}
					else {
						continue;
					}
				}
			}
			
		}
		Route.removeAllPaths();
		Route.addPaths(Search.solution);
	}
	
	public static void changePaths() {
		for (int i = 0; i < 500; i++) {
			List<Route> newSolution = new ArrayList<>(Search.solution);
			Collections.shuffle(newSolution);
			Route.removeAllPaths();
			for (Route path : newSolution) {
				path.addPath();
			}
			if (Search.isFeasible(newSolution) && Search.computerCost(newSolution) < Search.cost) {
				Search.solution = newSolution;
				Search.cost = Search.computerCost(newSolution);
			}
			else {
				Route.removeAllPaths();
				Route.addPaths(Search.solution);
			}
		}
	}
}
