/*
 * Online Structure Learner by Revision (OSLR) is an online relational
 * learning algorithm that can handle continuous, open-ended
 * streams of relational examples as they arrive. We employ
 * techniques from theory revision to take advantage of the already
 * acquired knowledge as a starting point, find where it should be
 * modified to cope with the new examples, and automatically update it.
 * We rely on the Hoeffding's bound statistical theory to decide if the
 * model must in fact be updated accordingly to the new examples.
 * The system is built upon ProPPR statistical relational language to
 * describe the induced models, aiming at contemplating the uncertainty
 * inherent to real data.
 *
 * Copyright (C) 2017-2018 Victor Guimarães
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

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.cmu.ml.proppr.learn;

import edu.cmu.ml.proppr.examples.DprExample;
import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.prove.DprProver;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.SRWOptions;
import edu.cmu.ml.proppr.util.StatusLogger;
import edu.cmu.ml.proppr.util.math.ParamVector;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Set;

public class DprSRW extends SRW {

    public static final double DEFAULT_STAYPROB = DprProver.STAYPROB_DEFAULT;
    private static final Logger log = LogManager.getLogger(DprSRW.class);
    private double stayProb;

    public DprSRW() {
        super();
        init(DEFAULT_STAYPROB);
    }

    private void init(double istayProb) {
        //set walk parameters here
        stayProb = istayProb;
        this.cumloss = new LossData();
    }

    public DprSRW(SRWOptions params) {
        super(params);
        init(DEFAULT_STAYPROB);
    }

    public DprSRW(double istayProb) {
        super();
        this.init(istayProb);
    }

    public DprSRW(SRWOptions params, double istayProb) {
        super(params);
        this.init(istayProb);
    }

    @Override
    protected void load(ParamVector<String, ?> params, PosNegRWExample example) {
    }

    @Override
    protected void inference(ParamVector<String, ?> params, PosNegRWExample example, StatusLogger status) {
        DprExample ex = (DprExample) example;

        // startNode maps node->weight
        TIntDoubleMap query = ex.getQueryVec();
        if (query.size() > 1) { throw new UnsupportedOperationException("Can't do multi-node queries"); }

        // maps storing the probability and remainder weights of the nodes:
        ex.p = new double[ex.getGraph().node_hi];
        ex.r = new double[ex.getGraph().node_hi];

        // initializing the above maps:
        Arrays.fill(ex.p, 0.0);
        Arrays.fill(ex.r, 0.0);

        for (TIntDoubleIterator it = query.iterator(); it.hasNext(); ) {
            it.advance();
            ex.r[it.key()] = it.value();
        }

        // maps storing the gradients of p and r for each node:
        ex.dp = new TIntDoubleMap[ex.getGraph().node_hi];
        ex.dr = new TIntDoubleMap[ex.getGraph().node_hi];

        // initializing the above maps:
//		for(int node : example.getGraph().getNodes()) {
//			dp.put(node, new TObjectDoubleHashMap<String>());
//			dr.put(node, new TObjectDoubleHashMap<String>());
//			for(String feature : (example.getGraph().getFeatureSet()))
//			{
//				dp.get(node).put(feature, 0.0);
//				dr.get(node).put(feature, 0.0);
//			}
//		}

        // APR Algorithm:
        int completeCount = 0;
        while (completeCount < ex.getGraph().node_hi) {
            if (log.isDebugEnabled()) { log.debug("Starting pass"); }
            completeCount = 0;
            for (int u = 0; u < ex.getGraph().node_hi; u++) {
                double ru = ex.r[u];
                int udeg = ex.getGraph().node_near_hi[u] - ex.getGraph().node_near_lo[u];
                if (ru / (double) udeg > c.apr.epsilon) {
                    while (ru / udeg > c.apr.epsilon) {
                        this.push(u, params, ex);
                        if (ex.r[u] > ru) { throw new IllegalStateException("r increasing! :("); }
                        ru = ex.r[u];
                    }
                } else {
                    completeCount++;
                    if (log.isDebugEnabled()) { log.debug("Counting " + u); }
                }
            }
            if (log.isDebugEnabled()) {
                log.debug(completeCount + " of " + ex.getGraph().node_hi + " completed this pass");
            } else if (log.isInfoEnabled() && status.due(3)) {
                log.info(Thread.currentThread() + " inference: " + completeCount + " of " + ex.getGraph().node_hi + "" +
                                 " completed this pass");
            }
        }

//		GradientComponents g = new GradientComponents();
//		g.p = p;
//		g.d = dp;
//		return g;
    }

    /**
     * Simulates a single lazy random walk step on the input vertex
     *
     * @param u        the vertex to be 'pushed'
     * @param p
     * @param r
     * @param g
     * @param paramVec
     * @param dp
     * @param dr
     */
    public void push(int u, ParamVector<String, ?> paramVec, DprExample ex) {
        log.debug("Pushing " + u);

        // update p for the pushed node:
        ex.p[u] += c.apr.alpha * ex.r[u];
        if (ex.dr[u] == null) { ex.dr[u] = new TIntDoubleHashMap(); }
        TIntDoubleMap dru = ex.dr[u];

        TIntDoubleMap unwrappedDotP = new TIntDoubleHashMap();
        for (int eid = ex.getGraph().node_near_lo[u], xvi = 0; eid < ex.getGraph().node_near_hi[u]; eid++, xvi++) {
            int v = ex.getGraph().edge_dest[eid];
            unwrappedDotP.put(v, dotP(ex.getGraph(), eid, paramVec));
        }

        // calculate the sum of the weights (raised to exp) of the edges adjacent to the input node:
        double rowSum = this.totalEdgeProbWeight(ex.getGraph(), u, paramVec);

        // calculate the gradients of the rowSums (needed for the calculation of the gradient of r):
        TIntDoubleMap drowSums = new TIntDoubleHashMap();
        TIntDoubleMap prevdr = new TIntDoubleHashMap();

        Set<String> exampleFeatures = ex.getGraph().getFeatureSet();

        for (String feature : exampleFeatures) {
            int flid = ex.getGraph().featureLibrary.getId(feature);
//			log.debug("dru["+feature+"] = "+dru.get(feature));
            // simultaneously update the dp for the pushed node:
            if (trainable(feature)) {
                if (ex.dp[u] == null) { ex.dp[u] = new TIntDoubleHashMap(); }
                Dictionary.increment(ex.dp[u], flid, c.apr.alpha * dru.get(flid));
            }
            double drowSum = 0;
            for (int eid = ex.getGraph().node_near_lo[u], xvi = 0; eid < ex.getGraph().node_near_hi[u]; eid++, xvi++) {
                int v = ex.getGraph().edge_dest[eid];
                if (hasFeature(ex.getGraph(), eid, flid)) { //g.getFeatures(u, v).containsKey(feature)) {
                    drowSum += c.squashingFunction.computeDerivative(unwrappedDotP.get(v));
                }
            }
            drowSums.put(flid, drowSum);

            // update dr for the pushed vertex, storing dr temporarily for the calculation of dr for the other vertices:
            prevdr.put(flid, dru.get(flid));
            dru.put(flid, dru.get(flid) * (1 - c.apr.alpha) * stayProb);
        }

        // update dr for other vertices:
        for (int eid = ex.getGraph().node_near_lo[u], xvi = 0; eid < ex.getGraph().node_near_hi[u]; eid++, xvi++) {
            int v = ex.getGraph().edge_dest[eid];
            double dotP = c.squashingFunction.edgeWeight(unwrappedDotP.get(v));
            double ddotP = c.squashingFunction.computeDerivative(unwrappedDotP.get(v));
            for (String feature : exampleFeatures) {
                int flid = ex.getGraph().featureLibrary.getId(feature);
                int contained = hasFeature(ex.getGraph(), eid, flid) ? 1 : 0;
                if (ex.dr[v] == null) { ex.dr[v] = new TIntDoubleHashMap(); }
                double vdr = Dictionary.safeGet(ex.dr[v], flid, 0.0);

                // whoa this is pretty gross.
                vdr += (1 - stayProb) * (1 - c.apr.alpha) * ((prevdr.get(flid) * dotP / rowSum) + (ex.r[u] * (
                        (contained * ddotP * rowSum) - (dotP * drowSums.get(flid))) / (rowSum * rowSum)));

                ex.dr[v].put(flid, vdr);
            }
        }

        // update r for all affected vertices:
        double ru = ex.r[u];
        ex.r[u] = ru * stayProb * (1 - c.apr.alpha);
        for (int eid = ex.getGraph().node_near_lo[u], xvi = 0; eid < ex.getGraph().node_near_hi[u]; eid++, xvi++) {
            int v = ex.getGraph().edge_dest[eid];
            // calculate edge weight on v:
            double dotP = c.squashingFunction.edgeWeight(unwrappedDotP.get(v));
            ex.r[v] += (1 - stayProb) * (1 - c.apr.alpha) * (dotP / rowSum) * ru;
        }
    }

    private double dotP(LearningGraph g, int eid, ParamVector<String, ?> paramVec) {
        double dotP = 0;
        for (int fid = g.edge_labels_lo[eid]; fid < g.edge_labels_hi[eid]; fid++) {
            dotP += paramVec.get(g.featureLibrary.getSymbol(g.label_feature_id[fid]))
                    * g.label_feature_weight[fid];
        }
        return dotP;
    }

    public double totalEdgeProbWeight(LearningGraph g, int uid, ParamVector<String, ?> p) {
        double sum = 0.0;
        for (int eid = g.node_near_lo[uid]; eid < g.node_near_hi[uid]; eid++) {
            double ew = c.squashingFunction.edgeWeight(g, eid, p);
            sum += ew;
        }
        if (Double.isInfinite(sum)) { return Double.MAX_VALUE; }
        return sum;
    }

    // linear in # features on the edge :(
    private boolean hasFeature(LearningGraph g, int eid, int flid) {
        for (int fid = g.edge_labels_lo[eid]; fid < g.edge_labels_hi[eid]; fid++) {
            if (g.label_feature_id[fid] == flid) { return true; }
        }
        return false;
    }

    @Override
    protected void regularization(ParamVector<String, ?> params, PosNegRWExample ex, TIntDoubleMap gradient) {

        for (String f : regularizer.localFeatures(params, ex.getGraph())) {
            double value = Dictionary.safeGet(params, f);
            double ret = fixedWeightRules.isFixed(f) ? 0.0 : 2 * c.mu * value;
            this.cumloss.add(LOSS.REGULARIZATION, c.mu * Math.pow(value, 2));
            gradient.adjustOrPutValue(ex.getGraph().featureLibrary.getId(f), ret, ret);
        }
    }

    @Override
    public PosNegRWExample makeExample(String string, LearningGraph g,
                                       TIntDoubleMap queryVec, int[] posList, int[] negList) {
        return new DprExample(string, g, queryVec, posList, negList);
    }
}
