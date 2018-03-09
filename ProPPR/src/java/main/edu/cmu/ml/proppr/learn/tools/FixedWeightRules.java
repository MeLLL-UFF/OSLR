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

package edu.cmu.ml.proppr.learn.tools;

import java.util.ArrayList;

import edu.cmu.ml.proppr.util.math.ParamVector;

/** A simple programmable procedure to determine if a feature is fixed
(i.e., the weight will not be changed in training) or not.  The
procedure is specified by a single string, which is a
colon-separated list of pairs SPEC[=DECISION] where SPEC is any
string (ending in * to specify a prefix) and DECISION is 'y' or 'n' 
(default y).  

To decide whether a particular feature S is of fixed weight, you go 
through the rules, and take the DECISION associated with the first 
spec that matches S.


For example: 

"fixedWeight=y:*=n" means tune all features that aren't "fixedWeight".

"f(*=n:*=y" means tune only features that start with "f("

If no spec matches then the decision return is false.
**/
public class FixedWeightRules {
	private ArrayList<DecisionRule> ruleList;
	class DecisionRule {
		public String spec;
		public boolean fixed;
		public boolean prefix;
		public DecisionRule(String s,boolean fix) {
			prefix = s.endsWith("*");
			spec = prefix?s.substring(0,s.length()-1):s;
			fixed = fix;
		}
		public boolean claim(String f) {
			return prefix?f.startsWith(spec):f.equals(spec);
		}
		public String toString() {
			return "if feature" + (prefix?" starts with ":" is ")+spec+" it is "+(fixed?"":"not")+" fixed";
		}
	}
	
	public FixedWeightRules() {
		ruleList = new ArrayList<DecisionRule>();
	}
	public FixedWeightRules(String[] init) {
		this();
		for (String r : init) {
			String[] opts = r.split("=",2);
			ruleList.add(new DecisionRule(opts[0], opts.length==1 || "y".equals(opts[1])));
		}
	}

	public boolean isFixed(String feature) {
		for (DecisionRule r : ruleList) {
			if (r.claim(feature)) return r.fixed;
		}
		return false;
	}

	public void addExact(String feature) {
		ruleList.add(new DecisionRule(feature,true));
	}
	
	public void initializeFixed(ParamVector<String,?> params, String feature) {
		/* 
		 * Future work: Could add syntax to fix features at arbitrary values here.
		 */
		params.put(feature, 1.0);
	}
	public String toString() 
	{
		StringBuilder sb = new StringBuilder();
		for (DecisionRule r:ruleList) {
			sb.append(r.toString()+"; ");
		}
		return sb.toString();
	}
}
