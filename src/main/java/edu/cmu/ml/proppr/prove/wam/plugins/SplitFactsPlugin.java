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

package edu.cmu.ml.proppr.prove.wam.plugins;

import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.Outlink;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.WamInterpreter;
import edu.cmu.ml.proppr.util.APROptions;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Manage multiple .cfacts files, supporting splitting a functor across multiple files
 *
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 */
public class SplitFactsPlugin extends WamPlugin {

    protected List<FactsPlugin> plugins = new LinkedList<FactsPlugin>();

    public SplitFactsPlugin(APROptions apr) {
        super(apr);
    }

    @Override
    public String about() {
        StringBuilder sb = new StringBuilder("union(");
        for (FactsPlugin p : plugins) { sb.append(p.about()).append(","); }
        sb.setCharAt(sb.length() - 1, ')');
        return sb.toString();
    }

    @Override
    public boolean _claim(String jumpto) {
        for (FactsPlugin p : plugins) { if (p._claim(jumpto)) { return true; } }
        return false;
    }

    @Override
    public List<Outlink> outlinks(State state, WamInterpreter wamInterp,
                                  boolean computeFeatures) throws LogicProgramException {
        List<Outlink> ret = new ArrayList<Outlink>();
        for (FactsPlugin p : plugins) {
            if (p._claim(state.getJumpTo())) {
                wamInterp.restoreState(state);
                List<Outlink> outs = p.outlinks(state, wamInterp, computeFeatures);
                ret.addAll(outs);
            }
        }
        return ret;
    }

    public void add(FactsPlugin p) {
        this.plugins.add(p);
    }
}
