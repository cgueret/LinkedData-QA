/**
 * 
 */
package nl.vu.qa_for_lod;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import nl.vu.qa_for_lod.data.Results;

import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
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
import org.gephi.statistics.plugin.GraphDistance;
import org.gephi.statistics.plugin.PageRank;
import org.openide.util.Lookup;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
// http://gephi.org/docs/toolkit/org/gephi/statistics/spi/Statistics.html
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
	private Node addNode(Resource resource) {
		Node node = graphModel.factory().newNode(resource.getURI());
		node.getNodeData().setLabel(resource.getURI());
		directedGraph.addNode(node);
		return node;
	}

	/**
	 * @param statement
	 * @param size
	 *            Use size to mark seed VS extra nodes
	 */
	public void addStatement(Statement statement) {
		Resource subject = statement.getSubject();
		Property predicate = statement.getPredicate();
		RDFNode object = statement.getObject();
		if (!(object instanceof Resource))
			return;

		// Add the triple to the Gephi graph
		Node sNode = graphModel.getGraph().getNode(subject.getURI());
		if (sNode == null)
			sNode = addNode(subject);
		Node oNode = graphModel.getGraph().getNode(object.asResource().getURI());
		if (oNode == null)
			oNode = addNode(object.asResource());
		Edge edge = graphModel.factory().newEdge(sNode, oNode, 1.0f, true);
		edge.getEdgeData().setLabel(predicate.getURI());
		directedGraph.addEdge(edge);
	}

	/**
	 * @param statements
	 */
	public void addStatements(Collection<Statement> statements) {
		for (Statement statement : statements)
			this.addStatement(statement);
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
	 * @param string
	 * 
	 */
	public void dump(String string) {
		// Configure the rendering of the graph
		// (from http://wiki.gephi.org/index.php/Toolkit_-_Export_graph)
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

	/**
	 * @return
	 */
	public Set<Node> getNodes() {
		Set<Node> nodes = new HashSet<Node>();
		for (Node node : graphModel.getGraph().getNodes())
			nodes.add(node);
		return nodes;
	}

	/**
	 * Compute the centrality of all the nodes in the network
	 * 
	 * @return A list of nodes name with their centrality value
	 */
	// http://wiki.gephi.org/index.php/Toolkit_-_Statistics_and_Metrics
	public void getNodesCentrality(Results results) {
		// Get graph model and attribute model of current workspace
		GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
		AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();

		// Get Centrality
		GraphDistance distance = new GraphDistance();
		distance.setDirected(true);
		distance.setRelative(false);
		distance.execute(graphModel, attributeModel);

		// Iterate over values
		int col = attributeModel.getNodeTable().getColumn(GraphDistance.BETWEENNESS).getIndex();
		for (Node n : directedGraph.getNodes()) {
			Double centrality = (Double) n.getNodeData().getAttributes().getValue(col);
			results.put(n.getNodeData().getLabel(), centrality);
		}
	}

	/**
	 * @return
	 */
	public void getNodesDegree(Results results) {
		for (Node node : directedGraph.getNodes())
			results.put(node.getNodeData().getLabel(), Double.valueOf(directedGraph.getDegree(node)));
	}

	/**
	 * @return
	 */
	public void getNodesPopularity(Results results) {
		// Get graph model and attribute model of current workspace
		GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
		AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();

		// Get PageRank
		PageRank pageRank = new PageRank();
		pageRank.setUndirected(false);
		pageRank.execute(graphModel, attributeModel);

		// Place the results in the results class
		int col = attributeModel.getNodeTable().getColumn(PageRank.PAGERANK).getIndex();
		for (Node n : directedGraph.getNodes()) {
			Double centrality = (Double) n.getNodeData().getAttributes().getValue(col);
			results.put(n.getNodeData().getLabel(), centrality);
		}
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
	 * @param seedURIs
	 */
	// TODO Parallelise this
	public void loadGraphFromSeeds(Set<Resource> seedURIs) {
		// Get a data fetcher
		DataFetcher fetcher = new DataFetcher();

		// This list will be used for the second level
		Set<Resource> neighbours = new HashSet<Resource>();

		// Load the data from the seeds
		for (Resource resource : seedURIs) {
			for (Statement stmt : fetcher.get(resource)) {
				addStatement(stmt);
				neighbours.add(stmt.getObject().asResource());
			}
		}

		// Connect the neighbours among them
		for (Resource resource : neighbours)
			for (Statement stmt : fetcher.get(resource))
				if (containsResource(stmt.getObject().asResource()))
					addStatement(stmt);

		// Close the data fetcher
		fetcher.close();
	}
}
