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

public class WeightedRulesTest {
	public static final File DIR = new File("src/testcases/weighted");
	public static final File RULES = new File(DIR,"weightedRules.wam");
	public static final File FACTS = new File(DIR,"weightedRules.cfacts");

	@Test
	public void test() throws IOException, LogicProgramException {
		APROptions apr = new APROptions();
		Prover p = new DprProver(apr);
		WamProgram program = WamBaseProgram.load(RULES);
		WamPlugin plugins[] = new WamPlugin[] {FactsPlugin.load(apr, FACTS, false)};
		Grounder grounder = new Grounder(apr, p, program, plugins);
		assertTrue(plugins[0].claim("ruleWeight#/2"));

		Query query = Query.parse("shoppingList(X)");
		ProofGraph pg = new StateProofGraph(new InferenceExample(query, 
				new Query[] {Query.parse("shoppingList(kidney_beans)")}, 
				new Query[] {Query.parse("shoppingList(cinnamon)")}),
				apr,new SimpleSymbolTable<Feature>(),program, plugins);
//		Map<String,Double> m = p.solutions(pg);
//		System.out.println(Dictionary.buildString(m, new StringBuilder(), "\n").toString());
		GroundedExample ex = grounder.groundExample(p, pg);
		ex.getGraph().serialize();
		String serialized = grounder.serializeGroundedExample(pg, ex).replaceAll("\t", "\n");
		System.out.println( serialized );
		assertTrue("Rule weights must appear in ground graph (2.0)",serialized.indexOf("2.0")>=0);
		assertTrue("Rule weights must appear in ground graph (3.0)",serialized.indexOf("3.0")>=0);
	}

}
