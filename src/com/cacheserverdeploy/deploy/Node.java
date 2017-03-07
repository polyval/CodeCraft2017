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
	
	public boolean isServer = false;
	
	public Node(int vertexId) {
		this.vertexId = vertexId;
	}
	
	@Override
	public String toString() {
		return Integer.toString(this.vertexId);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + vertexId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;
		if (vertexId != other.vertexId)
			return false;
		return true;
	}
	
}
