/**
 * 
 */
package nl.vu.qa_for_lod.graph.impl;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFReader;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;

import nl.vu.qa_for_lod.graph.DataProvider;
import nl.vu.qa_for_lod.graph.Direction;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class DataProviderMap implements DataProvider {
	// Logger
	final static Logger logger = LoggerFactory.getLogger(DataProviderMap.class);
	// The sets of statements
	private final Map<Resource, Set<Statement>> outgoingStatements;
	private final Map<Resource, Set<Statement>> incomingStatements;
	
	
	/**
	 * This data provider serves data from some maps.
	 * 
	 * Use DataProviderMapUtils for convenience methods for creating instances of it.
	 * 
	 * 
	 * @throws Exception
	 */
	public DataProviderMap(Map<Resource, Set<Statement>> outgoingStatements, Map<Resource, Set<Statement>> incomingStatements)
	{
		this.outgoingStatements = outgoingStatements;
		this.incomingStatements = incomingStatements;
	}
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.graph.DataProvider#close()
	 */
	public void close() {
		outgoingStatements.clear();
		incomingStatements.clear();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.qa_for_lod.graph.DataProvider#get(com.hp.hpl.jena.rdf.model.Resource
	 * )
	 */
	public Set<Statement> get(Resource resource, Direction direction)
	{
		Set<Statement> result = new HashSet<Statement>();
		
		if (direction.equals(Direction.IN) || direction.equals(Direction.BOTH)) {
			if (incomingStatements.get(resource) != null) {
				result.addAll(incomingStatements.get(resource));
			}
		}
		
		if (direction.equals(Direction.OUT) || direction.equals(Direction.BOTH)) {
			if (outgoingStatements.get(resource) != null) {
				result.addAll(outgoingStatements.get(resource));
			}
		}
		return result;
	}

	/**
	 * @return
	 */
	public Set<Resource> getResources() {
		logger.warn("ONLY RETURNING OUTGOING RESOURCES - I THINK THE DIRECTION SHOULD BE TAKEN INTO ACCOUNT HERE"); 

		return outgoingStatements.keySet();
	}


	/**
	 * @return
	 */
	public int size() {
		Set<Statement> s = new HashSet<Statement>();
		for (Set<Statement> stmt : outgoingStatements.values())
			s.addAll(stmt);
		return s.size();
	}
}
