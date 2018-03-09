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

package edu.cmu.ml.proppr.prove.wam.plugins;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.junit.Test;

import edu.cmu.ml.proppr.Grounder;
import edu.cmu.ml.proppr.examples.GroundedExample;
import edu.cmu.ml.proppr.examples.InferenceExample;
import edu.cmu.ml.proppr.prove.DprProver;
import edu.cmu.ml.proppr.prove.Prover;
import edu.cmu.ml.proppr.prove.wam.Feature;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.StateProofGraph;
import edu.cmu.ml.proppr.prove.wam.WamBaseProgram;
import edu.cmu.ml.proppr.prove.wam.WamProgram;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.SimpleSymbolTable;

public class WeightedEdgeTest {
		public static final File DIR = new File("src/testcases/weighted");
		public static final File RULES = new File(DIR,"dbWeights.wam");
		public static final File FACTS = new File(DIR,"tinycorpus.cfacts");
		public static final File GRAPH = new File(DIR,"tinycorpus.graph");

		@Test
		public void testFacts() throws IOException, LogicProgramException {
			APROptions apr = new APROptions();
			testOne(apr, FactsPlugin.load(apr, FACTS, false));
		}
		
		@Test
		public void testGraph() throws IOException, LogicProgramException {
			APROptions apr = new APROptions();
			testOne(apr, LightweightGraphPlugin.load(apr, GRAPH, 1000));
		}
		
		public void testOne(APROptions apr, WamPlugin plug)  throws IOException, LogicProgramException {
			Prover p = new DprProver(apr);
			WamProgram program = WamBaseProgram.load(RULES);
			WamPlugin plugins[] = new WamPlugin[] {plug};
			Grounder grounder = new Grounder(apr, p, program, plugins);
			assertTrue("Missing weighted functor",plugins[0].claim("hasWord#/3"));

			Query query = Query.parse("words(p1,W)");
			ProofGraph pg = new StateProofGraph(new InferenceExample(query, 
					new Query[] {Query.parse("words(p1,good)")}, 
					new Query[] {Query.parse("words(p1,thing)")}),
					apr,new SimpleSymbolTable<Feature>(),program, plugins);
//			Map<String,Double> m = p.solutions(pg);
//			System.out.println(Dictionary.buildString(m, new StringBuilder(), "\n").toString());
			GroundedExample ex = grounder.groundExample(p, pg);
			String serialized = ex.getGraph().serialize(true).replaceAll("\t", "\n");
			//String serialized = grounder.serializeGroundedExample(pg, ex).replaceAll("\t", "\n");
			System.out.println( serialized );
			assertTrue("Label weights must appear in ground graph (0.9)",serialized.indexOf("0.9")>=0);
			assertTrue("Label weights must appear in ground graph (0.1)",serialized.indexOf("0.1")>=0);
//			Map<String,Double> m = p.solvedQueries(pg);
//			System.out.println(Dictionary.buildString(m, new StringBuilder(), "\n"));
			
			
			
			Query query2 = Query.parse("words2(p1,W)");
			ProofGraph pg2 = new StateProofGraph(new InferenceExample(query2, 
					new Query[] {Query.parse("words(p1,good)")}, 
					new Query[] {Query.parse("words(p1,thing)")}),
					apr,new SimpleSymbolTable<Feature>(),program, plugins);
//			Map<String,Double> m = p.solutions(pg);
//			System.out.println(Dictionary.buildString(m, new StringBuilder(), "\n").toString());
			GroundedExample ex2 = grounder.groundExample(p, pg2);
			String serialized2 = ex2.getGraph().serialize(true).replaceAll("\t", "\n");
			//String serialized = grounder.serializeGroundedExample(pg, ex).replaceAll("\t", "\n");
			System.out.println( serialized2 );
			assertTrue("Label weights must appear in ground graph (0.9)",serialized2.indexOf("0.9")>=0);
			assertTrue("Label weights must appear in ground graph (0.1)",serialized2.indexOf("0.1")>=0);
		}


}
