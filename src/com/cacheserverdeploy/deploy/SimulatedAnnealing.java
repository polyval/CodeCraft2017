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
	public static double temper;
	public static double bestCost;
	public static List<Path> solution = new ArrayList<Path>();
	public static long startTime = System.nanoTime();

	private static double COOLINGRATE = 0.95;

	public static List<Integer> bestServers = new ArrayList<Integer>();
	
	public static Random random = new Random(System.currentTimeMillis());
	
	
	
	private static void initialize() {
		for (int i : Graph.clientVertexId) {
			bestServers.add(i);
		}
		
		bestCost = Graph.serverCost * Graph.clientVertexNum;
		temper = bestCost;
	}
	
	public static void simulatedAnnealing(){

//		initialize();
		temper = bestCost;
		System.out.println("the initial servers location" + bestServers);
		System.out.println("intial cost:" + bestCost);
		
		List<Integer> curServers = new ArrayList<Integer>(bestServers);
		List<Integer> newServers = new ArrayList<Integer>();
		int curCost = 0;
		int newCost = 0;

		double delta;
		curCost = (int) bestCost;
		
		int k = 1;
		int count = 0;
		while ((System.nanoTime() - startTime) / 1000000 < 89000 && temper > 0.00000001) {
//			if (k >= 3) {
//				k = 1;
//			}
			newServers = dropTAddK(curServers, 2, 1);
			newCost = getAllCost(newServers);
			delta = newCost - curCost;
			
			if (delta < 0.0) {
//				k = 1;
				curServers = SearchServers.getCurServers();
				curCost = newCost;
		
				System.out.println("current best servers location" + curServers);
				System.out.println("current best cost:" + curCost);
			}
			else {
				double rnd = (double) (Math.random());
				double p = Math.exp(-delta / temper);
				if(p > rnd) {
					curServers = SearchServers.getCurServers();
					curCost = newCost;
					
					System.out.println("current servers location" + curServers);
					System.out.println("current cost:" + curCost);
				}
//				else {
//					k++;
//				}
			}


			if (newCost < bestCost) {
				count = 0;
				bestServers = new ArrayList<Integer>(newServers);
				bestCost = newCost;
				System.out.println("the best servers location" + bestServers);
				System.out.println("the best all cost:" + bestCost);
			}
			else {
				count++;
			}
			
			if (count > 50) {
				temper *= (1 + COOLINGRATE);
				count = 0;
			}
			temper *= COOLINGRATE;		
		}
	}
	
	public static void move() {
		initialize();
		List<Integer> curServers = new ArrayList<Integer>();
		int count = 0;
		int newCost = 0;
		while ((System.nanoTime() - startTime) / 1000000 < 88500) {
			if (count <= 50) {
				curServers = SearchServers.getRandomServers(1, bestServers);
			}
			else {
				for (int i = 0; i < bestServers.size(); i++) {
					curServers = new ArrayList<Integer>(bestServers);
					curServers.remove(i);
					newCost = getAllCost(curServers);
					if (newCost < bestCost) {
						bestServers = SearchServers.getCurServers();
						bestCost = newCost;
						System.out.println("the best servers location by dropping" + bestServers);
						System.out.println("the best all cost by dropping:" + bestCost);
						break;
					}
				}
				count = 0;
				continue;
			}
			newCost = getAllCost(curServers);
			if (newCost < bestCost) {
				bestServers = SearchServers.getCurServers();
				bestCost = newCost;
				System.out.println("the best servers location" + bestServers);
				System.out.println("the best all cost:" + bestCost);
			}
			if (curServers.size() == bestServers.size()) {
				count++;
			}
			else {
				count = 0;
			}
		}
	}
	
	public static int getAllCost(List<Integer> parentServers)
	{
		List<Integer> newServers = new ArrayList<Integer>(parentServers);
		long start = System.nanoTime();
		int[] flowCost = Zkw.getFlowCostGivenServers(newServers);
		System.out.println((System.nanoTime() - start) / 1000000 + "ms");
		int flow = flowCost[0];
		int cost = flowCost[1];
		
		if (flow < Graph.totalFlow) {
			return Integer.MAX_VALUE;
		}
		
		newServers.clear();
		for (Edge e : Graph.resAdj[Graph.vertexNum]) {
			if (e.residualFlow < Integer.MAX_VALUE) {
				newServers.add(e.target);
			}
		}
		
		cost += newServers.size() * Graph.serverCost;
		return cost;
		
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
		
		move();
		Zkw.getFlowCostGivenServers(bestServers);
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
		Client[] clients = new Client[Graph.clientVertexNum];
		for (int i = 0; i < Graph.clientVertexNum; i++) {
			clients[i] = new Client(Graph.clientVertexId[i], Graph.clientDemand[i]);
		}
		Arrays.sort(clients);
		for (Client client : clients) {
			bestServers.add(client.vertexId);
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
				System.out.println("new best servers location by dropping" + bestServers);
				System.out.println("new best cost:" + bestCost);
			}
			else {
				bestServers.add(i, removedServer);
				i++;
			}
		}
	}
	
	public static void dropDeterministicMove() {
		Client[] clients = new Client[Graph.clientVertexNum];
		for (int i = 0; i < Graph.clientVertexNum; i++) {
			clients[i] = new Client(Graph.clientVertexId[i], Graph.clientDemand[i]);
		}
		Arrays.sort(clients);
		for (Client client : clients) {
			bestServers.add(client.vertexId);
		}
		
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
		for (int j = 1; j < removedServers.size(); j++) {
			removed.add(removedServers.get(j).vertexId);
		}
		
		for (int server : removed) {
			if ((System.nanoTime() - startTime) / 1000000 > 88500) {
				break;
			}
			int count = 0;
			int index = serverIndex.get(server);
			for (int j = 0; j < bestServers.size(); j++) {
				if (count >= 10 || (System.nanoTime() - startTime) / 1000000 > 88500) {
					break;
				}
				if (serverIndex.get(bestServers.get(j)) < index) {
					continue;
				}
				
				count++;
				List<Integer> newServers = new ArrayList<Integer>(bestServers);
				newServers.set(j, server);
				newCost = getAllCost(newServers);
				if (newCost < bestCost) {
					bestCost = newCost;
					bestServers = new ArrayList<Integer>(newServers);
					System.out.println("new best servers location by moving" + bestServers);
					System.out.println("new best cost:" + bestCost);
					break;
				}
			}
		}
	}
	
	public static List<Integer> getMaxOutput() {
		List<Integer> outPut = new ArrayList<Integer>();
		for (int i = 0; i < bestServers.size(); i++) {
			int capacity = 0;
			for (Edge e : Graph.adj[bestServers.get(i)]) {
				capacity += e.bandwidth;
			}
			outPut.add(capacity);
		}
		return outPut;
	}
	
	public static void main(String[] args) {
		String[] graphContent = FileUtil.read("E:\\codecraft\\cdn\\case_example\\0\\case8.txt", null);
		Graph.makeGraph(graphContent);

		long startTime = System.nanoTime();
//		move();
//		moverefresh(); 
//		dropUntil();
		dropDeterministicMove();
//		simulatedAnnealing();
//		List<Integer> output = getMaxOutput();
//		for (int i = 0; i < bestServers.size(); i++) {
//			System.out.println(bestServers.get(i).toString() + ": " + output.get(i));
//		}
//		System.out.println(bestServers);
		System.out.println(bestServers.size());
		Zkw.clear();
		Zkw.setSuperSource(bestServers);
//		Zkw.getMinCostFlow(Graph.vertexNum, Graph.vertexNum + 1);
//		solution = Zkw.getPaths();
//		System.out.println(solution);
//		System.out.println(Zkw.deepCheck(solution));
//		List<ArrayList<Integer>> allPaths = new ArrayList<ArrayList<Integer>>();
		long endTime = System.nanoTime();
		System.out.println((endTime - startTime) / 1000000 + "ms");
		System.out.println(bestCost);
//		AnalyseUtil.saveTofile();
	}
}
