/**
 * 
 */
package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
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
			temp = augment(source, source, sink, Integer.MAX_VALUE, dist, visited);
			while (temp > 0) {
				flow += temp;
				cost += temp * dist[source];
				Arrays.fill(visited, false);
				temp = augment(source, source, sink, Integer.MAX_VALUE, dist, visited);
			}
			
			if (!lable(sink, dist, visited)) {
				break;
			}
		}
		flowCost[0] = flow;
		flowCost[1] = cost;
		return flowCost;
	}
	
	public static int augment(int u, int source, int sink, int flow, int[] dist, boolean[] visited) {
		if (u == sink) {
			return flow;
		}
		visited[u] = true;
		for (Edge e : Graph.resAdj[u]) {
			int v = e.target;
			if (e.residualFlow > 0 && !visited[v] && dist[v] + e.cost == dist[u]) {
				int temp = augment(v, source, sink, Math.min(e.residualFlow, flow), dist, visited);
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
	
	public static boolean deepCheck(List<Path> solution) {
		int[][] edgesBandwidth = new int[Graph.vertexNum][Graph.vertexNum];
		int[] demands = new int[Graph.clientVertexNum];
		for (Path path : solution) {
			demands[path.clientId] += path.flow;
			for (int i = 0; i < path.nodes.size() - 1; i++) {
				edgesBandwidth[path.nodes.get(i)][path.nodes.get(i+1)] += path.flow;
			}
		}
		
		for (int i = 0; i < Graph.clientVertexNum; i++) {
			if (demands[i] != Graph.clientDemand[i]) {
				return false;
			}
		}
		
		for (int i = 0; i < Graph.vertexNum; i++) {
			for (int j = 0; j < Graph.vertexNum; j++) {
				if (edgesBandwidth[i][j] > Graph.edgeBandwidth[i][j]) {
					return false;
				}
			}
		}
		
		return true;
		
	}
	
	public static List<Path> getPaths() {
		List<Path> paths = new ArrayList<Path>();
		int sink = Graph.vertexNum + 1;
		int source = Graph.vertexNum;
		int totalFlow = 0;
		boolean[] visited = new boolean[Graph.resVertexNum];
		while (totalFlow < Graph.totalFlow) {
			Arrays.fill(visited, false);
			List<Edge> path = new ArrayList<Edge>();
			totalFlow += dfs(source, sink, path, paths, visited);
		}
		return paths;
	}
	
	public static int dfs(int u, int sink, List<Edge> path, List<Path> paths, boolean[] visited) {
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
			int curFlow = dfs(edge.target, sink, path, paths, visited);
			if (curFlow > 0) {
				return curFlow;
			}
			path.remove(path.size() - 1);
		}
		return 0;
	}
	
	public static void main(String[] args) {
		String[] graphContent = FileUtil.read("E:\\codecraft\\cdn\\case_example\\case99.txt", null);
		Graph.makeGraph(graphContent);
//		
		List<Integer> servers = new ArrayList<Integer>();
		servers.add(20);
		servers.add(26);
		servers.add(22);
		servers.add(48);
		servers.add(15);
		servers.add(12);
		servers.add(37);
		
		setSuperSource(servers);
		long startTime = System.nanoTime();
		
		List<Path> allPaths = new ArrayList<Path>();
		System.out.println(Arrays.toString(getMinCostFlow(1000, 1001)));
		allPaths = getPaths();
		System.out.println(allPaths);
		System.out.println(deepCheck(allPaths));
		long endTime = System.nanoTime();
		System.out.println((endTime - startTime) / 1000000 + "ms");
	}
}
