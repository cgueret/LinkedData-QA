/**
 * 
 */
package nl.vu.qa_for_lod.graph.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import nl.vu.qa_for_lod.graph.Direction;
import nl.vu.qa_for_lod.graph.EndPoint;

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
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.sparql.expr.E_IsURI;
import com.hp.hpl.jena.sparql.expr.ExprVar;
import com.hp.hpl.jena.sparql.syntax.ElementFilter;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.Template;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
// TODO Add flag to SPARQL also for incoming relations
public class DataAcquisitionTask implements Callable<Set<Statement>> {
	// Logger
	final static Logger logger = LoggerFactory.getLogger(DataAcquisitionTask.class);
	// List of time to wait, in second, before trying a resource again
	final static int[] RETRY_DELAY = { 1, 5, 10 };
	// Target resource
	final Resource resource;
	// The model to bind the statements to
	final Model model;
	// List of end points to query
	final List<EndPoint> endPoints;
	// The direction telling if we need to retrieve RPO,SPR or both for R
	final Direction direction;
	// Maximum numbers of triples to retrieve from sparql and rdf docs
	// If direction is both, then each direction gets hasf of it
	final Integer maxResultSetSize = 150;
	// Maximum number of failures before we give up on an host
	final Integer maxHostErrorCount = 3;
	// How often an unavailable host has been attempted to contact
	// FIXME This map should be shared by all threads
	Map<String, Integer> unavailableHostToReferenceCount = new HashMap<String, Integer>();
	
	
	// Increments the value for a given key
	public static <K> int current(Map<K, Integer> map, K key) {
		Integer value = map.get(key);
		return value == null ? 0 : value;
	}

	public static <K> void increment(Map<K, Integer> map, K key) {
		map.put(key, current(map, key) + 1);
	}

	
	/**
	 * @param endPoints
	 * @param resource
	 * @param direction
	 * @param model
	 */
	public DataAcquisitionTask(List<EndPoint> endPoints, Resource resource, Direction direction, Model model) {
		this.resource = resource;
		this.model = model;
		this.direction = direction;
		this.endPoints = endPoints;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.Callable#call()
	 */
	public Set<Statement> call() throws Exception {
		// Set of statements retrieved
		Set<Statement> statements = new HashSet<Statement>();

		// Until we are successful, try every end point
		int endpointIndex = 0;
		while (statements.isEmpty() && endpointIndex != endPoints.size()) {
			queryEndPoint(statements, endPoints.get(endpointIndex));
			if (!statements.isEmpty())
				logger.info(String.format("Got %d results for %s from %s", statements.size(), resource,
						endPoints.get(endpointIndex)));
			endpointIndex++;
		}

		// Last chance, dereference the resource
		if (statements.isEmpty())
			dereferenceResource(statements);

		return statements;
	}

	/**
	 * @param statements
	 * @param string
	 * @return
	 */
	private boolean queryEndPoint(Set<Statement> statements, EndPoint endPoint) {

		// Half the value of maxResultSetSize for both directions
		Integer resultSetSize = null;
		if(maxResultSetSize != null) {
			if(direction.equals(Direction.BOTH)) {
				resultSetSize = maxResultSetSize / 2;
			} else {
				resultSetSize = maxResultSetSize;
			}
		}

		// Outgoing triples
		if (direction.equals(Direction.OUT) || direction.equals(Direction.BOTH)) {
			// Compose the query
			Node p = Node.createVariable("p");
			Node o = Node.createVariable("o");
			Query query = QueryFactory.create();
			query.setQueryConstructType();
			Triple triple = new Triple(resource.asNode(), p, o);
			ElementGroup group = new ElementGroup();
			group.addTriplePattern(triple);
			group.addElementFilter(new ElementFilter(new E_IsURI(new ExprVar(o))));
			BasicPattern bgp = new BasicPattern();
			bgp.add(triple);
			query.setConstructTemplate(new Template(bgp));
			query.setQueryPattern(group);

			if(resultSetSize != null) {
				query.setLimit(resultSetSize);
			}
			
			// Execute and add the results
			QueryEngineHTTP qExec = (QueryEngineHTTP) QueryExecutionFactory.sparqlService(endPoint.getURI(), query);
			if (endPoint.getGraph() != null)
				qExec.addDefaultGraph(endPoint.getGraph());
			try {
				statements.addAll(qExec.execConstruct().listStatements().toSet());
			} catch (Exception e) {
				logger.warn("Error with " + endPoint.toString());
				e.printStackTrace();
			} finally {
				if (qExec != null)
					qExec.close();
			}
		}

		// Incoming triples		
		if (direction.equals(Direction.IN) || direction.equals(Direction.BOTH)) {
			// Compose the query
			Node p = Node.createVariable("p");
			Node s = Node.createVariable("s");
			Query query = QueryFactory.create();
			query.setQueryConstructType();
			Triple triple = new Triple(s, p, resource.asNode());
			ElementGroup group = new ElementGroup();
			group.addTriplePattern(triple);
			BasicPattern bgp = new BasicPattern();
			bgp.add(triple);
			query.setConstructTemplate(new Template(bgp));
			query.setQueryPattern(group);

			if(resultSetSize != null) {
				query.setLimit(resultSetSize);
			}
			
			// Execute and add the results
			QueryEngineHTTP qExec = (QueryEngineHTTP) QueryExecutionFactory.sparqlService(endPoint.getURI(), query);
			if (endPoint.getGraph() != null)
				qExec.addDefaultGraph(endPoint.getGraph());
			try {
				statements.addAll(qExec.execConstruct().listStatements().toSet());
			} catch (Exception e) {
				logger.warn("Error with " + endPoint.toString());
				e.printStackTrace();
			} finally {
				if (qExec != null)
					qExec.close();
			}
		}

		return (statements.size() > 0);
	}

	/**
	 * @param statements
	 * @return
	 */
	private boolean dereferenceResource(Set<Statement> statements) {
		// Try to download the resource
		boolean retry = true;
		int retryCount = 0;
		while (retry) {
			// Be optimistic
			retry = false;

			// We blacklist the hostname if it causes troubles
			String hostName = "";
			try {
				URL resourceUrl = new URL(resource.getURI());
				hostName = resourceUrl.getHost();

				Integer errorCount = unavailableHostToReferenceCount.get(hostName);
				if(errorCount != null && errorCount > maxHostErrorCount) {
					logger.warn("Error count for host '" + hostName + "' exceeded limit - skipped.");
				}

				// Get the data
				Any23 runner = new Any23();
				runner.setHTTPUserAgent("LATC QA tool <c.d.m.gueret@vu.nl>");
				HTTPClient httpClient = runner.getHTTPClient();
				DocumentSource source = new HTTPDocumentSource(httpClient, resource.getURI());
				MyTripleHandler handler = new MyTripleHandler(resource, model, direction);
				runner.extract(source, handler);
				statements.addAll(handler.getBuffer());

				// Reduce the statements
				// FIXME Is there a way to skip statements before the whole document
				// has been downloaded?
				int statementIndex = 0;
				for(Iterator<Statement> it = statements.iterator(); it.hasNext();) {
					it.next();
					if(statementIndex > maxResultSetSize) {
						it.remove();
					}
					
					statementIndex++;
				}
				int removalCount = Math.max(statementIndex - maxResultSetSize, 0);
				if(removalCount > 0) {
					logger.warn("Skipped " + removalCount + " triples from document.");
				}
				
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
				increment(unavailableHostToReferenceCount, hostName);
				logger.warn("Host '" + hostName + "' error count is: " + unavailableHostToReferenceCount.get(hostName) + "/" + maxHostErrorCount);
			} catch (MalformedURLException e) {
				// We could not parse the uri (similar to next catch block)
			} catch (URISyntaxException e) {
				// Error in URI, unlikely to change
			} catch (IOException e) {
				// 404 or alike, not worth trying again
				increment(unavailableHostToReferenceCount, hostName);
				logger.warn("Host '" + hostName + "' error count is: " + unavailableHostToReferenceCount.get(hostName) + "/" + maxHostErrorCount);
			} catch (ExtractionException e) {
				// Something is wrong with the data, give up
			} catch (Exception e) {
				// What?! Ok, just give up anyway
				// e.printStackTrace();
			}
		}

		return false;
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		List<EndPoint> endPoints = new ArrayList<EndPoint>();
		endPoints.add(new EndPoint("http://dbpedia.org/sparql","http://dbpedia.org"));

		DataAcquisitionTask me = new DataAcquisitionTask(endPoints,
				ResourceFactory.createResource("http://dbpedia.org/resource/Amsterdam"), Direction.BOTH,
				ModelFactory.createDefaultModel());
		me.call();
	}

}
