package nl.vu.qa_for_lod.graph.impl;

import org.deri.any23.extractor.ExtractionContext;
import org.deri.any23.writer.TripleHandler;
import org.deri.any23.writer.TripleHandlerException;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * A TripleHandler which writes triples into a Jena model.
 * 
 * Note: Only triples with URI-only terms are written right now (I think)
 * 
 * 
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 *
 */
public class TripleHandlerModel
	implements TripleHandler
{
    private Model model;

    public TripleHandlerModel(Model model) {
        this.model = model;
    }


	public void startDocument(URI documentURI) throws TripleHandlerException {
	}

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
	public void receiveTriple(org.openrdf.model.Resource s, URI p, Value o, URI g, ExtractionContext context)
			throws TripleHandlerException {

		if (o instanceof org.openrdf.model.Resource && !(o instanceof org.openrdf.model.BNode)
				&& s instanceof org.openrdf.model.Resource && !(s instanceof org.openrdf.model.BNode)) {
			org.openrdf.model.Resource r = (org.openrdf.model.Resource) o;

            Resource jenaS = model.createResource(s.stringValue());
			Property jenaP = model.createProperty(p.stringValue());
			RDFNode jenaO = model.createResource(r.stringValue());

            model.add(jenaS, jenaP, jenaO);
            /*
			// Add outgoing triple
			if (s.toString().equals(resource.getURI())
					&& (direction.equals(Direction.OUT) || direction.equals(Direction.BOTH)))
				buffer.add(model.createStatement(jenaS, jenaP, jenaO));

			// Add incoming triple
			if (o.toString().equals(resource.getURI())
					&& (direction.equals(Direction.IN) || direction.equals(Direction.BOTH)))
				buffer.add(model.createStatement(jenaS, jenaP, jenaO));
		    */
		}
	}

	public void receiveNamespace(String prefix, String uri, ExtractionContext context) throws TripleHandlerException {
	}

	public void closeContext(ExtractionContext context) throws TripleHandlerException {
	}

	public void endDocument(URI documentURI) throws TripleHandlerException {
	}

	public void setContentLength(long contentLength) {
	}
	
	public void close() throws TripleHandlerException {
	}
	
	public Model getModel() {
		return model;
	}
}