/**
 * 
 */
package nl.vu.qa_for_lod.graph.impl;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import org.deri.any23.Any23;
import org.deri.any23.extractor.ExtractionException;
import org.deri.any23.http.HTTPClient;
import org.deri.any23.source.DocumentSource;
import org.deri.any23.source.HTTPDocumentSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class DataAcquisitionTask implements Callable<Boolean> {
	// Logger
	final static Logger logger = LoggerFactory.getLogger(DataAcquisitionTask.class);
	// List of time to wait, in second, before trying a resource again
	final static int[] RETRY_DELAY = { 1, 5, 10 };
	// The runner from Any23
	private final Any23 runner = new Any23();
	private final Resource resource;
	private final Set<Statement> statements = new HashSet<Statement>();
	private final Model model;

	/**
	 * @param resource
	 * @param model
	 */
	public DataAcquisitionTask(Resource resource, Model model) {
		if (runner.getHTTPUserAgent() == null)
			runner.setHTTPUserAgent("LATC QA tool prototype");
		this.resource = resource;
		this.model = model;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.Callable#call()
	 */
	public Boolean call() throws Exception {
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
				MyTripleHandler handler = new MyTripleHandler(resource, model);
				runner.extract(source, handler);
				statements.addAll(handler.getBuffer());

				// We were successful
				return Boolean.TRUE;
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
				}
			} catch (IOException e) {
				// 404 or alike, not worth trying again
			} catch (URISyntaxException e) {
				// Error in URI, unlikely to change
			} catch (ExtractionException e) {
				// Something is wrong with the data, give up
			} catch (Exception e) {
				// What?! Ok, just give up anyway
				e.printStackTrace();
			}
		}

		return Boolean.FALSE;
	}

	/**
	 * @return
	 */
	public Set<Statement> getStatements() {
		return statements;
	}

}
