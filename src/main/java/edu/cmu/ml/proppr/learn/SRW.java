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

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.examples.PprExample;
import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.learn.tools.ClippedExp;
import edu.cmu.ml.proppr.learn.tools.FixedWeightRules;
import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.learn.tools.SquashingFunction;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.SRWOptions;
import edu.cmu.ml.proppr.util.StatusLogger;
import edu.cmu.ml.proppr.util.math.ParamVector;
import edu.cmu.ml.proppr.util.math.SimpleParamVector;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Random walk learning
 * <p>
 * Flow of information:
 * <p>
 * Train on example =
 * load (initialize example parameters and compute M/dM)
 * inference (compute p/dp)
 * sgd (compute empirical loss gradient and apply to parameters)
 * <p>
 * Accumulate gradient =
 * load  (initialize example parameters and compute M/dM)
 * inference (compute p/dp)
 * gradient (compute empirical loss gradient)
 *
 * @author krivard
 */
public class SRW {

    public static final String FIXED_WEIGHT_FUNCTOR = "fixedWeight";
    private static final Logger log = LogManager.getLogger(SRW.class);
    private static final int MAX_ZERO_LOGS = 10;
    private static final Random random = new Random();
    protected FixedWeightRules fixedWeightRules;
    //	protected Set<String> untrainedFeatures;
    protected int epoch;
    protected SRWOptions c;
    protected LossData cumloss;
    protected ZeroGradientData zeroGradientData;
    protected int zeroLogsThisEpoch = 0;
    protected RegularizationSchedule regularizer;
    protected LossFunction lossf = new PosNegLoss();

    public SRW() {
        this(new SRWOptions());
    }

    public SRW(SRWOptions params) {
        this.c = params;
        this.epoch = 1;
//		this.untrainedFeatures = new TreeSet<String>();
        this.fixedWeightRules = new FixedWeightRules();
        this.cumloss = new LossData();
        this.zeroGradientData = new ZeroGradientData();
    }

    public static void seed(long seed) {
        random.setSeed(seed);
    }

    public static SquashingFunction DEFAULT_SQUASHING_FUNCTION() {
        return new ClippedExp();
    }

    public static HashMap<String, List<String>> constructAffinity(File affgraph) {
        if (affgraph == null) { throw new IllegalArgumentException("Missing affgraph file!"); }
        //Construct the affinity matrix from the input
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(affgraph));
            HashMap<String, List<String>> affinity = new HashMap<String, List<String>>();
            String line = null;
            while ((line = reader.readLine()) != null) {
                String[] items = line.split("\\t");
                if (!affinity.containsKey(items[0])) {
                    List<String> pairs = new ArrayList<String>();
                    pairs.add(items[1]);
                    affinity.put(items[0], pairs);
                } else {
                    List<String> pairs = affinity.get(items[0]);
                    pairs.add(items[1]);
                    affinity.put(items[0], pairs);
                }
            }
            reader.close();
            return affinity;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public static HashMap<String, Integer> constructDegree(Map<String, List<String>> affinity) {
        HashMap<String, Integer> diagonalDegree = new HashMap<String, Integer>();
        for (String key : affinity.keySet()) {
            diagonalDegree.put(key, affinity.get(key).size());
        }
        if (log.isDebugEnabled()) { log.debug("d size:" + diagonalDegree.size()); }
        return diagonalDegree;
    }

    /**
     * Modify the parameter vector by taking a gradient step along the dir suggested by this example.
     *
     * @param params
     * @param example
     */
    public void trainOnExample(ParamVector<String, ?> params, PosNegRWExample example, StatusLogger status) {
        if (log.isDebugEnabled()) {
            log.debug("Training on " + example);
        } else if (log.isInfoEnabled() && status.due(2)) {
            log.info(Thread.currentThread() + " Training on " + example);
        }

        initializeFeatures(params, example.getGraph());
        regularizer.prepareForExample(params, example.getGraph(), params);
        load(params, example);
        inference(params, example, status);
        sgd(params, example);
    }

    /**
     * adds new features to params vector @ 1% random perturbation
     */
    public void initializeFeatures(ParamVector<String, ?> params, LearningGraph graph) {
        for (String f : graph.getFeatureSet()) {
            if (!params.containsKey(f)) {
                if (trainable(f)) {
                    params.put(f, c.squashingFunction.defaultValue() + 0.01 * random.nextDouble());
                } else { fixedWeightRules.initializeFixed(params, f); }
            }
        }
    }

    /**
     * fills M, dM in ex
     **/
    protected void load(ParamVector<String, ?> params, PosNegRWExample example) {
        PprExample ex = (PprExample) example;
        int dM_cursor = 0;
        for (int uid = 0; uid < ex.getGraph().node_hi; uid++) {
            // (a); (b): initialization
            double tu = 0;
            TIntDoubleMap dtu = new TIntDoubleHashMap();
            int udeg = ex.getGraph().node_near_hi[uid] - ex.getGraph().node_near_lo[uid];
            double[] suv = new double[udeg];
            double[][] dfu = new double[udeg][];
            // begin (c): for each neighbor v of u,
            for (int eid = ex.getGraph().node_near_lo[uid], xvi = 0; eid < ex.getGraph().node_near_hi[uid]; eid++,
                    xvi++) {
                int vid = ex.getGraph().edge_dest[eid];
                // i. s_{uv} = w * phi_{uv}, a scalar:
                suv[xvi] = 0;
                for (int lid = ex.getGraph().edge_labels_lo[eid]; lid < ex.getGraph().edge_labels_hi[eid]; lid++) {
                    suv[xvi] += params.get(ex.getGraph().featureLibrary.getSymbol(ex.getGraph()
                                                                                          .label_feature_id[lid])) *
                            ex.getGraph().label_feature_weight[lid];
                }
                // ii. t_u += f(s_{uv}), a scalar:
                tu += c.squashingFunction.edgeWeight(suv[xvi]);
                // iii. df_{uv} = f'(s_{uv})* phi_{uv}, a vector, as sparse as phi_{uv}
                // by looping over features i in phi_{uv}
                double[] dfuv = new double[ex.getGraph().edge_labels_hi[eid] - ex.getGraph().edge_labels_lo[eid]];
                double cee = c.squashingFunction.computeDerivative(suv[xvi]);
                for (int lid = ex.getGraph().edge_labels_lo[eid], dfuvi = 0; lid < ex.getGraph().edge_labels_hi[eid];
                     lid++, dfuvi++) {
                    // iii. again
                    dfuv[dfuvi] = cee * ex.getGraph().label_feature_weight[lid];
                    // iv. dt_u += df_{uv}, a vector, as sparse as sum_{v'} phi_{uv'}
                    // by looping over features i in df_{uv}
                    // (identical to features i in phi_{uv}, so we use the same loop)
                    dtu.adjustOrPutValue(ex.getGraph().label_feature_id[lid], dfuv[dfuvi], dfuv[dfuvi]);
                }
                dfu[xvi] = dfuv;
            }
            // end (c)

            // begin (d): for each neighbor v of u,
            double scale = (1 / (tu * tu));
            for (int eid = ex.getGraph().node_near_lo[uid], xvi = 0; eid < ex.getGraph().node_near_hi[uid]; eid++,
                    xvi++) {
                int vid = ex.getGraph().edge_dest[eid];
                ex.dM_lo[uid][xvi] = dM_cursor;//dM_features.size();
                // create the vector dM_{uv} = (1/t^2_u) * (t_u * df_{uv} - f(s_{uv}) * dt_u)
                // by looping over features i in dt_u

                // getting the df offset for features in dt_u is awkward, so we'll first iterate over features in df_uv,
                // then fill in the rest
                int[] seenFeatures = new int[ex.getGraph().edge_labels_hi[eid] - ex.getGraph().edge_labels_lo[eid]];
                for (int lid = ex.getGraph().edge_labels_lo[eid], dfuvi = 0; lid < ex.getGraph().edge_labels_hi[eid];
                     lid++, dfuvi++) {
                    int fid = ex.getGraph().label_feature_id[lid];
                    ex.dM_feature_id[dM_cursor] = fid; //dM_features.add(fid);
                    double dMuvi = (tu * dfu[xvi][dfuvi] - c.squashingFunction.edgeWeight(suv[xvi]) * dtu.get(fid));
                    if (tu == 0) {
                        if (dMuvi != 0) { throw new IllegalStateException("tu=0 at u=" + uid + "; example " + ex); }
                    } else { dMuvi *= scale; }
                    ex.dM_value[dM_cursor] = dMuvi; //dM_values.add(dMuvi);
                    dM_cursor++;
                    seenFeatures[dfuvi] = fid; //save this feature so we can skip it later
                }
                Arrays.sort(seenFeatures);
                // we've hit all the features in df_uv, now we do the remaining features in dt_u:
                for (TIntDoubleIterator it = dtu.iterator(); it.hasNext(); ) {
                    it.advance();
                    // skip features we already added in the df_uv loop
                    if (Arrays.binarySearch(seenFeatures, it.key()) >= 0) { continue; }
                    ex.dM_feature_id[dM_cursor] = it.key();//dM_features.add(it.key());
                    // zero the first term, since df_uv doesn't cover this feature
                    double dMuvi = scale * (-c.squashingFunction.edgeWeight(suv[xvi]) * it.value());
                    ex.dM_value[dM_cursor] = dMuvi; //dM_values.add(dMuvi);
                    dM_cursor++;
                }
                ex.dM_hi[uid][xvi] = dM_cursor;//dM_features.size();
                // also create the scalar M_{uv} = f(s_{uv}) / t_u
                ex.M[uid][xvi] = c.squashingFunction.edgeWeight(suv[xvi]);
                if (tu == 0) {
                    if (ex.M[uid][xvi] != 0) {
                        throw new IllegalStateException("tu=0 at u=" + uid + "; example " + ex);
                    }
                } else { ex.M[uid][xvi] /= tu; }
            }
        }
    }

    /**
     * fills p, dp
     *
     * @param params
     */
    protected void inference(ParamVector<String, ?> params, PosNegRWExample example, StatusLogger status) {
        PosNegRWExample ex = example;
        ex.p = new double[ex.getGraph().node_hi];
        ex.dp = new TIntDoubleMap[ex.getGraph().node_hi];
        Arrays.fill(ex.p, 0.0);
        // copy query into p
        for (TIntDoubleIterator it = ex.getQueryVec().iterator(); it.hasNext(); ) {
            it.advance();
            ex.p[it.key()] = it.value();
        }
        for (int i = 0; i < c.apr.maxDepth; i++) {
            if (log.isInfoEnabled() && status.due(3)) { log.info("APR: iter " + (i + 1) + " of " + (c.apr.maxDepth)); }
            inferenceUpdate(ex, status);
        }

    }

    /**
     * edits params
     */
    protected void sgd(ParamVector<String, ?> params, PosNegRWExample ex) {
        TIntDoubleMap gradient = gradient(params, ex);
        // apply gradient to param vector
        for (TIntDoubleIterator grad = gradient.iterator(); grad.hasNext(); ) {
            grad.advance();
            if (grad.value() == 0) { continue; }
            String feature = ex.getGraph().featureLibrary.getSymbol(grad.key());
            if (trainable(feature)) {
                params.adjustValue(feature, -learningRate(feature) * grad.value());
                if (params.get(feature).isInfinite()) {
                    log.warn("Infinity at " + feature + "; gradient " + grad.value());
                }
            }
        }
    }

    public boolean trainable(String feature) {
//		return !(untrainedFeatures.contains(feature) || feature.startsWith(FIXED_WEIGHT_FUNCTOR));
        return !fixedWeightRules.isFixed(feature);
    }

    protected void inferenceUpdate(PosNegRWExample example, StatusLogger status) {
        PprExample ex = (PprExample) example;
        double[] pNext = new double[ex.getGraph().node_hi];
        TIntDoubleMap[] dNext = new TIntDoubleMap[ex.getGraph().node_hi];
        // p: 2. for each node u
        for (int uid = 0; uid < ex.getGraph().node_hi; uid++) {
            if (log.isInfoEnabled() && status.due(4)) {
                log.info("Inference: node " + (uid + 1) + " of " + (ex.getGraph().node_hi));
            }
            // p: 2(a) p_u^{t+1} += alpha * s_u
            pNext[uid] += c.apr.alpha * Dictionary.safeGet(ex.getQueryVec(), uid, 0.0);
            // p: 2(b) for each neighbor v of u:
            for (int eid = ex.getGraph().node_near_lo[uid], xvi = 0; eid < ex.getGraph().node_near_hi[uid]; eid++,
                    xvi++) {
                int vid = ex.getGraph().edge_dest[eid];
                // p: 2(b)i. p_v^{t+1} += (1-alpha) * p_u^t * M_uv
                if (vid >= pNext.length) {
                    throw new IllegalStateException("vid=" + vid + " > pNext.length=" + pNext.length);
                }
                pNext[vid] += (1 - c.apr.alpha) * ex.p[uid] * ex.M[uid][xvi];
                // d: i. for each feature i in dM_uv:
                if (dNext[vid] == null) { dNext[vid] = new TIntDoubleHashMap(ex.dM_hi[uid][xvi] - ex.dM_lo[uid][xvi]); }
                for (int dmi = ex.dM_lo[uid][xvi]; dmi < ex.dM_hi[uid][xvi]; dmi++) {
                    // d_vi^{t+1} += (1-alpha) * p_u^{t} * dM_uvi
                    if (ex.dM_value[dmi] == 0) { continue; }
                    double inc = (1 - c.apr.alpha) * ex.p[uid] * ex.dM_value[dmi];
                    dNext[vid].adjustOrPutValue(ex.dM_feature_id[dmi], inc, inc);
                }
                // d: ii. for each feature i in d_u^t
                if (ex.dp[uid] == null) {
                    continue; // skip when d is empty
                }
                for (TIntDoubleIterator it = ex.dp[uid].iterator(); it.hasNext(); ) {
                    it.advance();
                    if (it.value() == 0) { continue; }
                    // d_vi^{t+1} += (1-alpha) * d_ui^t * M_uv
                    double inc = (1 - c.apr.alpha) * it.value() * ex.M[uid][xvi];
                    dNext[vid].adjustOrPutValue(it.key(), inc, inc);
                }
            }
        }

        // sanity check on p
        if (log.isDebugEnabled()) {
            double sum = 0;
            for (double d : pNext) { sum += d; }
            if (Math.abs(sum - 1.0) > c.apr.epsilon) { log.error("invalid p computed: " + sum); }
        }
        ex.p = pNext;
        ex.dp = dNext;
    }

    protected TIntDoubleMap gradient(ParamVector<String, ?> params, PosNegRWExample example) {
        PosNegRWExample ex = example;
        Set<String> features = this.regularizer.localFeatures(params, ex.getGraph());
        TIntDoubleMap gradient = new TIntDoubleHashMap(features.size());
        // add regularization term
        regularization(params, ex, gradient);

        int nonzero = lossf.computeLossGradient(params, example, gradient, this.cumloss, c);
        for (int i : gradient.keys()) {
            gradient.put(i, gradient.get(i) / example.length());
        }
        if (nonzero == 0) {
            this.zeroGradientData.numZero++;
            if (this.zeroGradientData.numZero < MAX_ZERO_LOGS) {
                this.zeroGradientData.examples.append("\n").append(ex);
            }
        }
        return gradient;
    }

    protected double learningRate(String feature) {
        return Math.pow(this.epoch, -2) * c.eta;
    }

    /**
     * template: update gradient with regularization term
     */
    protected void regularization(ParamVector<String, ?> params, PosNegRWExample ex, TIntDoubleMap gradient) {
        regularizer.regularization(params, ex, gradient);
    }

    public void accumulateGradient(ParamVector<String, ?> params, PosNegRWExample example,
                                   ParamVector<String, ?> accumulator, StatusLogger status) {
        log.debug("Gradient calculating on " + example);

        initializeFeatures(params, example.getGraph());
        ParamVector<String, Double> prepare = new SimpleParamVector<String>();
        regularizer.prepareForExample(params, example.getGraph(), prepare);
        load(params, example);
        inference(params, example, status);
        TIntDoubleMap gradient = gradient(params, example);

        for (Map.Entry<String, Double> e : prepare.entrySet()) {
            if (trainable(e.getKey())) { accumulator.adjustValue(e.getKey(), -e.getValue() / example.length()); }
        }
        for (TIntDoubleIterator it = gradient.iterator(); it.hasNext(); ) {
            it.advance();
            String feature = example.getGraph().featureLibrary.getSymbol(it.key());
            if (trainable(feature)) {
                accumulator.adjustValue(example.getGraph().featureLibrary.getSymbol(it.key()), it.value() / example
                        .length());
            }
        }
    }

    //////////////////////////// copypasta from SRW.java:

    public ZeroGradientData getZeroGradientData() {
        return this.zeroGradientData;
    }

    /**
     * Allow subclasses to do additional parameter processing at the end of an epoch
     **/
//	public void cleanupParams(ParamVector<String,?> params, ParamVector apply) {}
    public FixedWeightRules fixedWeightRules() {
        return this.fixedWeightRules;
    }

    public void setFixedWeightRules(FixedWeightRules f) {
        this.fixedWeightRules = f;
    }

    public SquashingFunction getSquashingFunction() {
        return c.squashingFunction;
    }

//	/** Allow subclasses to filter feature list **/
//	public Set<String> localFeatures(ParamVector<String,?> paramVec, LearningGraph graph) {
//		return paramVec.keySet();
//	}
//	/** Allow subclasses to swap in an alternate parameter implementation **/
//	public ParamVector<String,?> setupParams(ParamVector<String,?> params) { return params; }

//	/** Allow subclasses to do pre-example calculations (e.g. lazy regularization) **/
//	public void prepareForExample(ParamVector params, LearningGraph graph, ParamVector apply) {}

    public void setSquashingFunction(SquashingFunction f) {
        c.squashingFunction = f;
    }

    public void setEpoch(int e) {
        this.epoch = e;
        this.zeroGradientData = new ZeroGradientData();
    }

    public void clearLoss() {
        this.cumloss.clear();
        this.cumloss.add(LOSS.LOG, 0.0);
        this.cumloss.add(LOSS.REGULARIZATION, 0.0);
    }

    public LossData _cumulativeLoss() {
        return this.cumloss;
    }

    public LossData cumulativeLoss() {
        return this.cumloss.copy();
    }

    public SRWOptions getOptions() {
        return c;
    }

    public void setAlpha(double d) {
        c.apr.alpha = d;
    }

    public void setMu(double d) {
        c.mu = d;
    }

    public PosNegRWExample makeExample(String string, LearningGraph g,
                                       TIntDoubleMap queryVec, int[] posList, int[] negList) {
        return new PprExample(string, g, queryVec, posList, negList);
    }

    public ParamVector setupParams(ParamVector params) {
        return regularizer.setupParams(params);
    }

    public void cleanupParams(ParamVector params, ParamVector apply) {
        regularizer.cleanupParams(params, apply);
    }

    public SRW copy() {
        Class<? extends SRW> clazz = this.getClass();
        try {
            SRW copy = clazz.getConstructor(SRWOptions.class).newInstance(this.c);
            copy.setRegularizer(this.regularizer.copy(copy));
            copy.setLossFunction(this.lossf.clone());
            copy.fixedWeightRules = this.fixedWeightRules;
            return copy;
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (CloneNotSupportedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        throw new UnsupportedOperationException("Programmer error in SRW subclass " + clazz.getName()
                                                        + ": Must provide the standard SRW constructor signature, or else override copy()");
    }

    public RegularizationSchedule getRegularizer() {
        return this.regularizer;
    }

    public void setRegularizer(RegularizationSchedule r) {
        this.regularizer = r;
    }

    public LossFunction getLossFunction() {
        return this.lossf;
    }

    public void setLossFunction(LossFunction f) {
        this.lossf = f;
    }

    public class ZeroGradientData {

        public int numZero = 0;
        public StringBuilder examples = new StringBuilder();

        public void add(ZeroGradientData z) {
            if (numZero < MAX_ZERO_LOGS) {
                examples.append(z.examples);
            }
            numZero += z.numZero;
        }
    }
}
