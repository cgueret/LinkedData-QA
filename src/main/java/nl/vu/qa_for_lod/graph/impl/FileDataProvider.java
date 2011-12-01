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
public class FileDataProvider implements DataProvider {
	// Logger
	final static Logger logger = LoggerFactory.getLogger(FileDataProvider.class);
	// The sets of statements
	private final Map<Resource, Set<Statement>> outgoingStatements = new HashMap<Resource, Set<Statement>>();
	private final Map<Resource, Set<Statement>> incomingStatements = new HashMap<Resource, Set<Statement>>();

	private Model model;
	
	/**
	 * This data provider serves a set of triples loaded from a file.
	 * 
	 * @param fileName
	 *            the file containing the triples serialised as N3
	 * @param max
	 *            maximum amount of triples to read
	 * 
	 * @throws Exception
	 */
	public FileDataProvider(String fileName, int max) throws Exception {
		// Read the file
		model = ModelFactory.createDefaultModel();
		RDFReader reader = model.getReader("N3");
		reader.setProperty("WARN_REDEFINITION_OF_ID", "EM_IGNORE"); // speed
		InputStream in = FileManager.get().open(fileName);
		if (in == null) {
			throw new Exception();
		}
		reader.read(model, in, null);

		int count = 0;
		StmtIterator iter = model.listStatements();
		while (iter.hasNext()) {
			Statement stmt = iter.nextStatement();
			if (stmt.getSubject() instanceof Resource && stmt.getObject() instanceof Resource) {
				if (max == 0 || count < max) {
					count++;
					index(outgoingStatements, stmt, stmt.getSubject());
					index(incomingStatements, stmt, stmt.getObject().asResource());
				}
			}
		}
		//model.close();

		logger.info(String.format("Loaded %d triples from %s", size(), fileName));
	}

	public Model getModel()
	{
		return model;
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
	public Set<Statement> get(Resource resource, Direction direction) {
		Set<Statement> statements = new HashSet<Statement>();
		if (direction.equals(Direction.IN) || direction.equals(Direction.BOTH))
			if (incomingStatements.get(resource) != null)
				statements.addAll(incomingStatements.get(resource));
		if (direction.equals(Direction.OUT) || direction.equals(Direction.BOTH))
			if (outgoingStatements.get(resource) != null)
				statements.addAll(outgoingStatements.get(resource));
		return statements;
	}

	/**
	 * @return
	 */
	public Set<Resource> getResources() {
		return outgoingStatements.keySet();
	}

	/**
	 * @param statementsMap
	 * @param statement
	 * @param resource
	 */
	private void index(Map<Resource, Set<Statement>> statementsMap, Statement statement, Resource resource) {
		Set<Statement> statements = statementsMap.get(resource);
		if (statements == null) {
			statements = new HashSet<Statement>();
			statementsMap.put(resource, statements);
		}
		statements.add(statement);
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
