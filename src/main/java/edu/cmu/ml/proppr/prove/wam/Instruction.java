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

package edu.cmu.ml.proppr.prove.wam;

public class Instruction {

    public final OP opcode;
    public final int i1, i2;
    public final String s;

    /**
     * comment, returnp, fclear, freport
     */
    public Instruction(OP o) {
        this.opcode = o;
        i1 = i2 = 0;
        s = null;
    }

    /**
     * allocate,pushfreevar, pushboundvar, fpushboundvar, ffindall
     */
    public Instruction(OP o, int i) {
        this.opcode = o;
        this.i1 = i;
        i2 = 0;
        s = null;
    }

    /**
     * callp, pushconst, fpushconst
     */
    public Instruction(OP o, String s) {
        this.opcode = o;
        this.s = s;
        i1 = i2 = 0;
    }

    /**
     * initfreevar, unifyboundvar
     */
    public Instruction(OP o, int i1, int i2) {
        this.opcode = o;
        this.i1 = i1;
        this.i2 = i2;
        s = null;
    }

    /**
     * unifyconst, fpushstart
     */
    public Instruction(OP o, String s, int i) {
        this.opcode = o;
        this.s = s;
        this.i1 = i;
        i2 = 0;
    }

    public static Instruction parseInstruction(String line) {
        String[] parts = line.split("\t", 2);
        String[] args;
        if (parts.length < 2) {
            args = new String[0];
        } else {
            args = parts[1].split("\t");
        }
        switch (OP.valueOf(parts[0])) {
            case comment:
                return new Instruction(OP.comment);
            case allocate:
                return new Instruction(OP.allocate, Integer.parseInt(args[0]));
            case callp:
                return new Instruction(OP.callp, args[0]);
            case returnp:
                return new Instruction(OP.returnp);
            case pushconst:
                return new Instruction(OP.pushconst, args[0]);
            case pushfreevar:
                return new Instruction(OP.pushfreevar, Integer.parseInt(args[0]));
            case pushboundvar:
                return new Instruction(OP.pushboundvar, Integer.parseInt(args[0]));
            case unifyconst:
                return new Instruction(OP.unifyconst, args[0], Integer.parseInt(args[1]));
            case initfreevar:
                return new Instruction(OP.initfreevar, Integer.parseInt(args[0]), Integer.parseInt(args[1]));
            case unifyboundvar:
                return new Instruction(OP.unifyboundvar, Integer.parseInt(args[0]), Integer.parseInt(args[1]));
            case fclear:
                return new Instruction(OP.fclear);
            case fpushstart:
                return new Instruction(OP.fpushstart, args[0], Integer.parseInt(args[1]));
            case fpushconst:
                return new Instruction(OP.fpushconst, args[0]);
            case fpushboundvar:
                return new Instruction(OP.fpushboundvar, Integer.parseInt(args[0]));
            case fpushweight:
                return new Instruction(OP.fpushweight);
            case freport:
                return new Instruction(OP.freport);
            case ffindall:
                return new Instruction(OP.ffindall, Integer.parseInt(args[0]));
        }
        throw new UnsupportedOperationException("No known instruction '" + parts[2] + "'");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.opcode.toString()).append("(");
        switch (this.opcode) {
            case pushfreevar:
            case pushboundvar:
            case fpushboundvar:
            case ffindall:
            case allocate:
                sb.append(i1);
                break;
            case fpushconst:
            case callp:
            case pushconst:
                sb.append(s);
                break;
            case unifyconst:
            case fpushstart:
                sb.append(s).append(",").append(i1);
                break;
            case initfreevar:
            case unifyboundvar:
                sb.append(i1).append(",").append(i2);
                break;
        }
        sb.append(")");
        return sb.toString();
    }

    public enum OP {
        comment,
        allocate,
        callp,
        returnp,
        pushconst,
        pushfreevar,
        pushboundvar,
        initfreevar,
        fclear(true),
        fpushstart(true),
        fpushconst(true),
        fpushboundvar(true),
        freport(true),
        ffindall(true),
        fpushweight(true),
        unifyconst,
        unifyboundvar;
        private final boolean feature;

        OP() {
            this(false);
        }

        OP(boolean isFeature) {
            this.feature = isFeature;
        }

        public boolean isFeature() {
            return this.feature;
        }
    }
}
