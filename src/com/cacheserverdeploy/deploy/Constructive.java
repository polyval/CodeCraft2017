/**
 * 
 */
package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author JwZhou
 *
 */
public class Constructive {
	
	/**
	 * Constructs the initial solution using greedy heuristic.
	 */
	public static void greedyConstruct() {
		
		Arrays.sort(Graph.clientNodes, new Comparator<Node>() {
			@Override
			public int compare(Node n1, Node n2) {
				// It may has some other criteria.
				return n2.demands - n1.demands;
			}
		});
		
		while (isUnsatisfiedDemands() && Search.servers.size() < Graph.clientVertexNum - 1) {
			int lowerBound = Integer.MAX_VALUE;
			Node newServer = null;
			ArrayList<Route> bestPaths = new ArrayList<Route>();
			
			for (Node node : Graph.nodes) {
				if (!node.isServer) {
					// Cost induced by setting this node as server.
					int newCost = 0;
					
					// For restoring the paths.
					ArrayList<Route> addedPaths = new ArrayList<Route>();
					
					//Start with client needing most demands
					for (Node clientNode : Graph.clientNodes) {
						// Client node that still has demands.
						if (clientNode.demands > 0) {
							
							// Shortest paths from one node to client node.
							for (Route path : Route.getShortestPaths(node.vertexId, clientNode.vertexId)) {
								// No need to add this path.
								if (clientNode.demands == 0 || path.averageCost * path.maxBandwidth >= Graph.serverCost) {
									break;
								}
								
								addedPaths.add(path);
								newCost += addPath(path);
							}
						}
					}
//					
//					// Start with path that costs less.
//					List<Route> allPaths = new ArrayList<Route>();
//					for (Node clientNode : Graph.clientNodes) {
//						// Client node that still has demands.
//						allPaths.addAll(Route.getShortestPaths(node.vertexId, clientNode.vertexId));
//					}
//					Collections.sort(allPaths);
//					for (Route path : allPaths) {
//						if (Graph.nodes[path.client].demands > 0 && newCost < Graph.serverCost) {
//							addedPaths.add(path);
//							newCost += addPath(path);
//						}
//					}
					
					
					// Add penalties for unserved demands
					for (Node clientNode : Graph.clientNodes) {
						newCost += clientNode.demands * 8;
					}
					
					// This node as server is better than last one.
					if (newCost < lowerBound) {
						// Update the best server solution.
						newServer = node;
						lowerBound = newCost;
						bestPaths = addedPaths;
					}
					
					restorePaths(addedPaths);
				}
			}
			// Update the solution
			for (Route path : bestPaths) {
				addPath(path);
			}
			
			newServer.isServer = true;
			Search.servers.add(newServer);
			Search.solution.addAll(bestPaths);
			Search.cost = Search.computerCost(Search.solution);
		}
		if (Search.isFeasible(Search.solution)) {
			Search.isFeasible = true;
		}
		else {
			Search.cost = Graph.serverCost * Graph.clientVertexNum;
			Search.solution.clear();
			Search.servers = Arrays.asList(Graph.clientNodes);
		}
	}
	
	public static List<Route> greedyConstructMultipleTimes() {
	
		List<Route> newSolution = new ArrayList<Route>();
		List<Node> newServers = new ArrayList<Node>();
		
		Search.reset();
		
		Arrays.sort(Graph.clientNodes, new Comparator<Node>() {
			@Override
			public int compare(Node n1, Node n2) {
				// It may has some other criteria.
				return n2.demands - n1.demands;
			}
		});
		
		while (isUnsatisfiedDemands() && newServers.size() < Graph.clientVertexNum) {
			int lowerBound = Integer.MAX_VALUE;
			Node newServer = null;
			ArrayList<Route> bestPaths = new ArrayList<Route>();
			
			for (Node node : Graph.nodes) {
				if (!node.isServer) {
					// Cost induced by setting this node as server.
					int newCost = 0;
					
					// For restoring the paths.
					ArrayList<Route> addedPaths = new ArrayList<Route>();
					
//					Start with client needing most demands
					for (Node clientNode : Graph.clientNodes) {
						// Client node that still has demands.
						if (clientNode.demands > 0) {
							
							// Shortest paths from one node to client node.
							for (Route path : Route.getShortestPaths(node.vertexId, clientNode.vertexId)) {
								// No need to add this path.
								if (clientNode.demands == 0 || path.averageCost * path.maxBandwidth >= Graph.serverCost) {
									break;
								}
								
								addedPaths.add(path);
								newCost += addPath(path);
							}
						}
					}
//					
//					// Start with path that costs less.
//					List<Route> allPaths = new ArrayList<>();
//					for (Node clientNode : Graph.clientNodes) {
//						// Client node that still has demands.
//						allPaths.addAll(Route.getShortestPaths(node.vertexId, clientNode.vertexId));
//					}
//					Collections.sort(allPaths);
//					for (Route path : allPaths) {
//						if (Graph.nodes[path.client].demands > 0 && newCost < Graph.serverCost) {
//							addedPaths.add(path);
//							newCost += addPath(path);
//						}
//					}
					
					
					// Add penalties for unserved demands
					for (Node clientNode : Graph.clientNodes) {
						newCost += clientNode.demands * 8;
					}
					
					// This node as server is better than last one.
					if (newCost < lowerBound) {
						// Update the best server solution.
						newServer = node;
						lowerBound = newCost;
						bestPaths = addedPaths;
					}
					
					restorePaths(addedPaths);
				}
			}
			// Update the solution
			for (Route path : bestPaths) {
				addPath(path);
			}
			
			newServer.isServer = true;
			newServers.add(newServer);
			newSolution.addAll(bestPaths);
		}
		return newSolution;
	}

	public static void greedyConstructByBandwidth() {
		
		Arrays.sort(Graph.clientNodes, new Comparator<Node>() {
			@Override
			public int compare(Node n1, Node n2) {
				// It may has some other criteria.
				return n2.demands - n1.demands;
			}
		});
		
		while (isUnsatisfiedDemands() && Search.servers.size() < Graph.clientVertexNum - 1) {
			int lowerBound = 0;
			Node newServer = null;
			ArrayList<Route> bestPaths = new ArrayList<Route>();
			
			for (Node node : Graph.nodes) {
				if (!node.isServer) {
					// Cost induced by setting this node as server.
					int newCost = 0;
					int totalBandwidth = 0;
					
					// For restoring the paths.
					ArrayList<Route> addedPaths = new ArrayList<Route>();
					
					// Start with client needing most demands
//					for (Node clientNode : Graph.clientNodes) {
//						// Client node that still has demands.
//						if (clientNode.demands > 0) {
//							
//							// Shortest paths from one node to client node.
//							for (Route path : Route.getShortestPaths(node.vertexId, clientNode.vertexId)) {
//								// No need to add this path.
//								if (clientNode.demands == 0 || path.averageCost * path.maxBandwidth >= Graph.serverCost) {
//									break;
//								}
//								
//								addedPaths.add(path);
//								newCost += addPath(path);
//							}
//						}
//					}
//					
					// Start with path that costs less.
					List<Route> allPaths = new ArrayList<Route>();
					for (Node clientNode : Graph.clientNodes) {
						// Client node that still has demands.
						allPaths.addAll(Route.getShortestPaths(node.vertexId, clientNode.vertexId));
					}
					Collections.sort(allPaths);
					for (Route path : allPaths) {
						if (Graph.nodes[path.client].demands > 0 && newCost < Graph.serverCost) {
							addedPaths.add(path);
							newCost += addPath(path);
							totalBandwidth += path.occupiedBandwidth;
						}
					}
					
					// This node as server is better than last one.
					if (totalBandwidth > lowerBound) {
						// Update the best server solution.
						newServer = node;
						lowerBound = totalBandwidth;
						bestPaths = addedPaths;
					}
					
					restorePaths(addedPaths);
				}
			}
			// Update the solution
			for (Route path : bestPaths) {
				addPath(path);
			}
			
			newServer.isServer = true;
			Search.servers.add(newServer);
			Search.solution.addAll(bestPaths);
			Search.cost = Search.computerCost(Search.solution);
			Search.updateSolution(Search.solution);
			if (Search.isFeasible(Search.solution)) {
				Search.isFeasible = true;
			}
		}
		
	}
	
	/**
	 * Connects a client with a server with one path.
	 * 
	 * @return cost by this path.
	 */
	public static int addPath(Route path) {
		// Actually, this path is unavailable
		if (path.occupiedBandwidth == path.maxBandwidth) {
			return 0;
		}
		path.addPath();
		return path.averageCost * path.occupiedBandwidth;
	}
	
	/**
	 * Checks if there is any unsatisfied demands.
	 * @return
	 */
	public static boolean isUnsatisfiedDemands() {
		for(Node clientNode : Graph.clientNodes) {
			if (clientNode.demands != 0) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Restores all the paths.
	 * @param changedPaths
	 * @param occupiedBandwidths
	 */
	public static void restorePaths(List<Route> changedPaths) {
			for (int i = 0; i < changedPaths.size(); i++) {
				changedPaths.get(i).removePath();
			}
	}
}
