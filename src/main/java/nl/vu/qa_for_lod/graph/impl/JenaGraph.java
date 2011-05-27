/**
 * 
 */
package nl.vu.qa_for_lod.graph.impl;

import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

import nl.vu.qa_for_lod.graph.Graph;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class JenaGraph implements Graph {
	private final Model model = ModelFactory.createDefaultModel();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.qa_for_lod.graph.Graph#addStatement(com.hp.hpl.jena.rdf.model.Statement
	 * )
	 */
	public void addStatement(Statement statement) {
		model.add(statement);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.graph.Graph#clear()
	 */
	public void clear() {
		model.removeAll();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.qa_for_lod.graph.Graph#containsResource(com.hp.hpl.jena.rdf.model
	 * .Resource)
	 */
	public boolean containsResource(Resource resource) {

		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.qa_for_lod.graph.Graph#getDegree(com.hp.hpl.jena.rdf.model.Resource
	 * )
	 */
	public int getDegree(Resource resource) {
		Set<Statement> edges = new HashSet<Statement>();
		for (Statement statement : model.listStatements(resource, (Property) null, (RDFNode) null).toList())
			edges.add(statement);
		for (Statement statement : model.listStatements((Resource) null, (Property) null, resource).toList())
			edges.add(statement);
		return edges.size();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.qa_for_lod.graph.Graph#getNeighbours(com.hp.hpl.jena.rdf.model.
	 * Resource, java.lang.String)
	 */
	public Set<Resource> getNeighbours(Resource resource, String propertyURI) {
		Set<Resource> neighbours = new HashSet<Resource>();

		Property property = null;
		if (propertyURI != null)
			property = model.createProperty(propertyURI);

		for (Statement statement : model.listStatements(resource, property, (RDFNode) null).toList())
			neighbours.add(statement.getObject().asResource());
		for (Statement statement : model.listStatements((Resource) null, property, resource).toList())
			neighbours.add(statement.getSubject());

		return neighbours;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.qa_for_lod.graph.Graph#containsEdge(com.hp.hpl.jena.rdf.model.Resource
	 * , com.hp.hpl.jena.rdf.model.Resource)
	 */
	public boolean containsEdge(Resource fromResource, Resource toResource, boolean directed) {
		boolean connected = model.listStatements(fromResource, (Property) null, toResource).toList().size() > 0;
		// If we ignore the direction and from->to doesn't work, try the inverse
		if (!directed && !connected)
			connected = model.listStatements(toResource, (Property) null, fromResource).toList().size() > 0;
		return connected;
	}

}
