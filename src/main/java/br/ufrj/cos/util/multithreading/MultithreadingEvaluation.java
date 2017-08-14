/*
 * Probabilist Logic Learner is a system to learn probabilistic logic
 * programs from data and use its learned programs to make inference
 * and answer queries.
 *
 * Copyright (C) 2017 Victor Guimarães
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

package br.ufrj.cos.util.multithreading;

import br.ufrj.cos.core.LearningSystem;
import br.ufrj.cos.knowledge.example.Example;
import br.ufrj.cos.knowledge.theory.evaluation.AsyncTheoryEvaluator;
import br.ufrj.cos.knowledge.theory.evaluation.metric.TheoryMetric;
import br.ufrj.cos.logic.HornClause;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import static br.ufrj.cos.util.log.InferenceLog.*;

/**
 * Class to handle the multithreading evaluation of candidate revision.
 * <p>
 * Created on 24/06/17.
 *
 * @author Victor Guimarães
 */
public class MultithreadingEvaluation<V, E> {

    /**
     * The logger
     */
    public static final Logger logger = LogManager.getLogger();
    /**
     * The default value for numberOfThreads.
     */
    public static final int DEFAULT_NUMBER_OF_THREADS = 1;
    /**
     * The default value for evaluationTimeout.
     */
    public static final int DEFAULT_EVALUATION_TIMEOUT = 300;
    protected final LearningSystem learningSystem;
    protected final TheoryMetric theoryMetric;
    protected final int evaluationTimeout;
    protected final AsyncEvaluatorTransformer<V, E> transformer;
    /**
     * The maximum number of threads this class is allowed to create.
     */
    public int numberOfThreads = DEFAULT_NUMBER_OF_THREADS;

    /**
     * Constructor with necessary parameters.
     *
     * @param learningSystem    the learning system
     * @param theoryMetric      the internal metric
     * @param evaluationTimeout the evaluation time out
     * @param transformer       the transformer
     */
    public MultithreadingEvaluation(LearningSystem learningSystem,
                                    TheoryMetric theoryMetric, int evaluationTimeout,
                                    AsyncEvaluatorTransformer<V, E> transformer) {
        this.learningSystem = learningSystem;
        this.theoryMetric = theoryMetric;
        this.evaluationTimeout = evaluationTimeout;
        this.transformer = transformer;
    }

    /**
     * Submits one evaluator {@link HornClause} to the pool and returns its {@link Future} value.
     *
     * @param evaluator      the evaluator {@link HornClause}
     * @param evaluationPool the pool
     * @return the {@link Future} value
     */
    public static <E> Future<AsyncTheoryEvaluator<E>> submitCandidate(AsyncTheoryEvaluator<E> evaluator,
                                                                      ExecutorService evaluationPool) {
        try {
            return evaluationPool.submit((Callable<AsyncTheoryEvaluator<E>>) evaluator);
        } catch (Exception e) {
            logger.error(ERROR_EVALUATING_CLAUSE.toString(), e);
        }
        return null;
    }

    /**
     * Submits the candidate {@link HornClause}s to the evaluation pool.
     *
     * @param candidates     the candidates
     * @param evaluationPool the pool
     * @param examples       the examples
     * @return the {@link Set} of {@link Future} evaluations.
     */
    protected Set<Future<AsyncTheoryEvaluator>> submitCandidates(Iterable<? extends V> candidates,
                                                                 ExecutorService evaluationPool,
                                                                 Collection<? extends Example> examples) {
        Set<Future<AsyncTheoryEvaluator>> futures = new LinkedHashSet<>();
        AsyncTheoryEvaluator evaluator;
        for (V candidate : candidates) {
            evaluator = new AsyncTheoryEvaluator(examples,
                                                 learningSystem.getTheoryEvaluator(),
                                                 theoryMetric, evaluationTimeout);
            evaluator = transformer.transform(evaluator, candidate);
            futures.add(MultithreadingEvaluation.submitCandidate(evaluator, evaluationPool));
        }
        futures.remove(null);
        return futures;
    }

    /**
     * Evaluates the candidate clauses against the metric, and returns the best evaluated {@link HornClause}.
     * <p>
     * Performs the evaluation in parallel, using {@link #numberOfThreads} threads.
     *
     * @param candidates the candidate clauses
     * @param examples   the examples
     * @return the best evaluated {@link HornClause}
     */
    public AsyncTheoryEvaluator getBestClausesFromCandidates(Collection<? extends V> candidates,
                                                             Collection<? extends Example> examples) {
        if (candidates == null || candidates.isEmpty()) { return null; }
        AsyncTheoryEvaluator bestClause = null;
        int numberOfThreads = Math.max(Math.min(this.numberOfThreads, candidates.size()), 1);
        try {
            logger.info(BEGIN_ASYNC_EVALUATION.toString(), candidates.size());
            ExecutorService evaluationPool = Executors.newFixedThreadPool(numberOfThreads);
            Set<Future<AsyncTheoryEvaluator>> futures = submitCandidates(candidates, evaluationPool, examples);

            evaluationPool.shutdown();
            evaluationPool.awaitTermination((int) (evaluationTimeout * (futures.size() + 1.0) / numberOfThreads),
                                            TimeUnit.SECONDS);
            evaluationPool.shutdownNow();
            logger.info(END_ASYNC_EVALUATION);
            bestClause = retrieveEvaluatedMetrics(futures, null);
        } catch (InterruptedException e) {
            logger.error(ERROR_EVALUATING_CLAUSE.toString(), e);
        }
        return bestClause;
    }

    /**
     * Retrieves the evaluations from the {@link Future} {@link AsyncTheoryEvaluator}s and appends it to a
     * {@link Map}. Also, returns the best evaluated {@link HornClause}.
     *
     * @param futures       the {@link Future} {@link AsyncTheoryEvaluator}
     * @param evaluationMap the {@link Map} with the evaluations
     * @return the best evaluated {@link HornClause}
     */
    @SuppressWarnings("SameParameterValue")
    public AsyncTheoryEvaluator retrieveEvaluatedMetrics(Set<Future<AsyncTheoryEvaluator>> futures,
                                                         Map<HornClause, Double> evaluationMap) {
        AsyncTheoryEvaluator evaluated;
        double bestClauseValue = theoryMetric.getDefaultValue();
        AsyncTheoryEvaluator bestClause = null;
        int count = 0;
        for (Future<AsyncTheoryEvaluator> future : futures) {
            try {
                evaluated = future.get();
                if (!evaluated.isEvaluationFinished()) { continue; }
                count++;
                if (evaluationMap != null) {
                    evaluationMap.put(evaluated.getHornClause(), evaluated.getEvaluation());
                }
                if (theoryMetric.compare(evaluated.getEvaluation(), bestClauseValue) >= 0) {
                    bestClauseValue = evaluated.getEvaluation();
                    bestClause = evaluated;
                }
            } catch (InterruptedException | ExecutionException e) {
                logger.error(ERROR_EVALUATING_CLAUSE.toString(), e);
            } catch (CancellationException ignored) {
                logger.error(EVALUATION_THEORY_TIMEOUT.toString(),
                             evaluationTimeout);
            }
        }
        logger.debug(EVALUATED_TIMEOUT_PROPORTION.toString(),
                     (double) count / futures.size() * 100, futures.size());
        return bestClause;
    }

}
