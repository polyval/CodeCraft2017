/**
 * 
 */
package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * @author JwZhou
 *
 */
public class Search {
	
	public static List<Node> servers = new ArrayList<Node>();
	public static ArrayList<Node> clientNodes = new ArrayList<Node>(Graph.clientVertexNum);
	public static List<Route> solution = new ArrayList<Route>();
	public static List<Integer> occupiedBandwidths = new ArrayList<Integer>();
	public static int cost = Graph.clientVertexNum * Graph.serverCost;
	public static boolean isFeasible = false;
	public static long startTime = System.nanoTime();
	
	public static void initialize() {
		// Get all the client nodes.
		for (int i = 0; i < Graph.clientVertexNum; i++) {
			clientNodes.add(Graph.nodes[Graph.clientVertexId[i]]);
		}
		// Sort the clients by its demands reversely.
		Collections.sort(clientNodes, new Comparator<Node>() {
			@Override
			public int compare(Node n1, Node n2) {
				// It may has some other criteria.
				return n2.demands - n1.demands;
			}
		});
	}
	
	/*
	 * Resets the data.
	 */
	public static void reset() {
		// Reset edges.
		for (List<Edge> edges : Graph.adj) {
			for (Edge edge : edges) {
				edge.bandwidth = Graph.edgeBandwidth[edge.source][edge.target];
			}
		}
		// Reset paths
		for (List<Route> paths : Route.shortestPaths.values()) {
			for (Route path : paths) {
				path.occupiedBandwidth = 0;
				path.computeBandwidthAndCost();
			}
		}
		// Reset client demands
		for (int i = 0; i < Graph.clientVertexNum; i++) {
			Graph.nodes[Graph.clientVertexId[i]].demands = Graph.clientDemand[i];
		}
	}
	
	public static boolean isFeasible(List<Route> solution) {
		int[] demands = new int[Graph.clientVertexNum];
		for (Route route : solution) {
			demands[Graph.nodes[route.client].clientId] += route.occupiedBandwidth;
		}
		for (int i = 0; i < Graph.clientVertexNum; i++) {
			if (demands[i] != Graph.clientDemand[i]) {
				return false;
			}
		}
		return true;
	}
	
	public static int computerCost(List<Route> solution) {
		int cost = 0;
		cost += servers.size() * Graph.serverCost;
		for (Route route : solution) {
			cost += route.averageCost * route.occupiedBandwidth;
		}
		return cost;
	}
	
	public static void updateSolution(List<Route> newSolution) {
		solution.clear();
		solution = new ArrayList<Route>(newSolution);
		cost = computerCost(solution);
		occupiedBandwidths.clear();
		for (Route path : solution) {
			occupiedBandwidths.add(path.occupiedBandwidth);
		}
	}
	
	public static void refreshSolution() {
		for (int i = 0; i < solution.size(); i++) {
			solution.get(i).occupiedBandwidth = occupiedBandwidths.get(i);
		}
	}
	
	public static int getCurTotalOutput() {
		int totalBandwidths = 0;
		for (int bandwidth : occupiedBandwidths) {
			totalBandwidths += bandwidth;
		}
		return totalBandwidths;
	}
	
	public static int getOutput(List<Route> newSolution) {
		int remainingBandwidths = 0;
		for (Node client : Graph.clientNodes) {
			remainingBandwidths += client.demands;
		}
		int total = 0;
		for (int demands : Graph.clientDemand) {
			total += demands;
		}
		return total - remainingBandwidths;
	}
	
	public static boolean deepCheck(List<Route> solution) {
		int[][] edgesBandwidth = new int[Graph.vertexNum][Graph.vertexNum];
		int[] demands = new int[Graph.clientVertexNum];
		for (Route path : solution) {
			demands[Graph.nodes[path.client].clientId] += path.occupiedBandwidth;
			for (int i = 0; i < path.nodes.size() - 1; i++) {
				edgesBandwidth[path.nodes.get(i)][path.nodes.get(i+1)] += path.occupiedBandwidth;
			}
		}
		
		for (int i = 0; i < Graph.clientVertexNum; i++) {
			if (demands[i] != Graph.clientDemand[i]) {
				return false;
			}
		}
		
		for (int i = 0; i < Graph.vertexNum; i++) {
			for (int j = 0; j < Graph.vertexNum; j++) {
				if (edgesBandwidth[i][j] > Graph.edgeBandwidth[i][j]) {
					return false;
				}
			}
		}
		
		return true;
		
	}
	
	public static String[] getResults(String[] graphContent) {
		Graph.makeGraph(graphContent);
		Constructive.greedyConstruct();
		VNS.changePathsSwapFirst(Search.solution);
		VNS.moveServerVNS();
		
		refreshSolution();
		
		List<String> res = new ArrayList<String>();
		
		if (!deepCheck(solution) || cost > Graph.serverCost * Graph.clientVertexNum) {
			String[] resString = new String[Graph.clientVertexNum + 2];
			resString[0] = String.valueOf(Graph.clientVertexNum);
			resString[1] = "";
			
			for (int i = 0; i < Graph.clientVertexNum; i++) {
				Node client = Graph.clientNodes[i];
				resString[i + 2] = client.vertexId + " " + client.clientId + " " + Graph.clientDemand[client.clientId];
			}
			return resString;
		}
		
		for (Route path : solution) {
			if (path.occupiedBandwidth != 0) {
				res.add(path.toString());
			}
		}
		
		String[] resString = new String[res.size() + 2];
		resString[0] = String.valueOf(res.size());
		resString[1] = "";
		for (int i = 0; i < res.size(); i++) {
			resString[i+2] = res.get(i); 
		}
		
		return resString;
	}
}
