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

package br.ufrj.cos.knowledge.manager;

import br.ufrj.cos.core.LearningSystem;
import br.ufrj.cos.knowledge.example.AtomExample;
import br.ufrj.cos.knowledge.example.Example;
import br.ufrj.cos.knowledge.example.ProPprExample;
import br.ufrj.cos.logic.Atom;
import br.ufrj.cos.logic.HornClause;
import br.ufrj.cos.util.ExceptionMessages;
import br.ufrj.cos.util.InitializationException;
import br.ufrj.cos.util.LanguageUtils;
import br.ufrj.cos.util.LogMessages;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages the examples by putting them in a tree structure based on the theory.
 * <p>
 * Created on 16/06/17.
 *
 * @author Victor Guimarães
 */
public class TreeExampleManager extends IncomingExampleManager {

    /**
     * The logger
     */
    public static final Logger logger = LogManager.getLogger();

    protected TreeTheory treeTheory;

    /**
     * Splits the example into its covered and not covered parts, based on the coveredAtoms.
     * <p>
     * Use the methods {@link #getCoveredExampleFromSplit(Pair)} and {@link #getNotCoveredExampleFromSplit(Pair)} to
     * retrieve the parts.
     *
     * @param example      the example
     * @param coveredAtoms the covered atom
     * @return a pair with the not covered and covered part
     */
    protected static Pair<ProPprExample, ProPprExample> splitCoveredExamples(Example example, Set<Atom> coveredAtoms) {
        List<AtomExample> covered = new ArrayList<>();
        List<AtomExample> notCovered = new ArrayList<>();
        Atom goal = example.getGoalQuery();
        for (AtomExample grounded : example.getGroundedQuery()) {
            (coveredAtoms.contains(grounded.getAtom()) ? covered : notCovered).add(grounded);
        }

        return new ImmutablePair<>(new ProPprExample(goal, notCovered), new ProPprExample(goal, covered));
    }

    /**
     * Gets the not covered part from a {@link Pair} of {@link Example} splitted by
     * {@link #splitCoveredExamples(Example, Set)}.
     *
     * @param split the {@link Pair} of {@link Example} splitted by {@link #splitCoveredExamples(Example, Set)}
     * @return the uncovered part
     */
    protected static ProPprExample getNotCoveredExampleFromSplit(Pair<ProPprExample, ?> split) {
        return split.getLeft();
    }

    /**
     * Gets the covered part from a {@link Pair} of {@link Example} splitted by
     * {@link #splitCoveredExamples(Example, Set)}.
     *
     * @param split the {@link Pair} of {@link Example} splitted by {@link #splitCoveredExamples(Example, Set)}
     * @return the covered part
     */
    protected static ProPprExample getCoveredExampleFromSplit(Pair<?, ProPprExample> split) {
        return split.getRight();
    }

    /**
     * Adds the example to the leaf.
     *
     * @param leaf             the leaf
     * @param exampleFromSplit the example from split
     * @param modifiedLeaves   the set of modified leaves
     * @param leafExamples     the map of examples per leaves
     * @return {@code true} if this operation changes the set of modified list, {@code false} otherwise
     */
    protected boolean addExamplesToLeaf(Node<HornClause> leaf, ProPprExample exampleFromSplit,
                                        Set<Node<HornClause>> modifiedLeaves,
                                        Map<Node<HornClause>, Set<Example>> leafExamples) {
        if (!exampleFromSplit.getGroundedQuery().isEmpty()) {
            leafExamples.computeIfAbsent(leaf, e -> new HashSet<>()).add(exampleFromSplit);
            learningSystem.getExamples().add(exampleFromSplit);
            return modifiedLeaves.add(leaf);
        }

        return false;
    }

    @Override
    public void initialize() throws InitializationException {
        super.initialize();
        if (treeTheory == null) {
            throw new InitializationException(
                    ExceptionMessages.errorFieldsSet(this, TreeTheory.class.getSimpleName()));
        }
        treeTheory.initialize();
    }

    @Override
    public void incomingExamples(Collection<? extends Example> examples) {
        Map<String, Set<Node<HornClause>>> modifiedLeaves = placeIncomingExamples(examples);
        logger.debug(LogMessages.CALLING_REVISION_ON_EXAMPLES.toString(), learningSystem.getExamples().size());
        callRevision(modifiedLeaves);
    }

    /**
     * Places the incoming examples into the correct leaves and returns the set of the modified leaves.
     *
     * @param examples the examples
     * @return the leaves which was modified due to the addition of examples
     */
    protected Map<String, Set<Node<HornClause>>> placeIncomingExamples(Iterable<? extends Example> examples) {
        Map<String, Set<Node<HornClause>>> modifiedLeavesMap = new HashMap<>();
        Node<HornClause> root;
        Set<Atom> coveredExamples;
        Pair<ProPprExample, ProPprExample> split;
        String predicate;
        for (Example example : examples) {
            predicate = LanguageUtils.getPredicateFromAtom(example.getGoalQuery());
            Set<Node<HornClause>> modifiedLeaves = modifiedLeavesMap.computeIfAbsent(predicate, e -> new HashSet<>());
            Map<Node<HornClause>, Set<Example>> leafExamples = treeTheory.getLeafExampleMapFromTree(predicate);
            root = treeTheory.getTreeForExample(example, predicate);

            coveredExamples = transverseTheoryTree(root, example, modifiedLeaves, leafExamples);
            split = splitCoveredExamples(example, coveredExamples);
            addExamplesToLeaf(root.getDefaultChild(), getNotCoveredExampleFromSplit(split),
                              modifiedLeaves, leafExamples);
        }
        return modifiedLeavesMap;
    }

    /**
     * Transverses the theory tree passing the covered example to the respective sons and repeating the process for
     * each son. All the leaves modified by the process will be appended to the modifiedLeaves set.
     *
     * @param root           the root of the tree
     * @param example        the covered example at the root level, already evaluated by the root node
     * @param modifiedLeaves the set of modified leaves
     * @param leafExamples   the map of examples of the leaves, given the predicate
     * @return a set of covered atoms by the root
     */
    protected Set<Atom> transverseTheoryTree(Node<HornClause> root, Example example,
                                             Set<Node<HornClause>> modifiedLeaves,
                                             Map<Node<HornClause>, Set<Example>> leafExamples) {
        Map<Example, Map<Atom, Double>>
                inferred = learningSystem.inferExamples(root.getElement(), example.getGroundedQuery());
        Set<Atom> coveredExamples = inferred.values().stream().
                flatMap(e -> e.keySet().stream()).collect(Collectors.toSet());
        Pair<ProPprExample, ProPprExample> split = splitCoveredExamples(example, coveredExamples);

        ProPprExample coveredExampleFromSplit = getCoveredExampleFromSplit(split);
        if (root.getChildren().isEmpty()) {
            addExamplesToLeaf(root, coveredExampleFromSplit, modifiedLeaves, leafExamples);
        } else {
            pushExampleToChild(root, coveredExampleFromSplit, modifiedLeaves, leafExamples);
        }
        return coveredExamples;
    }

    /**
     * Pushes the examples to the node, if the node has more children, recursively pushes to its children as well.
     *
     * @param node           the node
     * @param example        the example
     * @param modifiedLeaves the modifiedLeaves set to save the modified leaves
     * @param leafExamples   the leaf examples map to save the examples of each leaf
     */
    protected void pushExampleToChild(Node<HornClause> node, Example example, Set<Node<HornClause>> modifiedLeaves,
                                      Map<Node<HornClause>, Set<Example>> leafExamples) {
        Set<Atom> allCoveredExamples = new HashSet<>();
        Set<Atom> coveredExamples;
        Pair<ProPprExample, ProPprExample> split;
        for (Node<HornClause> child : node.getChildren()) {
            coveredExamples = transverseTheoryTree(child, example, modifiedLeaves, leafExamples);
            allCoveredExamples.addAll(coveredExamples);
        }
        split = splitCoveredExamples(example, allCoveredExamples);
        addExamplesToLeaf(node.getDefaultChild(), getNotCoveredExampleFromSplit(split), modifiedLeaves, leafExamples);
    }

    /**
     * Labels each modified leaf as the revision point and call the {@link LearningSystem} for a revision. It is not
     * guaranteed that the revision will occur.
     *
     * @param modifiedLeaves the modified leaves
     */
    protected void callRevision(Map<String, Set<Node<HornClause>>> modifiedLeaves) {
        List<Set<Example>> targets;
        for (Map.Entry<String, Set<Node<HornClause>>> entry : modifiedLeaves.entrySet()) {
            treeTheory.revisionLeaves = new ArrayList<>();
            targets = new ArrayList<>();
            for (Node<HornClause> leaf : entry.getValue()) {
                Set<Example> target = treeTheory.getExampleFromLeaf(entry.getKey(), leaf);
                if (target != null && !target.isEmpty()) {
                    targets.add(target);
                    treeTheory.revisionLeaves.add(leaf);
                }
            }
            learningSystem.reviseTheory(targets);
        }
    }

    /**
     * Gets the tree theory.
     *
     * @return tree theory
     */
    public TreeTheory getTreeTheory() {
        return treeTheory;
    }

    /**
     * Sets the tree theory.
     *
     * @param treeTheory tree theory
     * @throws InitializationException if the {@link TreeTheory} is already set
     */
    public void setTreeTheory(TreeTheory treeTheory) throws InitializationException {
        if (this.treeTheory != null) {
            throw new InitializationException(
                    LanguageUtils.formatLogMessage(ExceptionMessages.ERROR_RESET_FIELD_NOT_ALLOWED.toString(),
                                                   TreeTheory.class.getSimpleName()));
        }
        this.treeTheory = treeTheory;
    }

}
