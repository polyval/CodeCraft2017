/**
 * 
 */
package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author JwZhou
 *
 */
public class Neighbor {
	
	public static void allocate() {
		ArrayList<Route> newSolution = new ArrayList<>(Search.solution);
		ArrayList<ArrayList<Integer>> allCompetitivePaths = new ArrayList<>();
		
		for (int i = 0; i < newSolution.size() - 1; i++) {
			ArrayList<Integer> competitivePaths = new ArrayList<>();
			competitivePaths.add(i);
			
			// Client attaches to this node.
			if (newSolution.get(i).getEdges().isEmpty()) {
				continue;
			}
			// Actually should find the paths with same edges, but that is too cumbersome.
			for (int j = i + 1; j < newSolution.size(); j++) {
				if (newSolution.get(j).getEdges().isEmpty()) {
					continue;
				}
				// Find paths has same edge with path i, but it doesn't mean all the same edges are identical.
				if (!Collections.disjoint(newSolution.get(i).getEdges(), newSolution.get(j).getEdges())) {
					competitivePaths.add(j);
				}
			}
			
			if (competitivePaths.size() > 1) {
				allCompetitivePaths.add(competitivePaths);
			}
		}
		
		// Reallocate
		for (ArrayList<Integer> competitivePaths : allCompetitivePaths) {
			for (int i = competitivePaths.size() - 1; i > 0; i--) {
				for (int j = competitivePaths.size() - 1; j > competitivePaths.size() - 1 - i; j--) {
					ArrayList<Integer> newPathsId = new ArrayList<>(competitivePaths);
					Collections.swap(newPathsId, j, j - 1);
					reallocate(newSolution, newPathsId);
				}
			}
		}
		// TODO: Should record the changes in case the solution is infeasible, backtracking to last solution.
		if (Search.isFeasible(newSolution)) {
			Search.solution = newSolution;
		}
	}
	
	/**
	 * Changes the order that the path added to the network.
	 * @param paths
	 * @param pathsId
	 */
	public static void reallocate(ArrayList<Route> paths, ArrayList<Integer> pathsId) {
		int cost = 0;
		int totalBandwidth = 0;
		// New bandwidths that allocate to the path.
		Integer[] occupiedBandwidths = new Integer[pathsId.size()];
		
		for (int i = 0; i < pathsId.size(); i++) {
			Route path = paths.get(pathsId.get(i));
			cost += path.averageCost * path.occupiedBandwidth;
			occupiedBandwidths[i] = path.occupiedBandwidth;
			totalBandwidth += path.occupiedBandwidth;
			
			path.removePath();
		}
		
		int newCost = 0;
		int newTotalBandwidth = 0;
		for (int i = 0; i < pathsId.size(); i++) {
			Route path = paths.get(pathsId.get(i));
			Constructive.addPath(path);
			newCost += path.averageCost * path.occupiedBandwidth;
			newTotalBandwidth += path.occupiedBandwidth;
		}
		
		if( newCost + (totalBandwidth - newTotalBandwidth) * 10 - cost <  0) {
			// Paths have already been updated.
			return;
		}
		else {
			// Restore paths.
			Constructive.restorePaths(paths, Arrays.asList(occupiedBandwidths));
		}
	}
	
}
