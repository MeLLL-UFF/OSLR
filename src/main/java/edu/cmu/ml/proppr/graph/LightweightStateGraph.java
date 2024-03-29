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

package edu.cmu.ml.proppr.graph;

import edu.cmu.ml.proppr.prove.wam.Feature;
import edu.cmu.ml.proppr.prove.wam.Outlink;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.util.SimpleSymbolTable;
import edu.cmu.ml.proppr.util.SymbolTable;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntDoubleProcedure;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.strategy.HashingStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class LightweightStateGraph implements InferenceGraph {

    private static final Logger log = LogManager.getLogger(LightweightStateGraph.class);
    private static final Map<Feature, Double> DEFAULT_FD = Collections.emptyMap();
    private final List<State> DEFAULT_NEAR = Collections.emptyList();
    private final SymbolTable<State> nodeTab;
    private final SymbolTable<Feature> featureTab;
    private final TIntObjectHashMap<TIntArrayList> near = new TIntObjectHashMap<TIntArrayList>();
    private final TIntObjectHashMap<TIntObjectHashMap<TIntDoubleHashMap>> edgeFeatureDict = new
            TIntObjectHashMap<TIntObjectHashMap<TIntDoubleHashMap>>();
    private int edgeCount = 0;

    public LightweightStateGraph() {
        this(new SimpleSymbolTable<State>(), new SimpleSymbolTable<Feature>());
    }

    public LightweightStateGraph(SymbolTable<State> ntab, SymbolTable<Feature> ftab) {
        this.nodeTab = ntab;
        this.featureTab = ftab;
    }

    public LightweightStateGraph(HashingStrategy<State> nodeHash) {
        this(new SimpleSymbolTable<State>(nodeHash), new SimpleSymbolTable<Feature>());
    }

    public LightweightStateGraph(HashingStrategy<State> nodeHash, SymbolTable<Feature> ftab) {
        this(new SimpleSymbolTable<State>(nodeHash), ftab);
    }

    @Override
    public State getState(int u) {
        return nodeTab.getSymbol(u);
    }

    @Override
    public int nodeSize() {
        return this.nodeTab.size();
    }

    @Override
    public int edgeSize() {
        return this.edgeCount;
    }

    /**
     * Serialization format: tab-delimited fields
     * 1: node count
     * 2: edge count
     * 3: featurename1:featurename2:featurename3:...:featurenameN
     * 4..N: srcId->dstId:fId_1,fId_2,...,fId_k
     * <p>
     * All IDs are indexed starting at 1.
     *
     * @return
     */
    @Override
    public String serialize() {
        return serialize(false);
    }

    @Override
    public String serialize(boolean featureIndex) {
        StringBuilder ret = new StringBuilder().append(this.nodeSize()) //numNodes
                .append("\t")
                .append(this.edgeCount)
                .append("\t"); // waiting for label dependency size
        int labelDependencies = 0;

        StringBuilder sb = new StringBuilder();
        boolean first = true;

        if (featureIndex) {
            sb.append("\t");
            for (int fi = 1; fi <= this.featureTab.size(); fi++) {
                if (!first) { sb.append(LearningGraphBuilder.FEATURE_INDEX_DELIM); } else { first = false; }
                Feature f = this.featureTab.getSymbol(fi);
                sb.append(f);
            }
        }

        // foreach src node
        for (TIntObjectIterator<TIntArrayList> it = this.near.iterator(); it.hasNext(); ) {
            it.advance();
            int ui = it.key();
            TIntArrayList nearu = it.value();
            HashSet<Integer> outgoingFeatures = new HashSet<Integer>();
            //foreach dst from src
            for (TIntIterator vit = nearu.iterator(); vit.hasNext(); ) {
                int vi = vit.next();
                sb.append("\t");
                sb.append(ui).append(LearningGraphBuilder.SRC_DST_DELIM).append(vi);
                sb.append(LearningGraphBuilder.EDGE_DELIM);
                //foreach feature on src,dst
                for (TIntDoubleIterator fit = edgeFeatureDict.get(ui).get(vi).iterator(); fit.hasNext(); ) {
                    fit.advance();
                    int fi = fit.key();
                    double wi = fit.value();
                    outgoingFeatures.add(fi);
                    sb.append(fi).append(LearningGraphBuilder.FEATURE_WEIGHT_DELIM)
                            .append(wi).append(LearningGraphBuilder.EDGE_FEATURE_DELIM);
                }
                // drop last ','
                sb.deleteCharAt(sb.length() - 1);
            }
            labelDependencies += outgoingFeatures.size() * nearu.size();
        }

        ret.append(labelDependencies).append(sb);
        return ret.toString();
    }

    @Override
    public void setOutlinks(int uid, List<Outlink> outlinks) {
        setOutlinks(this.nodeTab.getSymbol(uid), outlinks);
    }

    @Override
    public int getId(State u) {
        return nodeTab.getId(u);
    }

    public void setOutlinks(State u, List<Outlink> outlinks) {
        // wwc: why are we saving these outlinks as a trove thing? space?
        int ui = this.nodeTab.getId(u);
        if (near.containsKey(ui)) {
            log.warn("Overwriting previous outlinks for state " + u);
            edgeCount -= near.get(ui).size();
        }
        TIntArrayList nearui = new TIntArrayList(outlinks.size());
        near.put(ui, nearui);
        TIntObjectHashMap<TIntDoubleHashMap> fui = new TIntObjectHashMap<TIntDoubleHashMap>();
        edgeFeatureDict.put(ui, fui);
        for (Outlink o : outlinks) {
            int vi = this.nodeTab.getId(o.child);
            nearui.add(vi);
            edgeCount++;
            TIntDoubleHashMap fvui = new TIntDoubleHashMap(o.fd.size());
            for (Map.Entry<Feature, Double> e : o.fd.entrySet()) {
                fvui.put(this.featureTab.getId(e.getKey()), e.getValue());
            }
            fui.put(vi, fvui);
        }
    }

    public State getRoot() {
        return nodeTab.getSymbol(1);
    }

    /**
     * Return the neighbors of node u.
     */
    public List<Outlink> getOutlinks(State u) {
        // wwc: why do we need to recompute these each time?
        List<Outlink> result = new ArrayList<Outlink>();
        for (State v : near(u)) {
            Map<Feature, Double> fd = getFeatures(u, v);
            result.add(new Outlink(fd, v));
        }
        return result;
    }

    public List<State> near(State u) {
        int ui = this.nodeTab.getId(u);
        if (!near.containsKey(ui)) { return DEFAULT_NEAR; }
        TIntArrayList vs = near.get(ui);
        final ArrayList<State> ret = new ArrayList<State>(vs.size());
        vs.forEach(new TIntProcedure() {
            @Override
            public boolean execute(int vi) {
                ret.add(nodeTab.getSymbol(vi));
                return true;
            }
        });
        return ret;
    }

    public Map<Feature, Double> getFeatures(State u, State v) {
        int ui = this.nodeTab.getId(u), vi = this.nodeTab.getId(v);
        if (!edgeFeatureDict.containsKey(ui)) { return DEFAULT_FD; }
        TIntObjectHashMap<TIntDoubleHashMap> fu = edgeFeatureDict.get(ui);
        if (!fu.containsKey(vi)) { return DEFAULT_FD; }
        TIntDoubleHashMap fuvi = fu.get(vi);
        final HashMap<Feature, Double> ret = new HashMap<Feature, Double>();
        fuvi.forEachEntry(new TIntDoubleProcedure() {
            @Override
            public boolean execute(int fi, double wt) {
                ret.put(featureTab.getSymbol(fi), wt);
                return true;
            }
        });
        return ret;
    }

    public boolean outlinksDefined(State u) {
        return near.containsKey(this.nodeTab.getId(u));
    }

    @Override
    public String toString() {
        return this.serialize(true);
    }
}
