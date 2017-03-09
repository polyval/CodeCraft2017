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
						
						List<Route> allPaths = new ArrayList<>();
						for (Node server : newServers) {
							for (Node clientNode : Graph.clientNodes) {
								allPaths.addAll(Route.getShortestPaths(server.vertexId, clientNode.vertexId));
							}
						}
//						Collections.sort(allPaths);
						if(isBetter(allPaths)) {
							Search.servers = newServers;
							changePathsSwapFirst(allPaths);
							break;
						}
					
					}
					else {
						continue;
					}
				}
			}
			
		}
	}
	
	public static void moveServer() {
		
		boolean findBetter = true;
		while (findBetter) {
			findBetter = false;
			for (int i = 0; i < Search.servers.size(); i++) {
				// Choose one server to move out.
				List<Node> newServers = new ArrayList<>(Search.servers);
				for (Node node : Graph.nodes) {
					if (Search.servers.contains(node)) {
						continue;
					}
					newServers.set(i, node);
					List<Route> newSolution = getBestSolutionGivenServers(newServers);
					if (newSolution != null) {
						findBetter = true;
						Search.servers = newServers;
						Search.solution = newSolution;
						Search.cost = Search.computerCost(newSolution);
						break;
					}
				}
			}
			if (!findBetter) {
				break;
			}
		}
		
	}
	
	public static void moveServerVNS() {
		// Number of servers to change.
		int k = 1;
		boolean findBetter = true;
		while (findBetter && k <= Search.servers.size()) {
			findBetter = false;
			// Collections of indices of server to move out.
			List<ArrayList<Integer>> neighborhood = Util.combine(Search.servers.size(), k);
			// Collections of indices of nodes to move in.
			List<ArrayList<Integer>> moveInNodes = Util.getNodesCombinations(Search.servers, k);
			for (ArrayList<Integer> neighbor : neighborhood) {
					
					for (List<Integer> moveInNodesId : moveInNodes) {
						List<Node> newServers = new ArrayList<>(Search.servers);
						// Get new servers
						for (int i = 0; i < k; i++) {
							newServers.set(neighbor.get(i), Graph.nodes[moveInNodesId.get(i)]);
						}
						
						List<Route> newSolution = getBestSolutionGivenServers(newServers);
						if (newSolution != null) {
							findBetter = true;
							Search.servers = newServers;
							Search.solution = newSolution;
							Search.cost = Search.computerCost(newSolution);
							break;
						}
					}
					
					if (findBetter) {
						break;
					}
			}
			if (!findBetter) {
				findBetter = true;
				k++;
			}
		}
		
	}
	
	public static List<Route> getBestSolutionGivenServers(List<Node> servers) {
		List<Route> newSolution = new ArrayList<>();
		for (Node server : servers) {
			for (Node clientNode : Graph.clientNodes) {
				newSolution.addAll(Route.getShortestPaths(server.vertexId, clientNode.vertexId));
			}
		}
		Collections.sort(newSolution);
		if (isBetter(newSolution)) {
			return newSolution;
		}
		return null;
	}
	
	public static void changePathsRandom() {
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
	
	public static void changePathsSwapFirst(List<Route> solution) {
		List<Route> newSolution = new ArrayList<>(solution);
		
		boolean findBetter = true;
		while (findBetter) {
			findBetter = false;
			for(int i = 0; i < newSolution.size() - 1; i++) {
				for (int j = 0; j < newSolution.size(); j++) {
					Collections.swap(newSolution, i, j);
					if (isBetter(newSolution)) {
						Search.solution = newSolution;
						Search.cost = Search.computerCost(newSolution);
						findBetter = true;
						break;
					}
					Collections.swap(newSolution, i, j);
				}
				if (findBetter) {
					break;
				}
			}
			
			if (!findBetter) {
				break;
			}
		}
	}
	
	public static void changePathsSwapBest() {
		List<Route> newSolution = new ArrayList<>(Search.solution);
		
		
		boolean findBetter = true;
		
		while (findBetter) {
			findBetter = false;
			
			int cost = Search.cost;
			int[] bestSwap = new int[2];
			
			for(int i = 0; i < newSolution.size() - 1; i++) {
				for (int j = 0; j < newSolution.size(); j++) {
					Collections.swap(newSolution, i, j);
					if (isBetter(newSolution)) {
						findBetter = true;
						if (Search.cost < cost) {
							bestSwap[0] = i;
							bestSwap[1] = j;
						}
					}
					Collections.swap(newSolution, i, j);
				}
			}
			
			if (!findBetter) {
				break;
			}
			Collections.swap(newSolution, bestSwap[0], bestSwap[1]);
		}
	}
	
	public static boolean isBetter(List<Route> newSolution) {
		Route.removeAllPaths();
		Route.addPaths(newSolution);
		if (Search.isFeasible(newSolution) && Search.computerCost(newSolution) < Search.cost) {
			return true;
		}
		Route.removeAllPaths();
		Route.addPaths(Search.solution);
		return false;
	}
}
