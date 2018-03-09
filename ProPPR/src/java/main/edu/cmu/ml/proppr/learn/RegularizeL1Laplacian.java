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

import java.util.List;

import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.SRWOptions;
import edu.cmu.ml.proppr.util.math.MuParamVector;
import edu.cmu.ml.proppr.util.math.ParamVector;

public class RegularizeL1Laplacian extends RegularizeL1 {
	
	@Override
	protected void lazyUpdate(SRWOptions c, MuParamVector<String> params, ParamVector<String,?> apply, String f,
			LossData loss, double learningRate) {
		int gap = getGap(params,f);
		if (gap==0) return;
		double value = Dictionary.safeGet(params,f);

		double laplacian = 0;
		double sumofsquares = 0;

		String target = "#" + f;
		if(c.diagonalDegree.containsKey(target)){
			double positive = c.diagonalDegree.get(target)*value;
			double negativeSum = 0;
			sumofsquares = value*value;
			List<String> sims = c.affinity.get(target);
			for (String s : sims) {
				double svalue = Dictionary.safeGet(params,s);
				negativeSum -= svalue;
				sumofsquares = sumofsquares + svalue*svalue;
			}
			laplacian = positive + negativeSum;
			//System.out.println("f: " + f +" laplacian:" + laplacian);
		}

		//Laplacian
		double powerTerm = Math.pow(1 - 2 * c.zeta * learningRate * laplacian, gap);
		double weightDecay = laplacian * (powerTerm - 1);
		Dictionary.increment(params, f, weightDecay);
		loss.add(LOSS.REGULARIZATION, gap * c.zeta * Math.pow(value, 2));

		//L1 with a proximal operator              
		//signum(w) * max(0.0, abs(w) - shrinkageVal)

		double shrinkageVal = gap * learningRate * c.mu;
		if((c.mu != 0) && (!Double.isInfinite(shrinkageVal))){
			weightDecay = Math.signum(value) * Math.max(0.0, Math.abs(value) - shrinkageVal);
			Dictionary.set(params, f, weightDecay);
			//FIXME: why is this being set instead of incremented?
			//FIXME: opportunity for out-of-date `value`; probably out to convert to a try loop
			loss.add(LOSS.REGULARIZATION, gap * c.mu);   
		}          		
	}
}
