/**
 * 
 */
package com.cacheserverdeploy.deploy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author JwZhou
 *
 */
public class Edge implements Comparable<Edge> {
	
	public static Map<Pair<Integer, Integer>, Edge> edgeMap = new HashMap<>();
	public final int source;
	public final int target;
	public final int cost;
	public int bandwidth;
	
	public Edge(int source, int target, int cost, int bandwidth) {
		this.source = source;
		this.target = target;
		this.cost = cost;
		this.bandwidth = bandwidth;
		edgeMap.put(new Pair<Integer, Integer>(source, target), this);
	}
	
	// Reversely.
	@Override
	public int compareTo(Edge that) {
		if (this.bandwidth / this.cost == that.bandwidth / that.bandwidth) {
			return this.cost - that.cost;
		}
		return that.bandwidth / that.cost - this.bandwidth / this.cost;
	}
} 
