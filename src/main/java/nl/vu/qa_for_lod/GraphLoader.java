/**
 * 
 */
package nl.vu.qa_for_lod;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.gephi.graph.api.Node;

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

		graph.printStats();
		
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
