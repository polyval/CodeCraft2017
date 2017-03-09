/**
 * 
 */
package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.List;

import com.filetool.util.FileUtil;

/**
 * @author JwZhou
 *
 */
public class Util {
	
	public static List<ArrayList<Integer>> combine(int size, int k) {
        ArrayList<ArrayList<Integer>> rst = new ArrayList<ArrayList<Integer>>();
        ArrayList<Integer> solution = new ArrayList<Integer>();
        
        helper(rst, solution, size, k, 0);
        return rst;
    }
    
    private static void helper(ArrayList<ArrayList<Integer>> rst, ArrayList<Integer> solution, int size, int k, int start) {

        if (solution.size() == k){
            rst.add(new ArrayList(solution));
            return;
        }
        
        for(int i = start; i< size; i++){
            solution.add(i);
            
            // the new start should be after the next number after i
            helper(rst, solution, size, k, i+1); 
            solution.remove(solution.size() - 1);
        }
    }
    
    public static List<ArrayList<Integer>> getNodesCombinations(List<Node> exclusive, int k) {
    	 ArrayList<ArrayList<Integer>> rst = new ArrayList<ArrayList<Integer>>();
         ArrayList<Integer> solution = new ArrayList<Integer>();
         
         nodeHelper(rst, solution, exclusive, k, 0);
         return rst;
    }
    
    private static void nodeHelper(ArrayList<ArrayList<Integer>> rst, ArrayList<Integer> solution, List<Node> exclusive, int k, int start) {

        if (solution.size() == k){
            rst.add(new ArrayList(solution));
            return;
        }
        
        for(int i = start; i< Graph.vertexNum; i++){
            if (exclusive.contains(Graph.nodes[i])) {
            	continue;
            }
        	solution.add(i);
            
            // the new start should be after the next number after i
            nodeHelper(rst, solution, exclusive, k, i+1); 
            solution.remove(solution.size() - 1);
        }
    }
     
	
	public static void main(String[] args) {
		String[] graphContent = FileUtil.read("E:\\codecraft\\cdn\\case_example\\case1.txt", null);
		Graph.makeGraph(graphContent);
		
		List<Node> exclusive = new ArrayList<>();
		exclusive.add(Graph.nodes[3]);
		exclusive.add(Graph.nodes[6]);
		exclusive.add(Graph.nodes[7]);
		System.out.println(getNodesCombinations(exclusive, 3));
	}
}
