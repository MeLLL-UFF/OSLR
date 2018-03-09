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

package edu.cmu.ml.proppr.learn.tools;

public class ClippedExp<G> extends SquashingFunction<G> {
	private static final double CLIP=Math.exp(1);
	
	@Override
	public double compute(double sum) {
		return sum < 1 ? Math.exp(sum) : CLIP*sum;
	}

	@Override
	public double computeDerivative(double weight) {
		return weight < 1 ? Math.exp(weight) : CLIP;
	}

	@Override
	public double defaultValue() {
		return 0.0;
	}

	@Override
	public String toString() { return "clipped exponential"; }
}
