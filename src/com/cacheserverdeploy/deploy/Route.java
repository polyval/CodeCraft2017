/**
 * 
 */
package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author JwZhou
 *
 */
public class Route implements Comparable<Route>{
	
	public int client;
	public int server;
	public int maxBandwidth;
	public int averageCost;
	public int occupiedBandwidth;
	public ArrayList<Integer> nodes = new ArrayList<>();
	public List<Edge> edges = new ArrayList<>();
	public static Map<Pair<Integer, Integer>, ArrayList<Route>> shortestPaths = new HashMap<>();
	
	public Route(int server, int client) {
		this.server = server;
		this.client = client;
		nodes.add(server);
	}
	
	public List<Edge> getEdges() {
		if (edges.isEmpty()) {
			for (int i = 0; i < nodes.size() - 1; i++) {
				edges.add(Edge.edgeMap.get(new Pair<Integer, Integer>(nodes.get(i), nodes.get(i + 1))));
			}
		}
		return edges;
	}
	
	/**
	 * Computes the maximum bandwidth and average cost.
	 */
	public void computeBandwidthAndCost() {
		averageCost = 0;
		maxBandwidth = Integer.MAX_VALUE;
		
		getEdges();
		
		for (Edge edge : edges) {
			maxBandwidth = Math.min(maxBandwidth, edge.bandwidth);
			averageCost += edge.cost;
		}
	}
	
	/**
	 * Updates edges' bandwidth.
	 */
	public void updateEdgesBandwidth() {
		for (int i = 0; i < nodes.size() - 1; i++) {
			Edge edge = Edge.edgeMap.get(new Pair<Integer, Integer>(nodes.get(i), nodes.get(i + 1)));
			edge.bandwidth -= occupiedBandwidth;
		}
	}
	
	/**
	 * 
	 * @param occupiedBandwidth - last time's occupied bandwidth.
	 */
	public void restorePath(int occupiedBandwidth) {
		removePath();
		this.occupiedBandwidth = occupiedBandwidth;
		
		Graph.nodes[client].demands -= this.occupiedBandwidth;
		updateEdgesBandwidth();
	}
	
	/**
	 * Adds this path to network.
	 */
	public void addPath() {
		computeBandwidthAndCost();
		this.occupiedBandwidth = Math.min(maxBandwidth, Graph.nodes[client].demands);
		Graph.nodes[client].demands -= this.occupiedBandwidth;
		updateEdgesBandwidth();
	}
	
	/**
	 * Removes this path from network.
	 */
	public void removePath() {
		for (Edge edge : edges) {
			edge.bandwidth += occupiedBandwidth;
		}
		Graph.nodes[client].demands += occupiedBandwidth;
		occupiedBandwidth = 0;
		computeBandwidthAndCost();
	}
	
	public int getClientRemaningDemands() {
		return Graph.nodes[client].demands;
	}
	
	
	@Override
	public String toString() {
		return nodes.toString() + " " + occupiedBandwidth + " " + client;
	}

	@Override
	public int compareTo(Route other) {
		if (this.averageCost != other.averageCost) {
			return this.averageCost - other.averageCost;
		}
		return other.maxBandwidth - this.maxBandwidth;
	}
}
