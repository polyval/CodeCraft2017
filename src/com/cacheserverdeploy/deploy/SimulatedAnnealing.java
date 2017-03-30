package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.filetool.util.FileUtil;

public class SimulatedAnnealing {
	
	public static double bestCost;
	public static List<Path> solution = new ArrayList<Path>();
	public static long startTime = System.nanoTime();
	public static List<Integer> bestServers = new ArrayList<Integer>();
	public static Random random = new Random();
	
	public static int getAllCost(List<Integer> parentServers)
	{
		List<Integer> newServers = new ArrayList<Integer>(parentServers);
		long start = System.nanoTime();
		Zkw.computeFlowCostGivenServers(newServers);
		System.out.println((System.nanoTime() - start) / 1000000 + "ms");
		
		if (Zkw.totalFlow < Graph.totalFlow) {
			return Integer.MAX_VALUE;
		}
		
		newServers.clear();
		for (Edge e : Graph.resAdj[Graph.vertexNum]) {
			if (e.residualFlow < Integer.MAX_VALUE) {
				newServers.add(e.target);
			}
		}
		
		Zkw.totalCost += newServers.size() * Graph.serverCost;
		return Zkw.totalCost;
		
	}
	
	public static List<Integer> dropTAddK(List<Integer> parentServers, int t, int k) {
		List<Integer> newServers = new ArrayList<Integer>(parentServers);
		
		for (int i = t; i < t; i++) {
			newServers.remove(random.nextInt(newServers.size()));
		}
		
		for (int i = 0; i < k; i++) {
			int addServer = random.nextInt(Graph.vertexNum);
			while (parentServers.contains(addServer)) {
				addServer = random.nextInt(Graph.vertexNum);
			}
			
			// Construct new servers.
			newServers.add(addServer);
		}
		
		return newServers;
	}
	
	public static String[] getResults(String[] graphContent) {
		Graph.makeGraph(graphContent);
		
		dropDeterministic();
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
	
	public static void dropDeterministic() {
		initialize();
		
		int i = 0;
		int newCost = 0;
		// Drop
		while (i < bestServers.size()) {
			int removedServer = bestServers.get(i);
			bestServers.remove(i);
			newCost = getAllCost(bestServers);
			if (newCost < bestCost) {
				bestCost = newCost;
				System.out.println("new best servers location by dropping" + bestServers);
				System.out.println("new best cost:" + bestCost);
			}
			else {
				bestServers.add(i, removedServer);
				i++;
			}
		}
	}
	
	public static void selectiveDrop() {
		initialize();
		
		Map<Integer, Integer> serverIndex = new HashMap<Integer, Integer>();
		for (int i = 0; i < Graph.clientVertexNum; i++) {
			serverIndex.put(bestServers.get(i), i);
		}
		
		int i = 0;
		int newCost = 0;
		Set<Integer> tabu = new HashSet<Integer>();
		// Drop
		List<Integer> removed = new ArrayList<Integer>();
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
				removed.add(bestServers.get(bestRemoveIndex));
				bestServers.remove(bestRemoveIndex);
				System.out.println("new best servers location by dropping" + bestServers);
				System.out.println("new best cost:" + bestCost);
			}
			else {
				i++;
			}
		}
		// Move
//		selectiveAdd(removed);
		reintroduceDroppedServers(removed, serverIndex);
		drop();
		addDroppedServers(removed);
	}
	
	public static void selectiveAdd(List<Integer> promisingServers) {
		int startIndex = 0;
		int newCost = 0;
		while (!promisingServers.isEmpty() && startIndex < promisingServers.size()) {
			int bestIndex = -1;
			int curIndex = startIndex;
			int curBestCost = Integer.MAX_VALUE;
			while (curIndex < promisingServers.size() && curIndex - startIndex < 10) {
				bestServers.add(promisingServers.get(curIndex));
				newCost = getAllCost(bestServers);
				
				bestServers.remove(bestServers.size() - 1);
				if (newCost >= bestCost) {
					curIndex++;
					continue;
				}
				
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
				System.out.println("new best servers location by adding" + bestServers);
				System.out.println("new best cost:" + bestCost);
			}
			else {
				startIndex++;
			}
		}
	}
	
	public static void drop() {
		int dropIndex = 0;
		int newCost;
		while (dropIndex < bestServers.size() && (System.nanoTime() - startTime) / 1000000 < 88500) {
			int droppedServer = bestServers.get(dropIndex);
			bestServers.remove(dropIndex);
			
			newCost = getAllCost(bestServers);
			if (newCost < bestCost) {
				bestCost = newCost;
				System.out.println("new best servers location by dropping" + bestServers);
				System.out.println("new best cost:" + bestCost);
			}
			else {
				bestServers.add(dropIndex, droppedServer);
				dropIndex++;
			}
		}
	}
	
	public static void dropDeterministicMove() {
		initialize();
		
		Map<Integer, Integer> serverIndex = new HashMap<Integer, Integer>();
		for (int i = 0; i < Graph.clientVertexNum; i++) {
			serverIndex.put(bestServers.get(i), i);
		}
		
		bestCost = Graph.serverCost * Graph.clientVertexNum;
		int i = 0;
		int newCost = 0;
		// Drop
		List<Integer> removed = new ArrayList<Integer>();
		while (i < bestServers.size()) {
			int removedServer = bestServers.get(i);
			bestServers.remove(i);
			newCost = getAllCost(bestServers);
			if (newCost < bestCost) {
				bestCost = newCost;
				removed.add(removedServer);
				System.out.println("new best servers location by dropping" + bestServers);
				System.out.println("new best cost:" + bestCost);
			}
			else {
				bestServers.add(i, removedServer);
				i++;
			}
		}
		
		// Move
		addDroppedServers(removed);
		reintroduceDroppedServers(removed, serverIndex);
		drop();
	}
	
	public static void addDroppedServers(List<Integer> removed) {
		sortRemovedServers(removed);
		
		int i = 0;
		while (i < removed.size()) {
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
	
	public static void reintroduceDroppedServers(List<Integer> removed, Map<Integer, Integer> serverIndex) {
		sortRemovedServers(removed);
		
		for (int i = 0; i < removed.size(); i++) {
			if ((System.nanoTime() - startTime) / 1000000 > 88500) {
				break;
			}
			int count = 0;
			int server = removed.get(i);
			int index = serverIndex.get(server);
			int newCost;
			for (int j = 0; j < bestServers.size(); j++) {
				if (count >= 20 || (System.nanoTime() - startTime) / 1000000 > 88500) {
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
	
	/**
	 * Sorts the removed servers by its potential maximum output.
	 * 
	 * @param removed - removed servers.
	 */
	public static void sortRemovedServers(List<Integer> removed) {
		List<Client> removedServers = new ArrayList<Client>();
		for (int removedServer : removed) {
			removedServers.add(Client.getClient(removedServer));
		}
		Collections.sort(removedServers, new Comparator<Client>() {

			@Override
			public int compare(Client o1, Client o2) {
				return o2.getOutput() - o1.getOutput();
			}
			
		});
		removed.clear();
		for (int j = 0; j < removedServers.size(); j++) {
			removed.add(removedServers.get(j).vertexId);
		}
	}
	
	public static void initialize() {
		Client[] clients = new Client[Graph.clientVertexNum];
		for (int i = 0; i < Graph.clientVertexNum; i++) {
			clients[i] = new Client(Graph.clientVertexId[i], Graph.clientDemand[i]);
		}
		Arrays.sort(clients);
		for (Client client : clients) {
			bestServers.add(client.vertexId);
		}
		bestCost = Graph.clientVertexNum * Graph.serverCost;
	}
	
	public static void main(String[] args) {
		String[] graphContent = FileUtil.read("E:\\codecraft\\cdn\\case_example\\1\\case6.txt", null);
		Graph.makeGraph(graphContent);

		long startTime = System.nanoTime();
//		dropUntil();
//		dropDeterministicMove();
		selectiveDrop();
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
