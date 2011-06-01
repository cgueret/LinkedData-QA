/**
 * 
 */
package nl.vu.qa_for_lod.graph.impl;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFReader;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;

import nl.vu.qa_for_lod.graph.DataProvider;

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class FileDataProvider implements DataProvider {
	private final Map<Resource, Set<Statement>> data = new HashMap<Resource, Set<Statement>>();

	/**
	 * @param string
	 * @throws Exception
	 */
	public FileDataProvider(String fileName) throws Exception {
		// Read the file
		Model model = ModelFactory.createDefaultModel();
		RDFReader reader = model.getReader("N3");
		reader.setProperty("WARN_REDEFINITION_OF_ID", "EM_IGNORE"); // speed
		InputStream in = FileManager.get().open(fileName);
		if (in == null) {
			throw new Exception();
		}
		reader.read(model, in, null);

		StmtIterator iter = model.listStatements();
		while (iter.hasNext()) {
			Statement stmt = iter.nextStatement();
			if (stmt.getObject() instanceof Resource) {
				index(stmt.getSubject(), stmt);
				index(stmt.getObject().asResource(), stmt);
			}
		}
		model.close();

	}

	/**
	 * @param asResource
	 * @param stmt
	 */
	private void index(Resource resource, Statement statement) {
		Set<Statement> statements = data.get(resource);
		if (statements == null) {
			statements = new HashSet<Statement>();
			data.put(resource, statements);
		}
		statements.add(statement);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * nl.vu.qa_for_lod.graph.DataProvider#get(com.hp.hpl.jena.rdf.model.Resource
	 * )
	 */
	public Set<Statement> get(Resource resource) {
		return data.get(resource);
	}

	/**
	 * @return
	 */
	public Set<Resource> getResources() {
		return data.keySet();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.vu.qa_for_lod.graph.DataProvider#close()
	 */
	public void close() {
		data.clear();
	}

	/**
	 * @return
	 */
	public int size() {
		Set<Statement> s = new HashSet<Statement>();
		for (Set<Statement> stmt : data.values())
			s.addAll(stmt);
		return s.size();
	}
}