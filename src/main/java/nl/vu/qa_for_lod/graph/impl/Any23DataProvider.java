/**
 * 
 */
package nl.vu.qa_for_lod.graph.impl;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.deri.any23.Any23;
import org.deri.any23.extractor.ExtractionContext;
import org.deri.any23.extractor.ExtractionException;
import org.deri.any23.http.HTTPClient;
import org.deri.any23.source.DocumentSource;
import org.deri.any23.source.HTTPDocumentSource;
import org.deri.any23.writer.TripleHandler;
import org.deri.any23.writer.TripleHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.tdb.TDBFactory;

import nl.vu.qa_for_lod.graph.DataProvider;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class Any23DataProvider implements DataProvider {
	// List of time to wait, in second, before trying a resource again
	private final int[] RETRY_DELAY = { 1, 5, 10 };

	protected class MyHandler implements TripleHandler {
		private final Set<Statement> buffer = new HashSet<Statement>();
		private final Resource resource;

		/**
		 * @param resource
		 * @param model
		 */
		public MyHandler(Resource resource) {
			this.resource = resource;
		}

		public void close() throws TripleHandlerException {
		}

		public void closeContext(ExtractionContext context) throws TripleHandlerException {
		}

		public void endDocument(org.openrdf.model.URI documentURI) throws TripleHandlerException {
		}

		public void openContext(ExtractionContext context) throws TripleHandlerException {
		}

		public void receiveNamespace(String prefix, String uri, ExtractionContext context)
				throws TripleHandlerException {
		}

		public void receiveTriple(org.openrdf.model.Resource s, org.openrdf.model.URI p, org.openrdf.model.Value o,
				org.openrdf.model.URI g, ExtractionContext context) throws TripleHandlerException {
			if (o instanceof org.openrdf.model.Resource && !(o instanceof org.openrdf.model.BNode)
					&& s.toString().equals(resource.getURI())) {
				org.openrdf.model.Resource r = (org.openrdf.model.Resource) o;
				com.hp.hpl.jena.rdf.model.Resource jenaS = model.createResource(s.stringValue());
				com.hp.hpl.jena.rdf.model.Property jenaP = model.createProperty(p.stringValue());
				com.hp.hpl.jena.rdf.model.RDFNode jenaO = model.createResource(r.stringValue());
				buffer.add(model.createStatement(jenaS, jenaP, jenaO));
			}
		}

		public void setContentLength(long contentLength) {
		}

		public void startDocument(org.openrdf.model.URI documentURI) throws TripleHandlerException {
		}

		public Set<Statement> getBuffer() {
			return buffer;
		}

	}

	private final static Logger logger = LoggerFactory.getLogger(Any23DataProvider.class);
	private final static Resource THIS = ResourceFactory.createResource("http://example.org/this");
	private final static Property HAS_BLACK_LISTED = ResourceFactory.createProperty("http://example.org/blacklisted");
	private final Any23 runner = new Any23();
	private final ReentrantLock lock = new ReentrantLock(false);
	private final Model model;

	/**
	 * @param cacheDir
	 * 
	 */
	public Any23DataProvider(String cacheDir) {
		model = TDBFactory.createModel(cacheDir);
		runner.setHTTPUserAgent("LATC QA tool prototype");
		//model.removeAll(THIS, HAS_BLACK_LISTED, (RDFNode)null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.graph.DataProvider#close()
	 */
	public void close() {
		lock.lock();
		model.close();
		lock.unlock();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.qa_for_lod.graph.DataProvider#get(com.hp.hpl.jena.rdf.model.Resource
	 * )
	 */
	public Set<Statement> get(Resource resource) {
		// Try to get the content from the cache
		lock.lock();
		Set<Statement> stmts = model.listStatements(resource, (Property) null, (RDFNode) null).toSet();
		boolean blackListed = model.contains(THIS, HAS_BLACK_LISTED, resource);
		lock.unlock();

		// If we don't have anything and the resource is not blacklisted,
		// download it
		if (stmts.size() == 0 && !blackListed) {
			// Try to download the resource
			boolean retry = true;
			int retryCount = 0;
			while (retry) {
				// Be optimistic
				retry = false;

				try {
					// Get the data
					HTTPClient httpClient = runner.getHTTPClient();
					DocumentSource source = new HTTPDocumentSource(httpClient, resource.getURI());
					MyHandler handler = new MyHandler(resource);
					runner.extract(source, handler);
					stmts = handler.getBuffer();

					// Save the data
					lock.lock();
					for (Statement stmt : stmts)
						model.add(stmt);
					model.commit();
					lock.unlock();
				} catch (SocketTimeoutException e) {
					if (retryCount < RETRY_DELAY.length) {
						// Sleep and retry
						logger.warn("Time out for " + resource.getURI() + ", retry in " + RETRY_DELAY[retryCount]
								+ " seconds");
						try {
							Thread.sleep(RETRY_DELAY[retryCount] * 1000);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
						retry = true;
						retryCount++;
					} else {
						// Black list
						logger.warn("out");
						blackList(resource);
					}
				} catch (IOException e) {
					// 404 or alike, not worth trying again
					logger.warn("io");
					blackList(resource);
				} catch (URISyntaxException e) {
					// Error in URI, unlikely to change
					logger.warn("u");
					blackList(resource);
				} catch (ExtractionException e) {
					// Something is wrong with the data, give up
					logger.warn("e");
					blackList(resource);
				} catch (Exception e) {
					// What?! Ok, just give up anyway
					logger.warn("?");
					e.printStackTrace();
					blackList(resource);
				}
			}
		}

		return stmts;
	}

	/**
	 * @param resource
	 */
	private void blackList(Resource resource) {
		lock.lock();
		model.add(THIS, HAS_BLACK_LISTED, resource);
		logger.warn("Added " + resource.getURI() + " to the black list");
		lock.unlock();
	}
}
