package nl.vu.qa_for_lod;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import nl.vu.qa_for_lod.crawler.HostState;
import nl.vu.qa_for_lod.crawler.HostStateManager;
import nl.vu.qa_for_lod.graph.impl.DataAcquisitionTask2;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

import com.google.common.base.Joiner;
import com.sun.jersey.spi.container.servlet.ServletContainer;



@Path("/status")
@Produces("text/plain")
public class StatusServer {

	public HostStateManager getHostStateManager() {
		return HostStateManager.getDefaultInstance();
	}
	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public StreamingOutput getStatus() {
		
		return new StreamingOutput() {

			@Override
			public void write(OutputStream output) throws IOException,
					WebApplicationException {

				PrintWriter out = new PrintWriter(output);
				Map<String, HostState> map = getHostStateManager().getMap();
				
				//out.println("Status: " + map.size() " + hosts q")
				out.println("#" + Joiner.on("\t").join("hostname", "banned", "last request", "#requests", "#no triples", "#timeouts", "status-codes"));  

				
				for(Entry<String, HostState> entry : map.entrySet()) {
					HostState state = entry.getValue();
					
					long now = System.currentTimeMillis();
					float passed = (now - state.getLastRequestTime()) / 1000.0f;
					
					// FIXME The dependency to DataAcquisitionTask.isAcceptedHost is ugly this way
					out.println(Joiner.on("\t").join(state.getName(), !DataAcquisitionTask2.isAcceptedHost(state), passed, state.getRequestCount(), state.getEmptyResultCount(), state.getReadTimeOutCount(), state.getHttpStatusCounts().toString()));  
				}
				
				out.flush();
			}

		};
	}

	
	public StatusServer()
	{
	}
	
	public static Server startServer()
		throws Exception
	{
		ServletHolder sh = new ServletHolder(ServletContainer.class);
	
		/*
		 * For 0.8 and later the "com.sun.ws.rest" namespace has been renamed to
		 * "com.sun.jersey". For 0.7 or early use the commented out code instead
		 */
		// sh.setInitParameter("com.sun.ws.rest.config.property.resourceConfigClass",
		// "com.sun.ws.rest.api.core.PackagesResourceConfig");
		// sh.setInitParameter("com.sun.ws.rest.config.property.packages",
		// "jetty");
		sh.setInitParameter(
				"com.sun.jersey.config.property.resourceConfigClass",
				"com.sun.jersey.api.core.PackagesResourceConfig");
		sh.setInitParameter("com.sun.jersey.config.property.packages",
				"nl.vu.qa_for_lod");
	
		Server server = new Server(7531);
		Context context = new Context(server, "/", Context.SESSIONS);
		context.addServlet(sh, "/*");
		server.start();
		
		return server;
	}
}

