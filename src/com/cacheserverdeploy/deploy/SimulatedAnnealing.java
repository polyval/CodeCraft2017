package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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

		initialize();
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
			newServers = SearchServers.getRandomServers(1, curServers);
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
				double p = Math.exp(-delta / temper) / curServers.size();
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
	
	public static void moverefresh() {
		initialize();
		List<Integer> curServers = new ArrayList<Integer>();
		int count = 0;
		int newCost = 0;
		List<Integer> startServers = new ArrayList<Integer>(bestServers);
		while ((System.nanoTime() - startTime) / 1000000 < 88500) {
			count++;
			if (count % 10 == 0) {
				startServers = bestServers;
			}
			curServers = SearchServers.getRandomServers(1, startServers);
		
			newCost = getAllCost(curServers);
			if (newCost < bestCost) {
				bestServers = SearchServers.getCurServers();
				bestCost = newCost;
				System.out.println("the best servers location" + bestServers);
				System.out.println("the best all cost:" + bestCost);
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
	
	public static List<Integer> dropTwoAddOne(List<Integer> parentServers) {
		List<Integer> newServers = new ArrayList<Integer>(parentServers);
		// Get indices to drop.
		newServers.remove(random.nextInt(newServers.size()));
		newServers.remove(random.nextInt(newServers.size()));
		
		int addServer = random.nextInt(Graph.vertexNum);
		while (parentServers.contains(addServer)) {
			addServer = random.nextInt(Graph.vertexNum);
		}
		
		// Construct new servers.
		newServers.add(addServer);
		
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
	
	public static void drop() {
		initialize();
		List<Integer> newServers = new ArrayList<Integer>();
		while ((System.nanoTime() - startTime) / 1000000 < 88500) {
			newServers = new ArrayList<Integer>(bestServers);
			newServers.remove(random.nextInt(newServers.size()));
			int newCost = getAllCost(newServers);
			if (newCost < bestCost) {
				bestCost = newCost;
				bestServers = new ArrayList<Integer>(newServers);
				System.out.println("the best servers location" + bestServers);
				System.out.println("the best all cost:" + bestCost);
			}
		}
	}
	
	public static void dropUntil() {
		initialize();
		List<Integer> newServers = new ArrayList<Integer>();
		Set<Integer> tabu = new HashSet<Integer>();
		int removeIndex = 0;
		while ((System.nanoTime() - startTime) / 1000000 < 88500) {
			newServers = new ArrayList<Integer>(bestServers);
			for (int i = 0; i < newServers.size(); i++) {
				removeIndex = random.nextInt(newServers.size());
				if (tabu.contains(newServers.get(removeIndex))) {
					continue;
				}
				break;
			}
			
			if (tabu.contains(newServers.get(removeIndex))) {
				break;
			}
			
			int removedServer = newServers.get(removeIndex);
			newServers.remove(removeIndex);
			int newCost = getAllCost(newServers);
			if (newCost < bestCost) {
				bestCost = newCost;
				bestServers = new ArrayList<Integer>(newServers);
				System.out.println("new best servers location by dropping" + bestServers);
				System.out.println("new best cost:" + bestCost);
			}
			else {
				tabu.add(removedServer);
			}
		}
		
		int nonImprovedCount = 0;
		while ((System.nanoTime() - startTime) / 1000000 < 88500) {
			nonImprovedCount++;
			
			newServers = SearchServers.getRandomServers(1, bestServers);
			int newCost = getAllCost(newServers);
			if (newCost < bestCost) {
				nonImprovedCount = 0;
				bestCost = newCost;
				bestServers = SearchServers.getCurServers();
				System.out.println("new best servers location by moving" + bestServers);
				System.out.println("new best cost:" + bestCost);
				continue;
			}
			
			if (nonImprovedCount > 1000) {
				break;
			}
		}
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
		// Move
		i = 0;
		int j = 1;
		List<Integer> neighbor = new ArrayList<Integer>();
		boolean findBetter = false;
		while (i < bestServers.size() - 1) {
			int firstServer = bestServers.get(i);
			int secondServer = bestServers.get(i + j);
			if (!neighbor.isEmpty()) {
				neighbor.clear();
			}
			addNeighbor(firstServer, neighbor);
			addNeighbor(secondServer, neighbor);
			
			List<Integer> newServers = new ArrayList<Integer>(bestServers);
			newServers.remove(i);
			newServers.remove(i + j - 1);
			findBetter = false;
			for (int server : neighbor) {
				if (server != firstServer && server != secondServer) {
					newServers.add(server);
					newCost = getAllCost(newServers);
					if (newCost < bestCost) {
						findBetter = true;
						bestCost = newCost;
						bestServers = new ArrayList<Integer>(newServers);
						System.out.println("new best servers location by moving" + bestServers);
						System.out.println("new best cost:" + bestCost);
						break;
					}
					newServers.remove(newServers.size() - 1);
				}
			}
			
			j++;
			if (findBetter || i + j == bestServers.size() - 1) {
				i++;
				j = 1;
			}
		}
		
	}
	
	public static void addNeighbor(int server, List<Integer> neighbor) {
		for (Edge e : Graph.adj[server]) {
			if (!neighbor.contains(e.target)) {
				neighbor.add(e.target);
			}
		}
	}
	
	public static void main(String[] args) {
		String[] graphContent = FileUtil.read("E:\\codecraft\\cdn\\case_example\\0\\case4.txt", null);
		Graph.makeGraph(graphContent);

		long startTime = System.nanoTime();
//		simulatedAnnealing();
//		move();
//		moverefresh(); 
//		dropUntil();
		dropDeterministic();
		System.out.println(bestServers);
		System.out.println(bestServers.size());
		Zkw.clear();
		Zkw.setSuperSource(bestServers);
		Zkw.getMinCostFlow(Graph.vertexNum, Graph.vertexNum + 1);
		solution = Zkw.getPaths();
		System.out.println(solution);
		System.out.println(Zkw.deepCheck(solution));
//		List<ArrayList<Integer>> allPaths = new ArrayList<ArrayList<Integer>>();
		long endTime = System.nanoTime();
		System.out.println((endTime - startTime) / 1000000 + "ms");
		System.out.println(bestCost);
//		AnalyseUtil.saveTofile();
	}
}
