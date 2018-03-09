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

import edu.cmu.ml.proppr.learn.tools.FixedWeightRules;
import edu.cmu.ml.proppr.prove.wam.WamBaseProgram;
import edu.cmu.ml.proppr.prove.wam.WamProgram;
import edu.cmu.ml.proppr.prove.wam.plugins.*;
import edu.cmu.ml.proppr.util.multithreading.Multithreading;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.Arrays;
import java.util.Properties;

/**
 * Configuration engine for input files, output files and (for whatever reason) constants/hyperparameters.
 * <p>
 * For modules (prover, grounder, trainer, tester, etc) see ModuleConfiguration subclass.
 *
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 */
public class Configuration {

    public static final int FORMAT_WIDTH = 18;
    public static final String FORMAT_STRING = "%" + FORMAT_WIDTH + "s";
    public static final String EXAMPLES_FORMAT = "f(A1,A2)\\t{+|-}f(a1,a2)\\t...";
    /**
     * file.
     */
    public static final int USE_QUERIES = 0x1;
    /* set files */
    public static final int USE_GROUNDED = 0x2;
    public static final int USE_ANSWERS = 0x4;
    public static final int USE_TRAIN = 0x8;
    public static final int USE_TEST = 0x10;
    public static final int USE_PARAMS = 0x20;
    public static final int USE_GRADIENT = 0x40;
    public static final int USE_INIT_PARAMS = 0x80;
    public static final String QUERIES_FILE_OPTION = "queries";
    public static final String GROUNDED_FILE_OPTION = "grounded";
    public static final String SOLUTIONS_FILE_OPTION = "solutions";
    public static final String TRAIN_FILE_OPTION = "train";
    public static final String TEST_FILE_OPTION = "test";
    public static final String PARAMS_FILE_OPTION = "params";
    public static final String INIT_PARAMS_FILE_OPTION = "initParams";
    public static final String GRADIENT_FILE_OPTION = "gradient";
    /**
     * constant. programFiles, ternaryIndex
     */
    public static final int USE_WAM = 0x1;

	/* set constants */
    /**
     * constant.
     */
    public static final int USE_THREADS = 0x2;
    public static final int USE_EPOCHS = 0x4;
    public static final int USE_FORCE = 0x10;
    public static final int USE_ORDER = 0x20;
    public static final int USE_DUPCHECK = 0x40;
    public static final int USE_THROTTLE = 0x80;
    public static final int USE_EMPTYGRAPHS = 0x100;
    public static final int USE_FIXEDWEIGHTS = 0x200;
    public static final int USE_COUNTFEATURES = 0x400;
    /**
     * module.
     */
    public static final int USE_SQUASHFUNCTION = 0x1;
    public static final int USE_GROUNDER = 0x2;
    public static final int USE_SRW = 0x4;
    public static final int USE_TRAINER = 0x8;
    public static final int USE_PROVER = 0x10;
    /** */
    public static final String PROPFILE = "config.properties";
    protected static final Logger log = LogManager.getLogger(Configuration.class);
    // wwc: protected so that ModuleConfiguration can give warnings...
    protected static final String PRUNEDPREDICATE_CONST_OPTION = "prunedPredicates";
    private static final String PROGRAMFILES_CONST_OPTION = "programFiles";
    private static final String TERNARYINDEX_CONST_OPTION = "ternaryIndex";
    private static final String APR_CONST_OPTION = "apr";
    private static final String THREADS_CONST_OPTION = "threads";
    private static final String EPOCHS_CONST_OPTION = "epochs";
    private static final String FORCE_CONST_OPTION = "force";

    /* set class for module. Options for this section are handled in ModuleConfiguration.java. */
    private static final String ORDER_CONST_OPTION = "order";
    private static final String DUPCHECK_CONST_OPTION = "duplicateCheck";
    private static final String THROTTLE_CONST_OPTION = "throttle";
    private static final String EMPTYGRAPHS_CONST_OPTION = "includeEmptyGraphs";
    private static final String FIXEDWEIGHTS_CONST_OPTION = "fixedWeights";
    private static final String COUNTFEATURES_CONST_OPTION = "countFeatures";
    private static final boolean DEFAULT_COMBINE = true;

    private static final int USE_APR = USE_WAM | USE_PROVER | USE_SRW;
    private static final int USE_SMART_COUNTFEATURES = USE_COUNTFEATURES | USE_WAM;
    private static Configuration instance;
    public File queryFile = null;
    public File testFile = null;
    public File groundedFile = null;
    public File paramsFile = null;
    public File initParamsFile = null;
    public File solutionsFile = null;
    public File gradientFile = null;
    public WamProgram program = null;
    public WamPlugin[] plugins = null;
    public String[] programFiles = null;
    public int nthreads = -1;
    public APROptions apr = new APROptions();
    public int epochs = 5;
    public boolean force = false;
    public boolean ternaryIndex = false;
    public boolean maintainOrder = true;
    public boolean includeEmptyGraphs = false;
    public int duplicates = (int) 1e6;
    public int throttle = Multithreading.DEFAULT_THROTTLE;
    public FixedWeightRules fixedWeightRules = null;
    public FixedWeightRules prunedPredicateRules = null;
    public boolean countFeatures = true;

    private Configuration() {
    }

    public Configuration(String[] args, int inputFiles, int outputFiles, int constants, int modules) {
        setInstance(this);
        log.info("");
        boolean combine = DEFAULT_COMBINE;
        int[] flags = {inputFiles, outputFiles, constants, modules};

        Options options = new Options();
        options.addOption(Option.builder().longOpt("profile")
                                  .desc("Holds all computation & loading until the return key is pressed.")
                                  .build());
        addOptions(options, flags);

        CommandLine line = null;
        try {
            DefaultParser parser = new DefaultParser();

            // if the user specified a properties file, add those values at the beginning
            // (so that command line args override them)
            if (combine) { args = combinedArgs(args); }

            // this is terrible: we just read a Properties from a file and serialized it to String[],
            // and now we're going to put it back into a Properties object. But Commons CLI
            // doesn't know how to handle unrecognized options, so ... that's what we gotta do.
            Properties props = new Properties();
            for (int i = 0; i < args.length; i++) {
                if (args[i].startsWith("--")) {
                    if (!options.hasOption(args[i])) {
                        System.err.println("Unrecognized option: " + args[i]);
                        continue; // skip unrecognized options
                    }
                    if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                        props.setProperty(args[i], args[i + 1]);
                        i++;
                    } else { props.setProperty(args[i], "true"); }
                }
            }

            // parse the command line arguments
            line = parser.parse(options, new String[0], props);
            if (line.hasOption("profile")) {
                System.out.println("Holding for profiler setup; press any key to proceed.");
                System.in.read();
            }
            retrieveSettings(line, flags, options);

        } catch (Exception exp) {
            if (args[0].equals("--help")) { usageOptions(options, flags); }
            StringWriter sw = new StringWriter();
            exp.printStackTrace(new PrintWriter(sw));
            usageOptions(options, flags, exp.getMessage() + "\n" + sw);
        }
    }

    public static Configuration getInstance() {
        return instance;
    }

    public static void setInstance(Configuration i) {
//		if (instance!=null) throw new IllegalStateException("Configuration is a singleton");
        instance = i;
    }

    public static void missing(int options, int[] flags) {
        StringBuilder sb = new StringBuilder("Missing required option:\n");
        switch (options) {
            case USE_WAM:
                sb.append("\tprogramFiles");
                break;
            default:
                throw new UnsupportedOperationException("Bad programmer! Add handling to Configuration.missing for " +
                                                                "flag " + options);
        }
        Configuration c = new Configuration();
        Options o = new Options();
        c.addOptions(o, flags);
        c.usageOptions(o, flags, sb.toString());
    }

    /**
     * For all option flags as specified in this file, addOptions creates
     * and adds Option objects to the Options object.
     */
    protected void addOptions(Options options, int[] allFlags) {
        int flags;

        options.addOption(
                OptionBuilder
                        .withLongOpt("help")
                        .withDescription("Print usage syntax.")
                        .create());

        // input files
        flags = inputFiles(allFlags);
        if (isOn(flags, USE_QUERIES)) {
            options.addOption(checkOption(
                    OptionBuilder
                            .withLongOpt(QUERIES_FILE_OPTION)
                            .isRequired()
                            .withArgName("file")
                            .hasArg()
                            .withDescription("Queries. Format (discards after tab): " + EXAMPLES_FORMAT)
                            .create()));
        }
        if (isOn(flags, USE_GROUNDED)) {
            options.addOption(checkOption(
                    OptionBuilder
                            .withLongOpt(GROUNDED_FILE_OPTION)
                            .isRequired()
                            .withArgName("file")
                            .hasArg()
                            .withDescription("Grounded examples. Format: query\\tkeys,,\\tposList,,\\tnegList,," +
                                                     "\\tgraph")
                            .create()));
        }
        if (isOn(flags, USE_TRAIN)) {
            options.addOption(checkOption(
                    OptionBuilder
                            .withLongOpt(TRAIN_FILE_OPTION)
                            .isRequired()
                            .withArgName("file")
                            .hasArg()
                            .withDescription("Training examples. Format: " + EXAMPLES_FORMAT)
                            .create()));
        }
        if (isOn(flags, USE_TEST)) {
            options.addOption(checkOption(
                    OptionBuilder
                            .withLongOpt(TEST_FILE_OPTION)
                            .isRequired()
                            .withArgName("file")
                            .hasArg()
                            .withDescription("Testing examples. Format: " + EXAMPLES_FORMAT)
                            .create()));
        }
        if (isOn(flags, USE_PARAMS)) {
            options.addOption(checkOption(
                    OptionBuilder
                            .withLongOpt(PARAMS_FILE_OPTION)
                            .withArgName("file")
                            .hasArg()
                            .withDescription("Learned walker parameters. Format: feature\\t0.000000")
                            .create()));
        }
        if (isOn(flags, USE_INIT_PARAMS)) {
            options.addOption(checkOption(
                    OptionBuilder
                            .withLongOpt(INIT_PARAMS_FILE_OPTION)
                            .withArgName("file")
                            .hasArg()
                            .withDescription("Learned walker parameters. Same format as --params, but used to " +
                                                     "warm-start a learner.")
                            .create()));
        }

        // output files
        flags = outputFiles(allFlags);
        if (isOn(flags, USE_ANSWERS)) {
            options.addOption(checkOption(
                    OptionBuilder
                            .withLongOpt(SOLUTIONS_FILE_OPTION)
                            .isRequired()
                            .withArgName("file")
                            .hasArg()
                            .withDescription("Output answers")
                            .create()));
        }
        if (isOn(flags, USE_QUERIES)) {
            options.addOption(checkOption(
                    OptionBuilder
                            .withLongOpt(QUERIES_FILE_OPTION)
                            .withArgName("file")
                            .hasArg()
                            .withDescription("Output queries")
                            .create()));
        }
        if (isOn(flags, USE_GROUNDED)) {
            options.addOption(checkOption(
                    OptionBuilder
                            .withLongOpt(GROUNDED_FILE_OPTION)
                            .isRequired()
                            .withArgName("file")
                            .hasArg()
                            .withDescription("Output grounded examples.")
                            .create()));
        }
        if (isOn(flags, USE_GRADIENT)) {
            options.addOption(checkOption(
                    OptionBuilder
                            .withLongOpt(GRADIENT_FILE_OPTION)
                            .isRequired()
                            .withArgName("file")
                            .hasArg()
                            .withDescription("Output gradient.")
                            .create()));
        }
        if (isOn(flags, USE_TRAIN)) {
            options.addOption(checkOption(
                    OptionBuilder
                            .withLongOpt(TRAIN_FILE_OPTION)
                            .isRequired()
                            .withArgName("file")
                            .hasArg()
                            .withDescription("Output training examples.")
                            .create()));
        }
        if (isOn(flags, USE_TEST)) {
            options.addOption(checkOption(
                    OptionBuilder
                            .withLongOpt(TEST_FILE_OPTION)
                            .isRequired()
                            .withArgName("file")
                            .hasArg()
                            .withDescription("Output testing examples.")
                            .create()));
        }
        if (isOn(flags, USE_PARAMS)) {
            options.addOption(checkOption(
                    OptionBuilder
                            .withLongOpt(PARAMS_FILE_OPTION)
                            .isRequired()
                            .withArgName("file")
                            .hasArg()
                            .withDescription("Output learned walker parameters.")
                            .create()));
        }

        // constants
        flags = constants(allFlags);
        if (isOn(flags, USE_WAM)) {
            options.addOption(checkOption(
                    OptionBuilder
                            .withLongOpt(PROGRAMFILES_CONST_OPTION)
                            .withArgName("file:...:file")
                            .hasArgs()
                            .withValueSeparator(':')
                            .withDescription("Description of the logic program. Permitted extensions: .wam, .cfacts, " +
                                                     ".graph, .sparse")
                            .create()));
            options.addOption(checkOption(
                    OptionBuilder
                            .withLongOpt(TERNARYINDEX_CONST_OPTION)
                            .withArgName("true|false")
                            .hasArg()
                            .withDescription("Turn on A1A2 index for facts of arity >= 3.")
                            .create()));
            options.addOption(checkOption(
                    Option.builder(PRUNEDPREDICATE_CONST_OPTION)
                            .hasArgs()
                            .argName("exact[={y|n}]:prefix*")
                            .valueSeparator(':')
                            .desc("Specify predicates names that will be pruned by PruningIdDprProver, specified in " +
                                          "same format as fixedWeights")
                            .build()));
        }
        if (isOn(flags, USE_THREADS)) {
            options.addOption(checkOption(
                    OptionBuilder
                            .withLongOpt(THREADS_CONST_OPTION)
                            .withArgName("integer")
                            .hasArg()
                            .withDescription("Use x worker threads. (Pls ensure x < #cores)")
                            .create()));
        }
        if (isOn(flags, USE_EPOCHS)) {
            options.addOption(checkOption(
                    OptionBuilder
                            .withLongOpt(EPOCHS_CONST_OPTION)
                            .withArgName("integer")
                            .hasArg()
                            .withDescription("Use x training epochs (default = 5)")
                            .create()));
        }
        if (isOn(flags, USE_FORCE)) {
            options.addOption(checkOption(
                    OptionBuilder
                            .withLongOpt("force")
                            .withDescription("Ignore errors and run anyway")
                            .create()));
        }
        if (anyOn(flags, USE_APR)) {
            options.addOption(checkOption(
                    OptionBuilder
                            .withLongOpt(APR_CONST_OPTION)
                            .withArgName("options")
                            .hasArgs()
                            .withValueSeparator(':')
                            .withDescription("Pagerank options. Default: eps=1e-4:alph=0.1:depth=5\n"
                                                     + "Syntax: param=value:param=value...\n"
                                                     + "Available parameters:\n"
                                                     + "eps, alph, depth")
                            .create()));
        }
        if (isOn(flags, USE_ORDER)) {
            options.addOption(checkOption(
                    OptionBuilder
                            .withLongOpt(ORDER_CONST_OPTION)
                            .withArgName("o")
                            .hasArg()
                            .withDescription("Set ordering of outputs wrt inputs. Valid options:\n"
                                                     + "same, maintain (keep input ordering)\n"
                                                     + "anything else (reorder outputs to save time/memory)")
                            .create()
                                         ));
        }
        if (anyOn(flags, USE_DUPCHECK | USE_WAM)) {
            options.addOption(checkOption(
                    OptionBuilder
                            .withLongOpt(DUPCHECK_CONST_OPTION)
                            .withArgName("size")
                            .hasArg()
                            .withDescription("Default: " + duplicates + "\nCheck for duplicates, expecting <size> " +
                                                     "values. Increasing <size> is cheap.\n"
                                                     + "To turn off duplicate checking, set to -1.")
                            .create()));
        }
        if (isOn(flags, USE_THROTTLE)) {
            options.addOption(checkOption(
                    OptionBuilder
                            .withLongOpt(THROTTLE_CONST_OPTION)
                            .withArgName("integer")
                            .hasArg()
                            .withDescription("Default: -1\nPause buffering of new jobs if unfinished queue grows " +
                                                     "beyond x. -1 to disable.")
                            .create()));
        }
        if (isOn(flags, USE_EMPTYGRAPHS)) {
            options.addOption(checkOption(
                    OptionBuilder
                            .withLongOpt(EMPTYGRAPHS_CONST_OPTION)
                            .withDescription("Include examples with no pos or neg labeled solutions")
                            .create()));
        }
        if (isOn(flags, USE_FIXEDWEIGHTS)) {
            options.addOption(checkOption(
                    Option.builder()
                            .longOpt(FIXEDWEIGHTS_CONST_OPTION)
                            .hasArgs()
                            .argName("exact[={y|n}]:prefix*")
                            .valueSeparator(':')
                            .desc("Specify patterns of features to keep fixed at 1.0 or permit tuning. End in * for a" +
                                          " prefix, otherwise uses exact match. Fixed by default; specify '=n' to " +
                                          "permit tuning. First matching rule decides.")
                            .build()));
        }
        if (anyOn(flags, USE_SMART_COUNTFEATURES)) {
            options.addOption(checkOption(
                    Option.builder(COUNTFEATURES_CONST_OPTION)
                            .hasArg()
                            .argName("true|false")
                            .desc("Default: true\nTrack feature usage and tell me when I've e.g. run queries with the" +
                                          " wrong params file")
                            .build()));
        }

    }

    protected Option checkOption(Option o) {
        return o;
    }

    protected int modules(int[] flags) {
        return flags[3];
    }

    protected File getExistingFile(String filename) {
        File value = new File(filename);
        if (!value.exists()) { throw new IllegalArgumentException("File '" + value.getName() + "' must exist"); }
        return value;
    }

    protected void retrieveSettings(CommandLine line, int[] allFlags, Options options) throws IOException {
        int flags;

        if (line.hasOption("help")) { usageOptions(options, allFlags); }

        // input files: must exist already
        flags = inputFiles(allFlags);

        if (isOn(flags, USE_QUERIES) && line.hasOption(QUERIES_FILE_OPTION)) {
            this.queryFile = getExistingFile(line.getOptionValue(QUERIES_FILE_OPTION));
        }
        if (isOn(flags, USE_GROUNDED) && line.hasOption(GROUNDED_FILE_OPTION)) {
            this.groundedFile = getExistingFile(line.getOptionValue(GROUNDED_FILE_OPTION));
        }
        if (isOn(flags, USE_ANSWERS) && line.hasOption(SOLUTIONS_FILE_OPTION)) {
            this.solutionsFile = getExistingFile(line.getOptionValue(SOLUTIONS_FILE_OPTION));
        }
        if (isOn(flags, USE_TEST) && line.hasOption(TEST_FILE_OPTION)) {
            this.testFile = getExistingFile(line.getOptionValue(TEST_FILE_OPTION));
        }
        if (isOn(flags, USE_TRAIN) && line.hasOption(TRAIN_FILE_OPTION)) {
            this.queryFile = getExistingFile(line.getOptionValue(TRAIN_FILE_OPTION));
        }
        if (isOn(flags, USE_PARAMS) && line.hasOption(PARAMS_FILE_OPTION)) {
            this.paramsFile = getExistingFile(line.getOptionValue(PARAMS_FILE_OPTION));
        }
        if (isOn(flags, USE_INIT_PARAMS) && line.hasOption(INIT_PARAMS_FILE_OPTION)) {
            this.initParamsFile = getExistingFile(line.getOptionValue(INIT_PARAMS_FILE_OPTION));
        }
        if (isOn(flags, USE_GRADIENT) && line.hasOption(GRADIENT_FILE_OPTION)) {
            this.gradientFile = getExistingFile(line.getOptionValue(GRADIENT_FILE_OPTION));
        }

        // output & intermediate files: may not exist yet
        flags = outputFiles(allFlags);
        if (isOn(flags, USE_QUERIES) && line.hasOption(QUERIES_FILE_OPTION)) {
            this.queryFile = new File(line.getOptionValue(QUERIES_FILE_OPTION));
        }
        if (isOn(flags, USE_GROUNDED) && line.hasOption(GROUNDED_FILE_OPTION)) {
            this.groundedFile = new File(line.getOptionValue(GROUNDED_FILE_OPTION));
        }
        if (isOn(flags, USE_ANSWERS) && line.hasOption(SOLUTIONS_FILE_OPTION)) {
            this.solutionsFile = new File(line.getOptionValue(SOLUTIONS_FILE_OPTION));
        }
        if (isOn(flags, USE_TEST) && line.hasOption(TEST_FILE_OPTION)) {
            this.testFile = new File(line.getOptionValue(TEST_FILE_OPTION));
        }
        if (isOn(flags, USE_TRAIN) && line.hasOption(TRAIN_FILE_OPTION)) {
            this.queryFile = new File(line.getOptionValue(TRAIN_FILE_OPTION));
        }
        if (isOn(flags, USE_PARAMS) && line.hasOption(PARAMS_FILE_OPTION)) {
            this.paramsFile = new File(line.getOptionValue(PARAMS_FILE_OPTION));
        }
        if (isOn(flags, USE_GRADIENT) && line.hasOption(GRADIENT_FILE_OPTION)) {
            this.gradientFile = new File(line.getOptionValue(GRADIENT_FILE_OPTION));
        }

        // constants
        flags = constants(allFlags);
        if (isOn(flags, USE_WAM)) {
            if (line.hasOption(PROGRAMFILES_CONST_OPTION)) {
                this.programFiles = line.getOptionValues(PROGRAMFILES_CONST_OPTION);
            }
            if (line.hasOption(TERNARYINDEX_CONST_OPTION)) {
                this.ternaryIndex = Boolean.parseBoolean(line.getOptionValue(TERNARYINDEX_CONST_OPTION));
            }
            if (line.hasOption(PRUNEDPREDICATE_CONST_OPTION)) {
                this.prunedPredicateRules = new FixedWeightRules(line.getOptionValues(PRUNEDPREDICATE_CONST_OPTION));
            }
        }
        if (anyOn(flags, USE_APR)) {
            if (line.hasOption(APR_CONST_OPTION)) { this.apr = new APROptions(line.getOptionValues(APR_CONST_OPTION)); }
        }
        if (isOn(flags, USE_THREADS) && line.hasOption(THREADS_CONST_OPTION)) {
            this.nthreads = Integer.parseInt(line.getOptionValue(THREADS_CONST_OPTION));
        }
        if (isOn(flags, USE_EPOCHS) && line.hasOption(EPOCHS_CONST_OPTION)) {
            this.epochs = Integer.parseInt(line.getOptionValue(EPOCHS_CONST_OPTION));
        }
        if (isOn(flags, USE_FORCE) && line.hasOption(FORCE_CONST_OPTION)) { this.force = true; }
        if (isOn(flags, USE_ORDER) && line.hasOption(ORDER_CONST_OPTION)) {
            String order = line.getOptionValue(ORDER_CONST_OPTION);
            this.maintainOrder = order.equals("same") || order.equals("maintain");
        }
        if (anyOn(flags, USE_DUPCHECK | USE_WAM) && line.hasOption(DUPCHECK_CONST_OPTION)) {
            this.duplicates = (int) Double.parseDouble(line.getOptionValue(DUPCHECK_CONST_OPTION));
        }
        if (isOn(flags, USE_THROTTLE) && line.hasOption(THROTTLE_CONST_OPTION)) {
            this.throttle = Integer.parseInt(line.getOptionValue(THROTTLE_CONST_OPTION));
        }
        if (isOn(flags, USE_EMPTYGRAPHS) && line.hasOption(EMPTYGRAPHS_CONST_OPTION)) {
            this.includeEmptyGraphs = true;
        }
        if (isOn(flags, USE_FIXEDWEIGHTS) && line.hasOption(FIXEDWEIGHTS_CONST_OPTION)) {
            this.fixedWeightRules = new FixedWeightRules(line.getOptionValues(FIXEDWEIGHTS_CONST_OPTION));
        }
        if (anyOn(flags, USE_SMART_COUNTFEATURES)) {
            if (line.hasOption(COUNTFEATURES_CONST_OPTION)) {
                this.countFeatures = Boolean.parseBoolean(line.getOptionValue(COUNTFEATURES_CONST_OPTION));
            } else if (this.nthreads > 20) {
                log.warn("Large numbers of threads (>20, so " + this.nthreads + " qualifies) can cause a bottleneck " +
                                 "in FeatureDictWeighter. If you're " +
                                 "seeing lower system loads than expected and you're sure your examples/query/param " +
                                 "files are correct, you can reduce contention & increase speed performance by adding" +
                                 " " +
                                 "'--" + COUNTFEATURES_CONST_OPTION + " false' to your command line.");
            }
        }

        if (this.programFiles != null) { this.loadProgramFiles(line, allFlags, options); }
    }

    /**
     * Clears program and plugin list, then loads them from --programFiles option.
     *
     * @param flags
     * @param options
     * @throws IOException
     */
    protected void loadProgramFiles(CommandLine line, int[] flags, Options options) throws IOException {
        this.program = null;
        int nplugins = programFiles.length;
        for (String s : programFiles) { if (s.endsWith(".wam")) { nplugins--; } }
        this.plugins = new WamPlugin[nplugins];
        int i = 0;
        int wam, graph, facts;
        wam = graph = facts = 0;
        int iFacts = -1;
        for (String s : programFiles) {
            if (s.endsWith(".wam")) {
                if (this.program != null) {
                    usageOptions(options, flags, PROGRAMFILES_CONST_OPTION + ": Multiple WAM programs not supported");
                }
                this.program = WamBaseProgram.load(this.getExistingFile(s));
                wam++;
            } else if (i >= this.plugins.length) {
                usageOptions(options, flags, PROGRAMFILES_CONST_OPTION + ": Parser got very confused about how many " +
                        "plugins you specified. Send Katie a bug report!");
            } else if (s.endsWith(GraphlikePlugin.FILE_EXTENSION)) {
                this.plugins[i++] = LightweightGraphPlugin.load(this.apr, this.getExistingFile(s), this.duplicates);
                graph++;
            } else if (s.endsWith(FactsPlugin.FILE_EXTENSION)) {
                FactsPlugin p = FactsPlugin.load(this.apr, this.getExistingFile(s), this.ternaryIndex, this.duplicates);
                if (iFacts < 0) {
                    iFacts = i;
                    this.plugins[i++] = p;
                } else {
                    SplitFactsPlugin sf;
                    if (this.plugins[iFacts] instanceof FactsPlugin) {
                        sf = new SplitFactsPlugin(this.apr);
                        sf.add((FactsPlugin) this.plugins[iFacts]);
                        this.plugins[iFacts] = sf;
                    } else { sf = (SplitFactsPlugin) this.plugins[iFacts]; }
                    sf.add(p);
                }
                facts++;
            } else if (s.endsWith(SparseGraphPlugin.FILE_EXTENSION)) {
                this.plugins[i++] = SparseGraphPlugin.load(this.apr, this.getExistingFile(s));
            } else {
                usageOptions(options, flags, PROGRAMFILES_CONST_OPTION + ": Plugin type for " + s + " " +
                        "unsupported/unknown");
            }
        }
        if (facts > 1) { // trim array
            this.plugins = Arrays.copyOfRange(this.plugins, 0, i);
        }
        if (graph > 1) {
            log.warn("Consolidated graph files not yet supported! If the same functor exists in two files, facts in " +
                             "the later file will be hidden from the prover!");
        }
    }

    /**
     * Calls System.exit()
     */
    protected void usageOptions(Options options, int inputFile, int outputFile, int constants, int modules,
                                String msg) {
        usageOptions(options, new int[]{inputFile, outputFile, constants, modules}, msg);
    }

    /**
     * Calls System.exit()
     */
    protected void usageOptions(Options options, int[] flags, String msg) {
        HelpFormatter formatter = new HelpFormatter();
        int width = 74;

        String swidth = System.getenv("COLUMNS");
        if (swidth != null) {
            try {
                width = Integer.parseInt(swidth);
            } catch (NumberFormatException e) {}
        }
        //        formatter.setWidth(width);
        //        formatter.setLeftPadding(0);
        //        formatter.setDescPadding(2);
        StringBuilder syntax = new StringBuilder();
        constructUsageSyntax(syntax, flags);
        String printMsg = "";
        if (msg != null) { printMsg = ("\nBAD USAGE:\n" + msg + "\n"); }
        //        formatter.printHelp(syntax.toString(), options);
        PrintWriter pw = new PrintWriter(System.err);
        formatter.printHelp(pw, width, syntax.toString(), "", options, 0, 2, printMsg);
        pw.write("\n");
        pw.flush();
        pw.close();
        int stat = msg != null ? 1 : 0;
        System.exit(stat);
    }

    protected void constructUsageSyntax(StringBuilder syntax, int[] allFlags) {
        int flags;

        //input files
        flags = inputFiles(allFlags);
        if (isOn(flags, USE_QUERIES)) { syntax.append(" --").append(QUERIES_FILE_OPTION).append(" inputFile"); }
        if (isOn(flags, USE_GROUNDED)) {
            syntax.append(" --").append(GROUNDED_FILE_OPTION).append(" inputFile.grounded");
        }
        if (isOn(flags, USE_ANSWERS)) { syntax.append(" --").append(SOLUTIONS_FILE_OPTION).append(" inputFile"); }
        if (isOn(flags, USE_TRAIN)) { syntax.append(" --").append(TRAIN_FILE_OPTION).append(" inputFile"); }
        if (isOn(flags, USE_TEST)) { syntax.append(" --").append(TEST_FILE_OPTION).append(" inputFile"); }
        if (isOn(flags, USE_PARAMS)) { syntax.append(" --").append(PARAMS_FILE_OPTION).append(" params.wts"); }
        if (isOn(flags, USE_INIT_PARAMS)) {
            syntax.append(" --").append(INIT_PARAMS_FILE_OPTION).append(" initParams.wts");
        }

        //output files
        flags = outputFiles(allFlags);
        if (isOn(flags, USE_QUERIES)) { syntax.append(" --").append(QUERIES_FILE_OPTION).append(" outputFile"); }
        if (isOn(flags, USE_GROUNDED)) {
            syntax.append(" --").append(GROUNDED_FILE_OPTION).append(" outputFile.grounded");
        }
        if (isOn(flags, USE_ANSWERS)) { syntax.append(" --").append(SOLUTIONS_FILE_OPTION).append(" outputFile"); }
        if (isOn(flags, USE_TRAIN)) { syntax.append(" --").append(TRAIN_FILE_OPTION).append(" outputFile"); }
        if (isOn(flags, USE_TEST)) { syntax.append(" --").append(TEST_FILE_OPTION).append(" outputFile"); }
        if (isOn(flags, USE_PARAMS)) { syntax.append(" --").append(PARAMS_FILE_OPTION).append(" params.wts"); }
        if (isOn(flags, USE_GRADIENT)) { syntax.append(" --").append(GRADIENT_FILE_OPTION).append(" gradient.dwts"); }

        //constants
        flags = constants(allFlags);
        if (isOn(flags, USE_WAM)) {
            syntax.append(" --").append(PROGRAMFILES_CONST_OPTION).append(" file.wam:file.cfacts:file.graph");
        }
        if (isOn(flags, USE_WAM)) { syntax.append(" [--").append(TERNARYINDEX_CONST_OPTION).append(" true|false]"); }
        if (isOn(flags, USE_WAM)) {
            syntax.append(" [--").append(PRUNEDPREDICATE_CONST_OPTION).append(" predicate1:predicate2]");
        }
        if (isOn(flags, USE_THREADS)) { syntax.append(" [--").append(THREADS_CONST_OPTION).append(" integer]"); }
        if (isOn(flags, USE_EPOCHS)) { syntax.append(" [--").append(EPOCHS_CONST_OPTION).append(" integer]"); }
        if (isOn(flags, USE_FORCE)) { syntax.append(" [--").append(FORCE_CONST_OPTION).append("]"); }
        if (isOn(flags, USE_ORDER)) { syntax.append(" [--").append(ORDER_CONST_OPTION).append(" same|reorder]"); }
        if (anyOn(flags, USE_DUPCHECK | USE_WAM)) {
            syntax.append(" [--").append(DUPCHECK_CONST_OPTION).append(" -1|integer]");
        }
        if (isOn(flags, USE_THROTTLE)) { syntax.append(" [--").append(THROTTLE_CONST_OPTION).append(" integer]"); }
        if (isOn(flags, USE_EMPTYGRAPHS)) { syntax.append(" [--").append(EMPTYGRAPHS_CONST_OPTION).append("]"); }
        if (isOn(flags, USE_FIXEDWEIGHTS)) {
            syntax.append(" [--").append(FIXEDWEIGHTS_CONST_OPTION).append(" featureA:featureB()]");
        }
        if (anyOn(flags, USE_SMART_COUNTFEATURES)) {
            syntax.append(" [--").append(COUNTFEATURES_CONST_OPTION).append(" true|false]");
        }
    }

    protected int inputFiles(int[] flags) {
        return flags[0];
    }

    static boolean isOn(int flags, int flag) {
        return (flags & flag) == flag;
    }

    protected int outputFiles(int[] flags) {
        return flags[1];
    }

    protected int constants(int[] flags) {
        return flags[2];
    }

    static boolean anyOn(int flags, int flag) {
        return (flags & flag) > 0;
    }

    /**
     * Calls System.exit()
     */
    protected void usageOptions(Options options, int[] flags) {
        usageOptions(options, flags, null);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder("\n");
        String n = this.getClass().getCanonicalName();
        if (n == null) { sb.append("(custom configurator)"); } else { sb.append(n); }
        displayFile(sb, QUERIES_FILE_OPTION, queryFile);
        displayFile(sb, TEST_FILE_OPTION, testFile);
        displayFile(sb, GROUNDED_FILE_OPTION, groundedFile);
        displayFile(sb, INIT_PARAMS_FILE_OPTION, initParamsFile);
        displayFile(sb, PARAMS_FILE_OPTION, paramsFile);
        displayFile(sb, SOLUTIONS_FILE_OPTION, solutionsFile);
        displayFile(sb, GRADIENT_FILE_OPTION, gradientFile);
        if (!maintainOrder) { display(sb, "Output order", "reordered"); }
        if (this.programFiles != null) {
            display(sb, "Duplicate checking", duplicates > 0 ? ("up to " + duplicates) : "off");
        }
        display(sb, THREADS_CONST_OPTION, nthreads);
        return sb.toString();
    }

    private void displayFile(StringBuilder sb, String name, File f) {
        if (f != null) {
            sb.append("\n")
                    .append(String.format("%" + (FORMAT_WIDTH - 5) + "s file: ", name))
                    .append(f.getPath());
        }
    }

    private void display(StringBuilder sb, String name, Object value) {
        sb.append("\n")
                .append(String.format("%" + (FORMAT_WIDTH) + "s: %s", name, value.toString()));
    }

    protected String[] combinedArgs(String[] origArgs) {
        // if the user specified a properties file, add those values at the beginning
        // (so that command line args override them)
        if (System.getProperty(PROPFILE) != null) {
            String[] propArgs = fakeCommandLine(System.getProperty(PROPFILE));
            String[] args = new String[origArgs.length + propArgs.length];
            int i = 0;
            for (int j = 0; j < propArgs.length; j++) { args[i++] = propArgs[j]; }
            for (int j = 0; j < origArgs.length; j++) { args[i++] = origArgs[j]; }
            return args;
        }
        return origArgs;
    }

    protected String[] fakeCommandLine(String propsFile) {
        Properties props = new Properties();
        try {
            props.load(new BufferedReader(new FileReader(propsFile)));
            return fakeCommandLine(props);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(e);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected String[] fakeCommandLine(Properties props) {
        StringBuilder sb = new StringBuilder();
        for (String name : props.stringPropertyNames()) {
            sb.append(" --").append(name);
            if (props.getProperty(name) != null && !props.getProperty(name).equals("")) {
                sb.append(" ").append(props.getProperty(name));
            }
        }
        return sb.substring(1).split("\\s");
    }
}
