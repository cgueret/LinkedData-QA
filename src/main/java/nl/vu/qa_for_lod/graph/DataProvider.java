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
	 * 
	 */
	public void close();

	/**
	 * @param resource
	 * @return
	 */
	public Set<Statement> get(Resource resource);

}
