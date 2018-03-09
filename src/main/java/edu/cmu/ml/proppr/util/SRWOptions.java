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

package edu.cmu.ml.proppr.util;

import edu.cmu.ml.proppr.learn.SRW;
import edu.cmu.ml.proppr.learn.tools.ReLU;
import edu.cmu.ml.proppr.learn.tools.SquashingFunction;

import java.io.File;
import java.util.List;
import java.util.Map;

public class SRWOptions {

    public static final double DEFAULT_MU = .001;
    public static final double DEFAULT_ETA = 1.0;
    public static final double DEFAULT_DELTA = 0.5;
    public static final double DEFAULT_ZETA = 0;
    public static final File DEFAULT_AFFGRAPH = null;
    /**
     * regularization
     */
    public double mu;
    /**
     * learning rate
     */
    public double eta;
    /**
     * Negative instance booster.
     * If set < 0.5, the gradient component controlled by negative examples is
     * increased by a factor of:
     * log(1/h)/log(1/(1-h))
     * where h = max(p|positive examples) + delta
     */
    public double delta;
    /**
     * local L1 group lasso / laplacian
     */
    public double zeta;
    /**
     * local L1 group lasso / laplacian
     */
    public File affinityFile;
    /**
     * local L1 group lasso / laplacian
     */
    public Map<String, List<String>> affinity;
    /**
     * local L1 group lasso / laplacian
     */
    public Map<String, Integer> diagonalDegree;
    /**
     * wrapper function
     */
    public SquashingFunction squashingFunction;
    /**
     * minalpha projection
     */
    public APROptions apr;

    /** */
    public SRWOptions(APROptions options, SquashingFunction fn) {
        this(
                DEFAULT_MU,
                DEFAULT_ETA,
                fn,
                DEFAULT_DELTA,
                DEFAULT_AFFGRAPH,
                DEFAULT_ZETA,
                options);
    }

    public SRWOptions(
            double mu,
            double eta,
            SquashingFunction f,
            double delta,
            File affgraph,
            double zeta,
            APROptions options) {
        this.mu = mu;
        this.eta = eta;
        this.delta = delta;
        this.zeta = zeta;
        this.squashingFunction = f;
        this.apr = options;
        this.affinityFile = affgraph;
    }

    public SRWOptions() {
        this(
                DEFAULT_MU,
                DEFAULT_ETA,
                DEFAULT_SQUASHING_FUNCTION(),
                DEFAULT_DELTA,
                DEFAULT_AFFGRAPH,
                DEFAULT_ZETA,
                new APROptions());
    }

    public static SquashingFunction DEFAULT_SQUASHING_FUNCTION() {
        return new ReLU();
    }

    public void init() {
        if (zeta > 0) {
            affinity = SRW.constructAffinity(affinityFile);
            diagonalDegree = SRW.constructDegree(affinity);
        } else {
            affinity = null;
            diagonalDegree = null;
        }
    }

    public void set(String... setting) {
        switch (names.valueOf(setting[0])) {
            case mu:
                this.mu = Double.parseDouble(setting[1]);
                return;
            case eta:
                this.eta = Double.parseDouble(setting[1]);
                return;
            case delta:
                this.delta = Double.parseDouble(setting[1]);
                return;
            case zeta:
                this.zeta = Double.parseDouble(setting[1]);
                return;
            case affinityFile:
                File value = new File(setting[1]);
                if (!value.exists()) {
                    throw new IllegalArgumentException("File '" + value.getName() + "' must exist");
                }
                this.affinityFile = value;
                return;
            case apr:
                this.apr.set(setting[1], setting[2]);
        }
    }

    private enum names {
        mu,
        eta,
        delta,
        zeta,
        affinityFile,
        squashingFunction,
        apr
    }
}
