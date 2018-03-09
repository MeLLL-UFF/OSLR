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
