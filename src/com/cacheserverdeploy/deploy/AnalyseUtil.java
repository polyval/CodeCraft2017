/**
 * 
 */
package com.cacheserverdeploy.deploy;

import com.filetool.util.FileUtil;

/**
 * @author JwZhou
 *
 */
public class AnalyseUtil {
	
	public static int[] cost = new int[Graph.clientVertexNum];
	
	public static void saveTofile() {
		String[] res = new String[Graph.clientVertexNum];
		for (int i = 0; i < cost.length; i++) {
			String newString = String.valueOf(i + 1) + " " + cost[i];
			res[i] = newString;
		}
		FileUtil.write("E:\\codecraft\\cdn\\case_example\\plot.txt", res, false);
	}
}
