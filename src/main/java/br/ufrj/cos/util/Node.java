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

package br.ufrj.cos.util;

import java.util.Collection;
import java.util.HashSet;

/**
 * Class to manage a tree data structure.
 * <p>
 * Created on 14/06/17.
 *
 * @author Victor Guimarães
 */
@SuppressWarnings("ClassIndependentOfModule")
public class Node<E> {

    protected final Node<E> parent;
    protected final Node<E> defaultChild;
    protected final Collection<Node<E>> children;
    protected final E element;

    /**
     * Constructor with all parameters.
     *
     * @param parent       the parent
     * @param defaultChild the default child
     * @param element      the element stored in the node
     */
    protected Node(Node<E> parent, E defaultChild, E element) {
        this.parent = parent;
        this.defaultChild = new Node<>(this, defaultChild);
        this.children = new HashSet<>();
        this.element = element;
    }

    /**
     * Constructor for the default child.
     *
     * @param parent       the parent
     * @param childElement the element
     */
    protected Node(Node<E> parent, E childElement) {
        this.parent = parent;
        this.defaultChild = null;
        this.children = null;
        this.element = childElement;
    }

    /**
     * Builds a new tree.
     *
     * @param element      the element
     * @param defaultChild the default child
     * @param <E>          the type of the element
     * @return the root of the tree
     */
    public static <E> Node<E> newTree(E defaultChild, E element) {
        return new Node<>(null, defaultChild, element);
    }

    /**
     * Adds the children element to the parent node.
     *
     * @param parent       the parent
     * @param defaultChild the default child
     * @param child        the child's element
     * @param <E>          the element type
     * @return the node representation of the child if succeeds, {@code null} otherwise
     */
    public static <E> Node<E> addChildToNode(Node<E> parent, E defaultChild, E child) {
        Node<E> childNode = new Node<>(parent, defaultChild, child);
        if (parent.children != null && parent.children.add(childNode)) {
            return childNode;
        } else {
            return null;
        }
    }

    /**
     * Removes the child node from the tree.
     *
     * @param child the child node
     * @param <E>   the element type
     * @return {@code true} if the remove changes the tree, {@code false} otherwise
     */
    public static <E> boolean removeChildFromNode(Node<E> child) {
        return child.parent != null && child.parent.children != null && child.parent.children.remove(child);
    }

    /**
     * Gets the parent.
     *
     * @return the parent
     */
    public Node<E> getParent() {
        return parent;
    }

    /**
     * Gets the default child.
     *
     * @return the default child
     */
    public Node<E> getDefaultChild() {
        return defaultChild;
    }

    /**
     * Gets the children.
     *
     * @return the default children
     */
    public Collection<Node<E>> getChildren() {
        return children;
    }

    /**
     * Gets the element
     *
     * @return the element
     */
    public E getElement() {
        return element;
    }

    @Override
    public int hashCode() {
        int result = 31;
        if (parent == null) {
            return result;
        }
        result = 31 * parent.hashCode();
//        result = 31 * result + children.hashCode();
        result = 31 * result + (element != null ? element.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof Node)) { return false; }

        Node<?> node = (Node<?>) o;

        if (parent != null ? !parent.equals(node.parent) : node.parent != null) { return false; }
//        if (!children.equals(node.children)) { return false; }
        return element != null ? element.equals(node.element) : node.element == null;
    }

}
