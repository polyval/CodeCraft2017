/**
 * 
 */
package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * @author JwZhou
 *
 */
public class YenKSP {
	private static int k = 5;
	
	public static List<Route> kspYen(int start, int end) {
		List<Route> shortestPaths = new ArrayList<>();
		PriorityQueue<Route> candidatePaths = new PriorityQueue<>();
		
		Route shortest = dijkstraShortestPath(start, end);
		if (shortest == null || shortest.averageCost > Graph.serverCost) {
			return shortestPaths;
		}
		shortestPaths.add(shortest);
		
		for (int i = 1; i < k; i++) {
			List<Integer> path = shortestPaths.get(shortestPaths.size() - 1).nodes;
			for (int j = 0; j < path.size() - 1; j++) {
				int spurNode = path.get(j);
				List<Integer> rootPath = path.subList(0, j + 1);
				
				List<Pair<Integer, Integer>> edgesRemoved = new ArrayList<>();
				for (Route originPath : shortestPaths) {
					if (originPath.nodes.size() > j && Collections.indexOfSubList(originPath.nodes, rootPath) == 0) {
						// Remove edges.
						int edgeStart = originPath.nodes.get(j);
						int edgeEnd = originPath.nodes.get(j + 1);
						Pair<Integer, Integer> removedEdge = new Pair<Integer, Integer>(edgeStart, edgeEnd);
						Pair<Integer, Integer> removedEdgeOppsite = new Pair<Integer, Integer>(edgeEnd, edgeStart);
						Edge.edgeMap.get(removedEdge).removed = true;
						Edge.edgeMap.get(removedEdgeOppsite).removed = true;
						// Records removed edges.
						edgesRemoved.add(removedEdge);
						edgesRemoved.add(removedEdgeOppsite);
					}
				}
				
				// Remove nodes.
				for (int removeNodeId : rootPath) {
					if (removeNodeId != spurNode) {
						Graph.nodes[removeNodeId].removed = true;
					}
				}
				
				Route spurPath = dijkstraShortestPath(spurNode, end);
				
				if (spurPath != null) {
					List<Integer> totalPath = new ArrayList<>(rootPath.subList(0, j));
					totalPath.addAll(spurPath.nodes);
					Route newRoute = new Route(totalPath);
					if (!candidatePaths.contains(newRoute)) {
						candidatePaths.add(new Route(totalPath));
					}
				}
				
				// Restore edges.
				for (Pair<Integer, Integer> pair : edgesRemoved) {
					Edge.edgeMap.get(pair).removed = false;
				}
				// Restore nodes.
				for (int removeNodeId : rootPath) {
					Graph.nodes[removeNodeId].removed = false;
				}
			}
			
			if (candidatePaths.isEmpty()) {
				break;
			}
			shortestPaths.add(candidatePaths.poll());
		}
		
		return shortestPaths;
	}
	
	
	public static Route dijkstraShortestPath(int start, int end) {
		if (start == end) {
			return new Route(start, end);
		}
		
		int[] prev = new int[Graph.vertexNum];
		PriorityQueue<Node> candidates = new PriorityQueue<>(Collections.reverseOrder());
		
		for (Node node : Graph.nodes) {
			if (node.vertexId == start) {
				node.tentativeCost = 0;
			}
			node.tentativeCost = Integer.MAX_VALUE;
		}
		
		candidates.add(Graph.nodes[start]);
		Arrays.fill(prev, -1);
		
		while (!candidates.isEmpty()) {
			Node node = candidates.poll();
			if (node.vertexId == end) {
				break;
			}
			for (Edge neighborEdge : Graph.adj[node.vertexId]) {
				if (neighborEdge.removed || Graph.nodes[neighborEdge.target].removed) {
					continue;
				}
				Node neighbor = Graph.nodes[neighborEdge.target];
				if (neighborEdge.cost + node.tentativeCost < neighbor.tentativeCost) {
					neighbor.tentativeCost = neighborEdge.cost + node.tentativeCost;
					prev[neighbor.vertexId] = node.vertexId;
					candidates.add(neighbor);
				}
			}
		}
		
		ArrayList<Integer> path = new ArrayList<>();
		int cur = end;
		while (true) {
			path.add(cur);
			if (prev[cur] == start) {
				path.add(start);
				break;
			}
			else if (prev[cur] == -1) {
				return null;
			}
			cur = prev[cur];
		}
		
		Collections.reverse(path);
		
		return new Route(path);
	}
}
