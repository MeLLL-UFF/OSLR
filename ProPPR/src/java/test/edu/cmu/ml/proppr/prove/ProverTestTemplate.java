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

package edu.cmu.ml.proppr.prove;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.logging.log4j.Logger;import org.apache.logging.log4j.LogManager;;
import org.junit.Before;
import org.junit.Test;

import edu.cmu.ml.proppr.examples.InferenceExample;
import edu.cmu.ml.proppr.prove.FeatureDictWeighter;
import edu.cmu.ml.proppr.prove.InnerProductWeighter;
import edu.cmu.ml.proppr.prove.Prover;
import edu.cmu.ml.proppr.prove.wam.Feature;
import edu.cmu.ml.proppr.prove.wam.StateProofGraph;
import edu.cmu.ml.proppr.prove.wam.WamProgram;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.WamBaseProgram;
import edu.cmu.ml.proppr.prove.wam.plugins.FactsPlugin;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.SimpleSymbolTable;
import edu.cmu.ml.proppr.util.StatusLogger;
import edu.cmu.ml.proppr.util.SymbolTable;

public abstract class ProverTestTemplate {
	private static final Logger log = LogManager.getLogger(ProverTestTemplate.class);
	private static final String MILK_PROGRAM="src/testcases/classifyIDB.wam";
	private static final String MILK_FACTS="src/testcases/classifyEDB.cfacts";
	private static final String MEM_PROGRAM="src/testcases/memIDB.wam";
	private static final String MEM_FACTS="src/testcases/memEDB.cfacts";
	WamProgram lpMilk;
	FactsPlugin fMilk;
	WamProgram lpMem;
	FactsPlugin	fMem;
	Prover prover;
	double[] proveStateAnswers=new double[3];
	APROptions apr = new APROptions("depth=6");
	@Before
	public void setup() throws IOException {
		BasicConfigurator.configure(); Logger.getRootLogger().setLevel(Level.WARN);
		lpMilk = WamBaseProgram.load(new File(MILK_PROGRAM));
		fMilk = FactsPlugin.load(apr,new File(MILK_FACTS), false);
		lpMem = WamBaseProgram.load(new File(MEM_PROGRAM));
		fMem = FactsPlugin.load(apr,new File(MEM_FACTS), false);
	}
	

	@Test
	public void testProveState() throws LogicProgramException {
		log.info("testProveState");
		FeatureDictWeighter w = new InnerProductWeighter();
		SymbolTable<Feature> featureTab = new SimpleSymbolTable<Feature>();
		int milk = featureTab.getId(new Feature("milk"));
		w.put(featureTab.getSymbol(milk),2);
		prover.setWeighter(w);

		ProofGraph pg = prover.makeProofGraph(new InferenceExample(Query.parse("isa(elsie,X)"),null,null), apr, featureTab, lpMilk, fMilk);
		Map<State,Double> dist = prover.prove(pg, new StatusLogger());//("isa","elsie","X"));

		double query=0.0;
		double platypus=0.0;
		double others=0.0;
		double all=0.0;
		for(Map.Entry<State, Double> s : dist.entrySet()) {
			Query q = pg.fill(s.getKey());
			String arg2 = q.getRhs()[0].getArg(1).getName();
			if ("platypus".equals(arg2)) { platypus = Math.max(platypus, s.getValue()); }
			else if ("X1".equals(arg2)) { query = Math.max(query, s.getValue()); }
			else { others = Math.max(others, s.getValue()); }
			System.out.println(q+"\t"+s.getValue());
			all += s.getValue();
		}
		System.out.println();
		System.out.println("query    weight: "+query);
		System.out.println("platypus weight: "+platypus);
		System.out.println("others   weight: "+others);
//		assertTrue("query should retain most weight",query > Math.max(platypus,others));
		assertTrue("milk-featured paths should score higher than others",platypus>others);
		assertEquals("Total weight of all states should be around 1.0",1.0,all,10*this.apr.epsilon);
		
		assertEquals("Known features",1,prover.weighter.numKnownFeatures);
		assertEquals("Unknown features",5,prover.weighter.numUnknownFeatures);
	}
	
//	@Test
//	public void testSolDelta() {
//
//		PprProver ppr = new PprProver(20);
//		DprProver dpr = new DprProver(0.0000001, 0.03);
//	    
//		log.info("testSolDelta:mem(X,l_de)");
//	    assertEquals(0,maxSolDelta(dpr,ppr,lpMem,"mem","X","l_de"),0.05);
//
//	    log.info("testSolDelta:mem(X,l_abcde)");
//	    assertEquals(0,maxSolDelta(dpr,ppr,lpMem,"mem","X","l_abcde"),0.05);
//		
//	    log.info("testSolDelta:mem2(X,Y,l_abcde)");
//		assertEquals(0,maxSolDelta(dpr,ppr,lpMem,"mem2","X","Y","l_abcde"),0.05);
//		
//        log.info("testSolDelta:mem3(X,Y,Z,l_bcde)");
//	    assertEquals(0,maxSolDelta(dpr,ppr,lpMem,"mem3","X","Y","Z","l_bcde"),0.05);
//	    
//	    log.info("testSolDelta:isa(elsie,X)");
//	    assertEquals(0,maxSolDelta(dpr,ppr,lpMilk,"isa","elsie","X"),0.05);
//	}
//
//	public double maxSolDelta(Prover p1, Prover p2, LogicProgram lp, String goal, String ... args) {
//		Map<String,Double> sol1 = p1.solutionsForQuery(lp, goal, args);
//		Map<String,Double> sol2 = p2.solutionsForQuery(lp, goal, args);
//		double maxDelta = 0;
//		assertTrue("no solutions for 1",sol1.size() > 0 || sol2.size() == 0);
//		assertTrue("no solutions for 2",sol2.size() > 0 || sol1.size() == 0);
//		log.info("--- state ---\tdpr      \tppr      \tdelta");
//		for (Map.Entry<String, Double> w1 : sol1.entrySet()) {
//			Double w2 = Dictionary.safeGet(sol2,w1.getKey(),0.0);
//			//	        assertNotNull(w1.getKey()+" not present in sol2",w2);
//			double delta = Math.abs(w1.getValue() - w2);
//			log.info(String.format("%s\t%f\t%f\t%f",w1.getKey(),w1.getValue(),w2,delta));
//			maxDelta = Math.max(maxDelta, delta);
//		}
//		//	    for (String key : sol2.keySet()) assertTrue(key+" not present in sol1",sol1.containsKey(key));
//		return maxDelta;
//	}





}
