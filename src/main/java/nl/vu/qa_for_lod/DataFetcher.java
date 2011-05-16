/**
 * 
 */
package nl.vu.qa_for_lod;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.tdb.TDBFactory;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
// TODO When the dereferenced content is an HTML page, create triples for all
// the A links.
// TODO Keep connection open and check
// http://stackoverflow.com/questions/4612573/exception-using-httprequest-execute-invalid-use-of-singleclientconnmanager-co
public class DataFetcher {
	protected final Logger logger = LoggerFactory.getLogger(DataFetcher.class);
	private final Model model;

	private final Property HAS_BLACK_LIST;
	private final Resource THIS;

	/**
	 * 
	 */
	public DataFetcher() {
		model = TDBFactory.createModel("tdb-cache");
		THIS = model.createResource("http://example.org/this");
		HAS_BLACK_LIST = model.createProperty("http://example.org/blacklisted");

		// HACK Force re-try
		// model.removeAll(THIS, HAS_BLACK_LIST, null);
	}

	/**
	 * @param resource
	 * @return
	 */
	public Collection<Statement> get(Resource resource) {
		Set<Statement> statements = new HashSet<Statement>();

		// Check if the URI is in the cache
		if (model.contains(resource, null)) {
			// In the cache
			StmtIterator iter = model.listStatements(resource, (Property) null, (RDFNode) null);
			while (iter.hasNext()) {
				Statement stmt = iter.nextStatement();
				if (stmt.getSubject().equals(resource) && stmt.getObject() instanceof Resource)
					statements.add(stmt);
			}
		} else if (!model.contains(THIS, HAS_BLACK_LIST, resource)) {
			// Not in the cache, dereference the resource
			Model m = ModelFactory.createDefaultModel();
			try {
				getData(m, resource.getURI());

				// Insert the relevant statements in the cache and in the
				// results to be returned
				StmtIterator iter = m.listStatements();
				while (iter.hasNext()) {
					Statement stmt = iter.nextStatement();
					if (stmt.getSubject().equals(resource) && stmt.getObject() instanceof Resource) {
						statements.add(stmt);
						model.add(stmt);
					}
				}

				// If we don't get anything, black-list the resource
				if (statements.size() == 0)
					model.add(THIS, HAS_BLACK_LIST, resource);
			} catch (Exception e) {
				// Ignore the exception
				e.printStackTrace();
			} finally {
				// Get rid of the temporary model
				m.close();
			}
		} else {
			// logger.warn("Black-listed resource " + resource);
		}

		return statements;
	}

	/**
	 * @param m
	 * @param URI
	 * @throws Exception
	 */
	private void getData(Model m, String URI) throws Exception {
		// TODO try to get the data from a SPARQL end point
		// http://sparql.sindice.com/sparql
		// http://lod.openlinksw.com/sparql
		StringBuffer query = new StringBuffer();
		query.append("SELECT * WHERE {<").append(URI).append("> ?p ?o. FILTER (isURI(?o))}");

		// If we don't get any triple, try to de-reference the URI
		if (m.size() == 0)
			deferenceResource(m, URI);
	}

	/**
	 * @param URI
	 * @return
	 * @throws Exception
	 */
	private void deferenceResource(Model m, String URI) throws Exception {
		// Ask for some RDF XML content
		HttpEntity entity = null;
		HttpGet httpget = null;
		HttpClient httpclient = null;
		
		try {
			httpclient = new DefaultHttpClient();
			httpget = new HttpGet(URI);
			httpget.setHeader("Accept", "application/rdf+xml");
			HttpResponse response = httpclient.execute(httpget);
			entity = response.getEntity();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		// If we don't get it, return
		// TODO If the entity was not an RDF resource, remove the node from the graph
		if (entity == null || entity.getContentType() == null || !entity.getContentType().getValue().startsWith("application/rdf+xml")) {
			System.out.println(URI); 
			return;
		}
		
		InputStream instream = entity.getContent();
		try {
			m.read(new InputStreamReader(instream), null);
		} catch (Exception ex) {
			httpget.abort();
		} finally {
			instream.close();
			httpclient.getConnectionManager().shutdown();
		}

		System.out.println("Loaded " + m.size() + " triples from " + URI);
	}

	/**
	 * Close all the connections and flush the data remaining in memory
	 */
	public void close() {
		model.close();
	}
}
