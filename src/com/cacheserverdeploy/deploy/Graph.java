/**
 * 
 */
package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author JwZhou
 *
 */
public class Graph {
	
	public static final int vertexNumMax = 1000;
	public static final int clientVertexNumMax = 500;
	
	public static int vertexNum = 0;
	public static int edgeNum = 0;
	public static int clientVertexNum = 0;
	
	public static int serverCost = 0;
	public static int[] clientVertexId;
	public static int[] clientDemand;
	public static Node[] clientNodes;
	// Adjacency lists;
	public static ArrayList<Edge>[] adj;
	public static Node[] nodes;
	
	// Used to reset the data.
 	public static int[][] edgeWeight;
	public static int[][] edgeBandwidth;
	
	
	@SuppressWarnings("unchecked")
	public static void makeGraph(String[] graphContent) {
		String[] basic = graphContent[0].split("\\s+");
		vertexNum = Integer.parseInt(basic[0]);
		edgeNum = Integer.parseInt(basic[1]);
		clientVertexNum = Integer.parseInt(basic[2]);
		
		// Faster than simply add elements one by one.
		adj = (ArrayList<Edge>[]) new ArrayList[vertexNum];
		for (int i = 0; i < vertexNum; i++) {
			adj[i] = new ArrayList<>();
		}
		
		serverCost = Integer.parseInt(graphContent[2]);
		
		// Initialize.
		clientVertexId = new int[clientVertexNum];
		clientDemand = new int[clientVertexNum];
		clientNodes = new Node[clientVertexNum];
		edgeWeight = new int[vertexNum][vertexNum];
		edgeBandwidth = new int[vertexNum][vertexNum];
		nodes = new Node[vertexNum];
		
		// Read edges
		for (int i = 4; i < edgeNum + 4; i++) {
			String[] edgeInfo = graphContent[i].split("\\s+");
			int start = Integer.parseInt(edgeInfo[0]);
			int end = Integer.parseInt(edgeInfo[1]);
			int bandwidth = Integer.parseInt(edgeInfo[2]);
			int cost = Integer.parseInt(edgeInfo[3]);
			
			if (nodes[start] == null) {
				nodes[start] = new Node(start);
			}
			
			if (nodes[end] == null) {
				nodes[end] = new Node(end);
 			}
			
			// Simplify the computation of uplink and downlink.
			adj[start].add(new Edge(start, end, cost, bandwidth));
			adj[end].add(new Edge(end, start, cost, bandwidth));
			
			edgeWeight[start][end] = cost;
			edgeWeight[end][start] = cost;
			edgeBandwidth[start][end] = bandwidth;
			edgeBandwidth[end][start] = bandwidth;
		}
		
		// Read clients
		for (int i = edgeNum + 5; i < graphContent.length; i++) {
			String[] clientInfo = graphContent[i].split("\\s+");
			int clientId = Integer.parseInt(clientInfo[0]);
			int attachedVertexId = Integer.parseInt(clientInfo[1]);
			int demand = Integer.parseInt(clientInfo[2]);
			
			clientVertexId[clientId] = attachedVertexId;
			clientDemand[clientId] = demand;
			nodes[attachedVertexId].clientId = clientId;
			nodes[attachedVertexId].demands = demand;
			clientNodes[clientId] = nodes[attachedVertexId];
 		}
		
		
		for (ArrayList<Edge> edges : Graph.adj) {
			Collections.sort(edges);
		}
	}
}
