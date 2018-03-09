/*
 * Probabilist Logic Learner is a system to learn probabilistic logic
 * programs from data and use its learned programs to make inference
 * and answer queries.
 *
 * Copyright (C) 2018 Victor Guimarães
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package edu.cmu.ml.proppr;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.logging.log4j.Logger;import org.apache.logging.log4j.LogManager;;
import org.junit.Before;
import org.junit.Test;

import edu.cmu.ml.proppr.examples.GroundedExample;
import edu.cmu.ml.proppr.examples.InferenceExample;
import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.prove.DprProver;
import edu.cmu.ml.proppr.prove.IdDprProver;
import edu.cmu.ml.proppr.prove.IdPprProver;
import edu.cmu.ml.proppr.prove.PprProver;
import edu.cmu.ml.proppr.prove.Prover;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.StateProofGraph;
import edu.cmu.ml.proppr.prove.wam.WamBaseProgram;
import edu.cmu.ml.proppr.prove.wam.WamProgram;
import edu.cmu.ml.proppr.prove.wam.plugins.FactsPlugin;
import edu.cmu.ml.proppr.prove.wam.plugins.WamPlugin;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.StatusLogger;

public class GrounderTest {
	public static final String FACTS = "src/testcases/classifyPredict.cfacts";
	public static final String RULES = "src/testcases/classifyPredict.wam";


	@Before
	public void setup() {
		BasicConfigurator.configure(); Logger.getRootLogger().setLevel(Level.WARN);
	}
	@Test
	public void testGroundExample() throws IOException, LogicProgramException {
		doGroundExampleTest("dpr: ",new DprProver(),
				10, //nodes
				23, //edges
				1.0,//value
				"7",  //pos
				"8",//neg
				APROptions.MINALPH_DEFAULT); 
		doGroundExampleTest("ppr: ",new PprProver(),
				10, //nodes
				23, //edges
				1.0,//value
				"9",  //pos
				"10",//neg
				APROptions.MINALPH_DEFAULT); 
		doGroundExampleTest("dpr: ",new IdDprProver(),
				10, //nodes
				23, //edges
				1.0,//value
				"7",  //pos
				"8",//neg
				APROptions.MINALPH_DEFAULT); 
		doGroundExampleTest("ppr: ",new IdPprProver(),
				10, //nodes
				23, //edges
				1.0,//value
				"9",  //pos
				"10",//neg
				APROptions.MINALPH_DEFAULT); 
	}
	public void doGroundExampleTest(String msg, Prover p, int nodes, int edges, double value, String npos, String nneg,double alpha) throws IOException, LogicProgramException {
		APROptions apr = new APROptions();
		WamProgram program = WamBaseProgram.load(new File(RULES));
		WamPlugin plugins[] = new WamPlugin[] {FactsPlugin.load(apr, new File(FACTS), false)};
		Grounder grounder = new Grounder(apr, p, program, plugins);
		
		Query query = Query.parse("predict(howard,Y)");
		GroundedExample ex = grounder.groundExample(p, new InferenceExample(query, 
				new Query[] {Query.parse("predict(howard,bird)")}, 
				new Query[] {Query.parse("predict(howard,mammal)")}));

		makeAssertions(ex,msg,nodes,edges,value,1,npos,1,nneg);
		// predict(howard,Y)	+predict(howard,bird)	-predict(howard,mammal)
	}
	@Test
	public void noNegativeExamplesTest() throws IOException, LogicProgramException {
		APROptions apr = new APROptions();
		WamProgram program = WamBaseProgram.load(new File(RULES));
		WamPlugin plugins[] = new WamPlugin[] {FactsPlugin.load(apr, new File(FACTS), false)};
		Prover p = new DprProver(apr);
		Grounder grounder = new Grounder(apr, p, program, plugins);
		
		InferenceExample ix = new InferenceExample(Query.parse("predict(howard,Y)"), 
				new Query[] {Query.parse("predict(howard,bird)")}, 
				new Query[] {});
		GroundedExample ex = grounder.groundExample(p, ix);

		makeAssertions(ex,"dpr+",10,23,1.0,1,"7",0,"");
		
		ix = new InferenceExample(Query.parse("predict(howard,Y)"), 
				new Query[] {Query.parse("predict(howard,bird)")}, 
				new Query[] {});
		ProofGraph pg = p.makeProofGraph(ix,apr,program,plugins);
		State pos=null;
		Map<State,Double> sols = p.prove(pg, new StatusLogger());
		System.out.println(pg.serialize(ex));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void unboundLabelsTest() throws IOException, LogicProgramException {
		APROptions apr = new APROptions();
		WamProgram program = WamBaseProgram.load(new File(RULES));
		WamPlugin plugins[] = new WamPlugin[] {FactsPlugin.load(apr, new File(FACTS), false)};
		Prover p = new DprProver(apr);
		Grounder grounder = new Grounder(apr, p, program, plugins);
		
		InferenceExample ix = new InferenceExample(Query.parse("predict(howard,Bird)"), 
				new Query[] {Query.parse("predict(howard,Bird)")}, 
				new Query[] {});
		GroundedExample ex = grounder.groundExample(p, ix);
	}

	
	private void makeAssertions(GroundedExample ex, String msg,
			int nodes, int edges, double value, int npos, String pos, int nneg, String neg) {
		assertEquals(msg+"pos size",npos,ex.getPosList().size());
//		if (npos>0) assertEquals(msg+"pos value",pos,ex.getPosList().get(0));
		assertEquals(msg+"neg size",nneg,ex.getNegList().size());
//		if (nneg>0) assertEquals(msg+"neg value",neg,ex.getNegList().get(0));
		assertEquals(msg+"nodes",nodes,ex.getGraph().nodeSize());
		assertEquals(msg+"edges",edges,ex.getGraph().edgeSize());
		assertEquals(msg+"query vec value",value,ex.getQueryVec().values().iterator().next(),1e-5);
	}
	
}
