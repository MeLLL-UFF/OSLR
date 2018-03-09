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

package edu.cmu.ml.proppr.util.multithreading;

import java.util.concurrent.ThreadFactory;

public class NamedThreadFactory implements ThreadFactory {
	int next=1;
	String name;
	public NamedThreadFactory(String name) {
		this.name = name;
	}
	@Override
	public Thread newThread(Runnable r) {
		return new Thread(r, name+next++);
	}
	public void reset() {
		this.next=1;
	}
}
