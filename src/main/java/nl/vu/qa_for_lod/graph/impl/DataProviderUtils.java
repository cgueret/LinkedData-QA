package nl.vu.qa_for_lod.graph.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class DataProviderUtils {

	/**
	 * Creates a DataProvider from a model. 
	 * 
	 * @param model
	 * @return
	 */
	public static DataProviderMap create(Model model) {

		Map<Resource, Set<Statement>> outgoingStatements = new HashMap<Resource, Set<Statement>>();
		Map<Resource, Set<Statement>> incomingStatements = new HashMap<Resource, Set<Statement>>();
		
		StmtIterator iter = model.listStatements();
		while (iter.hasNext()) {
			Statement stmt = iter.nextStatement();
			if (stmt.getSubject() instanceof Resource && stmt.getObject() instanceof Resource) {
				index(outgoingStatements, stmt, stmt.getSubject());
				index(incomingStatements, stmt, stmt.getObject().asResource());

			}
		}

		return new DataProviderMap(outgoingStatements, incomingStatements);
	}
	
	/**
	 * @param statementsMap
	 * @param statement
	 * @param resource
	 */
	private static void index(Map<Resource, Set<Statement>> statementsMap, Statement statement, Resource resource) {
		Set<Statement> statements = statementsMap.get(resource);
		if (statements == null) {
			statements = new HashSet<Statement>();
			statementsMap.put(resource, statements);
		}
		statements.add(statement);
	}
	
}
