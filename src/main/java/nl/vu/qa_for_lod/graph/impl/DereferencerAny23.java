package nl.vu.qa_for_lod.graph.impl;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

import nl.vu.qa_for_lod.crawler.HostConfig;
import nl.vu.qa_for_lod.crawler.HostConfigManager;
import nl.vu.qa_for_lod.crawler.HostState;
import nl.vu.qa_for_lod.crawler.HostStateManager;

import org.aksw.commons.sparql.api.dereference.Dereferencer;
import org.aksw.commons.util.validation.UrlValidatorCached;
import org.deri.any23.Any23;
import org.deri.any23.extractor.ExtractionException;
import org.deri.any23.http.HTTPClient;
import org.deri.any23.source.DocumentSource;
import org.deri.any23.source.HTTPDocumentSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;


public class DereferencerAny23
	implements Dereferencer
{
	private static final Logger logger = LoggerFactory.getLogger(DereferencerAny23.class);
	private final static int[] RETRY_DELAY = { 1, 5, 10 };


	private UrlValidatorCached urlValidator = new UrlValidatorCached();
	
	private HostStateManager hostStateManager;
	private HostConfigManager hostConfigManager;
	private String userAgent;
	//private Any23 runner;
	
	public static DereferencerAny23 create(String userAgent) {
		//Any23 runner = new Any23();
		//runner.setHTTPUserAgent("LATC QA tool <c.d.m.gueret@vu.nl>");

		return new DereferencerAny23(userAgent, HostStateManager.getDefaultInstance(), HostConfigManager.getDefaultInstance());
	}

	public DereferencerAny23(String userAgent, HostStateManager hostStateManager, HostConfigManager hostConfigManager) {
		//this.runner = runner;
		this.userAgent = userAgent;
		this.hostStateManager = hostStateManager;
		this.hostConfigManager = hostConfigManager;
	}
 
	
	@Override
	public Model dereference(String url)
	{ 

		Model result = ModelFactory.createDefaultModel();
		
		if(!urlValidator.isValid(url)) {
			return result;
		}
		
		Resource resource = ResourceFactory.createResource(url);
		
		URL resourceUrl;
		try {
			resourceUrl = new URL(url);
		} catch (MalformedURLException e2) {
			logger.warn("Problem with resource " + url, e2);
			return result;
		}

		String hostName = resourceUrl.getHost();

		HostState hostState = hostStateManager.getOrCreate(hostName);
		if(!DataAcquisitionTask2.isAcceptedHost(hostState)) {
			return result;
		}
		
		HostConfig hostConfig = hostConfigManager.getConfig(hostName);

		
		
		// Try to download the resource
		boolean retry = true;
		int retryCount = 0;
		while (retry) {
			// Be optimistic
			retry = false;

			// We blacklist the hostname if it causes troubles
			try {
				
				// Delay the request
				long sleepTime = DataAcquisitionTask2.getDelay(hostConfig.getRequestRate(), hostState.getLastRequestTime());
				if(sleepTime > 0) {
					System.out.println("Delaying request for '" + hostName + "' by " + sleepTime + "ms");
					System.out.println("Host state is :" + hostState);
					
					DataAcquisitionTask2.doDelay(sleepTime);
				}
				
				// We are about to perform our request -
				hostState.incrementRequestCount();
				hostState.setLastRequestTime(System.currentTimeMillis());
				
				// Get the data

				Any23 runner = new Any23();
				runner.setHTTPUserAgent(userAgent);

				HTTPClient httpClient = runner.getHTTPClient();
				
				DocumentSource source = new HTTPDocumentSource(httpClient, resource.getURI());
				TripleHandlerModel handler = new TripleHandlerModel(result);

				runner.extract(source, handler);
				runner.getHTTPClient().close();

				if(url.trim().equals("http://www.w3.org/2002/07/owl#Thing")) {
					if(result.isEmpty()) {
						System.out.println("SHOULD NOT HAPPEN");
						System.exit(0);
					}
				}

				
				if(result.isEmpty()) {
					hostState.incrementEmptyResultCount();
				}				
				
				hostState.incrementHttpStatusCount(200);

				// Reduce the statements
				// FIXME Is there a way to abort fetching statements without having to
				// download the the whole document? -> Nope
				/*
				 * int statementIndex = 0; for (Iterator<Statement> it =
				 * statements.iterator(); it.hasNext();) { it.next(); if
				 * (statementIndex > maxResultSetSize) { it.remove(); }
				 * 
				 * statementIndex++; } int removalCount =
				 * Math.max(statementIndex - maxResultSetSize, 0); if
				 * (removalCount > 0) { logger.warn("Skipped " + removalCount +
				 * " triples from document."); }
				 */
				
				// We were successful
				return result;
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

				hostState.incrementReadTimeOutCount();
				logger.warn("SocketTimeoutException on Host '" + hostName + "' state is: " + hostState);
			} catch (IOException e) {
				// 404 or alike, not worth trying again
				hostState.incrementHttpStatusCount(404);
				
				logger.warn("IOException on Host '" + hostName + "' state is: " + hostState);
			} catch (ExtractionException e) {
				// Something is wrong with the data, give up
				hostState.incrementEmptyResultCount();
			
			} catch (Exception e) {
				hostState.incrementHttpStatusCount(404);
				
				logger.warn("Exception on Host '" + hostName + "' state is: " + hostState);
				// What?! Ok, just give up anyway
				// e.printStackTrace();
			}
		}

		return result;
	}

	@Override
	public void close() {
		
	}

}

