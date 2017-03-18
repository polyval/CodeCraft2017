/**
 * 
 */
package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.filetool.util.FileUtil;

/**
 * @author JwZhou
 *
 */
public class Zkw {
	
	public static int[] getMinCostFlow(int source, int sink) {
		int vertexNum = Graph.resVertexNum;
		int[] dist = new int[vertexNum];
		boolean[] visited = new boolean[vertexNum];
		int temp = 0;
		int flow = 0;
		int cost = 0;
		int[] flowCost = new int[2];
		
		while (true) {
			temp = augment(source, sink, Integer.MAX_VALUE, dist, visited);
			while (temp > 0) {
				flow += temp;
				cost += temp * dist[source];
				Arrays.fill(visited, false);
				temp = augment(source, sink, Integer.MAX_VALUE, dist, visited);
			}
			
			if (!lable(sink, dist, visited)) {
				break;
			}
		}
		flowCost[0] = flow;
		flowCost[1] = cost;
		return flowCost;
	}
	
	// Don't need it if there is no negative edge.
	public static void spfa(int source, int sink, int[] dist, boolean[] visited) {
		LinkedList<Integer> queue = new LinkedList<Integer>();
		
		Arrays.fill(dist, Integer.MAX_VALUE);
		
		dist[source] = 0;
		visited[source] = true;
		queue.push(source);
		
		while (!queue.isEmpty()) {
			int u = queue.poll();
			visited[u] = false;
			
			for (Edge e : Graph.resAdj[u]) {
				int v = e.target;
				if (e.residualFlow > 0 && dist[u] + e.cost < dist[v]) {
					dist[v] = dist[u] + e.cost;
					if (!visited[v]) {
						queue.push(v);
						visited[v] = true;
					}
				}
			}
		}
		
		for (int i = 0; i < dist.length; i++) {
			dist[i] = dist[sink] - dist[i];
		}
	}
	
	public static int augment(int u, int sink, int flow, int[] dist, boolean[] visited) {
		if (u == sink) {
			return flow;
		}
		visited[u] = true;
		for (Edge e : Graph.resAdj[u]) {
			int v = e.target;
			if (e.residualFlow > 0 && !visited[v] && dist[v] + e.cost == dist[u]) {
				int temp = augment(v, sink, Math.min(e.residualFlow, flow), dist, visited);
				if (temp > 0) {
					e.residualFlow -= temp;
					e.counterEdge.residualFlow += temp;
					return temp;
				}
			}
		}
		return 0;
	}
	
	public static boolean lable(int sink, int[] dist, boolean[] visited) {
		int tmp = Integer.MAX_VALUE;
		for (int u = 0; u < dist.length; u++) {
			if (!visited[u]) {
				continue;
			}
			for (Edge e : Graph.resAdj[u]) {
				int v = e.target;
				if (e.residualFlow > 0 && !visited[v] && dist[v] + e.cost > dist[u]) {
					tmp = Math.min(tmp, dist[v] + e.cost - dist[u]);
				}
			}
		}
		
		if (tmp == Integer.MAX_VALUE) {
			return false;
		}
		for (int i = 0; i < dist.length; i++) {
			if (visited[i]) {
				dist[i] += tmp;
			} 
		}
		
		Arrays.fill(visited, false);
		return true;
	}
	
	public static void setSuperSource(List<Integer> servers) {
		for (int server : servers) {
			Edge edge = new Edge(Graph.vertexNum, server, 0, Integer.MAX_VALUE);
			Edge resedge = new Edge(server, Graph.vertexNum, 0, Integer.MAX_VALUE);
			
			resedge.residualFlow = 0;
			edge.residualFlow = Integer.MAX_VALUE;
			resedge.counterEdge = edge;
			edge.counterEdge = resedge;
			resedge.isResidual = true;
			
			Graph.resAdj[Graph.vertexNum].add(edge);
			Graph.resAdj[server].add(resedge);
		}
	}
	
	/**
	 * Clears the residual graph.
	 */
	public static void clear() {
		// Detach super source.
		if (!Graph.resAdj[Graph.vertexNum].isEmpty()) {
			Graph.resAdj[Graph.vertexNum].clear();
		}
		// Restore residual flow for all edges.
		for (List<Edge> edges : Graph.resAdj) {
			for (Edge edge : edges) {
				if (edge.isResidual) {
					edge.residualFlow = 0;
				}
				else {
					edge.residualFlow = edge.bandwidth;
				}
			}
		}
	}
	
	public static void main(String[] args) {
		String[] graphContent = FileUtil.read("E:\\codecraft\\cdn\\case_example\\case99.txt", null);
		Graph.makeGraph(graphContent);
		
		List<Integer> servers = new ArrayList<Integer>();
		servers.add(44);
		servers.add(7);
		servers.add(13);
		servers.add(15);
		servers.add(22);
		servers.add(34);
		servers.add(38);
		
		setSuperSource(servers);
		long startTime = System.nanoTime();
		
		System.out.println(Arrays.toString(getMinCostFlow(50, 51)));
		long endTime = System.nanoTime();
		System.out.println((endTime - startTime) / 1000000 + "ms");
	}
}
