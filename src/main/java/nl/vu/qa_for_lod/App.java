package nl.vu.qa_for_lod;

import java.io.InputStream;

import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.project.api.ProjectController;
import org.openide.util.Lookup;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RDFReader;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;

public class App {
	public static void main(String[] args) throws Exception {
		System.out.println("Loading graph...");

		// Init Gephi and create a graph
		ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
		pc.newProject();
		GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
		DirectedGraph directedGraph = graphModel.getDirectedGraph();

		// Read the seed graph
		Model model = ModelFactory.createDefaultModel();
		RDFReader reader = model.getReader("N3");
		reader.setProperty("WARN_REDEFINITION_OF_ID", "EM_IGNORE"); // speed
		InputStream in = FileManager.get().open("data/links-cut.nt");
		if (in == null) {
			throw new Exception();
		}
		reader.read(model, in, null);

		// Iterate through the content of the seed graph
		StmtIterator iter = model.listStatements();
		while (iter.hasNext()) {
			Statement stmt = iter.nextStatement();
			Resource subject = stmt.getSubject();
			Property predicate = stmt.getPredicate();
			RDFNode object = stmt.getObject();
			if (!(object instanceof Resource))
				continue;

			// Add the triple to the Gephi graph
			Node sNode = graphModel.getGraph().getNode(subject.getURI());
			if (sNode == null) {
				sNode = graphModel.factory().newNode(subject.getURI());
				sNode.getNodeData().setLabel(subject.getURI());
				// Use size to mark seed VS extra nodes
				sNode.getNodeData().setSize(1f);
			}
			Node oNode = graphModel.getGraph().getNode(object.asResource().getURI());
			if (oNode == null) {
				oNode = graphModel.factory().newNode(object.asResource().getURI());
				oNode.getNodeData().setLabel(object.asResource().getURI());
				oNode.getNodeData().setSize(1f);
			}
			Edge edge = graphModel.factory().newEdge(sNode, oNode, 1f, true);
			edge.getEdgeData().setLabel(predicate.getURI());
			directedGraph.addNode(sNode);
			directedGraph.addNode(oNode);
			directedGraph.addEdge(edge);
		}
		model.close();

		
		System.out.println("Nodes: " + directedGraph.getNodeCount());
		System.out.println("Edges: " + directedGraph.getEdgeCount());
	}
}
