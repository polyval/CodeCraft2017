/**
 * 
 */
package com.cacheserverdeploy.deploy;

/**
 * @author JwZhou
 *
 */
public class Node {

	public int vertexId;
	
	// Indicate whether it is client or not.
	public int clientId = -1;
	public int demands = -1;
	
	
	public Node(int vertexId) {
		this.vertexId = vertexId;
	}
}
