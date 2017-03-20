/**
 * 
 */
package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.filetool.util.FileUtil;

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
//		for (int i : Graph.clientVertexId) {
//			servers.add(i);
//		}
//		cost = Graph.serverCost * Graph.clientVertexNum;
//		
		// Drop one
		//      Local search
		// Drop two and add one.
		// Move one.
		// Move Two...
		
		int k = 1;
		int count = 0;
		int dropIndex = 0;
		int moveLimit = 100;
		int dropK = 1;
		while ((System.nanoTime() - startTime) / 1000000 < 89000) {
			if (count >= moveLimit || cost == Graph.clientVertexNum * Graph.serverCost) {
//				 Indicate we haven't tried all the possible drops.
				if (dropIndex < servers.size()) {
					// Drop one has improved the solution.
					if (dropOne(dropIndex)) {
						System.out.println("new best cost by drop " + servers);
						dropIndex++;
						count = 0;
						continue;
					}
					// Current drop is unsuccessful, try to drop next server in the next iteration.
					dropIndex++;
				}
				
				// Drop two add one.
//				if (dropTwoAddOne()) {
//					// The servers has been updated, we can drop from start again.
//					dropIndex = 0;
//					continue;
//				}
			}
			
//			if (dropK < servers.size()) {
//				if (dropK(dropK)) {
//					System.out.println("new best cost by drop " + servers + " " + cost);
//					dropK++;
//					count = 0;
//					continue;
//				}
//			}
//			else {
//				dropK = 1;
//				if (dropK(dropK)) {
//					System.out.println("new best cost by drop " + servers + " " + cost);
//					dropK++;
//					count = 0;
//					continue;
//				}
//			}
			
//			// In this case, move won't help.
//			if (cost == Graph.clientVertexNum * Graph.serverCost) {
//				continue;
//			}
			
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
			}
			else {
				System.out.println("no best found " + k + "servers, new servers " + servers);
				k++;
			}
			
			if (count >= moveLimit) {
				dropIndex = 0;
			}
			
			if (count > 1000) {
				return;
			}
		}
	}
	
	
	public static void greedy() {
		int totalFlow = 0;
		List<Integer> newServers = new ArrayList<Integer>();
		while (totalFlow < Graph.totalFlow) {
			int bestCost = Integer.MAX_VALUE;
			int bestFlow = 0;
			int bestServer = -1;
			for (int i = 0; i < Graph.vertexNum; i++) {
				if (newServers.contains(i)) {
					continue;
				}
				newServers.add(i);
				
				Zkw.clear();
				Zkw.setSuperSource(newServers);
				int[] flowCost = Zkw.getMinCostFlow(Graph.vertexNum, Graph.vertexNum + 1);
				int curFlow = flowCost[0];
				int curCost = flowCost[1];
				
				if (curFlow < bestFlow || curFlow == 0) {
					newServers.remove(newServers.size() - 1);
					continue;
				}
				
				// curFlow >= bestFlow
				if (curFlow > bestFlow || curCost <= bestCost) {
					bestFlow = flowCost[0];
					bestCost = flowCost[1];
					bestServer = i;
				}
				
				newServers.remove(newServers.size() - 1);
			}
			
			newServers.add(bestServer);
			totalFlow = bestFlow;
			cost = bestCost + newServers.size() * Graph.serverCost;
		}
		servers = newServers;
	}
	
	
	/**
	 * Gets new server combination.
	 * 
	 * @param k - number of servers to change.
	 * @return
	 */
	public static List<Integer> getRandomServers(int k, List<Integer> parentServers) {
		Random random = new Random();
		
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
		Zkw.clear();
		Zkw.setSuperSource(newServers);

		int[] flowCost = Zkw.getMinCostFlow(Graph.vertexNum, Graph.vertexNum + 1);
		int flow = flowCost[0];
		int newCost = flowCost[1];
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
		// Develop version.
//		if (AnalyseUtil.cost[newServers.size() - 1] == 0) {
//			AnalyseUtil.cost[newServers.size() - 1] = newCost;
//		}
//		else {
//			AnalyseUtil.cost[newServers.size() - 1] = Math.min(AnalyseUtil.cost[newServers.size() - 1], newCost);
//		}
		 
		if (newCost < cost) {
			cost = newCost;
			servers = newServers;
			return true;
		}
		return false;
	}
	
	public static String[] getResults(String[] graphContent) {
		Graph.makeGraph(graphContent);
		
		rvns();
		Zkw.clear();
		Zkw.setSuperSource(servers);
		Zkw.getMinCostFlow(Graph.vertexNum, Graph.vertexNum + 1);
		
		String[] res = new String[solution.size() + 2];
		res[0] = String.valueOf(solution.size());
		res[1] = "";
		for (int i = 2; i < res.length; i++) {
			res[i] = solution.get(i - 2).toString(); 
		}
		
		return res;
	}
	
	public static void main(String[] args) {
		String[] graphContent = FileUtil.read("E:\\codecraft\\cdn\\case_example\\case98.txt", null);
		Graph.makeGraph(graphContent);

		long startTime = System.nanoTime();
		greedy();
		rvns();
		System.out.println(servers);
		Zkw.clear();
		Zkw.setSuperSource(servers);
		Zkw.getMinCostFlow(Graph.vertexNum, Graph.vertexNum + 1);
		solution = Zkw.getPaths();
		System.out.println(solution);
		System.out.println(Zkw.deepCheck(solution));
//		List<ArrayList<Integer>> allPaths = new ArrayList<ArrayList<Integer>>();
		long endTime = System.nanoTime();
		System.out.println((endTime - startTime) / 1000000 + "ms");
		System.out.println(cost);
//		AnalyseUtil.saveTofile();
	}
}
