/**
 * 
 */
package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.filetool.util.LogUtil;

/**
 * @author JwZhou
 *
 */
public class VNS {
	
	
	public static void moveServer() {
		
		boolean findBetter = true;
		while (findBetter) {
			findBetter = false;
			for (int i = 0; i < Search.servers.size(); i++) {
				// Choose one server to move out.
				List<Node> newServers = new ArrayList<Node>(Search.servers);
				for (Node node : Graph.nodes) {
					if (System.nanoTime() - Search.startTime / 1000000 > 89000) {
						return;
					}
					if (Search.servers.contains(node)) {
						continue;
					}
					newServers.set(i, node);
					List<Route> newSolution = getBestSolutionGivenServers(newServers);
					if (newSolution != null) {
//						int cost = Search.cost;
//						changePathsSwapFirst(newSolution);
//						if (Search.cost < cost) {
//							findBetter = true;
//							Search.servers = newServers;
//							Search.updateSolution(newSolution);
//							System.out.println("new best cost by move server " + Search.cost);
//							break;
//						}
						findBetter = true;
						Search.servers = newServers;
						Search.updateSolution(newSolution);
						System.out.println("new best cost by move server " + Search.cost);
						break;
					}
				}
			}
			if (!findBetter) {
				break;
			}
		}
		
	}
	
	public static void moveTwoServer() {
		
		boolean findBetter = true;
		while (findBetter) {
			findBetter = false;
			// Collections of indices of server to move out.
			List<ArrayList<Integer>> neighborhood = Util.combine(Search.servers.size(), 2);
			// Collections of indices of nodes to move in.
			List<ArrayList<Integer>> moveInNodes = Util.getNodesCombinations(Search.servers, 2);
			
			for (ArrayList<Integer> neighbor : neighborhood) {
				
				for (List<Integer> moveInNodesId : moveInNodes) {
					if (System.nanoTime() - Search.startTime / 1000000 > 89000) {
						return;
					}
					List<Node> newServers = new ArrayList<Node>(Search.servers);
					// Get new servers
					for (int i = 0; i < 2; i++) {
						newServers.set(neighbor.get(i), Graph.nodes[moveInNodesId.get(i)]);
					}
					
					List<Route> newSolution = getBestSolutionGivenServers(newServers);
					if (newSolution != null) {
						int cost = Search.cost;
						changePathsSwapFirst(newSolution);
						if (Search.cost < cost) {
							findBetter = true;
							Search.servers = newServers;
							Search.updateSolution(newSolution);
							System.out.println("new best cost by move two servers, new servers " + Search.servers);
							System.out.println("new best cost by move two servers " + Search.cost);
							break;
						}
					}
				}
			}
			
			if (!findBetter) {
				break;
			}
		}
		
	}

	public static void moveThreeServer() {
	
		boolean findBetter = true;
		while (findBetter) {
			findBetter = false;
			// Collections of indices of server to move out.
			List<ArrayList<Integer>> neighborhood = Util.combine(Search.servers.size(), 3);
			// Collections of indices of nodes to move in.
			List<ArrayList<Integer>> moveInNodes = Util.getNodesCombinations(Search.servers, 3);
			
			for (ArrayList<Integer> neighbor : neighborhood) {
				
				for (List<Integer> moveInNodesId : moveInNodes) {
					if (System.nanoTime() - Search.startTime / 1000000 > 89000) {
						return;
					}
					List<Node> newServers = new ArrayList<Node>(Search.servers);
					// Get new servers
					for (int i = 0; i < 3; i++) {
						newServers.set(neighbor.get(i), Graph.nodes[moveInNodesId.get(i)]);
					}
					
					List<Route> newSolution = getBestSolutionGivenServers(newServers);
					if (newSolution != null) {
						findBetter = true;
						Search.servers = newServers;
						Search.updateSolution(newSolution);
						System.out.println("new best cost by move three servers " + Search.cost);
						changePathsSwapFirst(Search.solution);
						break;
					}
				}
			}
			
			if (!findBetter) {
				break;
			}
		}
	}
	
	public static void dropServer() {
		Map<Integer, List<Route>> serverPaths = new HashMap<Integer, List<Route>>();
		Map<Integer, Set<Integer>> droppedClients = new HashMap<Integer, Set<Integer>>(); 
		
		// Get up to date solution.
		Search.refreshSolution();
		for (Route path : Search.solution) {
			if (!serverPaths.containsKey(path.server)) {
				List<Route> route = new ArrayList<Route>();
				Set<Integer> clients = new HashSet<Integer>();
				route.add(path);
				clients.add(path.client);
 				serverPaths.put(path.server, route);
 				droppedClients.put(path.server, clients);
 				
			}
			else {
				serverPaths.get(path.server).add(path);
				droppedClients.get(path.server).add(path.client);
			}
		}
		
		for (int i = 0; i < Search.servers.size(); i++) {
			// Drop a server.
			int serverId = Search.servers.get(i).vertexId;
			
			System.out.println("drop server " + serverId);
			
			// Remove paths from network
			Constructive.restorePaths(serverPaths.get(serverId));
			// Get the new solution.
			List<Route> newSolution = new ArrayList<Route>(Search.solution);
			newSolution.removeAll(serverPaths.get(serverId));
			
			List<Integer> changedBandwidths = new ArrayList<Integer>();
			
			for (Route path : newSolution) {
				changedBandwidths.add(path.occupiedBandwidth);
			}
			
			// Clients that connect to the dropped server.
			List<Integer> lostClients = new ArrayList<Integer>(droppedClients.get(serverId));
			
			for (int j = 0; j < lostClients.size(); j++) {
				// Get paths from remaining servers to one lost client.
				int clientId = lostClients.get(j);
				List<Route> pathsToClients = new ArrayList<Route>();
				
				if (Graph.nodes[clientId].demands == 0) {
					continue;
				}
				
				for (int k = 0; k < Search.servers.size(); k++) {
					// Can not be dropped server.
					if (k == i) {
						continue;
					}
					pathsToClients.addAll(Route.getShortestPaths(Search.servers.get(k).vertexId, clientId));
				}
				Collections.sort(pathsToClients);
				for (Route path : pathsToClients) {
					if (path.maxBandwidth == 0 || path.maxBandwidth * path.averageCost > Graph.serverCost) {
						continue;
					}
					if (Graph.nodes[clientId].demands == 0) {
						break;
					}
					if (!newSolution.contains(path)) {
						newSolution.add(path);
					}
					path.addPath();
				}
			}
			
			if (Search.isFeasible(newSolution)) {
				System.out.println("drop server " + serverId + " is feasible");
				int cost = Search.cost;
				//TODO: This will change the occupied bandwidth of path in new solution.
				changePathsSwapFirst(newSolution);
				if (Search.cost < cost) {
					Search.servers.remove(i);
					break;
				}
			}
			else {
				System.out.println("drop server " + serverId + " is unfeasible");
				
				List<Node> newServerCandidates = getPromisingNodes();
				
//				for ()
//				
				
			}
		}
	}
	
	public static void addServerAndPaths() {
		
	}
	
	public static void moveServerVNS() {
		// Number of servers to change.
		int k = 1;
		boolean findBetter = true;
		while (findBetter && k <= 3) {
			findBetter = false;
			// Collections of indices of server to move out.
			List<ArrayList<Integer>> neighborhood = Util.combine(Search.servers.size(), k);
			// Collections of indices of nodes to move in.
			List<ArrayList<Integer>> moveInNodes = Util.getNodesCombinations(Search.servers, k);
			for (ArrayList<Integer> neighbor : neighborhood) {
					
					for (List<Integer> moveInNodesId : moveInNodes) {
						List<Node> newServers = new ArrayList<Node>(Search.servers);
						// Get new servers
						for (int i = 0; i < k; i++) {
							newServers.set(neighbor.get(i), Graph.nodes[moveInNodesId.get(i)]);
						}
						
						List<Route> newSolution = getBestSolutionGivenServers(newServers);
						if (newSolution != null) {
							findBetter = true;
							Search.servers = newServers;
							Search.updateSolution(newSolution);
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
		List<Route> newSolution = new ArrayList<Route>();
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
	
	// Not better than getBestSolutionGivenServers by cost.
	public static List<Route> getBestSolutionGivenServersStartFromClients(List<Node> servers) {
		List<Route> newSolution = new ArrayList<Route>();
		Route.removeAllPaths();
		for (Node clientNode : Graph.clientNodes) {
			for (Node server : servers) {
				for (Route path : Route.getShortestPaths(server.vertexId, clientNode.vertexId)) {
					// No need to add this path.
					if (clientNode.demands == 0 || path.averageCost * path.maxBandwidth >= Graph.serverCost) {
						break;
					}
					path.addPath();
					newSolution.add(path);
				}
			}
		}
		if (isBetter(newSolution)) {
			return newSolution;
		}
		return null;
	}
	
	public static void changePathsRandom(List<Route> solution) {
		List<Route> newSolution = new ArrayList<Route>(solution);
		for (int i = 0; i < 1000; i++) {
			Collections.shuffle(newSolution);
			if (isBetter(newSolution)) {
				Search.updateSolution(newSolution);
				System.out.println("new best cost by change paths randomly " + Search.cost);
			}
		}
	}
	
	public static void changePathsSwapFirst(List<Route> solution) {
		List<Route> newSolution = new ArrayList<Route>(solution);
		
		boolean findBetter = true;
		while (findBetter) {
			findBetter = false;
			for(int i = 0; i < newSolution.size() - 1; i++) {
				for (int j = 0; j < newSolution.size(); j++) {
					Collections.swap(newSolution, i, j);
					if (isBetter(newSolution)) {
						Search.updateSolution(newSolution);
						System.out.println("new best cost by change paths swap first " + Search.cost);
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
	
	public static void reinsert(List<Route> solution) {
		List<Route> newSolution = new ArrayList<Route>(solution);
		// Indicate whether the station has been inserted.
		boolean inserted = false;
		int i = 0; 
		// Didn't reinsert the last one.
		while (i < newSolution.size() - 1) {
			List<Route> leftPaths = new LinkedList<Route>(newSolution.subList(0, i));
			List<Route> rightPaths = new LinkedList<Route>(newSolution.subList(i + 1, newSolution.size()));
			// The Station to try the insertion.
			Route insertionRoute = newSolution.get(i);
	
			inserted = false;
			int rightSize = rightPaths.size();
			for (int j = 1; j < rightSize + 1; j++) {
				// Insert the station afterwards.
				rightPaths.add(j, insertionRoute);
				// Get the stations after insertion.
				List<Route> joinedPaths = new LinkedList<Route>(leftPaths);
				joinedPaths.addAll(rightPaths);
				// If a cost reduction is found.
				if (isBetter(joinedPaths)) {
					newSolution = joinedPaths;
					Search.updateSolution(newSolution);
					System.out.println("new improvement by reinsert " + Search.cost);
					inserted = true;
					break;
				}
				rightPaths.remove(j);
			}
			
			if (inserted == false) {
				i++;
			}
		}
		
	}
	
	public static void changePathsSwapBest(List<Route> solution) {
		List<Route> newSolution = new ArrayList<Route>(solution);
		
		
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
						if (Search.computerCost(newSolution) < cost) {
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
			Search.updateSolution(newSolution);
		}
	}
	
	public static boolean isBetter(List<Route> newSolution) {
		Search.reset();
		Route.addPaths(newSolution);
		if (Search.isFeasible(newSolution) && Search.computerCost(newSolution) < Search.cost) {
			return true;
		}
		if (!Search.isFeasible) {
			if (Search.isFeasible(newSolution) || 
					Search.computerCost(newSolution) - Search.cost + 8 * (Search.getCurTotalOutput() - Search.getOutput(newSolution)) < 0) {
				if (Search.isFeasible(newSolution)) {
					Search.isFeasible = true;
				}
				return true;
			}
		}
		return false;
	}
	
	public static boolean isCandidate(List<Route> newSolution) {
		Search.reset();
		Route.addPaths(newSolution);
		
		if (Search.isFeasible(newSolution)) {
			return true;
		}
		return false;
	}
	
	public static List<Node> getPromisingNodes() {
		List<Node> candidates = new ArrayList<Node>(Arrays.asList(Graph.nodes));
		Collections.sort(candidates, new Comparator<Node>() {

			@Override
			public int compare(Node o1, Node o2) {
				return  o2.getMaximumOutput() - o1.getMaximumOutput();
			}
			
		});
		return candidates;
	}
	
	public static int computeCost(List<Route> solution) {
		Search.reset();
		Route.addPaths(solution);
		
		return Search.computerCost(solution);
	}
}
