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

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.util.SRWOptions;
import edu.cmu.ml.proppr.util.math.ParamVector;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.TIntDoubleMap;

public class NormalizedPosLoss extends LossFunction {

    @Override
    public int computeLossGradient(ParamVector params, PosNegRWExample example, TIntDoubleMap gradient,
                                   LossData lossdata, SRWOptions c) {
        PosNegRWExample ex = example;
        int nonzero = 0;
        double mag = 0;

        //if there are no negative nodes or no positive nodes, the probability of positive nodes
        //is zero or 1, and the empirical loss gradient is zero.
        if (ex.getNegList().length == 0 || ex.getPosList().length == 0) { return nonzero; }

        double sumPos = 0;
        for (int a : ex.getPosList()) {
            sumPos += clip(ex.p[a]);
        }
        sumPos = clip(sumPos);

        for (int a : ex.getPosList()) {
            for (TIntDoubleIterator da = ex.dp[a].iterator(); da.hasNext(); ) {
                da.advance();
                if (da.value() == 0) { continue; }
                nonzero++;
                double aterm = -da.value() / sumPos;
                gradient.adjustOrPutValue(da.key(), aterm, aterm);
            }
        }

        lossdata.add(LOSS.LOG, -Math.log(sumPos));

        double sumPosNeg = 0;
        for (int pa : ex.getPosList()) {
            sumPosNeg += clip(ex.p[pa]);
        }
        for (int pa : ex.getNegList()) {
            sumPosNeg += clip(ex.p[pa]);
        }
        sumPosNeg = clip(sumPosNeg);

        for (int a : ex.getPosList()) {
            for (TIntDoubleIterator da = ex.dp[a].iterator(); da.hasNext(); ) {
                da.advance();
                if (da.value() == 0) { continue; }
                nonzero++;
                double bterm = da.value() / sumPosNeg;
                gradient.adjustOrPutValue(da.key(), bterm, bterm);
            }
        }
        for (int b : ex.getNegList()) {
            for (TIntDoubleIterator db = ex.dp[b].iterator(); db.hasNext(); ) {
                db.advance();
                if (db.value() == 0) { continue; }
                nonzero++;
                double bterm = db.value() / sumPosNeg;
                gradient.adjustOrPutValue(db.key(), bterm, bterm);
            }
        }

        lossdata.add(LOSS.LOG, Math.log(sumPosNeg));

        return nonzero;
    }
}
