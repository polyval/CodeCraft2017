/**
 * 
 */
package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author JwZhou
 *
 */
public class Route {
	
	public int client;
	public int server;
	public int maxBandwidth;
	public int averageCost;
	public int occupiedBandwidth;
	public ArrayList<Integer> nodes = new ArrayList<>();
	
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
			Edge edge = Edge.edgeMap.get(Arrays.asList(Integer.toString(nodes.get(i)), Integer.toString(nodes.get(i + 1))));
			maxBandwidth = Math.min(maxBandwidth, edge.bandwidth);
			averageCost += edge.cost;
		}
	}
	
	/*
	 * Updates edges' bandwidth.
	 */
	public void updateEdgesBandwidth() {
		for (int i = 0; i < nodes.size() - 1; i++) {
			Edge edge = Edge.edgeMap.get(Arrays.asList(Integer.toString(nodes.get(i)), Integer.toString(nodes.get(i + 1))));
			edge.bandwidth -= occupiedBandwidth;
		}
	}
	
	@Override
	public String toString() {
		return nodes.toString() + " " + occupiedBandwidth;
	}
}
