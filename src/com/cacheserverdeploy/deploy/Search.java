/**
 * 
 */
package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * @author JwZhou
 *
 */
public class Search {
	
	public static ArrayList<Node> servers = new ArrayList<>();
	public static ArrayList<Node> clientNodes = new ArrayList<>(Graph.clientVertexNum);
	public static ArrayList<Route> solution = new ArrayList<>();
	
	public static void initialize() {
		// Get all the client nodes.
		for (int i = 0; i < Graph.clientVertexNum; i++) {
			clientNodes.add(Graph.nodes[Graph.clientVertexId[i]]);
		}
		// Sort the clients by its demands reversely.
		Collections.sort(clientNodes, new Comparator<Node>() {
			@Override
			public int compare(Node n1, Node n2) {
				// It may has some other criteria.
				return n2.demands - n1.demands;
			}
		});
	}
	
	/**
	 *  HotSpot heuristic to construct the initial solution.
	 */
	public static void hotSpot(ArrayList<Node> unserverdClients) {
		
		Collections.sort(unserverdClients, new Comparator<Node>() {
			@Override
			public int compare(Node n1, Node n2) {
				// It may has some other criteria.
				return n2.demands - n1.demands;
			}
		});
		// The client which has maximum bandwidth demands.
		Node firstClient = unserverdClients.get(0);
		unserverdClients.remove(0);
		// The max bandwidth the client can get.
		int maxBandwidth = 0;
		for (Edge edge : Graph.adj[firstClient.vertexId]) {
			maxBandwidth += edge.bandwidth;
		}
		
		// Try to find a node to be the server.
		int serverNodeId = -1;
		// The links to the node can satisfy the client's demands.
		if (maxBandwidth >= firstClient.demands) {
			ArrayList<Edge> edges = Graph.adj[firstClient.vertexId];
			for (int i = 0; i < edges.size(); i++) {
				Edge edge = edges.get(i);
				if (edge.bandwidth <= 0) {
					continue;
				}
				serverNodeId = edge.target;
				servers.add(Graph.nodes[serverNodeId]);
				
				// Add one route to solution.
				Route route = new Route(serverNodeId, firstClient.clientId);
				route.nodes.add(firstClient.vertexId);
				route.computeBandwidthAndCost();
				route.occupiedBandwidth = Math.min(firstClient.demands, route.maxBandwidth);
				// Indicate this route is unavailable.
				if (route.occupiedBandwidth == 0) {
					continue;
				}
				firstClient.demands -= route.occupiedBandwidth;
				route.updateEdgesBandwidth();
				solution.add(route);
				break;
			}
			
		}
		
		// Indicate that all the links to client's attached node is unavailable.
		// There must have a server in the client's attached node.
		if (serverNodeId == -1) {
			servers.add(firstClient);
			
			// Add one route to solution.
			Route route = new Route(firstClient.vertexId, firstClient.clientId);
			route.occupiedBandwidth = Math.min(firstClient.demands, route.maxBandwidth);
			firstClient.demands -= route.occupiedBandwidth;
			solution.add(route);
			
			serverNodeId = firstClient.vertexId;
		}	
		
		// Link other clients to this server.
		for (ArrayList<Integer> r : bfsToClients(Graph.nodes[serverNodeId])) {
			Node clientAttachedNode = Graph.nodes[r.get(r.size() - 1)];
			Route newRoute = new Route(serverNodeId, clientAttachedNode.clientId);
			newRoute.nodes = r;
			
			newRoute.computeBandwidthAndCost();
			// Indicate this route is unavailable.
			if (newRoute.maxBandwidth <= 0) {
				continue;
			}
			newRoute.occupiedBandwidth = Math.min(clientAttachedNode.demands, newRoute.maxBandwidth);
			if (newRoute.occupiedBandwidth == 0) {
				continue;
			}
			clientAttachedNode.demands -= newRoute.occupiedBandwidth;
			newRoute.updateEdgesBandwidth();
			solution.add(newRoute);
			// This client has been served.
			unserverdClients.remove(clientAttachedNode);
		}
		
		// All clients need at least one server to serve it.
		if (!unserverdClients.isEmpty()) {
			hotSpot(unserverdClients);
		}
	}
	
	/**
	 * Returns routes to clients from one node, one client one route, it will lost some routes.
	 */
	public static ArrayList<ArrayList<Integer>> bfsToClients(Node node) {
		// Mark if a node is visited.
		boolean[] mark = new boolean[Graph.vertexNum];
		Queue<Integer> queue = new LinkedList<>();
		Queue<ArrayList<Integer>> trace = new LinkedList<ArrayList<Integer>>();
		ArrayList<ArrayList<Integer>> routesToClients = new ArrayList<ArrayList<Integer>>();
		
		queue.add(node.vertexId);
		ArrayList<Integer> first = new ArrayList<>();
		first.add(node.vertexId);
		trace.add(first);
		while (!queue.isEmpty()) {
			int size = queue.size();
			
			for (int i = 0; i < size; i++) {
				int id = queue.poll();
				ArrayList<Integer> route = trace.poll();
				// Wouldn't visit the node that has visited except the client node.
				if (!mark[id] || Graph.nodes[id].clientId != -1) {
					// Mark the node as visited
					mark[id] = true;
					// If the node attach to a client, save the route.
					if (Graph.nodes[id].clientId != -1) {
						routesToClients.add(route);
					}
					// Add next level's nodes.
					for (Edge edge : Graph.adj[id]) {
						if (!mark[edge.target]) {
							queue.add(edge.target);
							route.add(edge.target);
							trace.add(new ArrayList<>(route));
							route.remove(route.size() - 1);
							continue;
						}
						// If next node has visited but it is client node.
						if (Graph.nodes[edge.target].clientId != -1) {
							route.add(edge.target);
							routesToClients.add(new ArrayList<>(route));
							route.remove(route.size() - 1);
						}
					}
				}
			}	
		}
		return routesToClients;
	}
	
	/**
	 * From one node, find all paths to all clients.
	 * @param node
	 * @return
	 */
	public static ArrayList<ArrayList<Integer>> dfsToClients(Node node) {
		ArrayList<ArrayList<Integer>> routesToClients = new ArrayList<ArrayList<Integer>>();
		LinkedList<Integer> visitedNodes = new LinkedList<>();
		if (node.clientId != -1) {
			routesToClients.add(new ArrayList<Integer>(node.vertexId));
		}
		visitedNodes.add(node.vertexId);
		dfs(visitedNodes, routesToClients);
		return routesToClients;
	}
	
	/**
	 * Finds all simple paths between two nodes.
	 * @param nodeId
	 * @param destId
	 * @return
	 */
	public static ArrayList<ArrayList<Integer>> dfsToAnother(int nodeId, int destId) {
		ArrayList<ArrayList<Integer>> routesToClients = new ArrayList<ArrayList<Integer>>();
		LinkedList<Integer> visitedNodes = new LinkedList<>();

		visitedNodes.add(nodeId);
		dfsTo(visitedNodes, destId, routesToClients);
		return routesToClients;
	}
	
	public static void dfsTo(LinkedList<Integer> visited, int destId, ArrayList<ArrayList<Integer>> routesToClients) {
		int lastNodeId = visited.getLast();
		for (Edge edge : Graph.adj[lastNodeId]) {
			if (visited.contains(edge.target)) {
				continue;
			}	
			if (edge.target == destId) {
				visited.add(edge.target);
				routesToClients.add(new ArrayList<Integer>(visited));
				visited.removeLast();
				break;
			}
		}
		
		for (Edge edge : Graph.adj[lastNodeId]) {
			if (visited.contains(edge.target) || edge.target == destId) {
				continue;
			}
			visited.add(edge.target);
			dfsTo(visited, destId, routesToClients);
			visited.removeLast();
		}
		
	}
	
	// Too slow
	public static void dfs(LinkedList<Integer> visited, ArrayList<ArrayList<Integer>> routesToClients) {
		int lastNodeId = visited.getLast();
		for (Edge edge : Graph.adj[lastNodeId]) {
			if (visited.contains(edge.target)) {
				continue;
			}
			visited.add(edge.target);
			if (Graph.nodes[edge.target].clientId != -1) {
				routesToClients.add(new ArrayList<Integer>(visited));
				visited.removeLast();
				continue;
			}
			dfs(visited, routesToClients);
			visited.removeLast();
		}
	}
	
	
	/*
	 * Resets the data.
	 */
	public void reset() {
		
	}
	
	public static boolean isFeasible(ArrayList<Route> solution) {
		int[] demands = new int[Graph.clientVertexNum];
		for (Route route : solution) {
			demands[Graph.nodes[route.client].clientId] += route.occupiedBandwidth;
		}
		System.out.println(Arrays.toString(demands));
		System.out.println(Arrays.toString(Arrays.copyOfRange(Graph.clientDemand, 0, Graph.clientVertexNum)));
		for (int i = 0; i < Graph.clientVertexNum; i++) {
			if (demands[i] != Graph.clientDemand[i]) {
				return false;
			}
		}
		return true;
	}
	
	public static int computerCost() {
		int cost = 0;
		cost += servers.size() * Graph.serverCost;
		for (Route route : solution) {
			cost += route.averageCost * route.occupiedBandwidth;
		}
		return cost;
	}
}
