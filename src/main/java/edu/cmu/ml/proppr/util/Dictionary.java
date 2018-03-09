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

import edu.cmu.ml.proppr.util.math.ParamVector;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.Map.Entry;

/**
 * Utility methods for common tasks on String->Double &c.
 *
 * @author krivard
 */
public class Dictionary {

    private static final Logger log = LogManager.getLogger(Dictionary.class);

    /**
     * Increment the key's value, or set it if the key is new.
     *
     * @param map
     * @param key
     * @param value
     */
    public static void increment(TIntDoubleMap map, int key, double value) {
        if (!map.containsKey(key)) { map.put(key, sanitize(value, key)); } else {
            double newvalue = map.get(key) + value;
            map.put(key, sanitize(newvalue, key));
        }
    }

    private static <K> double sanitize(Double d, K... msg) {
        if (d.isInfinite()) {
            log.warn(d + " at " + msg + "; truncating");
            return (d > 0) ? Double.MAX_VALUE : -Double.MAX_VALUE; // does this even work?
        } else if (d.isNaN()) {
            StringBuilder sb = new StringBuilder();
            for (K k : msg) { sb.append(",").append(k); }
            throw new IllegalArgumentException("NaN encountered at " + sb.substring(1));
        }
        return d;
    }

    /**
     * Increment the key's value, or set it if the key is new.
     *
     * @param map
     * @param key
     * @param value
     */
    public static <K> void increment(TObjectDoubleMap<K> map, K key, double value) {
        if (!map.containsKey(key)) { map.put(key, sanitize(value, key)); } else {
            double newvalue = map.get(key) + value;
            map.put(key, sanitize(newvalue, key));
        }
    }

    /**
     * Set the key's value using standard sanitizer.
     *
     * @param map
     * @param key
     * @param value
     */
    public static void set(TIntDoubleMap map, int key, double value) {
        map.put(key, sanitize(value, key));
    }

    /**
     * Set the key's value using standard sanitizer.
     *
     * @param map
     * @param key
     * @param value
     */
    public static <K> void set(TObjectDoubleMap<K> map, K key, double value) {
        map.put(key, sanitize(value, key));
    }

    /**
     * Set the key's value using standard sanitizer.
     *
     * @param map
     * @param key
     * @param value
     */
    public static void set(ParamVector<String, ?> map, String key, double value) {
        map.put(key, sanitize(value, key));
    }

    /**
     * Increment the key's value, or set it if the key is new.
     * Adds a TreeMap if key1 is new.
     *
     * @param map
     * @param key1
     * @param key2
     * @param value
     */
    public static void increment(TIntObjectMap<TIntDoubleHashMap> map, int key1, int key2, Double value) {
        if (!map.containsKey(key1)) { map.put(key1, new TIntDoubleHashMap()); }
        TIntDoubleHashMap inner = map.get(key1);
        if (!inner.containsKey(key2)) { inner.put(key2, sanitize(value, key1, key2)); } else {
            double newvalue = inner.get(key2) + value;
            inner.put(key2, sanitize(newvalue, key1, key2));
        }
    }

    public static <K> void increment(TIntObjectMap<TObjectDoubleMap<K>> map, int key1,
                                     K key2, double value) {
        if (!map.containsKey(key1)) { map.put(key1, new TObjectDoubleHashMap<K>()); }
        TObjectDoubleMap<K> inner = map.get(key1);
        if (!inner.containsKey(key2)) { inner.put(key2, sanitize(value, key1, key2)); } else {
            double newvalue = inner.get(key2) + value;
            inner.put(key2, sanitize(newvalue, key1, key2));
        }
    }

    /**
     * Return the key's value, or provided default if the key is not in this map.
     *
     * @param map
     * @param key1
     * @param key2
     * @return
     */
    public static <K> K safeGet(TIntObjectMap<K> map, int key1, K dflt) {
        if (map.containsKey(key1)) {
            return map.get(key1);
        }
        return dflt;
    }

    /**
     * Serialize this map to a StringBuilder, using the specified delimiters between key:value pairs.
     * The string added to the StringBuilder is:
     * $delim1$key1:{buildString(map[key1],sb,delim2)} ... $delim1$keyN:{buildString(map[keyN],sb,delim2)}
     *
     * @param map
     * @param sb
     * @param delim1
     * @param delim2
     * @return
     */
    public static StringBuilder buildString(TIntObjectMap<TIntDoubleMap> map, StringBuilder sb, String delim1,
                                            String delim2) {
        for (TIntObjectIterator<TIntDoubleMap> e = map.iterator(); e.hasNext(); ) {
            e.advance();
            sb.append(delim1).append(e.key()).append(":");
            buildString(e.value(), sb, delim2);
        }
        return sb;
    }

    /**
     * Serialize this map to a StringBuilder, using the specified delimiter between key:value pairs.
     * The string added to the StringBuilder is:
     * $delim$key1:$value1$delim$key2:$value2 ... $delim$keyN$valueN
     *
     * @param map
     * @param sb
     * @param delim
     * @return
     */
    public static StringBuilder buildString(TIntDoubleMap map, StringBuilder sb, String delim) {
        for (TIntDoubleIterator e = map.iterator(); e.hasNext(); ) {
            e.advance();
            sb.append(delim).append(e.key()).append(":").append(e.value());
        }
        return sb;
    }

    public static <K> StringBuilder buildString(TObjectDoubleMap<K> map, StringBuilder sb, String delim) {
        for (TObjectDoubleIterator<K> e = map.iterator(); e.hasNext(); ) {
            e.advance();
            sb.append(delim).append(e.key()).append(":").append(e.value());
        }
        return sb;
    }

    public static <K> StringBuilder buildString(Iterable<K> keys, StringBuilder sb, String delim) {
        boolean first = true;
        for (K k : keys) {
            if (first) { first = false; } else { sb.append(delim); }
            sb.append(k);
        }
        return sb;
    }

    public static StringBuilder buildString(float[] keys, StringBuilder sb, String delim, boolean first) {
        for (float t : keys) {
            if (first) { first = false; } else { sb.append(delim); }
            sb.append(t);
        }
        return sb;
    }

    // augh java primitives
    public static StringBuilder buildString(int[] keys, StringBuilder sb, String delim) {
        return buildString(keys, sb, delim, true);
    }

    public static StringBuilder buildString(int[] keys, StringBuilder sb, String delim, boolean first) {
        for (int t : keys) {
            if (first) { first = false; } else { sb.append(delim); }
            sb.append(t);
        }
        return sb;
    }

    public static <T> StringBuilder buildString(T[] keys, StringBuilder sb, String delim) {
        return buildString(keys, sb, delim, true);
    }

    public static <T> StringBuilder buildString(T[] keys, StringBuilder sb, String delim, boolean first) {
        for (T t : keys) {
            if (first) { first = false; } else { sb.append(delim); }
            sb.append(t);
        }
        return sb;
    }

    // // removed by krivard 17 mar 2014 - not called anywhere
    //	public static void save(TIntDoubleMap map, String filename) {
    //		BufferedWriter writer;
    //		try {
    //			writer = new BufferedWriter(new FileWriter(filename));
    //			for (TIntDoubleIterator e = map.iterator(); e.hasNext(); ) {
    //				e.advance();
    //				writer.write(String.format("%s\t%f\n", String.valueOf(e.key()),e.value()));
    //			}
    //			writer.close();
    //		} catch (IOException e) {
    //			// TODO Auto-generated catch block
    //			e.printStackTrace();
    //		}
    //	}
    public static boolean safeContains(TIntObjectMap<TIntDoubleMap> map,
                                       int key1, int key2) {
        if (!map.containsKey(key1)) { return false; }
        return map.get(key1).containsKey(key2);
    }

    public static <K> boolean safeContains(
            TIntObjectMap<TObjectDoubleMap<K>> map, int key1, K key2) {
        if (!map.containsKey(key1)) { return false; }
        return map.get(key1).containsKey(key2);
    }

    /**
     * Reset the key's value, or set it if the key is new.
     *
     * @param map
     * @param key
     * @param value
     */
    public static <K> void reset(Map<K, Double> map, K key, Double value, String msg) {
        if (!map.containsKey(key)) { map.put(key, sanitize(value, msg)); } else {
            map.put(key, sanitize(value, key));
        }
    }

    public static <K> void increment(Map<K, Double> map, K key, Double value) {
        increment(map, key, value, String.valueOf(key));
    }

    /**
     * Increment the key's value, or set it if the key is new.
     *
     * @param map
     * @param key
     * @param value
     */
    public static <K> void increment(Map<K, Double> map, K key, Double value, String msg) {
        if (!map.containsKey(key)) { map.put(key, sanitize(value, msg)); } else {
            double newvalue = map.get(key) + value;
            map.put(key, sanitize(newvalue, key));
        }
    }

    public static <K> void increment(Map<K, Integer> map, K key) {
        if (!map.containsKey(key)) { map.put(key, 1); } else { map.put(key, map.get(key) + 1); }
    }

    /**
     * Increment the key's value, or set it if the key is new.
     * Adds a TreeMap if key1 is new.
     *
     * @param map
     * @param key1
     * @param key2
     * @param value
     */
    public static <K, L> void increment(Map<K, Map<L, Double>> map, K key1, L key2, Double value) {
        if (!map.containsKey(key1)) { map.put(key1, new TreeMap<L, Double>()); }
        Map<L, Double> inner = map.get(key1);
        if (!inner.containsKey(key2)) { inner.put(key2, sanitize(value, key1, key2)); } else {
            double newvalue = inner.get(key2) + value;
            inner.put(key2, sanitize(newvalue, key1, key2));
        }
    }

    /**
     * Return the key's value, or 0.0 if the key is not in this map.
     *
     * @param map
     * @param key1
     * @param key2
     * @return
     */
    public static <K, L> double safeGetGet(Map<K, Map<L, Double>> map, K key1, L key2) {
        if (map.containsKey(key1)) {
            return safeGet(map.get(key1), key2);
        }
        return 0.0;
    }

    /**
     * Return the key's value, or 0.0 if the key is not in this map.
     *
     * @param map
     * @param key
     * @return
     */
    public static <K> double safeGet(Map<K, Double> map, K key) {
        if (map.containsKey(key)) { return map.get(key); }
        return 0.0;
    }

    /**
     * Return the key's value, or 0.0 if the key is not in this map.
     *
     * @param map
     * @param key1
     * @param key2
     * @return
     */
    public static double safeGetGet(TIntObjectMap<TIntDoubleMap> map, int key1, int key2) {
        if (map.containsKey(key1)) {
            return safeGet(map.get(key1), key2);
        }
        return 0.0;
    }

    /**
     * Return the key's value, or 0.0 if the key is not in this map.
     *
     * @param map
     * @param key
     * @return
     */
    public static double safeGet(TIntDoubleMap map, int key) {
        return safeGet(map, key, 0.0);
    }

    /**
     * Return the key's value, or dflt if the key is not in this map.
     *
     * @param map
     * @param key
     * @return
     */
    public static double safeGet(TIntDoubleMap map, int key, double dflt) {
        if (map.containsKey(key)) { return map.get(key); }
        return dflt;
    }

    /**
     * Return the key's value, or 0.0 if the key is not in this map.
     *
     * @param map
     * @param key1
     * @param key2
     * @return
     */
    public static <K> double safeGetGet(TIntObjectMap<TObjectDoubleMap<K>> map, int key1, K key2) {
        if (map.containsKey(key1)) {
            return safeGet(map.get(key1), key2);
        }
        return 0.0;
    }

    public static <K> double safeGet(TObjectDoubleMap<K> map, K key) {
        if (map.containsKey(key)) { return map.get(key); }
        return 0.0;
    }

    /**
     * Return the key's value, or 0.0 if the key is not in this map.
     *
     * @param map
     * @param key1
     * @param key2
     * @return
     */
    public static <K, L, M> M safeGetGet(Map<K, Map<L, M>> map, K key1, L key2, M dflt) {
        if (map.containsKey(key1)) {
            return safeGet(map.get(key1), key2, dflt);
        }
        return dflt;
    }

    public static <K, V> V safeGet(Map<K, V> map, K key, V dflt) {
        if (map.containsKey(key)) { return map.get(key); }
        return dflt;
    }

    /**
     * Serialize this map to a StringBuilder, using the specified delimiters between key:value pairs.
     * The string added to the StringBuilder is:
     * $delim1$key1:{buildString(map[key1],sb,delim2)} ... $delim1$keyN:{buildString(map[keyN],sb,delim2)}
     *
     * @param map
     * @param sb
     * @param delim1
     * @param delim2
     * @return
     */
    public static <K1, K2> StringBuilder buildString(Map<K1, Map<K2, Double>> map, StringBuilder sb, String delim1,
                                                     String delim2) {
        for (Map.Entry<K1, Map<K2, Double>> e : map.entrySet()) {
            sb.append(delim1).append(e.getKey()).append(":");
            buildString(e.getValue(), sb, delim2);
        }
        return sb;
    }

    /**
     * Serialize this map to a StringBuilder, using the specified delimiter between key:value pairs.
     * The string added to the StringBuilder is:
     * $delim$key1:$value1$delim$key2:$value2 ... $delim$keyN$valueN
     *
     * @param map
     * @param sb
     * @param delim
     * @return
     */
    public static <K, V> StringBuilder buildString(Map<K, V> map, StringBuilder sb, String delim) {
        for (Map.Entry<K, V> e : map.entrySet()) {
            sb.append(delim).append(e.getKey()).append(":").append(e.getValue());
        }
        return sb;
    }

    public static <K> void save(Map<K, Double> map, String filename) {
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(filename));
            save(map, writer);
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static <K> void save(Map<K, Double> map, Writer writer) throws IOException {
        for (Map.Entry<K, Double> e : map.entrySet()) {
            writer.write(String.format("%s\t%.6g\n", String.valueOf(e.getKey()), e.getValue()));
        }
    }

    public static <K, L> boolean safeContains(Map<K, Map<L, Double>> map,
                                              K key1, L key2) {
        if (!map.containsKey(key1)) { return false; }
        return map.get(key1).containsKey(key2);
    }

    public static <K, L, M> void safeAppend(
            Map<K, Map<L, List<M>>> map, K key1, L key2, M newVal) {
        if (!map.containsKey(key1)) {
            map.put(key1, new HashMap<L, List<M>>());
        }
        safeAppend(map.get(key1), key2, newVal);
    }

    public static <K, V> void safeAppend(Map<K, List<V>> map,
                                         K key, V newVal) {
        if (!map.containsKey(key)) {
            map.put(key, new ArrayList<V>());
        }
        map.get(key).add(newVal);
    }

    public static <K, L, M> void safePut(Map<K, Map<L, TObjectDoubleMap<M>>> map,
                                         K key1, L key2, M key3, double wt) {
        if (!map.containsKey(key1)) { map.put(key1, new HashMap<L, TObjectDoubleMap<M>>()); }
        safePut(map.get(key1), key2, key3, wt);
    }

    public static <K, L> void safePut(Map<K, TObjectDoubleMap<L>> map,
                                      K key1, L key2, double wt) {
        if (!map.containsKey(key1)) { map.put(key1, new TObjectDoubleHashMap<L>()); }
        map.get(key1).put(key2, wt);
    }

    public static <K, L, M> Map<L, M> safeGet(
            Map<K, Map<L, M>> map,
            K key1, Map<L, M> dflt) {
        if (map.containsKey(key1)) { return map.get(key1); }
        return dflt;
    }

    public static <K, L, M> List<M> safeGetGet(
            Map<K, Map<L, List<M>>> map,
            K key1, L key2, List<M> dflt) {
        if (map.containsKey(key1)) {
            return safeGet(map.get(key1), key2, dflt);//map.get(key);
        }
        return dflt;
    }

    public static <K, V> List<V> safeGet(
            Map<K, List<V>> map,
            K key, List<V> dflt) {
        if (map.containsKey(key)) { return map.get(key); }
        return dflt;
    }

    /**
     * Given a dictionary that represents a sparse numeric vector,
     * rescale the values in-place to sum to some desired amount, and return the original (now changed) vector.
     *
     * @param map
     * @return
     */
    public static <K> Map<K, Double> normalize(Map<K, Double> map) {
        double z = 0.0;
        for (Double d : map.values()) { z += d; }
        if (z == 0) { return map; }
        for (Map.Entry<K, Double> e : map.entrySet()) { e.setValue(e.getValue() / z); }
        return map;
    }

    public static Map<String, Double> load(String filename) {
        return load(new ParsedFile(filename));
    }

    public static Map<String, Double> load(ParsedFile file) {
        return load(file, new HashMap<String, Double>());
    }

    public static Map<String, Double> load(ParsedFile file, Map<String, Double> map) {
        int i = 1;
        for (String line : file) {
            String[] parts = line.split("\t");
            if (parts.length != 2) { file.parseError(); }
            double d = Double.parseDouble(parts[1]);
            if (Double.isInfinite(d)) {
                throw new IllegalArgumentException("Can't load Infinity at line " + i + " of " + file.getFileName());
            }
            if (Double.isNaN(d)) {
                throw new IllegalArgumentException("Can't load NaN at line " + i + " of " + file.getFileName());
            }
            map.put(parts[0], d);
            i++;
        }
        file.close();
        return map;
    }

    /**
     * Given a weighted set, return a list of the elements in descending order.
     *
     * @param map
     * @return
     */
    public static <K> List<Map.Entry<K, Double>> sort(Map<K, Double> map) {
        List<Map.Entry<K, Double>> ret = new ArrayList<Map.Entry<K, Double>>();
        ret.addAll(map.entrySet());
        Collections.sort(ret, new Comparator<Map.Entry<K, Double>>() {
            @Override
            public int compare(Entry<K, Double> arg0, Entry<K, Double> arg1) {
                return arg1.getValue().compareTo(arg0.getValue());
            }
        });
        return ret;
    }

}
