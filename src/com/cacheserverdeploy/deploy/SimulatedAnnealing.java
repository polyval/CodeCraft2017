package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.filetool.util.FileUtil;

public class SimulatedAnnealing {
	public static double temper;
	public static double bestCost;
	public static List<Path> solution = new ArrayList<Path>();
	public static long startTime = System.nanoTime();

	private static double COOLINGRATE = 0.95;

	public static List<Integer> bestServers = new ArrayList<Integer>();
	
	public static Random random = new Random();
	
	
	
	private static void initialize() {
		for (int i : Graph.clientVertexId) {
			bestServers.add(i);
		}
		
		temper = Graph.serverCost * Graph.clientVertexNum;
		bestCost = temper;
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
		
//		while (true) {
//			newServers = new ArrayList<Integer>(dropOneServers(curServers));
//			System.out.println("the new servers location:" + newServers);
//			newCost = getAllCost(newServers);
//			
//			if (newCost < curCost) {
//				curServers = new ArrayList<Integer>(newServers);
//				bestServers = new ArrayList<Integer>(newServers);
//				curCost = newCost;
//				bestCost = curCost;
//				
//				System.out.println("the best servers location" + curServers);
//				System.out.println("the best all cost:" + curCost);
//				continue;
//			}
//			else {
//				break;
//			}
//		}

		while ((System.nanoTime() - startTime) / 1000000 < 89000 && temper > 0.000001) {
			newServers = SearchServers.getRandomServers(1, curServers);
			newCost = getAllCost(newServers);
			delta = newCost - curCost;
			
			if (delta < 0.0) {
				curServers = SearchServers.getCurServers();
				curCost = newCost;
		
				System.out.println("current best servers location" + curServers);
				System.out.println("current best cost:" + curCost);
			}
			else {
				double rnd = (double) (Math.random());
				double p = Math.exp(-delta / temper) / 5;
				if(p > rnd) {
					curServers = SearchServers.getCurServers();
					curCost = newCost;
					
					System.out.println("current servers location" + curServers);
					System.out.println("current cost:" + curCost);
				}
			}


			if (newCost < bestCost) {
				bestServers = new ArrayList<Integer>(newServers);
				bestCost = newCost;
				
				System.out.println("the best servers location" + bestServers);
				System.out.println("the best all cost:" + bestCost);
			}
			
			temper *= COOLINGRATE;		
		}
	}

	public static int getAllCost(List<Integer> parentServers)
	{
		List<Integer> newServers = new ArrayList<Integer>(parentServers);
		int[] flowCost = Zkw.getFlowCostGivenServers(newServers);
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
		
		parentServers=new ArrayList<Integer>(newServers);
		cost += newServers.size() * Graph.serverCost;
		return cost;
		
	}
	
	public static void main(String[] args) {
		String[] graphContent = FileUtil.read("E:\\codecraft\\cdn\\case_example\\1\\case8.txt", null);
		Graph.makeGraph(graphContent);

		long startTime = System.nanoTime();
		simulatedAnnealing();
		System.out.println(bestServers);
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
