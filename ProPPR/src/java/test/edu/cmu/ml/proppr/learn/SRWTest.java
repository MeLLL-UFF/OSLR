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

package edu.cmu.ml.proppr.learn;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.Logger;import org.apache.logging.log4j.LogManager;;
import org.junit.Before;
import org.junit.Test;

import edu.cmu.ml.proppr.RedBlueGraph;
import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.graph.LearningGraphBuilder;
import edu.cmu.ml.proppr.learn.ExampleFactory.PprExampleFactory;
import edu.cmu.ml.proppr.learn.SRW;
import edu.cmu.ml.proppr.learn.tools.Exp;
import edu.cmu.ml.proppr.learn.tools.ReLU;
import edu.cmu.ml.proppr.learn.tools.SquashingFunction;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.StatusLogger;
import edu.cmu.ml.proppr.util.math.ParamVector;
import edu.cmu.ml.proppr.util.math.SimpleParamVector;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
/**
 * These tests are on a graph without reset links in it.
 * @author krivard
 *
 */
public class SRWTest extends RedBlueGraph {
	private static final Logger log = LogManager.getLogger(SRWTest.class);
	protected SRW srw;
	protected ParamVector<String,?> uniformParams;
	protected TIntDoubleMap startVec;
	protected ExampleFactory factory;
	
	@Before
	@Override
	public void setup() {
		factory = new PprExampleFactory();
		initSrw();
		defaultSrwSettings();
		super.setup();
	}
	
	@Override
	public void moreSetup(LearningGraphBuilder lgb) {
		uniformParams = srw.getRegularizer().setupParams(new SimpleParamVector<String>(new ConcurrentHashMap<String,Double>()));
		for (String n : new String[] {"fromb","tob","fromr","tor"}) uniformParams.put(n,srw.getSquashingFunction().defaultValue());
		startVec = new TIntDoubleHashMap();
		startVec.put(nodes.getId("r0"),1.0);
	}
	
	public void defaultSrwSettings() {
		srw.setMu(0);
		srw.getOptions().set("apr","alpha","0.1");
//		srw.setWeightingScheme(new LinearWeightingScheme());
//		srw.setWeightingScheme(new ReLUWeightingScheme());
		srw.setSquashingFunction(new Exp());
	}
	
	public void initSrw() { 
		srw = new SRW();
		this.srw.setRegularizer(new RegularizationSchedule(this.srw, new Regularize()));
		srw.c.apr.maxDepth=10;
	}
	
	public TIntDoubleMap myRWR(TIntDoubleMap startVec, LearningGraph g, int maxT) {
		TIntDoubleMap vec = startVec;
		TIntDoubleMap nextVec = null;
		for (int t=0; t<maxT; t++) {
			nextVec = new TIntDoubleHashMap();
			int k=-1;
			for (int u : vec.keys()) { k++;
				int z = g.node_near_hi[u] - g.node_near_lo[u];// near(u).size();
				for (int eid = g.node_near_lo[u]; eid<g.node_near_hi[u]; eid++) { //TIntIterator it = g.near(u).iterator(); it.hasNext(); ) {
					int v = g.edge_dest[eid];
					double inc = vec.get(u) / z;
					Dictionary.increment(nextVec, v, inc);
					log.debug("Incremented "+u+", "+v+" by "+inc);
				}
			}
			vec = nextVec;
		}
		return nextVec;
	}
	
	public TIntDoubleMap myRWR(TIntDoubleMap startVec, LearningGraph g, int maxT, ParamVector<String,?> params, SquashingFunction scheme) {
		TIntDoubleMap vec = startVec;
		TIntDoubleMap nextVec = null;
		for (int t=0; t<maxT; t++) {
			nextVec = new TIntDoubleHashMap();
			int k=-1;
			for (int u : vec.keys()) { k++;
				// compute total edge weight:
				double z = 0.0;
				for (int eid = g.node_near_lo[u]; eid<g.node_near_hi[u]; eid++) {
					int v = g.edge_dest[eid];
					double suv = 0.0;
					for (int fid = g.edge_labels_lo[eid]; fid<g.edge_labels_hi[eid]; fid++) {
						suv += Dictionary.safeGet(params, (g.featureLibrary.getSymbol(g.label_feature_id[fid])), scheme.defaultValue()) * g.label_feature_weight[fid];
					}
					double ew = scheme.edgeWeight(suv);
					z+=ew;
				}

				for (int eid = g.node_near_lo[u]; eid<g.node_near_hi[u]; eid++) {
					int v = g.edge_dest[eid];
					double suv = 0.0;
					for (int fid = g.edge_labels_lo[eid]; fid<g.edge_labels_hi[eid]; fid++) {
						suv += Dictionary.safeGet(params, (g.featureLibrary.getSymbol(g.label_feature_id[fid])), scheme.defaultValue()) * g.label_feature_weight[fid];
					}
					double ew = scheme.edgeWeight(suv);
					double inc = vec.get(u) * ew / z;
					Dictionary.increment(nextVec, v, inc);
					log.debug("Incremented "+u+", "+v+" by "+inc);
				}
			}
			vec = nextVec;
		}
		return nextVec;
	}
	
//	 Test removed: We no longer compute rwr in SRW
//	
//	/**
//	 * Uniform weights should be the same as the unparameterized basic RWR
//	 */
//	@Test
//	public void testUniformRWR() {
//		log.debug("Test logging");
//		int maxT = 10;
//		
//		TIntDoubleMap baseLineVec = myRWR(startVec,brGraph,maxT);
//		uniformParams.put("id(restart)",srw.getWeightingScheme().defaultWeight());
//		TIntDoubleMap newVec = srw.rwrUsingFeatures(brGraph, startVec, uniformParams);
//		equalScores(baseLineVec,newVec);
//	}
	
//	
//	public ParamVector<String,?> makeParams(Map<String,Double> foo) {
//		return new SimpleParamVector(foo);
//	}
//	
//	public ParamVector<String,?> makeParams() {
//		return new SimpleParamVector();
//	}
	
	public ParamVector<String,?> makeGradient(SRW srw, ParamVector<String,?> paramVec, TIntDoubleMap query, int[] pos, int[] neg) {
		ParamVector<String,?> grad = new SimpleParamVector<String>();
		srw.accumulateGradient(paramVec, factory.makeExample("gradient",brGraph, query, pos,neg), grad, new StatusLogger());
		return grad;
	}
	
	@Test
	public void testGradient() {
//		if (this.getClass().equals(SRWTest.class)) return;
		
		int[] pos = new int[blues.size()]; { int i=0; for (String k : blues) pos[i++] = nodes.getId(k); }
		int[] neg = new int[reds.size()];  { int i=0; for (String k : reds)  neg[i++] = nodes.getId(k); }

		ParamVector<String,?> gradient = makeGradient(srw, uniformParams, startVec, pos, neg);
		System.err.println(Dictionary.buildString(gradient, new StringBuilder(), "\n").toString());

		// to favor blue (positive label) nodes,
		// we want the gradient to go downhill (negative) 
		// toward blue nodes (edges labeled 'tob')
		assertTrue("gradient @ tob "+gradient.get("tob"),gradient.get("tob") < 0);
		assertTrue("gradient @ tor "+gradient.get("tor"),gradient.get("tor") > 0);
		// we don't really care what happens on edges coming *from* blue nodes though:
//		assertTrue("gradient @ fromb "+gradient.get("fromb"),gradient.get("fromb")<1e-10);
//		assertTrue("gradient @ fromr "+gradient.get("fromr"),gradient.get("fromr")>-1e-10);
	}
	
	public double makeLoss(SRW srw, ParamVector<String,?> paramVec, TIntDoubleMap query, int[] pos, int[] neg) {
		srw.clearLoss();
		srw.accumulateGradient(paramVec, factory.makeExample("loss",brGraph, query, pos,neg), new SimpleParamVector<String>(), new StatusLogger());
		return srw.cumulativeLoss().total();
	}
	public double makeLoss(ParamVector<String,?> paramVec, PosNegRWExample example) {
		srw.clearLoss();
		srw.accumulateGradient(paramVec, example, new SimpleParamVector<String>(), new StatusLogger());
		return srw.cumulativeLoss().total();
	}
	
	protected ParamVector<String,?> makeBiasedVec() {
		return makeBiasedVec(new String[] {"tob"}, new String[] {"tor"});
	}
	protected ParamVector<String,?> makeBiasedVec(String[] upFeatures, String[] downFeatures) {
		ParamVector<String,?> biasedWeightVec = srw.getRegularizer().setupParams(new SimpleParamVector<String>(new ConcurrentHashMap<String,Double>()));
		biasedWeightVec.putAll(uniformParams);
		if (biasedWeightVec.get(upFeatures[0]).equals(1.0)) {
			for (String f : upFeatures)
				biasedWeightVec.put(f, 10.0);
			for (String f : downFeatures)
				biasedWeightVec.put(f, 0.1);
		} else {			
			for (String f : upFeatures)
				biasedWeightVec.put(f, 1.0);
			for (String f : downFeatures)
				biasedWeightVec.put(f, -1.0);
		}
		return biasedWeightVec;
	}
	
	@Test
	public void testLoss() {
		if (this.getClass().equals(SRWTest.class)) return;

		int[] pos = new int[blues.size()]; { int i=0; for (String k : blues) pos[i++] = nodes.getId(k); }
		int[] neg = new int[reds.size()];  { int i=0; for (String k : reds)  neg[i++] = nodes.getId(k); }

		double baselineLoss = makeLoss(this.srw, uniformParams, startVec, pos, neg);
		ParamVector<String,?> baselineGrad = makeGradient(srw, uniformParams, startVec, pos, neg);

		ParamVector<String,?> biasedWeightVec = makeBiasedVec();
		double biasedLoss = makeLoss(srw, biasedWeightVec, startVec, pos, neg);

		assertTrue(String.format("baselineLoss %f should be > than biasedLoss %f",baselineLoss,biasedLoss),
				baselineLoss > biasedLoss);

		double eps = .0001;
		ParamVector<String,?> nearlyUniformWeightVec = uniformParams.copy();
//		TObjectDoubleMap<String> baselineGrad = makeGradient(srw, uniformParams, startVec, pos, neg);
		System.err.println("\nbaseline gradient:"+Dictionary.buildString(baselineGrad, new StringBuilder(), "\n").toString());
		for (String f : baselineGrad.keySet()) Dictionary.increment(nearlyUniformWeightVec,f,-eps*baselineGrad.get(f));
		System.err.println("\nimproved params:"+Dictionary.buildString(nearlyUniformWeightVec, new StringBuilder(), "\n").toString());
		srw.clearLoss();
		double improvedBaselineLoss = makeLoss(this.srw, nearlyUniformWeightVec, startVec, pos,neg);
		System.err.println("\nbaselineLoss-improvedBaselineLoss="+(baselineLoss-improvedBaselineLoss));
		assertTrue("baselineLoss "+baselineLoss+" should be > improvedBaselineLoss "+improvedBaselineLoss, baselineLoss > improvedBaselineLoss);
	}
	
	
	/**
	 * check that learning on red/blue graph works
	 */
	@Test
	public void testLearn1() {
		
		TIntDoubleMap query = new TIntDoubleHashMap();
		query.put(nodes.getId("r0"), 1.0);
		int[] pos = new int[blues.size()]; { int i=0; for (String k : blues) pos[i++] = nodes.getId(k); }
		int[] neg = new int[reds.size()];  { int i=0; for (String k : reds)  neg[i++] = nodes.getId(k); }
		PosNegRWExample example = factory.makeExample("learn1",brGraph, query, pos, neg);
		
//		ParamVector weightVec = new SimpleParamVector();
//		weightVec.put("fromb",1.01);
//		weightVec.put("tob",1.0);
//		weightVec.put("fromr",1.03);
//		weightVec.put("tor",1.0);
//		weightVec.put("id(restart)",1.02);
		
		ParamVector<String,?> trainedParams = uniformParams.copy();
		double preLoss = makeLoss(trainedParams, example);
		srw.clearLoss();
		srw.trainOnExample(trainedParams,example, new StatusLogger());
		double postLoss = makeLoss(trainedParams, example);
		assertTrue(String.format("preloss %f >=? postloss %f",preLoss,postLoss), 
				preLoss == 0 || preLoss > postLoss);
	}
	
}