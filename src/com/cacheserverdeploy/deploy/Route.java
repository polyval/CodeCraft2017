/**
 * 
 */
package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
	public static Map<Pair<Integer, Integer>, ArrayList<Route>> shortestPaths = new HashMap<>();
	
	public Route(int server, int client) {
		this.server = server;
		this.client = client;
		nodes.add(server);
	}
	
	/**
	 * Computes the maximum bandwidth and average cost.
	 */
	public void computeBandwidthAndCost() {
		averageCost = 0;
		maxBandwidth = Integer.MAX_VALUE;
		for (int i = 0; i < nodes.size() - 1; i++) {
			Edge edge = Edge.edgeMap.get(new Pair<Integer, Integer>(nodes.get(i), nodes.get(i + 1)));
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
	
	public void assignBandWidthToClient() {
		this.occupiedBandwidth = Math.min(maxBandwidth, Graph.nodes[client].demands);
		Graph.nodes[client].demands -= this.occupiedBandwidth;
	}
	
	public void restorePath(int occupiedBandwidth) {
		this.occupiedBandwidth = occupiedBandwidth;
		Graph.nodes[client].demands += this.occupiedBandwidth;
		computeBandwidthAndCost();
		restoreEdgesBandwidth();
	}
	
	/**
	 * Restores edges' available bandwidth.
	 */
	private void restoreEdgesBandwidth() {
		for (int i = 0; i < nodes.size() - 1; i++) {
			Edge edge = Edge.edgeMap.get(new Pair<Integer, Integer>(nodes.get(i), nodes.get(i + 1)));
			edge.bandwidth += occupiedBandwidth;
		}
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
