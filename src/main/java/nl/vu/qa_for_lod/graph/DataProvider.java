/**
 * 
 */
package nl.vu.qa_for_lod.graph;

import java.util.Set;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public interface DataProvider {
	/**
	 * Close the data provider. Gives it an opportunity to shutdown any service
	 * that may have been started earlier
	 */
	public void close();

	/**
	 * Return a set of statements about a given resource
	 * 
	 * @param resource
	 *            a given resource r from the web of data
	 * @param direction
	 *            use incoming triples <s,p,r>, outgoing <r,p,o> or both.
	 * @return a set of statements
	 */
	public Set<Statement> get(Resource resource, Direction direction);
}
