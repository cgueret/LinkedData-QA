/**
 * 
 */
package nl.vu.qa_for_lod.graph;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class EndPoint {
	private final String uri;
	private final String graph;

	/**
	 * @param uri
	 * @param graph
	 */
	public EndPoint(String uri, String graph) {
		this.uri = uri;
		this.graph = graph;
	}

	/**
	 * @return the graph
	 */
	public String getGraph() {
		return graph;
	}

	/**
	 * @return the uri
	 */
	public String getURI() {
		return uri;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "EndPoint [uri=" + uri + ", graph=" + graph + "]";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((graph == null) ? 0 : graph.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EndPoint other = (EndPoint) obj;
		if (graph == null) {
			if (other.graph != null)
				return false;
		} else if (!graph.equals(other.graph))
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}
}
