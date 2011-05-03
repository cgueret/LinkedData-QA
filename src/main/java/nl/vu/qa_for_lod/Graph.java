/**
 * 
 */
package nl.vu.qa_for_lod;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.preview.api.ColorizerFactory;
import org.gephi.preview.api.EdgeColorizer;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.project.api.ProjectController;
import org.openide.util.Lookup;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class Graph {
	private DirectedGraph directedGraph;
	private GraphModel graphModel;

	/**
	 * Init Gephi and create a graph
	 */
	public Graph() {
		ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
		pc.newProject();
		graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
		directedGraph = graphModel.getDirectedGraph();
	}

	/**
	 * @param resource
	 * @return
	 */
	private Node addNode(Resource resource, float size) {
		Node node = graphModel.factory().newNode(resource.getURI());
		node.getNodeData().setLabel(resource.getURI());
		node.getNodeData().setSize(size);
		directedGraph.addNode(node);
		return node;
	}

	/**
	 * @param statement
	 * @param size
	 *            Use size to mark seed VS extra nodes
	 */
	public void addStatement(Statement statement, float size) {
		Resource subject = statement.getSubject();
		Property predicate = statement.getPredicate();
		RDFNode object = statement.getObject();
		if (!(object instanceof Resource))
			return;

		// Add the triple to the Gephi graph
		Node sNode = graphModel.getGraph().getNode(subject.getURI());
		if (sNode == null)
			sNode = addNode(subject, size);
		Node oNode = graphModel.getGraph().getNode(object.asResource().getURI());
		if (oNode == null)
			oNode = addNode(object.asResource(), size);
		Edge edge = graphModel.factory().newEdge(sNode, oNode, size, true);
		edge.getEdgeData().setLabel(predicate.getURI());
		directedGraph.addEdge(edge);
	}

	/**
	 * @param resource
	 * @return
	 */
	public boolean containsResource(Resource resource) {
		if (resource == null)
			return false;

		return (graphModel.getGraph().getNode(resource.getURI()) != null);
	}

	/**
	 * @return
	 */
	public Set<Node> getNodes(float size) {
		Set<Node> nodes = new HashSet<Node>();
		for (Node node : graphModel.getGraph().getNodes())
			if (node.getNodeData().getSize() == size)
				nodes.add(node);
		return nodes;
	}

	/**
	 * Output some stats
	 */
	public String getStats() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Nodes: ").append(directedGraph.getNodeCount());
		buffer.append(", Edges: ").append(directedGraph.getEdgeCount());
		return buffer.toString();
	}

	/**
	 * @param string
	 *            From http://wiki.gephi.org/index.php/Toolkit_-_Export_graph
	 */
	public void dump(String string) {
		// Configure the rendering of the graph
		PreviewModel model = Lookup.getDefault().lookup(PreviewController.class).getModel();
		model.getNodeSupervisor().setShowNodeLabels(Boolean.TRUE);
		ColorizerFactory colorizerFactory = Lookup.getDefault().lookup(ColorizerFactory.class);
		model.getUniEdgeSupervisor().setColorizer(
				(EdgeColorizer) colorizerFactory.createCustomColorMode(Color.LIGHT_GRAY));
		model.getBiEdgeSupervisor().setColorizer((EdgeColorizer) colorizerFactory.createCustomColorMode(Color.GRAY));
		model.getUniEdgeSupervisor().setEdgeScale(0.1f);
		model.getBiEdgeSupervisor().setEdgeScale(0.1f);
		model.getNodeSupervisor().setBaseNodeLabelFont(model.getNodeSupervisor().getBaseNodeLabelFont().deriveFont(8));

		// Export the graph in gexf and pdf
		ExportController ec = Lookup.getDefault().lookup(ExportController.class);
		try {
			ec.exportFile(new File(string + ".pdf"));
			ec.exportFile(new File(string + ".gexf"));
		} catch (IOException ex) {
			ex.printStackTrace();
			return;
		}
	}
}
