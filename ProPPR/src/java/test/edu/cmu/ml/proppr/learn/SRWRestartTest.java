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

import static org.junit.Assert.assertTrue;







import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.ml.proppr.graph.ArrayLearningGraphBuilder;
import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.graph.LearningGraphBuilder;
import edu.cmu.ml.proppr.graph.RWOutlink;
import edu.cmu.ml.proppr.util.math.ParamVector;
import edu.cmu.ml.proppr.util.math.SimpleParamVector;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

/**
 * These tests are on a graph which includes reset links.
 * @author krivard
 *
 */
public class SRWRestartTest extends SRWTest {
	public void initSrw() {
		srw = new SRW();
		this.srw.setRegularizer(new RegularizationSchedule(this.srw, new RegularizeL2()));
		srw.setAlpha(0.01);
	}
	
	@Override
	protected RWOutlink makeOutlink(LearningGraphBuilder lgb, Map<String,Double> fd, int dest) {
		if (dest != nodes.getId("r0")) return super.makeOutlink(lgb,fd,dest);
		int[] fid = new int[fd.size()+1];
		double[] wt = new double[fd.size()+1];
		int i=0;
		for (Map.Entry<String, Double> e : fd.entrySet()) {
			fid[i] = lgb.getFeatureLibrary().getId(e.getKey());
			wt[i] = e.getValue();
			i++;
		}
		fid[i] = lgb.getFeatureLibrary().getId("id(restart)");
		wt[i] = this.srw.getSquashingFunction().defaultValue();
		return new RWOutlink(fid,wt,dest);
	}
	
	@Override
	public void moreOutlinks(LearningGraphBuilder lgb,LearningGraph graph,int u) {
		ArrayLearningGraphBuilder b = ((ArrayLearningGraphBuilder) lgb);
		// first see if we already have a reset link (covered by makeOutlink())
		int r0 = nodes.getId("r0");
		if (b.outlinks[u] != null) {
			for (RWOutlink o : b.outlinks[u]) {
				if (o.nodeid == r0) {
					return;
				}
			}
		}
		// no reset link exists, so add one
		lgb.addOutlink(graph, u, new RWOutlink(new int[]{lgb.getFeatureLibrary().getId("id(restart)")},
				new double[]{this.srw.getSquashingFunction().defaultValue()},
				r0));
	}
	
	@Override
	public void moreSetup(LearningGraphBuilder lgb) {
		super.moreSetup(lgb);
		// add restart links to r0
//		for (int u : brGraph.getNodes()) {
//			TObjectDoubleMap<String> ff = new TObjectDoubleHashMap<String>();
//			ArrayLearningGraphBuilder b = ((ArrayLearningGraphBuilder) lgb);
//			int r0 = nodes.getId("r0");
//			RWOutlink outlinkR0 = null;
//			if (b.outlinks[u] != null) {
//				for (RWOutlink o : b.outlinks[u]) {
//					if (o.nodeid == r0) {
//						outlinkR0 = o;
//						break;
//					}
//				}
//			}
//			if (outlinkR0 == null) {
//				outlinkR0 = new RWOutlink(new int[1], new double[1], r0);
//				lgb.addOutlink(brGraph, u, outlinkR0);
//			}
//			outlinkR0.feature_id[0] = lgb.getFeatureLibrary().getId("id(restart)");
//			outlinkR0.,this.srw.getSquashingFunction().defaultValue());
//			lgb.addOutlink(brGraph, u, null); // cheat the label count
//		}
		uniformParams.put("id(restart)",this.srw.getSquashingFunction().defaultValue());
	}
//	@Override
//	public TObjectDoubleMap<String> makeGradient(SRW srw, ParamVector paramVec, TIntDoubleMap query, int[] pos, int[] neg) {
//		List<HiLo> trainingPairs = new ArrayList<HiLo>();
//		for (int p : pos) {
//			for (int n : neg) {
//				trainingPairs.add(new HiLo(p,n));
//			}
//		}
//		return srw.gradient(paramVec, new PairwiseRWExample(brGraph, query, trainingPairs));
//	}
	
//	@Override
//	public double makeLoss(SRW srw, ParamVector paramVec, TIntDoubleMap query, int[] pos, int[] neg) {		
//		List<HiLo> trainingPairs = new ArrayList<HiLo>();
//		for (int p : pos) {
//			for (int n : neg) {
//				trainingPairs.add(new HiLo(p,n));
//			}
//		}
//		return srw.empiricalLoss(paramVec, new PairwiseRWExample(brGraph, query, trainingPairs));
//	}
	
//	@Override
//	public void testUniformRWR() {}
	

	/**
	 * Biasing the params toward blue things should give the blue nodes a higher score.
	 */
	@Test
	public void testBiasedRWR() {
		int maxT = 10;
		
//		Map<String,Double> startVec = new TreeMap<String,Double>();
//		startVec.put("r0",1.0);
//		SRW<PairwiseRWExample> mysrw = new SRW<PairwiseRWExample>(maxT);
//		mysrw.setAlpha(0.01);
		TIntDoubleMap baseLineRwr = myRWR(startVec, brGraph, maxT, new SimpleParamVector<String>(), srw.getSquashingFunction());
		ParamVector<String,?> biasedParams = makeBiasedVec();
		
		TIntDoubleMap newRwr = myRWR(startVec, brGraph, maxT, biasedParams, srw.getSquashingFunction());
		
		System.err.println("baseline:");
		for (int node : baseLineRwr.keys()) System.err.println(node+"/"+nodes.getSymbol(node)+":"+baseLineRwr.get(node));
		System.err.println("biased:");
		for (int node : newRwr.keys()) System.err.println(node+"/"+nodes.getSymbol(node)+":"+newRwr.get(node));
		
		lowerScores(bluePart(baseLineRwr),bluePart(newRwr));
		lowerScores(redPart(newRwr),redPart(baseLineRwr));
	}
	

}
