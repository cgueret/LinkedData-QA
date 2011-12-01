package nl.vu.qa_for_lod.graph.impl;

import java.util.Set;

import nl.vu.qa_for_lod.graph.DataProvider;
import nl.vu.qa_for_lod.graph.Direction;

import org.aksw.commons.sparql.api.core.QueryExecutionFactory;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

/**
 * Fetches data using Describe Queries to a preconfigured
 * QueryExecutionFactory
 * 
 * @author Claus Stadler <cstadler@informatik.uni-leipzig.de>
 *
 */
public class DescribeDataProvider implements DataProvider {
	private QueryExecutionFactory<? extends QueryExecution> factory;

	@Override
	public void close() {
		// factory.close();
	}

	public static Set<Statement> filter(Model model, Resource resource, Direction direction) {
		switch(direction) {
		case BOTH: return model.listStatements(resource, null, (RDFNode)null).andThen(model.listStatements(null, null, resource)).toSet();
		case OUT:  return model.listStatements(resource, null, (RDFNode)null).toSet();
		case IN:   return model.listStatements(null, null, resource).toSet();
		default:   throw new RuntimeException("Should not happen");
		}		
	}
	
	public static void filterInPlace(Model model, Resource resource, Direction direction) {

		Set<Statement> retainedStmts = filter(model, resource, direction);

		StmtIterator it = model.listStatements();
		while(it.hasNext()) {
			Statement stmt = it.next();
			if(!retainedStmts.contains(stmt)) {
				it.remove();
			}
		}
		
		it.close();
	}
	
	@Override
	public Set<Statement> get(Resource resource, Direction direction) {
		// FIXME Build the query with Jena objects (no parsing overhead)
		QueryExecution qe =
				factory.createQueryExecution(CannedQueryUtils.describe(resource.asNode()));

		Model model = qe.execDescribe();

		//qe.close();
		return filter(model, resource, direction);
	}

}
