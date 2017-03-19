/**
 * 
 */
package com.cacheserverdeploy.deploy;

/**
 * @author JwZhou
 *
 */
public class Edge implements Comparable<Edge> {
	
	public final int source;
	public final int target;
	public final int cost;
	public int bandwidth;
	
	// For minimum cost flow.
	public boolean isResidual;
	public int residualFlow;
	public Edge counterEdge;
	
	public Edge(int source, int target, int cost, int bandwidth) {
		this.source = source;
		this.target = target;
		this.cost = cost;
		this.bandwidth = bandwidth;
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
