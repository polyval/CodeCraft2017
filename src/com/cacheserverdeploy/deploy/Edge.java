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
	
	public static Map<Pair<Integer, Integer>, Edge> edgeMap = new HashMap<Pair<Integer, Integer>, Edge>();
	public final int source;
	public final int target;
	public final int cost;
	public int bandwidth;
	
	// For minimum cost flow.
	public boolean isResidual;
	public int residualFlow;
	public Edge counterEdge;
	
	// For DFS.
	public boolean removed = false;
	
	
	public Edge(int source, int target, int cost, int bandwidth) {
		this.source = source;
		this.target = target;
		this.cost = cost;
		this.bandwidth = bandwidth;
		if (cost > 0 && target < Graph.vertexNum) {
			edgeMap.put(new Pair<Integer, Integer>(source, target), this);
		}
	}
	
	// Reversely.
	@Override
	public int compareTo(Edge that) {
		if (this.bandwidth / this.cost == that.bandwidth / that.bandwidth) {
			return this.cost - that.cost;
		}
		return that.bandwidth / that.cost - this.bandwidth / this.cost;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + source;
		result = prime * result + target;
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
		Edge other = (Edge) obj;
		if (source != other.source)
			return false;
		if (target != other.target)
			return false;
		return true;
	}
} 
