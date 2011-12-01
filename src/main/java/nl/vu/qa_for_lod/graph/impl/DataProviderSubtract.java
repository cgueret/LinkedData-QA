package nl.vu.qa_for_lod.graph.impl;

import java.util.HashSet;
import java.util.Set;

import nl.vu.qa_for_lod.graph.DataProvider;
import nl.vu.qa_for_lod.graph.Direction;

import org.aksw.commons.jena.ModelSetView;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * Subtracts a given set of statement from to output of an
 * underlying DataProvider
 * 
 * @author Claus Stadler <cstadler@informatik.uni-leipzig.de>
 *
 */
public class DataProviderSubtract
	implements DataProvider
{
	private DataProvider decoratee;
	private Set<Statement> statements;
	
	public DataProviderSubtract(DataProvider decoratee, Model model) {		
		this(decoratee, new HashSet<Statement>(new ModelSetView(model)));
	}
	
	public DataProviderSubtract(DataProvider decoratee, Set<Statement> statements) {
		this.decoratee = decoratee;
		this.statements = statements;
	}
	
	@Override
	public void close() {
		decoratee.close();
	}

	@Override
	public Set<Statement> get(Resource resource, Direction direction) {
		// FIXME I am doing a copy here in order to be on the safe side
		// Might be unneccesary
		Set<Statement> result = new HashSet<Statement>(decoratee.get(resource, direction));
		
		result.removeAll(statements);
		
		return result;
	}

}
