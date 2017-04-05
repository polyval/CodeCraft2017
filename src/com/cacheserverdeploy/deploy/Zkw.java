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
	
	public static int totalCost = 0;
	public static int totalFlow = 0;
	private static int pathCost = 0;
	private static int sink;
	private static int source;
	private static boolean[] visited;
	
	public static void computeMinCostFlow() {
		totalCost = 0;
		totalFlow = 0;
		pathCost = 0;
		source = Graph.vertexNum;
		sink = Graph.vertexNum + 1;
		visited = new boolean[Graph.resVertexNum];
		
//		while (lable()) {
//			Arrays.fill(visited, false);
//			while (augment(source, Integer.MAX_VALUE) != 0) {
//				Arrays.fill(visited, false);
//			}
//		}
		
		while (true) {
			Arrays.fill(visited, false);
			while (augment(source, Integer.MAX_VALUE) > 0) {
				Arrays.fill(visited, false);
			}
			if (!relable()) {
				break;
			}
		}
	}
	
	private static int augment(int u, int flow) {
		if (u == sink) {
			totalCost += pathCost * flow;
			totalFlow += flow;
			return flow;
		}
		visited[u] = true;
		int leftFlow = flow;
		for (Edge e : Graph.resAdj[u]) {
			int v = e.target;
			if (e.residualFlow != 0 && e.cost == 0 && !visited[v]) {
				int temp = augment(v, Math.min(e.residualFlow, leftFlow));
	
				e.residualFlow -= temp;
				e.counterEdge.residualFlow += temp;
				leftFlow -= temp;
				if (leftFlow <= 0) {
					return flow;
				}
			}
		}
		return flow - leftFlow;
	}
	
	private static boolean lable() {
		LinkedList<Integer> deque = new LinkedList<Integer>();
		int[] dist = new int[Graph.resVertexNum];
		
		Arrays.fill(dist, Integer.MAX_VALUE);
		dist[sink] = 0;
		deque.add(sink);
		
		while (!deque.isEmpty()) {
			int targetDist;
			int now = deque.getFirst();
			deque.removeFirst();
			
			for (Edge e : Graph.resAdj[now]) {
				if (e.counterEdge.residualFlow == 0) {
					continue;
				}
				targetDist = dist[now] - e.cost;
				if (targetDist < dist[e.target]) {
					dist[e.target] = targetDist;
					int compare = 0;
					if (deque.isEmpty()) {
						compare = dist[0];
					}
					else {
						compare = dist[deque.getFirst()];
					}
					if (dist[e.target] <= compare) {
						deque.addFirst(e.target);
					}
					else {
						deque.addLast(e.target);
					}
				}
			}
		}
		
		for (int i = 0; i < Graph.resVertexNum; i++) {
			for (Edge e : Graph.resAdj[i]) {
				e.cost += dist[e.target] - dist[i];
			}
		}
		
		pathCost += dist[source];
		return dist[source] != Integer.MAX_VALUE;
	}
	
	private static boolean relable() {
		int temp = Integer.MAX_VALUE;
		for (int i = 0; i < Graph.resVertexNum; i++) {
			if (visited[i]) {
				for (Edge e : Graph.resAdj[i]) {
					if (e.residualFlow > 0 && !visited[e.target] && e.cost < temp) {
						temp = e.cost;
					}
				}
			}
		}
		
		if (temp == Integer.MAX_VALUE) {
			return false;
		}
		
		for (int i = 0; i < Graph.resVertexNum; i++) {
			if (visited[i]) {
				for (Edge e : Graph.resAdj[i]) {
					e.cost -= temp;
					e.counterEdge.cost += temp;
				}
			}
		}
		
		pathCost += temp;
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
			for (Edge e : Graph.resAdj[Graph.vertexNum]) {
				Graph.resAdj[e.target].remove(Graph.resAdj[e.target].size() - 1);
			}
			Graph.resAdj[Graph.vertexNum].clear();
		}
		// Restore residual flow for all edges.
		for (List<Edge> edges : Graph.resAdj) {
			for (Edge edge : edges) {
				if (edge.isResidual) {
					edge.residualFlow = 0;
					edge.cost = edge.originCost;
				}
				else {
					edge.residualFlow = edge.bandwidth;
					edge.cost = edge.originCost;
				}
			}
		}
	}
	
	public static void computeFlowCostGivenServers(List<Integer> servers) {
		clear();
		setSuperSource(servers);
		computeMinCostFlow();
	}
	
	public static List<Path> getPaths() {
		List<Path> paths = new ArrayList<Path>();
		int totalFlow = 0;
		visited = new boolean[Graph.resVertexNum];
		while (totalFlow < Graph.totalFlow) {
			Arrays.fill(visited, false);
			List<Edge> path = new ArrayList<Edge>();
			totalFlow += dfs(source, path, paths, visited);
		}
		return paths;
	}
	
	private static int dfs(int u, List<Edge> path, List<Path> paths, boolean[] visited) {
		if (u == sink) {
			int flow = Integer.MAX_VALUE;
			List<Integer> nodes = new ArrayList<Integer>();
			for (Edge e : path) {
				flow = Math.min(e.bandwidth - e.residualFlow, flow);
				if (e.target != sink) {
					nodes.add(e.target);
				}
			}
			for (Edge e : path) {
				e.residualFlow += flow;
			}
			paths.add(new Path(nodes, flow));
			return flow;
		}
		visited[u] = true;
		for (Edge edge : Graph.resAdj[u]) {
			if (edge.isResidual || edge.bandwidth == edge.residualFlow || visited[edge.target]) {
				continue;
			}
			path.add(edge);
			int curFlow = dfs(edge.target, path, paths, visited);
			if (curFlow > 0) {
				return curFlow;
			}
			path.remove(path.size() - 1);
		}
		return 0;
	}
	
	public static void main(String[] args) {
		String[] graphContent = FileUtil.read("E:\\codecraft\\cdn\\case_example\\case0.txt", null);
		Graph.makeGraph(graphContent);
//		
		List<Integer> servers = new ArrayList<Integer>();
		servers.add(7);
		servers.add(43);
		servers.add(22);
		servers.add(13);
		servers.add(37);
		servers.add(15);
		servers.add(38);
		
		setSuperSource(servers);
		long startTime = System.nanoTime();
		
		long endTime = System.nanoTime();
		System.out.println((endTime - startTime) / 1000000);
	}
}
