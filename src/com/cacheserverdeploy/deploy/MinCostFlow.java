/**
 * 
 */
package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.filetool.util.FileUtil;

/**
 * @author JwZhou
 *
 */
public class MinCostFlow {
	public static int cost = 0;
	public static List<Integer> servers = new ArrayList<Integer>();
	public static Random random = new Random();
	
	public static boolean bellmanFord(int source, int sink, int[] parentVertex, Edge[] parentEdge) {
		int vertexNum = Graph.resVertexNum;
		
		int[] distance = new int[vertexNum];
		// Initialize
		for (int i = 0; i < vertexNum; i++) {
			parentVertex[i] = -1;
			distance[i] = Integer.MAX_VALUE;
		}
		// Find the shortest augmented path.
		distance[source] = 0;
		for(int i = 0; i< vertexNum -1; i++){
			// loop on all edges
			for (int u = 0; u < vertexNum; u++) {
				// Edges
				for (int e = 0; e < Graph.resAdj[u].size(); e++) {
					// Edge has residual flow.
					if (Graph.resAdj[u].get(e).residualFlow > 0) {
						Edge edge = Graph.resAdj[u].get(e);
						// The other end of the edge.
						int v = edge.target;
						int w = edge.cost;
						// Relax. !! Integer.MAX_VALUE + 3 < 0
						if (distance[u] != Integer.MAX_VALUE && distance[v] > distance[u] + w) {
							// Update.
							distance[v] = distance[u] + w;
							parentVertex[v] = u;
							parentEdge[v] = edge;
 						}
					}
				}
			}
		}
		
		if (parentVertex[sink] == -1) {
			return false;
		}
		return true;
	}
	
	public static int[] getMinCostFlow(int source, int sink, int requiredFlow, List<ArrayList<Integer>> allPaths) {
		int[] res = new int[2];
		int curFlow = 0;
		int curCost = 0;
		int[] parentVertex = new int[Graph.resVertexNum];
		Edge[] parentEdge = new Edge[Graph.resVertexNum];
		
		
		while (bellmanFord(source, sink, parentVertex, parentEdge))  {
			int pathFlow = Integer.MAX_VALUE;
			ArrayList<Integer> path = new ArrayList<Integer>();
			// Get the path flow.
			for (int v = sink; v != source; v = parentVertex[v]) {
				pathFlow = Math.min(pathFlow, parentEdge[v].residualFlow);
				if (v != sink && v != source) {
					path.add(v);
				}
			}
			
			// Get the right path.
			Collections.reverse(path);
//			System.out.println(path);
			allPaths.add(path);
			
			pathFlow = Math.min(pathFlow, requiredFlow - curFlow);
			
			// Update the edge flow.
			for (int v = sink; v != source; v = parentVertex[v]) {
				Edge edge = parentEdge[v];
				Edge counterEdge = edge.counterEdge;
				edge.residualFlow -= pathFlow;
				counterEdge.residualFlow += pathFlow;
				
				curCost += pathFlow * (edge.cost);
			}
			
			curFlow += pathFlow;
			if (curFlow == requiredFlow) {
				break;
			}
		}
		
		res[0] = curFlow;
		res[1] = curCost;
		
		return res;
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
	
	
	public static void greedy() {
		
	}
	
	/**
	 * Reduced VNS.
	 */
	public static void rvns() {
		// Get initial Solution
		for (int i : Graph.clientVertexId) {
			servers.add(i);
		}
		cost = Graph.serverCost * Graph.clientVertexNum;
		
		// Drop one
		//      Local search
		// Drop two and add one.
		// Move one.
		// Move Two...
		
		int k = 1;
		int count = 0;
		int dropIndex = 0;
		int dropK = 1;
		while ((System.nanoTime() - Search.startTime) / 1000000 < 49000) {
			
			// Indicate we haven't tried all the possible drops.
			if (dropIndex < servers.size()) {
				// Drop one has improved the solution.
				if (dropOne(dropIndex)) {
					System.out.println("new best cost by drop " + servers);
					// Try to drop next.
					dropIndex++;
					continue;
				}
				// Current drop is unsuccessful, try to drop next server in the next iteration.
				dropIndex++;
			}
			
//			if (dropK < servers.size()) {
//				if (dropK(dropK)) {
//					System.out.println("new best cost by drop " + servers + " " + cost);
//					dropK++;
//					continue;
//				}
//			}
//			else {
//				dropK = 1;
//				if (dropK(dropK)) {
//					System.out.println("new best cost by drop " + servers + " " + cost);
//					dropK++;
//					continue;
//				}
//			}
			
			// Drop two add one.
			if (dropTwoAddOne()) {
				// The servers has been updated, we can drop from start again.
				dropIndex = 0;
				continue;
			}
			
			// In this case, move won't help.
//			if (cost == Graph.clientVertexNum * Graph.serverCost) {
//				continue;
//			}
			
			// Number of successive iterations with no improvement.
			count++;
			if (k > 3) {
				k = 1;
			}
			List<Integer> newServers = getRandomServersFromNeighbor(k);
			if (isBetter(newServers)) {
				count = 0;
				dropIndex = 0;
				System.out.println("new best cost by move " + k + "servers, new servers " + servers);
				System.out.println("new best cost by move" + k + "servers, new servers " + cost);
			}
			else {
				System.out.println("no best found " + k + "servers, new servers " + servers);
				k++;
			}
			
			if (count > 1000) {
				return;
			}
		}
	}
	
	
	/**
	 * Gets new server combination.
	 * 
	 * @param k - number of servers to change.
	 * @return
	 */
	public static List<Integer> getRandomServers(int k) {
		Random random = new Random();
		
		List<Integer> newServers = new ArrayList<Integer>(servers);
		List<Integer> randoms = new ArrayList<Integer>();
		
		for (int i = 0; i < newServers.size(); i++) {
			randoms.add(i);
		}
		
		Collections.shuffle(randoms);
		
		for (int i = 0; i < k; i++) {
			int inIndex = random.nextInt(Graph.vertexNum);
			while (newServers.contains(inIndex)) {
				inIndex = random.nextInt(Graph.vertexNum);
			}
			newServers.set(randoms.get(i), inIndex);
		}
		return newServers;
	}
	
	public static List<Integer> getRandomServersFromNeighbor(int k) {
		Random random = new Random();
		
		List<Integer> newServers = new ArrayList<Integer>(servers);
		List<Integer> randoms = new ArrayList<Integer>();
		
		List<Edge> candidateEdges = new ArrayList<Edge>();
		
		for (int i = 0; i < newServers.size(); i++) {
			randoms.add(i);
		}
		
		Collections.shuffle(randoms);
		
		for (int i = 0; i < k; i++) {
			candidateEdges.addAll(Graph.adj[randoms.get(i)]);
		}
		Collections.shuffle(candidateEdges);
		
		for (int i = 0; i < k; i++) {
			for (Edge edge : candidateEdges) {
				if (!newServers.contains(edge.target)) {
					newServers.set(randoms.get(i), edge.target);
					break;
				}
			}
		}
		
		return newServers;
	}
	
	public static boolean dropOne(int dropIndex) {
		List<Integer> newServers = new ArrayList<Integer>(servers);
		newServers.remove(dropIndex);
		
		return isBetter(newServers);
	}
	
	public static boolean dropK(int k) {
		List<Integer> newServers = new ArrayList<Integer>(servers);
		for (int i = 0; i < k; i++) {
			newServers.remove(random.nextInt(newServers.size()));
		}
		return isBetter(newServers);
	}
	
	public static boolean dropTwoAddOne() {
		List<Integer> newServers = new ArrayList<Integer>(servers);
		// Get indices to drop.
		newServers.remove(random.nextInt(newServers.size()));
		newServers.remove(random.nextInt(newServers.size()));
		
		int addServer = random.nextInt(Graph.vertexNum);
		while (servers.contains(addServer)) {
			addServer = random.nextInt(Graph.vertexNum);
		}
		
		// Construct new servers.
		newServers.add(addServer);
		
		return isBetter(newServers);
	}
	
	public static boolean isBetter(List<Integer> newServers) {
		clear();
		setSuperSource(newServers);
		
		List<ArrayList<Integer>> allPaths = new ArrayList<ArrayList<Integer>>();
		int[] flowCost = getMinCostFlow(Graph.vertexNum, Graph.vertexNum + 1, Graph.totalFlow, allPaths);
		int newCost = flowCost[1];
		int flow = flowCost[0];
		// Not feasible
		if (flow < Graph.totalFlow) {
			return false;
		}
		
		newServers.clear();
		for (Edge e : Graph.resAdj[Graph.vertexNum]) {
			if (e.residualFlow < Integer.MAX_VALUE) {
				newServers.add(e.target);
			}
		}
		
		newCost += newServers.size() * Graph.serverCost;
		if ( newCost < cost) {
			cost = newCost;
			servers = newServers;
			return true;
		}
		return false;
	}
	
	public static void main(String[] args) {
		String[] graphContent = FileUtil.read("E:\\codecraft\\cdn\\case_example\\case3.txt", null);
		Graph.makeGraph(graphContent);
		
//		List<Integer> servers = new ArrayList<Integer>();
//		servers.add(44);
//		servers.add(7);
//		servers.add(13);
//		servers.add(15);
//		servers.add(22);
//		servers.add(34);
//		servers.add(38);
		
//		setSuperSource(servers);
		long startTime = System.nanoTime();
		rvns();
		System.out.println(cost);
		
//		System.out.println(getMinCostFlow(0, 48, 500));
		long endTime = System.nanoTime();
		System.out.println((endTime - startTime) / 1000000 + "ms");
	}
	
}
