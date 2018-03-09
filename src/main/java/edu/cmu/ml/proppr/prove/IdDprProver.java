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

package edu.cmu.ml.proppr.prove;

import edu.cmu.ml.proppr.prove.wam.CachingIdProofGraph;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.StatusLogger;
import edu.cmu.ml.proppr.util.math.LongDense;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * prover using depth-first approximate personalized pagerank
 *
 * @author wcohen, krivard
 */
public class IdDprProver extends Prover<CachingIdProofGraph> {

    public static final double STAYPROB_DEFAULT = 0.0;
    public static final double STAYPROB_LAZY = 0.5;
    private static final Logger log = LogManager.getLogger(IdDprProver.class);
    private static final boolean TRUELOOP_ON = true;
    protected final double stayProbability;
    protected final double moveProbability;
    public int completedStates = 0;
    protected LongDense.AbstractFloatVector params = null;
    protected IdDprProver parent = null;
    private int maxTreeDepth;

    public IdDprProver() {
        this(false);
    }

    public IdDprProver(boolean lazyWalk) {
        this(lazyWalk, new APROptions());
    }

    public IdDprProver(boolean lazyWalk, APROptions apr) {
        this((lazyWalk ? STAYPROB_LAZY : STAYPROB_DEFAULT), apr);
    }

    protected IdDprProver(double stayP, APROptions apr) {
        super(apr);
        this.stayProbability = stayP;
        this.moveProbability = 1.0 - stayProbability;
    }

    public IdDprProver(APROptions apr) {
        this(false, apr);
    }

    @Override
    public String toString() {
        return String.format("idpr:%.6g:%g", apr.epsilon, apr.alpha);
    }

    @Override
    public Prover<CachingIdProofGraph> copy() {
        IdDprProver copy = new IdDprProver(this.stayProbability, apr);
        copy.setWeighter(weighter);
        copy.params = this.params;
        if (this.parent != null) { copy.parent = this.parent; } else { copy.parent = this; }
        return copy;
    }

    @Override
    public Map<State, Double> prove(CachingIdProofGraph pg, StatusLogger status) {
        LongDense.FloatVector p = new LongDense.FloatVector();
        prove(pg, p, status);
        if (apr.traceDepth != 0) {
            log.info("== proof graph: edges/nodes " + pg.edgeSize() + "/" + pg.nodeSize());
            log.info(pg.treeView(apr.traceDepth, apr.traceRoot, weighter, p));
        }
        return pg.asMap(p);
    }

    @Override
    public Class<CachingIdProofGraph> getProofGraphClass() {
        return CachingIdProofGraph.class;
    }

    @Override
    public void configure(String param) {
        if (param.startsWith("maxTreeDepth=")) {
            this.maxTreeDepth = Integer.parseInt(param.substring(param.indexOf('=') + 1));
        }
    }

    protected void setFrozenParams(LongDense.AbstractFloatVector params) {
        if (this.params == null) { this.params = params; }
    }

    private LongDense.AbstractFloatVector getFrozenParams(CachingIdProofGraph pg) {
        if (params != null) { return params; }
        if (this.weighter.weights.size() == 0) { params = new LongDense.UnitVector(); } else {
            params = pg.paramsAsVector(this.weighter.weights, this.weighter.squashingFunction.defaultValue()); //
            // FIXME: default value should depend on f
            if (parent != null) { synchronized (parent) {parent.setFrozenParams(params);} }
        }
        return params;
    }

    protected void prove(CachingIdProofGraph pg, LongDense.FloatVector p, StatusLogger status) {
        LongDense.FloatVector r = new LongDense.FloatVector();
        LongDense.AbstractFloatVector params = getFrozenParams(pg);
        int state0 = pg.getRootId();
        r.set(state0, 1.0);
        int numPushes = 0;
        int numIterations = 0;
        double iterEpsilon = 1.0;
        this.completedStates = 0;
        for (int pushCounter = 0; ; ) {
            iterEpsilon = Math.max(iterEpsilon / 10, apr.epsilon);
            pushCounter = this.proveState(pg, p, r, state0, 0, iterEpsilon, params, status);
            numIterations++;
            if (log.isInfoEnabled() && status.due(1)) {
                log.info(Thread.currentThread() + " iteration: " + numIterations + " pushes: " + pushCounter + " " +
                                 "r-states: " + r.size() + " p-states: " + p.size());
            }
            if (iterEpsilon == apr.epsilon && pushCounter == 0) { break; }
            if (apr.stopEarly >= 0 && this.completedStates > apr.stopEarly) {
                log.info("Stopping early...");
                break;
            }
            numPushes += pushCounter;
        }
        //if(log.isInfoEnabled()) log.info(Thread.currentThread()+" total iterations "+numIterations+" total pushes
        // "+numPushes);
    }

    protected int proveState(CachingIdProofGraph cg, LongDense.FloatVector p, LongDense.FloatVector r,
                             int uid, int pushCounter, double iterEpsilon, LongDense.AbstractFloatVector params,
                             StatusLogger status) {
        return proveState(cg, p, r, uid, pushCounter, 1, iterEpsilon, params, status);
    }

    protected int proveState(CachingIdProofGraph cg, LongDense.FloatVector p, LongDense.FloatVector r,
                             int uid, int pushCounter, int depth, double iterEpsilon,
                             LongDense.AbstractFloatVector params,
                             StatusLogger status) {
        if (this.maxTreeDepth > 0 && depth > this.maxTreeDepth) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Rejecting eps %f @depth %d > %d ru %.6f deg %d state %s", iterEpsilon,
                                        depth, this.maxTreeDepth, r.get(uid), -1, uid));
            }
            return pushCounter;
        }
        try {
            int deg = cg.getDegreeById(uid, this.weighter);
            if (r.get(uid) / deg > iterEpsilon) {
                pushCounter += 1;
                try {
                    double z = cg.getTotalWeightOfOutlinks(uid, params, this.weighter);
                    // push this state as far as you can
                    while (r.get(uid) / deg > iterEpsilon) {
                        double ru = r.get(uid);
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Pushing eps %f @depth %d ru %.6f deg %d state %s", iterEpsilon,
                                                    depth, ru, deg, uid));
                        } else if (log.isInfoEnabled() && status.due(2)) {
                            log.info(String.format("Pushing eps %f @depth %d ru %.6f deg %d state %s", iterEpsilon,
                                                   depth, ru, deg, uid));
                        }

                        // p[u] += alpha * ru
                        //addToP(p,u,ru);
                        p.inc(uid, apr.alpha * ru);
                        // r[u] *= (1-alpha) * stay?
                        //r.put(u, (1.0-apr.alpha) * stayProbability * ru);
                        r.set(uid, (1.0 - apr.alpha) * stayProbability * ru);
                        // for each v near u
                        for (int i = 0; i < deg; i++) {
                            // r[v] += (1-alpha) * move? * Muv * ru
                            //Dictionary.increment(r, o.child, (1.0-apr.alpha) * moveProbability * (o.wt / z) * ru,"
                            // (elided)");
                            double wuv = cg.getIthWeightById(uid, i, params, this.weighter);
                            if (wuv == 0) { continue; }
                            int vid = cg.getIthNeighborById(uid, i, this.weighter);
                            if (cg.isCompleted(vid)) { this.completedStates++; }
                            r.inc(vid, (1.0 - apr.alpha) * moveProbability * (wuv / z) * ru);
                            if (Double.isNaN(r.get(vid))) {
                                log.debug("NaN in r at v=" + vid + " wuv=" + wuv + " z=" + z + " ru=" + ru);
                            }
                        }
                        if (log.isDebugEnabled()) {
                            // sanity-check r:
                            double sumr = 0;
                            for (int i = 0; i < r.size(); i++) { sumr += r.get(i); }
                            double sump = 0;
                            for (int i = 0; i < p.size(); i++) { sump += p.get(i); }
                            if (Math.abs(sump + sumr - 1.0) > apr.epsilon) {
                                log.debug("Should be 1.0 but isn't: after push sum p + r = " + sump + " + " + sumr + " = " + (sump + sumr));
                            }
                        }
                    }

                    // for each v near u:
                    for (int i = 0; i < deg; i++) {
                        // proveState(v):
                        // current pushcounter is passed down, gets incremented and returned, and
                        // on the next for loop iter is passed down again...
                        int vid = cg.getIthNeighborById(uid, i, this.weighter);
                        if (vid == cg.getRootId()) { continue; }
                        if (0 == cg.getIthWeightById(uid, i, params, this.weighter)) { continue; }
                        //pushCounter = this.proveState(pg,p,r,o.child,pushCounter,depth+1,iterEpsilon);
                        pushCounter = proveState(cg, p, r, vid, pushCounter, depth + 1, iterEpsilon, params, status);

                    }
                } catch (LogicProgramException e) {
                    throw new IllegalStateException(e);
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Rejecting eps %f @depth %d ru %.6f deg %d state %s", iterEpsilon, depth,
                                            r.get(uid), deg, uid));
                }
            }
        } catch (LogicProgramException e) {
            throw new IllegalStateException(e);
        }
        return pushCounter;
    }

    public double getAlpha() {
        return apr.alpha;
    }
}
