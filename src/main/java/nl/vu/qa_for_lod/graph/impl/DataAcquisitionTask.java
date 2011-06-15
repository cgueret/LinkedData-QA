/**
 * 
 */
package nl.vu.qa_for_lod.graph.impl;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.deri.any23.Any23;
import org.deri.any23.extractor.ExtractionException;
import org.deri.any23.http.HTTPClient;
import org.deri.any23.source.DocumentSource;
import org.deri.any23.source.HTTPDocumentSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.sparql.expr.E_IsURI;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprFunction;
import com.hp.hpl.jena.sparql.expr.ExprVar;
import com.hp.hpl.jena.sparql.syntax.ElementFilter;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.TemplateGroup;
import com.hp.hpl.jena.sparql.syntax.TemplateTriple;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
// TODO Add flag to SPARQL also for incoming relations
public class DataAcquisitionTask implements Callable<Boolean> {
	// Logger
	final static Logger logger = LoggerFactory.getLogger(DataAcquisitionTask.class);
	// List of time to wait, in second, before trying a resource again
	final static int[] RETRY_DELAY = { 1, 5, 10 };
	// Target resource
	final Resource resource;
	// Set of statements retrieved
	final Set<Statement> statements = new HashSet<Statement>();
	// The model to bind the statements to
	final Model model;
	// List of end points to query
	final List<String> endPoints;

	/**
	 * @param endPoints
	 * @param resource
	 * @param model
	 */
	public DataAcquisitionTask(List<String> endPoints, Resource resource, Model model) {
		this.resource = resource;
		this.model = model;
		this.endPoints = endPoints;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.Callable#call()
	 */
	public Boolean call() throws Exception {
		boolean success = false;

		// Until we are successfull, try every end point
		int endpointIndex = 0;
		while (!success && endpointIndex != endPoints.size()) {
			success = queryEndPoint(endPoints.get(endpointIndex));
			endpointIndex++;
		}

		// Last chance, dereference the resource
		if (!success)
			success = dereferenceResource();

		return success;
	}

	/**
	 * @return
	 */
	public Set<Statement> getStatements() {
		return statements;
	}

	/**
	 * @param string
	 * @return
	 */
	private boolean queryEndPoint(String serviceURI) {
		// Compose the query
		Node p = Node.createVariable("p");
		Node o = Node.createVariable("o");
		Query query = QueryFactory.create();
		query.setQueryConstructType();
		Triple triple = new Triple(resource.asNode(), p, o);
		ElementGroup group = new ElementGroup();
		group.addTriplePattern(triple);
		group.addElementFilter(new ElementFilter(new E_IsURI(new ExprVar(o))));
		TemplateGroup g = new TemplateGroup();
		g.add(new TemplateTriple(triple));
		query.setConstructTemplate(g);
		query.setQueryPattern(group);

		// Execute and add the results
		QueryExecution qExec = QueryExecutionFactory.sparqlService(serviceURI, query);
		Set<Statement> stmts = qExec.execConstruct().listStatements().toSet();
		statements.addAll(stmts);
		qExec.close();

		return (stmts.size() > 0);
	}

	/**
	 * @return
	 */
	private boolean dereferenceResource() {
		// Try to download the resource
		boolean retry = true;
		int retryCount = 0;
		while (retry) {
			// Be optimistic
			retry = false;

			try {
				// Get the data
				Any23 runner = new Any23();
				runner.setHTTPUserAgent("LATC QA tool prototype");
				HTTPClient httpClient = runner.getHTTPClient();
				DocumentSource source = new HTTPDocumentSource(httpClient, resource.getURI());
				MyTripleHandler handler = new MyTripleHandler(resource, model);
				runner.extract(source, handler);
				statements.addAll(handler.getBuffer());

				// We were successful
				return true;
			} catch (SocketTimeoutException e) {
				if (retryCount < RETRY_DELAY.length) {
					// Sleep and retry
					logger.warn("Time out for " + resource.getURI() + ", retry in " + RETRY_DELAY[retryCount]
							+ " seconds");
					try {
						Thread.sleep(RETRY_DELAY[retryCount] * 1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					retry = true;
					retryCount++;
				}
			} catch (IOException e) {
				// 404 or alike, not worth trying again
			} catch (URISyntaxException e) {
				// Error in URI, unlikely to change
			} catch (ExtractionException e) {
				// Something is wrong with the data, give up
			} catch (Exception e) {
				// What?! Ok, just give up anyway
				e.printStackTrace();
			}
		}

		return false;
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		List<String> endPoints = new ArrayList<String>();
		endPoints.add("http://dbpedia.org/sparql");

		DataAcquisitionTask me = new DataAcquisitionTask(endPoints,
				ResourceFactory.createResource("http://dbpedia.org/resource/Amsterdam"),
				ModelFactory.createDefaultModel());
		me.call();
	}

}
