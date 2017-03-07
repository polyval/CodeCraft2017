/**
 * 
 */
package com.cacheserverdeploy.deploy;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
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
		
		while (isUnsatisfiedDemands() && Search.servers.size() < Graph.clientVertexNum) {
			int lowerBound = Integer.MAX_VALUE;
			Node newServer = null;
			ArrayList<Route> bestPaths = new ArrayList<>();
			
			for (Node node : Graph.nodes) {
				if (!node.isServer) {
					// Cost induced by setting this node as server.
					int newCost = 0;
					
					// For restoring the paths.
					ArrayList<Integer> curOccupiedDemands = new ArrayList<>();
					ArrayList<Route> addedPaths = new ArrayList<>();
					
					// Start with client needing most demands
					for (Node clientNode : Graph.clientNodes) {
						// Client node that still has demands.
						if (clientNode.demands > 0) {
							
							// Shortest paths from one node to client node.
							for (Route path : Route.shortestPaths.get(new Pair<>(node.vertexId, clientNode.vertexId))) {
								// No need to add this path.
								if (clientNode.demands == 0) {
									break;
								}
								
								addedPaths.add(path);
								curOccupiedDemands.add(path.occupiedBandwidth);
								
								newCost += addPath(path);
							}
						}
					}
					
					// Add penalties for unserved demands
					for (Node clientNode : Graph.clientNodes) {
						newCost += clientNode.demands * 10;
					}
					
					// This node as server is better than last one.
					if (newCost < lowerBound) {
						// Update the best server solution.
						newServer = node;
						lowerBound = newCost;
						bestPaths = addedPaths;
					}
					
					restorePaths(addedPaths, curOccupiedDemands);
				}
			}
			// Update the solution
			for (Route path : bestPaths) {
				addPath(path);
			}

			Search.servers.add(newServer);
			Search.solution.addAll(bestPaths);
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
	 * @param paths
	 * @param occupiedBandwidths
	 */
	public static void restorePaths(ArrayList<Route> paths, List<Integer> occupiedBandwidths) {
		if (paths.size() != occupiedBandwidths.size()) {
			throw new InvalidParameterException();
		}
		else {
			for (int i = 0; i < paths.size(); i++) {
				paths.get(i).restorePath(occupiedBandwidths.get(i));
			}
		}
	}
}
