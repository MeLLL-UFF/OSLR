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

import org.junit.Test;

import edu.cmu.ml.proppr.examples.GroundedExample;
import edu.cmu.ml.proppr.examples.InferenceExample;
import edu.cmu.ml.proppr.prove.DprProver;
import edu.cmu.ml.proppr.prove.Prover;
import edu.cmu.ml.proppr.prove.wam.Feature;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.StateProofGraph;
import edu.cmu.ml.proppr.prove.wam.WamBaseProgram;
import edu.cmu.ml.proppr.prove.wam.WamProgram;
import edu.cmu.ml.proppr.prove.wam.plugins.FactsPlugin;
import edu.cmu.ml.proppr.prove.wam.plugins.GraphlikePlugin;
import edu.cmu.ml.proppr.prove.wam.plugins.LightweightGraphPlugin;
import edu.cmu.ml.proppr.prove.wam.plugins.WamPlugin;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.SimpleSymbolTable;

/** 

 * @author krivard
 *
 */
public class NodeMergingTest {
	public static final File DIR = new File("src/testcases/nodeMerging");
	public static final File DIAMOND_RULES = new File(DIR,"diamond.wam");
	public static final File DIAMOND_FACTS = new File(DIR,"diamond.facts");
	public static final File TRAPEZOID_RULES = new File(DIR,"trapezoid.wam");
	public static final File TRAPEZOID_FACTS = new File(DIR,"trapezoid.facts");
	public static final File RECURSION_RULES = new File(DIR,"recursion.wam");
	public static final File RECURSION_FACTS = new File(DIR,"recursion.facts");
	public static final File LOOP_RULES = new File(DIR,"loop.wam");
	public static final File LOOP_FACTS = new File(DIR,"loop.facts");
	public static final File MULTIRANK_RULES = new File(DIR,"multirankwalk.wam");
	public static final File MULTIRANK_GRAPH = new File(DIR,"multirankwalk.graph");

	
	private void doTest(File rules, File facts, 
			String squery,String[] spos,String[] sneg,
			int nodeSize, int posSize) throws LogicProgramException, IOException {
		APROptions apr = new APROptions();
		Prover p = new DprProver(apr);
		WamProgram program = WamBaseProgram.load(rules);
		WamPlugin plugins[] = null;
		if (facts.getName().endsWith(FactsPlugin.FILE_EXTENSION))
			plugins = new WamPlugin[] {FactsPlugin.load(apr, facts, false)};
		else if (facts.getName().endsWith(GraphlikePlugin.FILE_EXTENSION))
			plugins = new WamPlugin[] {LightweightGraphPlugin.load(apr, facts, -1)};
		Grounder grounder = new Grounder(apr, p, program, plugins);
		
		Query query = Query.parse(squery);
		Query[] pos = new Query[spos.length];
		for (int i=0; i<spos.length; i++) pos[i] = Query.parse(spos[i]);
		Query[] neg = new Query[sneg.length];
		for (int i=0; i<sneg.length; i++) neg[i] = Query.parse(sneg[i]);
		ProofGraph pg = new StateProofGraph(
				new InferenceExample(query, 
				pos,
				neg),
				apr,new SimpleSymbolTable<Feature>(),program, plugins);
		GroundedExample ex = grounder.groundExample(pg);
		System.out.println( grounder.serializeGroundedExample(pg, ex).replaceAll("\t", "\n"));
		if (nodeSize>=0) assertEquals("improper node duplication",nodeSize,ex.getGraph().nodeSize());
		if (posSize>=0) assertEquals("improper # solutions found",posSize, ex.getPosList().size());
	}
	/**
	 * Diamond case: Solution is reachable by two equal-length paths.
	 * 
	 *                                          ,--db:r-- [5] -. 
	 * [1] --p/2-- [2] --v/2-- [3] --q/3-- [4] <                --db:s-- [7] --t/1-- [8] 
	 *                                          `--db:r-- [6] -' 
	 * @throws IOException
	 * @throws LogicProgramException
	 */
	@Test
	public void testDiamond() throws IOException, LogicProgramException {
		doTest(DIAMOND_RULES,DIAMOND_FACTS,
				"p(a,P)",new String[] {"p(a,d)"},new String[0],
				8,-1);
	}
	
	/**
	 * Correct graph:
	 * 
	 *     ,-p/2-- [2] --db:q-- [4] --r/1-.
	 * [1] --------------p/2--------------- [3]
	 * 
	 * @throws IOException
	 * @throws LogicProgramException
	 */
	@Test
	public void testTrapezoid() throws IOException, LogicProgramException {

		doTest(TRAPEZOID_RULES,TRAPEZOID_FACTS,
				"p(a,P)",new String[] {"p(a,d)"},new String[0],
				4,-1);
	}
	
	@Test
	public void testRecursion() throws IOException, LogicProgramException {
		doTest(RECURSION_RULES,RECURSION_FACTS,
				"p(a,P)",new String[] {"p(a,d)"},new String[0],
				7,-1);

	}
	
	@Test
	public void testLoop() throws IOException, LogicProgramException {
		doTest(LOOP_RULES,LOOP_FACTS,
				"p(a,P)",new String[] {"p(a,d)"},new String[0],
				4,-1);

	}
	
	@Test
	public void testMultiRank() throws IOException, LogicProgramException {
		doTest(MULTIRANK_RULES,MULTIRANK_GRAPH,
				"predict(pos,X)",new String[] {"predict(pos,seed1)","predict(pos,f1)","predict(pos,other)"},new String[0],
				-1,3);

	}
}
