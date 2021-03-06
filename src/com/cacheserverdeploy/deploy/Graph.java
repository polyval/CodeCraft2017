/**
 * 
 */
package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author JwZhou
 *
 */
public class Graph {
	
	public static int vertexNum = 0;
	public static int edgeNum = 0;
	public static int clientVertexNum = 0;
	
	public static List<Integer> diffServerCost = new ArrayList<Integer>();
	public static List<Integer> diffServerCapacity = new ArrayList<Integer>();
	public static int[] vertexCost;
	public static int[] clientVertexId;
	public static int[] clientDemand;
	public static Map<Integer, Integer> vertexToClient = new HashMap<Integer, Integer>();
	
	// Adjacency lists;
	public static ArrayList<Edge>[] adj;
	
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
		
		// Read variable server cost
		int startIndex = 2;
		while (!graphContent[startIndex].isEmpty()) {
			String[] serverInfo = graphContent[startIndex].split("\\s+");
			diffServerCapacity.add(Integer.parseInt(serverInfo[1]));
			diffServerCost.add(Integer.parseInt(serverInfo[2]));
			startIndex++;
		}
		
		// Initialize.
		clientVertexId = new int[clientVertexNum];
		clientDemand = new int[clientVertexNum];
		// Read vertex cost
		vertexCost = new int[vertexNum];
		startIndex++;
		for (int i = startIndex; i < startIndex + vertexNum; i++) {
			vertexCost[i - startIndex] = Integer.parseInt(graphContent[i].split("\\s+")[1]);
		}
		
		// Read edges
		startIndex += vertexNum + 1;
		for (int i = startIndex; i < edgeNum + startIndex; i++) {
			String[] edgeInfo = graphContent[i].split("\\s+");
			int start = Integer.parseInt(edgeInfo[0]);
			int end = Integer.parseInt(edgeInfo[1]);
			int bandwidth = Integer.parseInt(edgeInfo[2]);
			int cost = Integer.parseInt(edgeInfo[3]);
			
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
		}
		
		// Read clients
		startIndex += edgeNum + 1;
		for (int i = startIndex; i < graphContent.length; i++) {
			String[] clientInfo = graphContent[i].split("\\s+");
			int clientId = Integer.parseInt(clientInfo[0]);
			int attachedVertexId = Integer.parseInt(clientInfo[1]);
			int demand = Integer.parseInt(clientInfo[2]);
			
			clientVertexId[clientId] = attachedVertexId;
			clientDemand[clientId] = demand;
			
			vertexToClient.put(attachedVertexId, clientId);
			
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
		
	}
}
