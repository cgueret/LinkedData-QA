/**
 * 
 */
package nl.vu.qa_for_lod.graph;

import java.util.Set;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public interface Graph {
	/**
	 * @param statement
	 */
	public void addStatement(Statement statement);

	/**
	 * 
	 */
	public void clear();

	/**
	 * @param fromResource
	 * @param toResource
	 * @param directed
	 * @return
	 */
	public boolean containsEdge(Resource fromResource, Resource toResource, boolean directed);

	/**
	 * @param resource
	 * @return
	 */
	public int getDegree(Resource resource);

	/**
	 * @param resource
	 * @param direction
	 * @param propertyURI
	 * @return
	 */
	public Set<Resource> getNeighbours(Resource resource, Direction direction, Property property);

}
