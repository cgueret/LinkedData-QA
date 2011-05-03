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
import java.util.HashSet;
import java.util.Set;

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
	 * @param seedURIs
	 */
	public void loadGraph(Set<Resource> seedURIs) {
		// Get a data fetcher
		DataFetcher fetcher = new DataFetcher();

		// This list will be used for the second level
		Set<Resource> neighbours = new HashSet<Resource>();

		// Load the data from the seeds
		for (Resource resource : seedURIs) {
			for (Statement stmt : fetcher.get(resource)) {
				graph.addStatement(stmt, 1f);
				neighbours.add(stmt.getObject().asResource());
			}
		}

		// Connect the neighbours among them
		for (Resource resource : neighbours)
			for (Statement stmt : fetcher.get(resource))
				if (graph.containsResource(stmt.getObject().asResource()))
					graph.addStatement(stmt, 1f);

		// Close the data fetcher
		fetcher.close();
	}
}
