/**
 * 
 */
package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
		
		// Cost by current paths.
		for (int i = 0; i < pathsId.size(); i++) {
			Route path = paths.get(pathsId.get(i));
			cost += path.averageCost * path.occupiedBandwidth;
			totalBandwidth += path.occupiedBandwidth;
			
			path.removePath();
		}
		
		// New cost after reallocating.
		int newCost = 0;
		int newTotalBandwidth = 0;
		// Paths that are reallocated
		List<Route> changedPaths = new ArrayList<>();
		for (int i = 0; i < pathsId.size(); i++) {
			Route path = paths.get(pathsId.get(i));
			Constructive.addPath(path);
			changedPaths.add(path);
			newCost += path.averageCost * path.occupiedBandwidth;
			newTotalBandwidth += path.occupiedBandwidth;
		}
		
		if( newCost + (totalBandwidth - newTotalBandwidth) * 8 - cost <  0 || !Search.isFeasible(paths)) {
			// Paths have already been updated.
			return;
		}
		else {
			// Restore paths.
			Constructive.restorePaths(changedPaths);
		}
	}
	
}
