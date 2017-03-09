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
	
	// ID of the end of the path.
	public int client;
	// ID of the start of the path.
	public int server;
	public int maxBandwidth;
	public int averageCost;
	public int occupiedBandwidth;
	public List<Integer> nodes = new ArrayList<>();
	public List<Edge> edges = new ArrayList<>();
	public static Map<Pair<Integer, Integer>, List<Route>> shortestPaths = new HashMap<>();
	
	public Route(int server, int client) {
		this.server = server;
		this.client = client;
		nodes.add(server);
		computeBandwidthAndCost();
	}
	
	public Route(List<Integer> nodes) {
		this.server = nodes.get(0);
		this.client = nodes.get(nodes.size() - 1);
		this.nodes = nodes;
		computeBandwidthAndCost();
	}
	
	public List<Edge> getEdges() {
		if (edges.isEmpty()) {
			for (int i = 0; i < nodes.size() - 1; i++) {
				edges.add(Edge.edgeMap.get(new Pair<Integer, Integer>(nodes.get(i), nodes.get(i + 1))));
			}
		}
		return edges;
	}
	
	public static List<Route> getShortestPaths(int start, int end) {
		Pair<Integer, Integer> pair = new Pair<>(start, end);
		if (shortestPaths.get(pair) == null) {
			shortestPaths.put(pair, YenKSP.kspYen(start, end, 5));
		}
		return shortestPaths.get(pair);
	}
	
	/**
	 * Computes the maximum bandwidth and average cost.
	 */
	public void computeBandwidthAndCost() {
		averageCost = 0;
		maxBandwidth = Integer.MAX_VALUE;
		
		for (Edge edge : getEdges()) {
			maxBandwidth = Math.min(maxBandwidth, edge.bandwidth);
			averageCost += edge.cost;
		}
	}
	
	/**
	 * Updates edges' bandwidth.
	 */
	public void updateEdgesBandwidth() {
		for (Edge edge : getEdges()) {
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
		removePath();
		computeBandwidthAndCost();
		this.occupiedBandwidth = Math.min(maxBandwidth, Graph.nodes[client].demands);
		Graph.nodes[client].demands -= this.occupiedBandwidth;
		updateEdgesBandwidth();
	}
	
	/**
	 * Removes this path from network.
	 */
	public void removePath() {
		// Restore edges' bandwidth.
		for (Edge edge : edges) {
			edge.bandwidth += occupiedBandwidth;
		}
		Graph.nodes[client].demands += occupiedBandwidth;
		occupiedBandwidth = 0;
		computeBandwidthAndCost();
	}
	
	public static void addPaths(List<Route> paths) {
		for (Route path : paths) {
			path.addPath();
		}
	}
	
	public static void removeAllPaths() {
		for (List<Route> paths : shortestPaths.values()) {
			for (Route path : paths) {
				path.removePath();
			}
		}
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Route other = (Route) obj;
		if (nodes == null) {
			if (other.nodes != null)
				return false;
		} else if (!nodes.equals(other.nodes))
			return false;
		return true;
	}
}
