/**
 * 
 */
package nl.vu.qa_for_lod.graph.impl;

import java.util.HashSet;
import java.util.Set;

import org.deri.any23.extractor.ExtractionContext;
import org.deri.any23.writer.TripleHandler;
import org.deri.any23.writer.TripleHandlerException;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class MyTripleHandler implements TripleHandler {
	private final Set<Statement> buffer = new HashSet<Statement>();
	private final com.hp.hpl.jena.rdf.model.Resource resource;
	private final Model model;

	/**
	 * @param resource2
	 * @param model
	 */
	public MyTripleHandler(com.hp.hpl.jena.rdf.model.Resource resource, Model model) {
		this.resource = resource;
		this.model = model;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.deri.any23.writer.TripleHandler#startDocument(org.openrdf.model.URI)
	 */
	public void startDocument(URI documentURI) throws TripleHandlerException {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.deri.any23.writer.TripleHandler#openContext(org.deri.any23.extractor
	 * .ExtractionContext)
	 */
	public void openContext(ExtractionContext context) throws TripleHandlerException {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.deri.any23.writer.TripleHandler#receiveTriple(org.openrdf.model.Resource
	 * , org.openrdf.model.URI, org.openrdf.model.Value, org.openrdf.model.URI,
	 * org.deri.any23.extractor.ExtractionContext)
	 */
	public void receiveTriple(Resource s, URI p, Value o, URI g, ExtractionContext context)
			throws TripleHandlerException {
		if (o instanceof org.openrdf.model.Resource && !(o instanceof org.openrdf.model.BNode)
				&& s.toString().equals(resource.getURI())) {
			org.openrdf.model.Resource r = (org.openrdf.model.Resource) o;
			com.hp.hpl.jena.rdf.model.Resource jenaS = model.createResource(s.stringValue());
			com.hp.hpl.jena.rdf.model.Property jenaP = model.createProperty(p.stringValue());
			com.hp.hpl.jena.rdf.model.RDFNode jenaO = model.createResource(r.stringValue());
			buffer.add(model.createStatement(jenaS, jenaP, jenaO));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.deri.any23.writer.TripleHandler#receiveNamespace(java.lang.String,
	 * java.lang.String, org.deri.any23.extractor.ExtractionContext)
	 */
	public void receiveNamespace(String prefix, String uri, ExtractionContext context) throws TripleHandlerException {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.deri.any23.writer.TripleHandler#closeContext(org.deri.any23.extractor
	 * .ExtractionContext)
	 */
	public void closeContext(ExtractionContext context) throws TripleHandlerException {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.deri.any23.writer.TripleHandler#endDocument(org.openrdf.model.URI)
	 */
	public void endDocument(URI documentURI) throws TripleHandlerException {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.deri.any23.writer.TripleHandler#setContentLength(long)
	 */
	public void setContentLength(long contentLength) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.deri.any23.writer.TripleHandler#close()
	 */
	public void close() throws TripleHandlerException {
	}

	/**
	 * @return
	 */
	public Set<Statement> getBuffer() {
		return buffer;
	}
}
