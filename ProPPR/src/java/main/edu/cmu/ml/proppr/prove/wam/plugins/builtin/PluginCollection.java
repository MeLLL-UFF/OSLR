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

package edu.cmu.ml.proppr.prove.wam.plugins.builtin;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.ml.proppr.prove.wam.Feature;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.WamInterpreter;
import edu.cmu.ml.proppr.prove.wam.plugins.WamPlugin;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.Dictionary;
/**
"""Used for collections of simple built-in plugins."""

def __init__(self):
    self.registery = {}
    self.helpText = {}
    self.fd = {'builtin':1}
def register(self,jumpTo,fun,help='no help available'):
    self.registery[jumpTo] = fun
    self.helpText[jumpTo] = help
def claim(self,jumpTo):
    return (jumpTo in self.registery)
def outlinks(self,state,wamInterp,computeFeatures=True):
    assert False,'abstract method called'
    
    @author William Cohen <wcohen@cs.cmu.edu>
    @author Kathryn Mazaitis <krivard@cs.cmu.edu>
**/
public abstract class PluginCollection extends WamPlugin {
	protected Map<String,PluginFunction> registry;
	protected Map<Feature, Double> fd;
	// TODO: helpText
	
	public PluginCollection(APROptions apr) {
		super(apr);
		this.fd = new HashMap<Feature,Double>();
		this.fd.put(new Feature("builtin"), 1.0);
	}

	@Override
	public String about() {
		return Dictionary.buildString(registry.keySet(), new StringBuilder(), ", ").toString();
	}

	@Override
	public boolean _claim(String jumpto) {
		return registry.containsKey(jumpto);
	}

	public void register(String jumpTo, PluginFunction fun) {
		if (registry == null) registry = new HashMap<String,PluginFunction>();
		registry.put(jumpTo, fun);
	}
	

}
