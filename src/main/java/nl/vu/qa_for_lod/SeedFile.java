package nl.vu.qa_for_lod;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFReader;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;

/**
 * 
 */

/**
 * @author Christophe Guéret <christophe.gueret@gmail.com>
 * 
 */
public class SeedFile {
	private final Set<Statement> statements = new HashSet<Statement>();

	/**
	 * @param string
	 * @throws Exception
	 */
	public SeedFile(String seedFileName) throws Exception {
		// Read the seed rdf
		Model model = ModelFactory.createDefaultModel();
		RDFReader reader = model.getReader("N3");
		reader.setProperty("WARN_REDEFINITION_OF_ID", "EM_IGNORE"); // speed
		InputStream in = FileManager.get().open(seedFileName);
		if (in == null) {
			throw new Exception();
		}
		reader.read(model, in, null);

		// Iterate through the content of the seed and create the graph
		StmtIterator iter = model.listStatements();
		int max = 250000;
		while (iter.hasNext() && max-- != 0) {
			Statement stmt = iter.nextStatement();
			if (!(stmt.getObject() instanceof Resource))
				continue;
			statements.add(stmt);
		}
		model.close();

	}

	/**
	 * @return
	 */
	public Set<Resource> getSeedResources() {
		Set<Resource> resources = new HashSet<Resource>();
		for (Statement statement : statements) {
			resources.add(statement.getSubject());
			resources.add(statement.getObject().asResource());
		}
		return resources;
	}

	/**
	 * @return
	 */
	public Set<Statement> getStatements() {
		return statements;
	}

}
