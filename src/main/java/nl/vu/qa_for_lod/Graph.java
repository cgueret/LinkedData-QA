/**
 * 
 */
package nl.vu.qa_for_lod;

import java.util.HashSet;
import java.util.Set;

import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
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
	public void printStats() {
		System.out.println("Nodes: " + directedGraph.getNodeCount());
		System.out.println("Edges: " + directedGraph.getEdgeCount());
	}
}
