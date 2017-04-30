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

package br.ufrj.cos.logic;

import br.ufrj.cos.util.LanguageUtils;

/**
 * Represents a constant {@link Term}.
 * <p>
 * Created on 14/04/17.
 *
 * @author Victor Guimarães
 */
public class Constant extends Term {

    /**
     * Constructs a constant with name
     *
     * @param name the name
     */
    public Constant(String name) {
        super(name);
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Constant)) {
            return false;
        }

        Constant constant = (Constant) o;

        return name.equals(constant.name);
    }

    @Override
    public String toString() {
        if (LanguageUtils.doesNameContainsSpecialCharacters(name)) {
            return LanguageUtils.surroundConstant(name);
        }

        return name;
    }
}