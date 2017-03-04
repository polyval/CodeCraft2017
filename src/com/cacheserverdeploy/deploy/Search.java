/**
 * 
 */
package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * @author JwZhou
 *
 */
public class Search {
	
	public ArrayList<Node> servers = new ArrayList<>();
	public ArrayList<Node> clientNodes = new ArrayList<>(Graph.clientVertexNum);
	public ArrayList<Route> solution = new ArrayList<>();
	
	public void initialize() {
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
	
	/*
	 *  HotSpot heuristic to construct the initial solution.
	 */
	public void hotSpot(ArrayList<Node> unserverdClients) {
		
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
				route.occupiedBandwidth = edge.bandwidth;
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
			route.occupiedBandwidth = firstClient.demands;
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
	
	/*
	 * Returns routes to clients from one node。
	 */
	public ArrayList<ArrayList<Integer>> bfsToClients(Node node) {
		// Mark if a node is visited.
		boolean[] mark = new boolean[Graph.vertexNum];
		Queue<Integer> queue = new LinkedList<>();
		Queue<ArrayList<Integer>> trace = new LinkedList<ArrayList<Integer>>();
		ArrayList<ArrayList<Integer>> routesToClients = new ArrayList<ArrayList<Integer>>();
		
		queue.add(node.vertexId);
		trace.add(new ArrayList<Integer>(node.vertexId));
		while (!queue.isEmpty()) {
			int size = queue.size();
			
			for (int i = 0; i < size; i++) {
				int id = queue.poll();
				ArrayList<Integer> route = trace.poll();
				// Wouldn't visit the node that has visited.
				if (!mark[id]) {
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
							trace.add(route);
						}
						route.remove(route.size() - 1);
					}
				}
			}	
		}
		return routesToClients;
	}
	
	
	/*
	 * Resets the data.
	 */
	public void reset() {
		
	}
	
	public boolean isFeasible() {
		//
		return true;
	}
}
