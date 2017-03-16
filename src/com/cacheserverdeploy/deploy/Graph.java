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
	// Do not use index to get node from here.
	public static Node[] clientNodes;
	// Adjacency lists;
	public static ArrayList<Edge>[] adj;
	public static Node[] nodes;
	
	// Used to reset the data.
 	public static int[][] edgeWeight;
	public static int[][] edgeBandwidth;
	
	// Residual Graph
	public static ArrayList<Edge>[] resAdj;
	public static int resVertexNum = 0;
	public static int totalFlow;

	
	@SuppressWarnings("unchecked")
	public static void makeGraph(String[] graphContent) {
		String[] basic = graphContent[0].split("\\s+");
		vertexNum = Integer.parseInt(basic[0]);
		edgeNum = Integer.parseInt(basic[1]);
		clientVertexNum = Integer.parseInt(basic[2]);
		
		// Residual vertex number.
		resVertexNum = vertexNum + 2;
		
		// Faster than simply add elements one by one.
		adj = (ArrayList<Edge>[]) new ArrayList[vertexNum];
		for (int i = 0; i < vertexNum; i++) {
			adj[i] = new ArrayList<Edge>();
		}
		
		// Initialize residual adjacency lists.
		resAdj = (ArrayList<Edge>[]) new ArrayList[resVertexNum];
		for (int i = 0; i < resVertexNum; i++) {
			resAdj[i] = new ArrayList<Edge>();
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
			Edge edgeOne = new Edge(start, end, cost, bandwidth);
			Edge edgeOpposite = new Edge(end, start, cost, bandwidth);
			edgeOne.residualFlow = edgeOne.bandwidth;
			edgeOpposite.residualFlow = edgeOpposite.bandwidth;
			
			// For residual graph.
			Edge resEdgeOne = new Edge(end, start, -cost, bandwidth);
			Edge resEdgeOpposite = new Edge(start, end, -cost, bandwidth);
			resEdgeOne.residualFlow = 0;
			resEdgeOpposite.residualFlow = 0;
			// Set residual edge.
			resEdgeOne.isResidual = true;
			resEdgeOpposite.isResidual = true;
			edgeOne.counterEdge = resEdgeOne;
			resEdgeOne.counterEdge = edgeOne;
			edgeOpposite.counterEdge = resEdgeOpposite;
			resEdgeOpposite.counterEdge = edgeOpposite;
			
			adj[start].add(edgeOne);
			adj[end].add(edgeOpposite);
			
			// For residual graph.
			resAdj[start].add(edgeOne);
			resAdj[start].add(resEdgeOpposite);
			resAdj[end].add(edgeOpposite);
			resAdj[end].add(resEdgeOne);
			
			
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
			
			// For residual graph, add edges from client to super sink.
			Edge edge = new Edge(attachedVertexId, resVertexNum - 1, 0, demand);
			Edge resEdge = new Edge(resVertexNum - 1, attachedVertexId, 0, demand);
			resEdge.isResidual = true;
			edge.counterEdge = resEdge;
			resEdge.counterEdge = edge;
			edge.residualFlow = edge.bandwidth;
			resEdge.residualFlow = 0;
			// Add edges to residual graph.
			resAdj[attachedVertexId].add(edge);
			resAdj[resVertexNum - 1].add(resEdge);
			
			totalFlow += demand;
 		}
		
		
		for (ArrayList<Edge> edges : Graph.adj) {
			Collections.sort(edges);
		}
	}
}
