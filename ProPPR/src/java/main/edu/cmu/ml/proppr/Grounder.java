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

package edu.cmu.ml.proppr;

import edu.cmu.ml.proppr.examples.GroundedExample;
import edu.cmu.ml.proppr.examples.InferenceExample;
import edu.cmu.ml.proppr.examples.InferenceExampleStreamer;
import edu.cmu.ml.proppr.learn.tools.SquashingFunction;
import edu.cmu.ml.proppr.prove.InnerProductWeighter;
import edu.cmu.ml.proppr.prove.Prover;
import edu.cmu.ml.proppr.prove.wam.*;
import edu.cmu.ml.proppr.prove.wam.plugins.WamPlugin;
import edu.cmu.ml.proppr.util.*;
import edu.cmu.ml.proppr.util.math.ParamVector;
import edu.cmu.ml.proppr.util.math.SimpleParamVector;
import edu.cmu.ml.proppr.util.multithreading.Multithreading;
import edu.cmu.ml.proppr.util.multithreading.Transformer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Exports a grounded example for each inference example in a data file.
 *
 * @author wcohen, krivard
 */
public class Grounder<P extends ProofGraph> {

    public static final String FEATURE_INDEX_EXTENSION = ".features";
    protected static final Logger log = LogManager.getLogger(Grounder.class);
    public static final String GROUNDED_SUFFIX = ".grounded";
    protected File graphKeyFile = null;
    protected Writer graphKeyWriter = null;
    protected GroundingStatistics statistics = new GroundingStatistics();

    protected APROptions apr;
    protected Prover<P> prover;
    protected WamProgram masterProgram;
    protected WamPlugin[] masterPlugins;
    protected int nthreads = 1;
    protected int throttle = Multithreading.DEFAULT_THROTTLE;
    private int empty;
    protected boolean includeUnlabeledGraphs = false;
    protected SymbolTable<Feature> featureTable = new ConcurrentSymbolTable<Feature>(ConcurrentSymbolTable
                                                                                             .HASHING_STRATEGIES
                                                                                             .identity);
    protected StatusLogger status = new StatusLogger();

    public Grounder(APROptions apr, Prover<P> p, WamProgram program, WamPlugin... plugins) {
        this.apr = apr;
        this.prover = p;
        this.masterProgram = program;
        this.masterPlugins = plugins;
    }

    public Grounder(int nthreads, int throttle, APROptions apr, Prover<P> p, WamProgram program, WamPlugin... plugins) {
        this(apr, p, program, plugins);
        this.nthreads = Math.max(1, nthreads);
        this.throttle = throttle;
    }

    public void addParams(ParamVector<String, ?> params, SquashingFunction<Goal> f) {
        this.prover.setWeighter(InnerProductWeighter.fromParamVec(params, f));
    }

    public void includeUnlabeledGraphs(boolean includeThem) {
        this.includeUnlabeledGraphs = includeThem;
    }

    public class GroundingStatistics {

        public GroundingStatistics() {
            log.debug("Resetting grounding statistics...");
        }

        // statistics
        public int totalPos = 0;
        public int totalNeg = 0;
        public int coveredPos = 0;
        public int coveredNeg = 0;
        public InferenceExample worstX = null;
        public double smallestFractionCovered = 1.0;
        public Integer count = 0;
        public Integer noPosNeg = 0;
        public Integer emptyGraph = 0;

        public void noPosNeg() {
            synchronized (noPosNeg) { noPosNeg++; }
        }

        public void emptyGraph() {
            synchronized (emptyGraph) { emptyGraph++; }
        }

        public void updateStatistics(InferenceExample ex, int npos, int nneg, int covpos, int covneg) {
            // keep track of some statistics - synchronized for multithreading
            synchronized (count) {
                count++;
                totalPos += npos;
                totalNeg += nneg;
                coveredPos += covpos;
                coveredNeg += covneg;
                double fractionCovered = covpos / (double) npos;
                if (fractionCovered < smallestFractionCovered) {
                    worstX = ex;
                    smallestFractionCovered = fractionCovered;
                }

//				if (log.isInfoEnabled()) {
//					long now = System.currentTimeMillis();
//					if (now-lastPrint > LOGUPDATE_MS) {
//						lastPrint = now;
//						log.info("Grounded "+count+" examples...");
//					}
//				}
            }
        }
    }

    public void groundExamples(File dataFile, File groundedFile, boolean maintainOrder) {
        status.start();
        try {
            if (this.graphKeyFile != null) {
                this.graphKeyWriter = new BufferedWriter(new FileWriter(this.graphKeyFile));
            }
            this.statistics = new GroundingStatistics();
            this.empty = 0;

            Multithreading<InferenceExample, String> m = new Multithreading<InferenceExample, String>(log, this
                    .status, maintainOrder);

            m.executeJob(
                    this.nthreads,
                    new InferenceExampleStreamer(dataFile).stream(),
                    new Transformer<InferenceExample, String>() {
                        @Override
                        public Callable<String> transformer(InferenceExample in, int id) {
                            return new Ground(in, id);
                        }
                    },
                    groundedFile,
                    this.throttle);

            reportStatistics(empty);

            File indexFile = new File(groundedFile.getParent(), groundedFile.getName() + FEATURE_INDEX_EXTENSION);
            serializeFeatures(indexFile, featureTable);

            if (this.graphKeyFile != null) { this.graphKeyWriter.close(); }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    long lastPrint = 0;//System.currentTimeMillis();

    /**
     * Requires non-empty graph; non-empty example
     */
    public String serializeGroundedExample(P pg, GroundedExample x) {
        return pg.serialize(x);
    }

    protected String serializeGraphKey(GroundedExample ex, P pg) {
        StringBuilder key = new StringBuilder();
        String s = pg.getExample().getQuery().toString();
        for (int i = 0; i < ex.getGraph().nodeSize(); i++) {
            key.append(s)
                    .append("\t")
                    .append(i + 1)
                    .append("\t")
                    .append((State) ex.getGraph().getState(i + 1))
                    .append("\n");
        }
        return key.toString();
    }

    protected void saveGraphKey(GroundedExample grounded, P pg) {
        String graphKey = serializeGraphKey(grounded, pg);
        synchronized (this.graphKeyWriter) {
            try {
                this.graphKeyWriter.write(graphKey);
            } catch (IOException e) {
                throw new IllegalStateException("Couldn't write to graph key file " + this.graphKeyFile.getName(), e);
            }
        }
    }

    protected void serializeFeatures(File indexFile, SymbolTable<Feature> featureTable) throws IOException {
        Writer w = new BufferedWriter(new FileWriter(indexFile));
        for (int i = 1; i <= featureTable.size(); i++) {
            w.write(featureTable.getSymbol(i).toString());
            w.write("\n");
        }
        w.close();
    }

    protected Prover<P> getProver() {
        return this.prover;
    }

    public GroundedExample groundExample(P pg) throws LogicProgramException {
        return this.groundExample(this.prover.copy(), pg);
    }

    /**
     * Run the prover to produce a proof of an example
     *
     * @param rawX
     * @return
     * @throws LogicProgramException
     */
    public GroundedExample groundExample(Prover<P> p, P pg) throws LogicProgramException {
        if (log.isTraceEnabled()) { log.trace("thawed example: " + pg.getExample().toString()); }
        Map<State, Double> ans = p.prove(pg, status);
        GroundedExample ground = pg.makeRWExample(ans);
        if (this.graphKeyFile != null) { saveGraphKey(ground, pg); }
        return ground;
    }

    public GroundedExample groundExample(Prover<P> p,
                                         InferenceExample inferenceExample) throws LogicProgramException {
        validateExample(inferenceExample);
        return this.groundExample(p, p.makeProofGraph(inferenceExample, apr, featureTable, masterProgram,
                                                      masterPlugins));
    }

    protected void reportStatistics(int empty) {
        if (!log.isInfoEnabled()) { return; }
        int skipped = statistics.noPosNeg + statistics.emptyGraph;
        log.info("Grounded: " + (statistics.count - skipped));
        log.info("Skipped: " + skipped + " = " + statistics.noPosNeg + " with no labeled solutions; " + statistics
                .emptyGraph + " with empty graphs");
        log.info("totalPos: " + statistics.totalPos
                         + " totalNeg: " + statistics.totalNeg
                         + " coveredPos: " + statistics.coveredPos
                         + " coveredNeg: " + statistics.coveredNeg);
        if (statistics.totalPos > 0) {
            log.info("For positive examples " + statistics.coveredPos
                             + "/" + statistics.totalPos
                             + " proveable [" + ((100.0 * statistics.coveredPos) / statistics.totalPos) + "%]");
        }
        if (statistics.totalNeg > 0) {
            log.info("For negative examples " + statistics.coveredNeg
                             + "/" + statistics.totalNeg
                             + " proveable [" + ((100.0 * statistics.coveredNeg) / statistics.totalNeg) + "%]");
        }
        if (statistics.worstX != null) {
            log.info("Example with fewest [" + 100.0 * statistics.smallestFractionCovered + "%] pos examples covered: "
                             + statistics.worstX.getQuery());
        }
    }

    public static class ExampleGrounderConfiguration extends CustomConfiguration {

        private File keyFile;

        public ExampleGrounderConfiguration(String[] args, int inputFiles, int outputFiles, int constants,
                                            int modules) {
            super(args, inputFiles, outputFiles, constants, modules);
        }

        @Override
        protected void addCustomOptions(Options options, int[] flags) {
            options.addOption(OptionBuilder
                                      .withLongOpt("graphKey")
                                      .withArgName("keyFile")
                                      .hasArg()
                                      .withDescription("Save a key to the grounded graphs providing the " +
                                                               "LogicProgramState definitions of the numbered nodes")
                                      .create());
            options.getOption(Configuration.PARAMS_FILE_OPTION).setRequired(false);
        }

        @Override
        protected void retrieveCustomSettings(CommandLine line, int[] flags,
                                              Options options) {
            if (line.hasOption("graphKey")) { this.keyFile = new File(line.getOptionValue("graphKey")); }
        }

        @Override
        public Object getCustomSetting(String name) {
            return keyFile;
        }
    }

    public void useGraphKeyFile(File keyFile) {
        log.info("Using graph key file " + keyFile.getName());
        this.graphKeyFile = keyFile;
    }

    public void validateExample(InferenceExample in) {
        for (Query[] labels : new Query[][]{in.getPosSet(), in.getNegSet()}) {
            for (Query q : labels) {
                for (Goal g : q.getRhs()) {
                    for (Argument a : g.getArgs()) {
                        if (a.isVariableAtom()) {
                            throw new IllegalArgumentException("Malformed query label (all arguments must be bound; " +
                                                                       "must start with lowercase): " + q.toString());
                        }
                    }
                }
            }
        }
    }

    ///////////////////////////////// Multithreading scaffold //////////////////////////

    /**
     * Transforms from inputs to outputs
     *
     * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
     */
    private class Ground implements Callable<String> {

        InferenceExample inf;
        int id;

        public Ground(InferenceExample in, int id) {
            validateExample(in);
            this.inf = in;
            this.id = id;
        }

        @Override
        public String call() throws Exception {
            P pg = prover.makeProofGraph(inf, apr, featureTable, masterProgram, masterPlugins);
            GroundedExample gx = groundExample(getProver().copy(), pg);
            InferenceExample ix = pg.getExample();
            statistics.updateStatistics(ix,
                                        ix.getPosSet().length, ix.getNegSet().length,
                                        gx.getPosList().size(), gx.getNegList().size());
            if (gx.getGraph().edgeSize() > 0) {
                if (gx.length() > 0 || includeUnlabeledGraphs) {
                    return (serializeGroundedExample(pg, gx));
                } else {
                    statistics.noPosNeg();
                    //log.warn("No positive or negative solutions for query "+id+":"+pg.getExample().getQuery()
                    // .toString()+"; skipping");
                }
            } else {
                statistics.emptyGraph(); //log.warn("Empty graph for example "+id);
            }
            return null;
        }
    }

    /////////////////////////////////////// Command line ////////////////////////////////
    public static void main(String... args) {
        try {
            int inputFiles = Configuration.USE_QUERIES | Configuration.USE_PARAMS;
            int outputFiles = Configuration.USE_GROUNDED;
            int constants = Configuration.USE_WAM | Configuration.USE_THREADS | Configuration.USE_ORDER |
                    Configuration.USE_EMPTYGRAPHS;
            int modules = Configuration.USE_GROUNDER | Configuration.USE_PROVER | Configuration.USE_SQUASHFUNCTION;

            ExampleGrounderConfiguration c = new ExampleGrounderConfiguration(args, inputFiles, outputFiles,
                                                                              constants, modules);
            System.out.println(c.toString());

            if (c.getCustomSetting("graphKey") != null) {
                c.grounder.useGraphKeyFile((File) c.getCustomSetting("graphKey"));
            }
            if (c.paramsFile != null) {
                ParamsFile file = new ParamsFile(c.paramsFile);
                c.grounder.addParams(new SimpleParamVector<String>(Dictionary.load(file)), c.squashingFunction);
                file.check(c);
            }
            long start = System.currentTimeMillis();
            c.grounder.groundExamples(c.queryFile, c.groundedFile, c.maintainOrder);
            System.out.println("Grounding time: " + (System.currentTimeMillis() - start));
            System.out.println("Done.");

        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
    }
}
