package nl.vu.qa_for_lod.graph.impl;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.expr.E_IsBlank;
import com.hp.hpl.jena.sparql.expr.E_IsURI;
import com.hp.hpl.jena.sparql.expr.E_LogicalNot;
import com.hp.hpl.jena.sparql.expr.ExprVar;
import com.hp.hpl.jena.sparql.syntax.ElementFilter;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.Template;

public class CannedQueryUtils {
	public static Query describe(Node node) {
		Query query = QueryFactory.create();
		query.setQueryDescribeType();
		query.getResultURIs().add(node);
		
		return query;		
	}
	
	public static Query incoming(Node object) {
		return incoming("s", "p", object);
	}
	
	public static Query incoming(String varNameS, String varNameP, Node object) {
		Node s = Node.createVariable(varNameS);
		Node p = Node.createVariable(varNameP);
		
		return inOutTemplate(s, p, object);
	}

	public static Query outgoing(Node subject) {
		return outgoing(subject, "p", "o");
	}
	
	public static Query outgoing(Node subject, String varNameP, String varNameO)
	{
		Node p = Node.createVariable(varNameP);
		Node o = Node.createVariable(varNameO);
		
		return inOutTemplate(subject, p, o);
	}
	
	public static Query inOutTemplate(Node s, Node p, Node o)
	{
		Query query = QueryFactory.create();
		query.setQueryConstructType();
		query.setDistinct(true);
		Triple triple = new Triple(s, p, o);
		ElementGroup group = new ElementGroup();
		group.addTriplePattern(triple);
		
		// Avoid non-uris as objects
		if(o.isVariable()) {
			group.addElementFilter(new ElementFilter(new E_IsURI(new ExprVar(o))));
			group.addElementFilter(new ElementFilter(new E_LogicalNot(new E_IsBlank(new ExprVar(o)))));
		}
		
		BasicPattern bgp = new BasicPattern();
		bgp.add(triple);
		query.setConstructTemplate(new Template(bgp));
		query.setQueryPattern(group);
		
		return query;
	}
}
