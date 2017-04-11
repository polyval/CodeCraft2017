package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.filetool.util.FileUtil;

public class Search {
	
	private static int bestCost;
	private static List<Path> solution = new ArrayList<Path>();
	private static long startTime = System.nanoTime();
	private static boolean debug = true;
	public static List<Integer> bestServers = new ArrayList<Integer>();
	public static List<Integer> bestServerTypes = new ArrayList<Integer>();
	private static List<Integer> curServerTypes = new ArrayList<Integer>();
	private static List<Integer> candidateServers = new ArrayList<Integer>();
	private static Node[] nodes;
	
	public static String[] getResults(String[] graphContent) {
		Graph.makeGraph(graphContent);
		
		dropDeterministicMove();
		Zkw.computeFlowCostGivenServers(bestServers, bestServerTypes);
		solution = Zkw.getPaths();
		
		String[] res = new String[solution.size() + 2];
		res[0] = String.valueOf(solution.size());
		res[1] = "";
		for (int i = 2; i < res.length; i++) {
			res[i] = solution.get(i - 2).toString(); 
		}
		
		return res;
	}
	
	private static void dropDeterministicMove() {
		initialize();
		
		Map<Integer, Integer> serverIndex = new HashMap<Integer, Integer>();
		for (int i = 0; i < bestServers.size(); i++) {
			serverIndex.put(bestServers.get(i), i);
		}
//		Collections.shuffle(bestServers);
//		sortByVertexCost(bestServers);
//		sortServers(bestServers);
//		Collections.reverse(bestServers);
		int i = 0;
		int newCost = 0;
		// Drop
		while (i < bestServers.size()) {
			int removedServer = bestServers.get(i);
			int removedServerType = bestServerTypes.get(i);
			bestServers.remove(i);
			bestServerTypes.remove(i);
			
			newCost = getAllCost(bestServers, bestServerTypes);
			if (newCost < bestCost) {
				bestCost = newCost;
				candidateServers.add(removedServer);
				if (debug) {
					System.out.println("new best servers location by dropping" + bestServers);
					System.out.println("new best cost:" + bestCost);
				}
			}
			else {
				bestServers.add(i, removedServer);
				bestServerTypes.add(i, removedServerType);
				i++;
			}
		}
		
//		reintroduceDroppedServers(candidateServers, serverIndex);
//		addDroppedServers(candidateServers);
		addNeighbor();
		modifyServerType();
		bestCost = getAllCost(bestServers, bestServerTypes);
		System.out.println("Split line------------------");
//		drop();
	}
	
	private static void selectiveDrop() {
		initialize();
		sortServers(bestServers);
		Collections.reverse(bestServers);
		
		Map<Integer, Integer> serverIndex = new HashMap<Integer, Integer>();
		for (int i = 0; i < Graph.clientVertexNum; i++) {
			serverIndex.put(bestServers.get(i), i);
		}
		
		int i = 0;
		int newCost = 0;
		Set<Integer> tabu = new HashSet<Integer>();
		// Drop
		while (i < bestServers.size()) {
			if (tabu.contains(bestServers.get(i))) {
				i++;
				continue;
			}
			
			int bestRemoveIndex = -1;
			int curRemoveIndex = i;
			int curBestCost = Integer.MAX_VALUE;
			while (curRemoveIndex < bestServers.size() && curRemoveIndex - i < 10) {
				int curRemovedServer = bestServers.get(curRemoveIndex);
				int curRemovedServerType = bestServerTypes.get(curRemoveIndex);
				bestServers.remove(curRemoveIndex);
				newCost = getAllCost(bestServers, bestServerTypes);
				
				bestServers.add(curRemoveIndex, curRemovedServer);
				bestServerTypes.add(curRemoveIndex, curRemovedServerType);
				
				// Not feasible to drop.
				if (newCost >= bestCost) {
					tabu.add(curRemovedServer);
					curRemoveIndex++;
					continue;
				}
				
				if (newCost < curBestCost) {
					curBestCost = newCost;
					bestRemoveIndex = curRemoveIndex;
				}
				
				curRemoveIndex++;
			}
			
			if (curBestCost < bestCost) {
				bestCost = curBestCost;
				candidateServers.add(bestServers.get(bestRemoveIndex));
				bestServers.remove(bestRemoveIndex);
				bestServerTypes.remove(bestRemoveIndex);
				if (debug) {
					System.out.println("new best servers location by dropping" + bestServers);
					System.out.println("new best cost:" + bestCost);
				}
			}
			else {
				i++;
			}
		}
		// Move
//		addDroppedServers(candidateServers);
//		reintroduceDroppedServers(candidateServers, serverIndex);
//		selectiveAdd(candidateServers);
//		addServerAscent(candidateServers);
//		drop();
		addNeighbor();
		modifyServerType();
		bestCost = getAllCost(bestServers, bestServerTypes);
	}
	
	private static int getAllCost(List<Integer> servers, List<Integer> serverTypes)
	{
//		long start = System.nanoTime();
		Zkw.computeFlowCostGivenServers(servers, serverTypes);
//		System.out.println((System.nanoTime() - start) / 1000000 + "ms");
		
		if (Zkw.totalFlow < Graph.totalFlow) {
			return Integer.MAX_VALUE;
		}
		
		List<Integer> userdServers = new ArrayList<Integer>();
		for (Edge e : Graph.resAdj[Graph.vertexNum]) {
			if (e.residualFlow < e.bandwidth) {
				userdServers.add(e.target);
			}
		}
		
		for (int i = 0; i < userdServers.size(); i++) {
			int serverVertex = userdServers.get(i);
			Zkw.totalCost += Graph.diffServerCost.get(serverTypes.get(servers.indexOf(Integer.valueOf(serverVertex))));
			Zkw.totalCost += Graph.vertexCost[serverVertex];
		}
		
		return Zkw.totalCost;
	}
	
	private static void selectiveAdd(List<Integer> promisingServers) {
		int startIndex = 0;
		int newCost = 0;
		while (!promisingServers.isEmpty() && startIndex < promisingServers.size() && (System.nanoTime() - startTime) / 1000000 < 88000) {
			int bestIndex = -1;
			int curIndex = startIndex;
			int curBestCost = Integer.MAX_VALUE;
			while (curIndex < promisingServers.size() && curIndex - startIndex < 10) {
				int server = promisingServers.get(curIndex);
				if (bestServers.contains(server)) {
					curIndex++;
					continue;
				}
				
				bestServers.add(server);
				newCost = getAllCost(bestServers, bestServerTypes);
				bestServers.remove(bestServers.size() - 1);
				
				if (newCost < curBestCost) {
					curBestCost = newCost;
					bestIndex = curIndex;
				}
				
				curIndex++;
			}
			
			if (curBestCost < bestCost) {
				bestCost = curBestCost;
				bestServers.add(promisingServers.get(bestIndex));
				promisingServers.remove(bestIndex);
				System.out.println("new best servers location by selective adding" + bestServers);
				System.out.println("new best cost:" + bestCost);
			}
			else {
				startIndex++;
			}
		}
	}
	
	private static void drop() {
		int dropIndex = 0;
		int newCost;
		while (dropIndex < bestServers.size() && (System.nanoTime() - startTime) / 1000000 < 88000) {
			int droppedServer = bestServers.get(dropIndex);
			int droppedServerType = bestServerTypes.get(dropIndex);
			bestServers.remove(dropIndex);
			bestServerTypes.remove(dropIndex);
			
			newCost = getAllCost(bestServers, bestServerTypes);
			if (newCost < bestCost) {
				bestCost = newCost;
				candidateServers.add(droppedServer);
				if (debug) {
					System.out.println("new best servers location by dropping" + bestServers);
					System.out.println("new best cost:" + bestCost);
				}
			}
			else {
				bestServers.add(dropIndex, droppedServer);
				bestServerTypes.add(dropIndex, droppedServerType);
				dropIndex++;
			}
		}
	}
	
	private static void addDroppedServers(List<Integer> removed) {
		sortServers(removed);
		
		int i = 0;
		while (i < removed.size() && (System.nanoTime() - startTime) / 1000000 < 88000) {
			bestServers.add(removed.get(i));
			bestServerTypes.add(0);
			int newCost = getAllCost(bestServers, bestServerTypes);
			if (newCost < bestCost) {
				// Update removed list.
				removed.remove(i);
				bestCost = newCost;
				System.out.println("new best servers location by adding" + bestServers);
				System.out.println("new best cost:" + bestCost);
			}
			else {
				bestServers.remove(bestServers.size() - 1);
				bestServerTypes.remove(bestServerTypes.size() - 1);
				i++;
			}
		}
	}
	
	private static void reintroduceDroppedServers(List<Integer> removed, Map<Integer, Integer> serverIndex) {
		sortServers(removed);
		
		for (int i = 0; i < removed.size(); i++) {
			if ((System.nanoTime() - startTime) / 1000000 > 88000) {
				break;
			}
			int count = 0;
			int server = removed.get(i);
			int index = serverIndex.get(server);
			int newCost;
			for (int j = 0; j < bestServers.size(); j++) {
				if (count >= 20 || (System.nanoTime() - startTime) / 1000000 > 88000) {
					break;
				}
				if (serverIndex.get(bestServers.get(j)) < index) {
					continue;
				}
				
				count++;
				List<Integer> newServers = new ArrayList<Integer>(bestServers);
				int changedServer = newServers.get(j);
				newServers.set(j, server);
				newCost = getAllCost(newServers, bestServerTypes);
				if (newCost < bestCost) {
					// Update removed list.
					removed.set(i, changedServer);
					bestCost = newCost;
					bestServers = new ArrayList<Integer>(newServers);
					if (debug) {
						System.out.println("new best servers location by moving" + bestServers);
						System.out.println("new best cost:" + bestCost);
					}
					break;
				}
			}
		}
	}
	
	
	
	private static List<Integer> getClientNeighbor() {
		List<Integer> neighbor = new ArrayList<Integer>();
		for (int client : Graph.clientVertexId) {
			for (Edge e : Graph.adj[client]) {
				if (!neighbor.contains(e.target)) {
//					new Node(e.target, 0);
					neighbor.add(e.target);
				}
			}
		}
		return neighbor;
	}
	
	private static void addNeighbor() {
//		List<Integer> neighbor = getClientNeighbor();
//		for (int client : Graph.clientVertexId) {
//			if (!neighbor.contains(client)) {
//				neighbor.add(client);
//			}
//		}
		List<Integer> neighbor = new ArrayList<Integer>();
		for (int i = 0; i < Graph.vertexNum; i++) {
			neighbor.add(i);
		}
		
		sortServers(neighbor);
		addServerAscent(neighbor);
	}
	
	private static void addServerAscent(List<Integer> candidateServers) {
		List<Integer> tempServers = new ArrayList<Integer>(bestServers);
		List<Integer> tempServerTypes = new ArrayList<Integer>(bestServerTypes);
		int tempCost = bestCost;
		
		int i = 0;
		while (i < candidateServers.size() && (System.nanoTime() - startTime) / 1000000 < 88000) {
			int newServer = candidateServers.get(i);
			i++;
			if (bestServers.contains(newServer)) {
				continue;
			}
			
			bestServers.add(newServer);
			for (int j = 0; j < Graph.diffServerCost.size(); j++) {
				if (Graph.diffServerCapacity.get(j) >= Node.getNode(newServer).getOutput() || j == Graph.diffServerCost.size() - 1) {
					bestServerTypes.add(j);
					break;
				}
			}
			bestCost = getAllCost(bestServers, bestServerTypes);
			drop();
			if (bestCost < tempCost) {
				tempServers = new ArrayList<Integer>(bestServers);
				tempCost = bestCost;
				tempServerTypes = new ArrayList<Integer>(bestServerTypes);
				if (debug) {
					System.out.println("new best servers location by adding neighbor" + newServer);
					System.out.println("new best cost:" + bestCost);
				}
				i = 0;
			}
			else {
				bestServers = new ArrayList<Integer>(tempServers);
				bestCost = tempCost;
				bestServerTypes = new ArrayList<Integer>(tempServerTypes);
			}
		}
		
//		for (int newServer : candidateServers) {
//			if ((System.nanoTime() - startTime) / 1000000 > 88000) {
//				break;
//			}
//			if (bestServers.contains(newServer)) {
//				continue;
//			}
//			bestServers.add(newServer);
//			bestCost = getAllCost(bestServers);
//			drop();
//			if (bestCost < tempCost) {
//				tempServers = new ArrayList<Integer>(bestServers);
//				tempCost = bestCost;
//				System.out.println("new best servers location by adding neighbor" + bestServers);
//				System.out.println("new best cost:" + bestCost);
//			}
//			else {
//				bestServers = new ArrayList<Integer>(tempServers);
//				bestCost = tempCost;
//			}
//		}
	}
	
	private static void initialize() {
		initializeNodes();
		for (int i = 0; i < Graph.clientVertexNum; i++) {
			bestServers.add(Graph.clientVertexId[i]);
			for (int j = 0; j < Graph.diffServerCost.size(); j++) {
				if (Graph.diffServerCapacity.get(j) >= Node.getNode(i).getOutput() || j == Graph.diffServerCost.size() - 1) {
					bestServerTypes.add(j);
					bestCost += Graph.diffServerCost.get(j);
					bestCost += Graph.vertexCost[i];
					break;
				}
			}
		}
		
//		Zkw.computeFlowCostGivenServers(bestServers, bestServerTypes);
//		bestCost = Zkw.totalCost;
//		bestServerTypes.clear();
//		bestServers.clear();
//		for (Edge edge : Graph.resAdj[Graph.vertexNum]) {
//			if (edge.residualFlow < edge.bandwidth) {
//				bestServers.add(edge.target);
//				bestServerTypes.add(3);
//				bestCost += Graph.diffServerCost.get(3);
//				bestCost += Graph.vertexCost[edge.target];
//			}
//		}
	}
	
	private static void sortByVertexCost(List<Integer> servers) {
		Node[] sortHelper = new Node[servers.size()];
		for (int i = 0; i < servers.size(); i++) {
			sortHelper[i] = Node.getNode(servers.get(i));
		}
		
		Arrays.sort(sortHelper);
		servers.clear();
		for (int i = 0; i < sortHelper.length; i++) {
			servers.add(sortHelper[i].vertexId);
		}
	}
	
	private static void initializeNodes() {
		nodes = new Node[Graph.vertexNum];
		for (int i = 0; i < Graph.vertexNum; i++) {
			int demand = 0;
			if (Graph.vertexToClient.containsKey(i)) {
				demand = Graph.clientDemand[Graph.vertexToClient.get(i)];
			}
			nodes[i] = new Node(i, demand, Graph.vertexCost[i]);
		}
	}
	
	public static void modifyServerType() {
		Zkw.computeFlowCostGivenServers(bestServers, bestServerTypes);
		for (int i = 0; i < bestServers.size(); i++) {
			int serverType = bestServerTypes.get(i);
			int server = bestServers.get(i);
			int usedBandwidth = Graph.resAdj[server].get(Graph.resAdj[server].size() - 1).residualFlow;
			for (int j = 0; j < Graph.diffServerCapacity.size(); j++) {
				if (j >= serverType) {
					break;
				}
				if (Graph.diffServerCapacity.get(j) >= usedBandwidth) {
					bestServerTypes.set(i, j);
					if (debug) {
						System.out.println("modify");
					}
					break;
				}
			}
		}
	}
	
	public static void analyze() {
		Map<Integer, Integer> count = new HashMap<Integer, Integer>();
		for (int i = 0; i < Graph.diffServerCapacity.size(); i++) {
			count.put(i, 0);
		}
		
		for (int i = 0; i < Graph.clientDemand.length; i++) {
			for (int j = 0; j < Graph.diffServerCapacity.size(); j++) {
				if (j == Graph.diffServerCapacity.size() - 1) {
					int c = count.get(j) + 1;
					count.put(j, c);
				}
				if (Graph.clientDemand[i] < Graph.diffServerCapacity.get(j)) {
					int c = count.get(j) + 1;
					count.put(j, c);
					break;
				}
			}
		}
		System.out.println(count);
	}
	
	public static void isAllClients() {
		List<Integer> diff = new ArrayList<Integer>();
		for (int server : bestServers) {
			boolean isClient = false;
			for (int client : Graph.clientVertexId) {
				if (client == server) {
					isClient = true;
				}
			}
			if (!isClient) {
				diff.add(server);
			}
		}
		System.out.println("Diff: " + diff);
	}
	
	/**
	 * Sorts the servers by its potential maximum output.
	 * 
	 */
	private static void sortServers(List<Integer> removed) {
		List<Node> removedServers = new ArrayList<Node>();
		for (int removedServer : removed) {
			removedServers.add(nodes[removedServer]);
		}
		Collections.sort(removedServers, new Comparator<Node>() {

			@Override
			public int compare(Node o1, Node o2) {
				return o2.getOutput() - o1.getOutput();
			}
			
		});
		removed.clear();
		for (int j = 0; j < removedServers.size(); j++) {
			removed.add(removedServers.get(j).vertexId);
		}
	}
	
	public static void main(String[] args) {
		String[] graphContent;
		if (debug) {
			graphContent = FileUtil.read("E:\\codecraft\\cdn\\case_example\\2\\case0.txt", null);
		}
		else {
			graphContent = FileUtil.read(args[0], null);
		}
		Graph.makeGraph(graphContent);
		
		long startTime = System.nanoTime();
//		analyze();
		dropDeterministicMove();
//		selectiveDrop();
		if (debug) {
			System.out.println(bestServers.size());
			System.out.println(bestServers);
			System.out.println(bestServerTypes);
			Zkw.clear();
			Zkw.setSuperSource(bestServers, bestServerTypes);
			Zkw.computeMinCostFlow();
			solution = Zkw.getPaths();
			System.out.println(solution);
			long endTime = System.nanoTime();
			System.out.println((endTime - startTime) / 1000000 + "ms");
		}
		else {
			System.out.println(args[0] + ":");
			isAllClients();
		}
		System.out.println(bestCost);
	}
}
