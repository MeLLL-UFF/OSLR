/*
 * Online Structure Learner by Revision (OSLR) is an online relational
 * learning algorithm that can handle continuous, open-ended
 * streams of relational examples as they arrive. We employ
 * techniques from theory revision to take advantage of the already
 * acquired knowledge as a starting point, find where it should be
 * modified to cope with the new examples, and automatically update it.
 * We rely on the Hoeffding's bound statistical theory to decide if the
 * model must in fact be updated accordingly to the new examples.
 * The system is built upon ProPPR statistical relational language to
 * describe the induced models, aiming at contemplating the uncertainty
 * inherent to real data.
 *
 * Copyright (C) 2017-2018 Victor Guimarães
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

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.cmu.ml.proppr.util;

public class StatusLogger {

    private static final int DEFAULT_PERIOD_MS = 3000;
    private final int period_ms;
    private long start, last;

    public StatusLogger() {
        this(DEFAULT_PERIOD_MS);
        this.start();
    }

    public StatusLogger(int p) {
        this.period_ms = p;
    }

    public void start() {
        this.start = this.last = System.currentTimeMillis();
    }

    public boolean due() {
        return due(0);
    }

    public boolean due(int level) {
        long now = System.currentTimeMillis();
        boolean ret = now - last > Math.exp(level) * period_ms;
        if (ret) { last = now; }
        return ret;
    }

    public long sinceLast() {
        return since(last);
    }

    public long since(long t) {
        return System.currentTimeMillis() - t;
    }

    public long sinceStart() {
        return since(start);
    }

    public long tick() {
        return last = System.currentTimeMillis();
    }
}
