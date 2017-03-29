/**
 * 
 */
package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * @author JwZhou
 *
 */
public class SearchServers {
	
	public static int cost = 0;
	public static List<Path> solution = new ArrayList<Path>();
	public static List<Integer> servers = new ArrayList<Integer>();
	public static Random random = new Random();
	public static long startTime = System.nanoTime();
	
	/**
	 * Reduced VNS.
	 */
	public static void rvns() {
		// Get initial Solution
		for (int i : Graph.clientVertexId) {
			servers.add(i);
		}
		cost = Graph.serverCost * Graph.clientVertexNum;
	
		int k = 1;
		int count = 0;
		boolean shouldSwitch = false;
		List<Integer> startServers = new ArrayList<Integer>(servers);
		while ((System.nanoTime() - startTime) / 1000000 < 89000) {
			
			// Number of successive iterations with no improvement.
			count++;
			if (k >= Math.min(servers.size(), 4)) {
				k = 1;
			}
			if (shouldSwitch) {
				startServers = servers;
			}
			List<Integer> newServers = getRandomServers(k, startServers);
			if (isBetter(newServers)) {
				count = 0;
				System.out.println("new best cost by move " + k + "servers, new servers " + servers);
				System.out.println("new best cost by move" + k + "servers, new servers " + cost);
				continue;
			}
			else {
				System.out.println("no best found " + k + "servers, new servers " + servers);
				k++;
			}
			
			if (count > 200) {
				shouldSwitch = true;
				startServers = servers;
			}
			
			if (count > 500) {
				return;
			}
		}
	}
	
	public static void rvnsForLargeInstance() {
		for (int i : Graph.clientVertexId) {
			servers.add(i);
		}
		cost = Graph.serverCost * Graph.clientVertexNum;
		
		int dropK = 1;
		int count = 0;
		int k = 1;
		while ((System.nanoTime() - startTime) / 1000000 < 88500) {
			if (dropK >= servers.size()) {
				dropK = 1;
			}
			
			if (dropK(dropK)) {
				System.out.println("new best cost by drop " + dropK + servers + " " + cost);
				dropK++;
				count = 0;
				continue;
			}

			// Number of successive iterations with no improvement.
			count++;
			if (k >= Math.min(servers.size(), 4)) {
				k = 1;
			}
			List<Integer> newServers = getRandomServers(k, servers);
			if (isBetter(newServers)) {
				count = 0;
				System.out.println("new best cost by move " + k + "servers, new servers " + servers);
				System.out.println("new best cost by move" + k + "servers, new servers " + cost);
				continue;
			}
			else {
				System.out.println("no best found " + k + "servers, new servers " + servers);
				k++;
			}
			if (count > 500) {
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
	public static List<Integer> getRandomServers(int k, List<Integer> parentServers) {
		
		List<Integer> newServers = new ArrayList<Integer>(parentServers);
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
	
	public static List<Integer> getRandomServersFromNeighbor(int k, List<Integer> parentServers) {
		
		List<Integer> newServers = new ArrayList<Integer>(parentServers);
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
	
	public static boolean addOne(List<Integer> parentServers) {
		List<Integer> newServers = new ArrayList<Integer>(parentServers);
		int newServer = random.nextInt(Graph.vertexNum);
		while (newServers.contains(newServer)) {
			newServer = random.nextInt(Graph.vertexNum);
		}
		return isBetter(newServers);
	}
	
	public static boolean dropK(int k) {
		List<Integer> newServers = new ArrayList<Integer>(servers);
		for (int i = 0; i < k; i++) {
			newServers.remove(random.nextInt(newServers.size()));
		}
		return isBetter(newServers);
	}
	
	public static boolean isBetter(List<Integer> newServers) {

		Zkw.computeFlowCostGivenServers(newServers);

		// Not feasible
		if (Zkw.totalFlow < Graph.totalFlow) {
			return false;
		}
		
		newServers.clear();
		for (Edge e : Graph.resAdj[Graph.vertexNum]) {
			if (e.residualFlow < Integer.MAX_VALUE) {
				newServers.add(e.target);
			}
		}
		
		Zkw.totalCost += newServers.size() * Graph.serverCost;
		 
		if (Zkw.totalCost < cost) {
			cost = Zkw.totalCost;
			servers = newServers;
			return true;
		}
		return false;
	}
	
	public static int getCurServersNumber() {
		int res = 0;
		for (Edge e : Graph.resAdj[Graph.vertexNum]) {
			if (e.residualFlow < Integer.MAX_VALUE) {
				res++;
			}
		}
		return res;
	}
	
	public static List<Integer> getCurServers() {
		List<Integer> curServers = new ArrayList<Integer>();
		for (Edge e : Graph.resAdj[Graph.vertexNum]) {
			if (e.residualFlow < Integer.MAX_VALUE) {
				curServers.add(e.target);
			}
		}
		return curServers;
	} 
}
