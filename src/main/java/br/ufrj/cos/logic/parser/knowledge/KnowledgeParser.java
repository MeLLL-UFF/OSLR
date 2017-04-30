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

/* KnowledgeParser.java */
/* Generated By:JavaCC: Do not edit this line. KnowledgeParser.java */

package br.ufrj.cos.logic.parser.knowledge;

import br.ufrj.cos.logic.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created on 15/04/17.
 *
 * @author Victor Guimarães
 */
public class KnowledgeParser implements KnowledgeParserConstants {

    static private int[] jj_la1_0;

    static {
        jj_la1_init_0();
    }

    final private int[] jj_la1 = new int[15];
    /**
     * Generated Token Manager.
     */
    public KnowledgeParserTokenManager token_source;
    /**
     * Current token.
     */
    public Token token;
    /**
     * Next token.
     */
    public Token jj_nt;
    SimpleCharStream jj_input_stream;
    private int jj_ntk;
    private int jj_gen;
    private java.util.List<int[]> jj_expentries = new java.util.ArrayList<int[]>();
    private int[] jj_expentry;
    private int jj_kind = -1;
    private int trace_indent = 0;
    private boolean trace_enabled;

    /**
     * Constructor with InputStream.
     */
    public KnowledgeParser(java.io.InputStream stream) {
        this(stream, null);
    }

    /**
     * Constructor with InputStream and supplied encoding
     */
    public KnowledgeParser(java.io.InputStream stream, String encoding) {
        try {
            jj_input_stream = new SimpleCharStream(stream, encoding, 1, 1);
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        token_source = new KnowledgeParserTokenManager(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 15; i++) {
            jj_la1[i] = -1;
        }
    }

    /**
     * Constructor.
     */
    public KnowledgeParser(java.io.Reader stream) {
        jj_input_stream = new SimpleCharStream(stream, 1, 1);
        token_source = new KnowledgeParserTokenManager(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 15; i++) {
            jj_la1[i] = -1;
        }
    }

    /**
     * Constructor with generated Token Manager.
     */
    public KnowledgeParser(KnowledgeParserTokenManager tm) {
        token_source = tm;
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 15; i++) {
            jj_la1[i] = -1;
        }
    }

    private static void jj_la1_init_0() {
        jj_la1_0 = new int[]{0x240, 0x240, 0x40, 0x1000, 0x280, 0x1000, 0x240, 0x4000, 0x30000, 0x1000, 0x400, 0x40,
                0x40300, 0x40200, 0x80,};
    }

    final public List parseKnowledge() throws ParseException {
        List clauses;
        clauses = new ArrayList();
        label_1:
        while (true) {
            switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                case DECIMAL:
                case CONSTANT: {
                    break;
                }
                default:
                    jj_la1[0] = jj_gen;
                    break label_1;
            }
            readKnowledgeLine(clauses);
        }
        jj_consume_token(0);
        {
            if ("" != null) {
                return clauses;
            }
        }
        throw new Error("Missing return statement in function");
    }

    final public List parseKnowledgeAppend(List clauses) throws ParseException {
        label_2:
        while (true) {
            switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                case DECIMAL:
                case CONSTANT: {
                    break;
                }
                default:
                    jj_la1[1] = jj_gen;
                    break label_2;
            }
            readKnowledgeLine(clauses);
        }
        jj_consume_token(0);
        {
            if ("" != null) {
                return clauses;
            }
        }
        throw new Error("Missing return statement in function");
    }

    final public void readKnowledgeLine(List clauses) throws ParseException {
        Clause clause = null;

        boolean weighted = false;
        double weight = -1;

        Atom atom;
        List body;

        // boolean featured = false;
        List features = null;
        Map variableMap;
        variableMap = new HashMap();
        switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
            case DECIMAL: {
                weight = readDecimal();
                jj_consume_token(WEIGHT_SEPARATOR);
                weighted = true;
                break;
            }
            default:
                jj_la1[2] = jj_gen;
        }
        atom = readAtom(variableMap);
        switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
            case END_OF_LINE_CHARACTER: {
                jj_consume_token(END_OF_LINE_CHARACTER);
                if (weighted) {
                    atom = new WeightedAtom(weight, atom);
                }
                clause = atom;
                break;
            }
            case IMPLICATION_SIGN: {
                jj_consume_token(IMPLICATION_SIGN);
                body = new ArrayList();
                switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                    case NEGATION:
                    case CONSTANT: {
                        readLiteral(body, variableMap);
                        label_3:
                        while (true) {
                            switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                                case LIST_SEPARATOR: {
                                    break;
                                }
                                default:
                                    jj_la1[3] = jj_gen;
                                    break label_3;
                            }
                            jj_consume_token(LIST_SEPARATOR);
                            readLiteral(body, variableMap);
                        }
                        break;
                    }
                    default:
                        jj_la1[4] = jj_gen;
                }
                switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                    case OPEN_FEATURES: {
                        jj_consume_token(OPEN_FEATURES);
                        features = new ArrayList();
                        switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                            case DECIMAL:
                            case CONSTANT: {
                                readFeature(features, variableMap);
                                label_4:
                                while (true) {
                                    switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                                        case LIST_SEPARATOR: {
                                            break;
                                        }
                                        default:
                                            jj_la1[5] = jj_gen;
                                            break label_4;
                                    }
                                    jj_consume_token(LIST_SEPARATOR);
                                    readFeature(features, variableMap);
                                }
                                break;
                            }
                            default:
                                jj_la1[6] = jj_gen;
                        }
                        jj_consume_token(CLOSE_FEATURES);
                        break;
                    }
                    default:
                        jj_la1[7] = jj_gen;
                }
                jj_consume_token(END_OF_LINE_CHARACTER);
                if (features != null) {
                    clause = new FeaturedClause(atom, new Conjunction(body), new Features(features));
                } else if (weighted) {
                    clause = new WeightedClause(weight, atom, new Conjunction(body));
                } else {
                    clause = new HornClause(atom, new Conjunction(body));
                }
                break;
            }
            default:
                jj_la1[8] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
        clauses.add(clause);
    }

    final public double readDecimal() throws ParseException {
        Token decimal;
        decimal = jj_consume_token(DECIMAL);
        {
            if ("" != null) {
                return Double.parseDouble(decimal.image);
            }
        }
        throw new Error("Missing return statement in function");
    }

    final public Atom readAtom(Map variableMap) throws ParseException {
        String predicate;
        List terms = new ArrayList();
        predicate = readPredicate();
        switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
            case OPEN_PREDICATE_ARGUMENT: {
                jj_consume_token(OPEN_PREDICATE_ARGUMENT);
                readTerm(terms, variableMap);
                label_5:
                while (true) {
                    switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
                        case LIST_SEPARATOR: {
                            break;
                        }
                        default:
                            jj_la1[9] = jj_gen;
                            break label_5;
                    }
                    jj_consume_token(LIST_SEPARATOR);
                    readTerm(terms, variableMap);
                }
                jj_consume_token(CLOSE_PREDICATE_ARGUMENT);
                break;
            }
            default:
                jj_la1[10] = jj_gen;
        }
        {
            if ("" != null) {
                return new Atom(predicate, terms);
            }
        }
        throw new Error("Missing return statement in function");
    }

    final public void readFeature(List features, Map variableMap) throws ParseException {
        boolean weighted = false;
        double weight = -1;
        Atom atom = null;
        switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
            case DECIMAL: {
                weight = readDecimal();
                jj_consume_token(WEIGHT_SEPARATOR);
                weighted = true;
                break;
            }
            default:
                jj_la1[11] = jj_gen;
        }
        atom = readAtom(variableMap);
        if (weighted) {
            atom = new WeightedAtom(weight, atom);
        }
        features.add(atom);
    }

    final public String readPredicate() throws ParseException {
        Token predicate;
        predicate = jj_consume_token(CONSTANT);
        {
            if ("" != null) {
                return predicate.image;
            }
        }
        throw new Error("Missing return statement in function");
    }

    final public void readTerm(List<Term> terms, Map variableMap) throws ParseException {
        Term term;
        switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
            case CONSTANT:
            case QUOTED: {
                term = readConstant();
                terms.add(term);
                break;
            }
            case VARIABLE: {
                term = readVariable(variableMap);
                terms.add(term);
                break;
            }
            default:
                jj_la1[12] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
    }

    final public Term readConstant() throws ParseException {
        Token constant;
        switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
            case CONSTANT: {
                constant = jj_consume_token(CONSTANT);
                break;
            }
            case QUOTED: {
                constant = jj_consume_token(QUOTED);
                token.image = token.image.substring(1, token.image.length() - 1);
                break;
            }
            default:
                jj_la1[13] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
        }
        {
            if ("" != null) {
                return new Constant(token.image);
            }
        }
        throw new Error("Missing return statement in function");
    }

    final public Term readVariable(Map variableMap) throws ParseException {
        Token variable;
        variable = jj_consume_token(VARIABLE);
        if (!variableMap.containsKey(variable.image)) {
            variableMap.put(variable.image, new Variable(variable.image));
        }

        {
            if ("" != null) {
                return (Variable) variableMap.get(variable.image);
            }
        }
        throw new Error("Missing return statement in function");
    }

    final public void readLiteral(List<Literal> literals, Map variableMap) throws ParseException {
        boolean negated = false;
        Atom atom;
        switch ((jj_ntk == -1) ? jj_ntk_f() : jj_ntk) {
            case NEGATION: {
                jj_consume_token(NEGATION);
                negated = true;
                break;
            }
            default:
                jj_la1[14] = jj_gen;
        }
        atom = readAtom(variableMap);
        literals.add(new Literal(atom, negated));
    }

    /**
     * Reinitialise.
     */
    public void ReInit(java.io.InputStream stream) {
        ReInit(stream, null);
    }

    /**
     * Reinitialise.
     */
    public void ReInit(java.io.InputStream stream, String encoding) {
        try {
            jj_input_stream.ReInit(stream, encoding, 1, 1);
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        token_source.ReInit(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 15; i++) {
            jj_la1[i] = -1;
        }
    }

    /**
     * Reinitialise.
     */
    public void ReInit(java.io.Reader stream) {
        if (jj_input_stream == null) {
            jj_input_stream = new SimpleCharStream(stream, 1, 1);
        } else {
            jj_input_stream.ReInit(stream, 1, 1);
        }
        if (token_source == null) {
            token_source = new KnowledgeParserTokenManager(jj_input_stream);
        }

        token_source.ReInit(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 15; i++) {
            jj_la1[i] = -1;
        }
    }

    /**
     * Reinitialise.
     */
    public void ReInit(KnowledgeParserTokenManager tm) {
        token_source = tm;
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 15; i++) {
            jj_la1[i] = -1;
        }
    }

    private Token jj_consume_token(int kind) throws ParseException {
        Token oldToken;
        if ((oldToken = token).next != null) {
            token = token.next;
        } else {
            token = token.next = token_source.getNextToken();
        }
        jj_ntk = -1;
        if (token.kind == kind) {
            jj_gen++;
            return token;
        }
        token = oldToken;
        jj_kind = kind;
        throw generateParseException();
    }

    /**
     * Get the next Token.
     */
    final public Token getNextToken() {
        if (token.next != null) {
            token = token.next;
        } else {
            token = token.next = token_source.getNextToken();
        }
        jj_ntk = -1;
        jj_gen++;
        return token;
    }

    /**
     * Get the specific Token.
     */
    final public Token getToken(int index) {
        Token t = token;
        for (int i = 0; i < index; i++) {
            if (t.next != null) {
                t = t.next;
            } else {
                t = t.next = token_source.getNextToken();
            }
        }
        return t;
    }

    private int jj_ntk_f() {
        if ((jj_nt = token.next) == null) {
            return (jj_ntk = (token.next = token_source.getNextToken()).kind);
        } else {
            return (jj_ntk = jj_nt.kind);
        }
    }

    /**
     * Generate ParseException.
     */
    public ParseException generateParseException() {
        jj_expentries.clear();
        boolean[] la1tokens = new boolean[19];
        if (jj_kind >= 0) {
            la1tokens[jj_kind] = true;
            jj_kind = -1;
        }
        for (int i = 0; i < 15; i++) {
            if (jj_la1[i] == jj_gen) {
                for (int j = 0; j < 32; j++) {
                    if ((jj_la1_0[i] & (1 << j)) != 0) {
                        la1tokens[j] = true;
                    }
                }
            }
        }
        for (int i = 0; i < 19; i++) {
            if (la1tokens[i]) {
                jj_expentry = new int[1];
                jj_expentry[0] = i;
                jj_expentries.add(jj_expentry);
            }
        }
        int[][] exptokseq = new int[jj_expentries.size()][];
        for (int i = 0; i < jj_expentries.size(); i++) {
            exptokseq[i] = jj_expentries.get(i);
        }
        return new ParseException(token, exptokseq, tokenImage);
    }

    /**
     * Trace enabled.
     */
    final public boolean trace_enabled() {
        return trace_enabled;
    }

    /**
     * Enable tracing.
     */
    final public void enable_tracing() {
    }

    /**
     * Disable tracing.
     */
    final public void disable_tracing() {
    }

}