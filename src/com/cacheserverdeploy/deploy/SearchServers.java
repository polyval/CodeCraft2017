/**
 * 
 */
package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
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
	public static LinkedList<List<Integer>> history = new LinkedList<List<Integer>>();
	
	/**
	 * Reduced VNS.
	 */
	public static void rvns() {
		// Get initial Solution
		for (int i : Graph.clientVertexId) {
			servers.add(i);
		}
		cost = Graph.serverCost * Graph.clientVertexNum;
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
		boolean shouldSwitch = false;
		List<Integer> startServers = new ArrayList<Integer>(servers);
		while ((System.nanoTime() - startTime) / 1000000 < 89000) {
//			if (count >= moveLimit || cost == Graph.clientVertexNum * Graph.serverCost) {
////				 Indicate we haven't tried all the possible drops.
//				if (dropIndex < servers.size()) {
//					// Drop one has improved the solution.
//					if (dropOne(dropIndex)) {
//						System.out.println("new best cost by drop " + servers);
//						dropIndex++;
//						count = 0;
//						continue;
//					}
//					// Current drop is unsuccessful, try to drop next server in the next iteration.
//					dropIndex++;
//				}
//				
				// Drop two add one.
//				if (dropTwoAddOne()) {
//					// The servers has been updated, we can drop from start again.
//					dropIndex = 0;
//					continue;
//				}
//			}
//			if (count >= moveLimit || cost == Graph.clientVertexNum * Graph.serverCost) {
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
			
			if (count >= moveLimit) {
				dropIndex = 0;
			}
			
			if (count > 200) {
				shouldSwitch = true;
				startServers = servers;
			}
			
			if (count > 1000) {
//				while (!history.isEmpty()) {
//					List<Integer> preServers = history.pop();
//					int iterationTimes = 500;
//					int i = 0;
//					while (i < iterationTimes && (System.nanoTime() - startTime) / 1000000 < 89000) {
//						if (k >= Math.min(preServers.size(), 4)) {
//							k = 1;
//						}
//						List<Integer> preNewServers = getRandomServers(k, preServers);
//
//						int[] flowCost = Zkw.getFlowCostGivenServers(preNewServers);
//						int flow = flowCost[0];
//						int newCost = flowCost[1];
//						// Not feasible
//						if (flow < Graph.totalFlow) {
//							System.out.println("no best found servers, new servers " + preNewServers);
//							k++;
//							i++;
//							continue;
//						}
//						
//						preNewServers.clear();
//						for (Edge e : Graph.resAdj[Graph.vertexNum]) {
//							if (e.residualFlow < Integer.MAX_VALUE) {
//								preNewServers.add(e.target);
//							}
//						}
//						
//						newCost += preNewServers.size() * Graph.serverCost;
//						if (newCost < cost) {
//							cost = newCost;
//							servers = preNewServers;
//							i = 0;
//							System.out.println("found best cost " + servers);
//							continue;
//						}
//						k++;
//						i++;
//					}
//				}
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
		while ((System.nanoTime() - startTime) / 1000000 < 89000) {
			if (dropK >= servers.size()) {
				dropK = 1;
			}
			
			if (dropK(dropK)) {
				System.out.println("new best cost by drop " + servers + " " + cost);
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

		int[] flowCost = Zkw.getFlowCostGivenServers(newServers);
		int flow = flowCost[0];
		int newCost = flowCost[1];
		// Not feasible
		if (flow < Graph.totalFlow) {
			return false;
		}
		
		List<Integer> oldServers = new ArrayList<Integer>(newServers);
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
			if (oldServers.size() < Graph.clientVertexNum && oldServers.size() > newServers.size()) {
				while (history.size() > 3) {
					history.removeLast();
				}
				history.push(oldServers);
			}
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
	
	public static String[] getResults(String[] graphContent) {
		Graph.makeGraph(graphContent);
		
		rvns();
		Zkw.getFlowCostGivenServers(servers);
		solution = Zkw.getPaths();
		
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
//		rvns();
		rvnsForLargeInstance();
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
