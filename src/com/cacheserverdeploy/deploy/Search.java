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
	private static List<Integer> bestServers = new ArrayList<Integer>();
	private static List<Integer> candidateServers = new ArrayList<Integer>();
	
	public static String[] getResults(String[] graphContent) {
		Graph.makeGraph(graphContent);
		
		dropDeterministicMove();
		Zkw.computeFlowCostGivenServers(bestServers);
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
		for (int i = 0; i < Graph.clientVertexNum; i++) {
			serverIndex.put(bestServers.get(i), i);
		}
		
		bestCost = Graph.serverCost * Graph.clientVertexNum;
		int i = 0;
		int newCost = 0;
		// Drop
		while (i < bestServers.size()) {
			int removedServer = bestServers.get(i);
			bestServers.remove(i);
			newCost = getAllCost(bestServers);
			if (newCost < bestCost) {
				bestCost = newCost;
				candidateServers.add(removedServer);
				System.out.println("new best servers location by dropping" + bestServers);
				System.out.println("new best cost:" + bestCost);
			}
			else {
				bestServers.add(i, removedServer);
				i++;
			}
		}
		
		// Move
		selectiveAdd(candidateServers);
		reintroduceDroppedServers(candidateServers, serverIndex);
		drop();
		addNeighbor();
	}
	
	private static void selectiveDrop() {
		initialize();
		
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
				bestServers.remove(curRemoveIndex);
				newCost = getAllCost(bestServers);
				
				bestServers.add(curRemoveIndex, curRemovedServer);
				
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
				System.out.println("new best servers location by dropping" + bestServers);
				System.out.println("new best cost:" + bestCost);
			}
			else {
				i++;
			}
		}
		// Move
//		selectiveAdd(candidateServers);
		addDroppedServers(candidateServers);
		reintroduceDroppedServers(candidateServers, serverIndex);
//		addServerAscent(candidateServers);
		drop();
		addNeighbor();
	}
	
	private static int getAllCost(List<Integer> servers)
	{
//		long start = System.nanoTime();
		Zkw.computeFlowCostGivenServers(servers);
//		System.out.println((System.nanoTime() - start) / 1000000 + "ms");
		
		if (Zkw.totalFlow < Graph.totalFlow) {
			return Integer.MAX_VALUE;
		}
		
		int serverNum = 0;
		for (Edge e : Graph.resAdj[Graph.vertexNum]) {
			if (e.residualFlow < Integer.MAX_VALUE) {
				serverNum++;
			}
		}
		
		Zkw.totalCost += serverNum * Graph.serverCost;
		return Zkw.totalCost;
		
	}
	
	private static void selectiveAdd(List<Integer> promisingServers) {
		int startIndex = 0;
		int newCost = 0;
		while (!promisingServers.isEmpty() && startIndex < promisingServers.size() && (System.nanoTime() - startTime) / 1000000 < 88500) {
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
				newCost = getAllCost(bestServers);
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
		while (dropIndex < bestServers.size() && (System.nanoTime() - startTime) / 1000000 < 88500) {
			int droppedServer = bestServers.get(dropIndex);
			bestServers.remove(dropIndex);
			
			newCost = getAllCost(bestServers);
			if (newCost < bestCost) {
				bestCost = newCost;
				candidateServers.add(droppedServer);
				System.out.println("new best servers location by dropping" + bestServers);
				System.out.println("new best cost:" + bestCost);
			}
			else {
				bestServers.add(dropIndex, droppedServer);
				dropIndex++;
			}
		}
	}
	
	private static void addDroppedServers(List<Integer> removed) {
		sortServers(removed);
		
		int i = 0;
		while (i < removed.size() && (System.nanoTime() - startTime) / 1000000 < 88500) {
			bestServers.add(removed.get(i));
			int newCost = getAllCost(bestServers);
			if (newCost < bestCost) {
				// Update removed list.
				removed.remove(i);
				bestCost = newCost;
				System.out.println("new best servers location by adding" + bestServers);
				System.out.println("new best cost:" + bestCost);
			}
			else {
				bestServers.remove(bestServers.size() - 1);
				i++;
			}
		}
	}
	
	private static void reintroduceDroppedServers(List<Integer> removed, Map<Integer, Integer> serverIndex) {
		sortServers(removed);
		
		for (int i = 0; i < removed.size(); i++) {
			if ((System.nanoTime() - startTime) / 1000000 > 60000) {
				break;
			}
			int count = 0;
			int server = removed.get(i);
			int index = serverIndex.get(server);
			int newCost;
			for (int j = 0; j < bestServers.size(); j++) {
				if (count >= 20 || (System.nanoTime() - startTime) / 1000000 > 60000) {
					break;
				}
				if (serverIndex.get(bestServers.get(j)) < index) {
					continue;
				}
				
				count++;
				List<Integer> newServers = new ArrayList<Integer>(bestServers);
				int changedServer = newServers.get(j);
				newServers.set(j, server);
				newCost = getAllCost(newServers);
				if (newCost < bestCost) {
					// Update removed list.
					removed.set(i, changedServer);
					bestCost = newCost;
					bestServers = new ArrayList<Integer>(newServers);
					System.out.println("new best servers location by moving" + bestServers);
					System.out.println("new best cost:" + bestCost);
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
					new Node(e.target, 0);
					neighbor.add(e.target);
				}
			}
		}
		return neighbor;
	}
	
	private static void addNeighbor() {
		List<Integer> neighbor = getClientNeighbor();
		for (int client : Graph.clientVertexId) {
			if (!neighbor.contains(client)) {
				neighbor.add(client);
			}
		}
//		List<Integer> neighbor = new ArrayList<Integer>();
//		for (int i = 0; i < Graph.vertexNum; i++) {
//			if (Client.getClient(i) == null) {
//				new Client(i, 0);
//			}
//			neighbor.add(i);
//		}
		
		sortServers(neighbor);
		addServerAscent(neighbor);
	}
	
	private static void addServerAscent(List<Integer> candidateServers) {
		List<Integer> tempServers = new ArrayList<Integer>(bestServers);
		int tempCost = bestCost;
		
		int i = 0;
		while (i < candidateServers.size() && (System.nanoTime() - startTime) / 1000000 < 88500) {
			int newServer = candidateServers.get(i);
			i++;
			if (bestServers.contains(newServer)) {
				continue;
			}
			bestServers.add(newServer);
			bestCost = getAllCost(bestServers);
			drop();
			if (bestCost < tempCost) {
				tempServers = new ArrayList<Integer>(bestServers);
				tempCost = bestCost;
				System.out.println("new best servers location by adding neighbor" + newServer);
				System.out.println("new best cost:" + bestCost);
				i = 0;
			}
			else {
				bestServers = new ArrayList<Integer>(tempServers);
				bestCost = tempCost;
			}
		}
		
//		for (int newServer : candidateServers) {
//			if ((System.nanoTime() - startTime) / 1000000 > 88500) {
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
	
	private static void moveBrutal() {
		int i = 0;
		while(i < bestServers.size()) {
			int moveOut = bestServers.get(i);
			boolean findBetter = false;
			for(int j = 0; j < Graph.vertexNum; j++) {
				if (bestServers.contains(j)) {
					continue;
				}
				bestServers.set(i, j);
				int newCost = getAllCost(bestServers);
				if (newCost < bestCost) {
					findBetter = true;
					bestCost = newCost;
					System.out.println("new best servers location by moving" + bestServers);
					break;
				}
				else {
					bestServers.set(i, moveOut);
				}
			}
			if (findBetter) {
				i = 0;
				continue;
			}
			i++;
		}
	}
	
	private static void initialize() {
		Node[] clients = new Node[Graph.clientVertexNum];
		for (int i = 0; i < Graph.clientVertexNum; i++) {
			clients[i] = new Node(Graph.clientVertexId[i], Graph.clientDemand[i]);
		}
		Arrays.sort(clients);
		for (Node client : clients) {
			bestServers.add(client.vertexId);
		}
		bestCost = Graph.clientVertexNum * Graph.serverCost;
	}
	
	/**
	 * Sorts the servers by its potential maximum output.
	 * 
	 */
	private static void sortServers(List<Integer> removed) {
		List<Node> removedServers = new ArrayList<Node>();
		for (int removedServer : removed) {
			removedServers.add(Node.getClient(removedServer));
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
		String[] graphContent = FileUtil.read("E:\\codecraft\\cdn\\case_example\\1\\case1.txt", null);
		Graph.makeGraph(graphContent);
		
		long startTime = System.nanoTime();
		dropDeterministicMove();
//		selectiveDrop();
//		Arrays.sort(Graph.clientVertexId);
//		System.out.println(Arrays.toString(Graph.clientVertexId));
//		List<Integer> output = getMaxOutput();
//		for (int i = 0; i < bestServers.size(); i++) {
//			System.out.println(bestServers.get(i).toString() + ": " + output.get(i));
//		}
//		System.out.println(bestServers);
		System.out.println(bestServers.size());
		Zkw.clear();
		Zkw.setSuperSource(bestServers);
		Zkw.computeMinCostFlow();
		solution = Zkw.getPaths();
		System.out.println(solution);
		long endTime = System.nanoTime();
		System.out.println((endTime - startTime) / 1000000 + "ms");
		System.out.println(bestCost);
	}
}
