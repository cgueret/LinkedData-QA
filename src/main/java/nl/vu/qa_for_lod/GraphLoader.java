/**
 * 
 */
package nl.vu.qa_for_lod;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.gephi.graph.api.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFReader;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class GraphLoader {
	// Logger
	protected final Logger logger = LoggerFactory.getLogger(DataFetcher.class);
	
	// The graph storing the data
	private Graph graph;

	/**
	 * @param graph
	 */
	public GraphLoader(Graph graph) {
		this.graph = graph;
	}

	/**
	 * @param string
	 * @throws Exception
	 */
	public void loadSeed(String seedFileName) throws Exception {
		// Read the seed rdf
		Model model = ModelFactory.createDefaultModel();
		RDFReader reader = model.getReader("N3");
		reader.setProperty("WARN_REDEFINITION_OF_ID", "EM_IGNORE"); // speed
		InputStream in = FileManager.get().open(seedFileName);
		if (in == null) {
			throw new Exception();
		}
		reader.read(model, in, null);

		// Iterate through the content of the seed and create the graph
		StmtIterator iter = model.listStatements();
		int max = 50;
		while (iter.hasNext() && max-- != 0) {
			Statement stmt = iter.nextStatement();
			if (!(stmt.getObject() instanceof Resource))
				continue;

			graph.addStatement(stmt, 1f);
		}
		model.close();
	}

	/**
	 * 
	 */
	public void expandGraph() {
		DataFetcher fetcher = new DataFetcher();

		// Expand nodes in the graph
		for (Node node : graph.getNodes(1f)) {
			for (Statement stmt : fetcher.get(node.toString())) {
				if (graph.containsResource(stmt.getSubject())) {
					graph.addStatement(stmt, 2f);
				}
			}
		}
		logger.info("After expansion => " + graph.getStats());

		// Connect the nodes of radius 2 to each other
		for (Node node : graph.getNodes(2f)) {
			for (Statement stmt : fetcher.get(node.toString())) {
				if (!(stmt.getObject() instanceof Resource))
					continue;

				if (graph.containsResource(stmt.getSubject()) && graph.containsResource(stmt.getObject().asResource())) {
					graph.addStatement(stmt, 3f);
				}
			}
		}
		logger.info("After connecting neighbours => " + graph.getStats());


		fetcher.close();
	}

	/**
	 * 
	 */
	public void expandGraph_prev() {
		// Expand nodes in the graph
		for (Node node : graph.getNodes(1f)) {
			Model m = ModelFactory.createDefaultModel();
			try {
				// Load the triples about that node
				String fileName = node.toString().replace('/', '_');
				File f = new File("cache/" + fileName);
				if (f.exists()) {
					// Content is cached
					m.read(new FileReader(f), null);
				} else {
					// Content is not cached
					m.read(node.toString());
					m.write(new FileOutputStream(f), null);
				}

				// Insert the nodes into the graph
				StmtIterator iter = m.listStatements();
				while (iter.hasNext()) {
					Statement stmt = iter.nextStatement();
					if (!(stmt.getObject() instanceof Resource))
						continue;

					if (graph.containsResource(stmt.getSubject()))
						graph.addStatement(stmt, 2f);
				}
			} catch (Exception e) {
				// Something went wrong with loading the resource
			} finally {
				m.close();
			}
		}

		graph.getStats();

		// Connect the nodes of radius 2 to each other
		for (Node node : graph.getNodes(2f)) {
			Model m = ModelFactory.createDefaultModel();

			try {
				// Load the triples about that node
				String fileName = node.toString().replace('/', '_');
				File f = new File("cache/" + fileName);
				if (f.exists()) {
					// Content is cached
					m.read(new FileReader(f), null);
				} else {
					HttpClient httpclient = new DefaultHttpClient();
					HttpGet httpget = new HttpGet(node.toString());
					httpget.setHeader("Accept", "application/rdf+xml");
					HttpResponse response = httpclient.execute(httpget);
					System.out.println(node.toString() + " -> " + response.getStatusLine());

					// Get hold of the response entity
					HttpEntity entity = response.getEntity();
					System.out.println(entity.getContentType().getValue());

					// If the response does not enclose an entity, there is no
					// need
					// to worry about connection release
					if (entity != null && response.getStatusLine().getStatusCode() == 200
							&& entity.getContentType().getValue().startsWith("application/rdf+xml")) {
						InputStream instream = entity.getContent();
						try {

							BufferedReader reader = new BufferedReader(new InputStreamReader(instream));
							// do something useful with the response

							System.out.println("parse");

							// Content is not cached
							m.read(reader.readLine());

							// TODO Wrap the loading code in an external class
							// TODO Use HTTPClient to load the URI with accept
							// on
							// RDF/XML
							// TODO Save only the useful information in ntriples
							m.write(new FileOutputStream(f), null);

							System.out.println(reader.readLine());

						} catch (IOException ex) {

							// In case of an IOException the connection will be
							// released
							// back to the connection manager automatically
							throw ex;

						} catch (RuntimeException ex) {

							// In case of an unexpected exception you may want
							// to abort
							// the HTTP request in order to shut down the
							// underlying
							// connection and release it back to the connection
							// manager.
							httpget.abort();
							throw ex;

						} finally {

							// Closing the input stream will trigger connection
							// release
							instream.close();

						}

						// When HttpClient instance is no longer needed,
						// shut down the connection manager to ensure
						// immediate deallocation of all system resources
						httpclient.getConnectionManager().shutdown();
					}

				}

				// Insert the nodes into the graph
				StmtIterator iter = m.listStatements();
				while (iter.hasNext()) {
					Statement stmt = iter.nextStatement();
					if (!(stmt.getObject() instanceof Resource))
						continue;

					if (graph.containsResource(stmt.getSubject()))
						if (graph.containsResource(stmt.getObject().asResource()))
							graph.addStatement(stmt, 3f);
				}
			} catch (Exception e) {
				// Something went wrong with loading the resource
			} finally {
				m.close();
			}
		}

	}
}
