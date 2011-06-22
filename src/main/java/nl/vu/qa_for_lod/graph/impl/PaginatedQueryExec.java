package nl.vu.qa_for_lod.graph.impl;

import java.util.HashSet;
import java.util.Set;

import nl.vu.qa_for_lod.graph.EndPoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class PaginatedQueryExec {
	static final Logger logger = LoggerFactory.getLogger(PaginatedQueryExec.class);
	private final static int PAGE_SIZE = 1000;

	/**
	 * @param endPoint
	 * @param query
	 * @param var
	 * @return
	 */
	public static Set<Statement> processConstruct(EndPoint endPoint, Query query) {
		query.setLimit(PAGE_SIZE);
		query.setOffset(0);

		Set<Statement> results = new HashSet<Statement>();
		boolean morePages = true;
		while (morePages) {
			long count = 0;
			QueryEngineHTTP queryExec = null;
			try {
				queryExec = (QueryEngineHTTP) QueryExecutionFactory.sparqlService(endPoint.getURI(), query);
				if (endPoint.getGraph() != null)
					queryExec.addDefaultGraph(endPoint.getGraph());
				Set<Statement> res = queryExec.execConstruct().listStatements().toSet();
				count = res.size();
				results.addAll(res);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (queryExec != null)
					queryExec.close();
			}
			morePages = (count == PAGE_SIZE);
			query.setOffset(query.getOffset() + PAGE_SIZE);
		}
		return results;
	}
}
