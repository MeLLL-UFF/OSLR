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

package edu.cmu.ml.proppr.prove;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;import org.apache.logging.log4j.LogManager;;

import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.Outlink;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.StateProofGraph;
import edu.cmu.ml.proppr.prove.wam.WamInterpreter;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.StatusLogger;
/**
 * A prover with scores based on simple depth-first-search
 * @author "William Cohen <wcohen@cs.cmu.edu>"
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class DfsProver extends Prover<StateProofGraph> {
	private static final Logger log = LogManager.getLogger(DfsProver.class);
	private static final double SEED_WEIGHT = 1.0;
	private Backtrace<State> backtrace = new Backtrace<State>(log);
	protected boolean trueLoop;

	public DfsProver() {
		init();
	}
	public DfsProver(APROptions apr) {
		super(apr);
		init();
	}
	public DfsProver(FeatureDictWeighter w, APROptions apr) {
		super(w,apr);
		init();
	}
	public DfsProver(FeatureDictWeighter w, APROptions apr, boolean trueLoop) {
		super(w,apr);
		init(trueLoop);
	}
	private void init() {
		init(ProofGraph.DEFAULT_TRUELOOP);
	}
	private void init(boolean trueLoop) {
		this.trueLoop = trueLoop;
	}
	public Prover<StateProofGraph> copy() {
		return new DfsProver(this.weighter, this.apr, this.trueLoop);
	}
	@Override
	public Class<StateProofGraph> getProofGraphClass() { return StateProofGraph.class; }

	protected class Entry {
		public State state;
		public double w;
		public Entry(State s, Double d) {this.state = s; this.w = d;}
	}
	protected List<Entry> dfs(StateProofGraph pg, State s, int depth) throws LogicProgramException {
		List<Entry> result = new LinkedList<DfsProver.Entry>();
		dfs(pg,s,depth,SEED_WEIGHT,result);
		return result;
	}
	/**
	 * Do depth first search from a state, yielding all states in the tree,
        together with the incoming weights. 
	 * @throws LogicProgramException */
	protected void dfs(StateProofGraph pg, State s, int depth, double incomingEdgeWeight, List<Entry> tail) throws LogicProgramException {
		beforeDfs(s, pg, depth);
		Entry e = new Entry(s,incomingEdgeWeight);
		tail.add(e);
		if (!s.isCompleted() && depth < this.apr.maxDepth) {
			backtrace.push(s);
			List<Outlink> outlinks = pg.pgOutlinks(s, trueLoop);
			if (outlinks.size() == 0) 
				if (log.isDebugEnabled()) log.debug("exit dfs: no outlinks for "+s);
			double z = 0;
			for (Outlink o : outlinks) {
				o.wt = this.weighter.w(o.fd);
				z += o.wt;
			}
			for (Outlink o : outlinks) {
				//skip resets
				if (o.child.equals(pg.getStartState())) continue;
				
				//recurse into non-resets
				e.w -= o.wt / z;
				dfs(pg, o.child, depth+1, o.wt / z, tail);
			}
			backtrace.pop(s);
		} else if (log.isDebugEnabled()) 
			log.debug("exit dfs: "+ (s.isCompleted() ? "state completed" : ("depth "+depth+">"+this.apr.maxDepth)));
	}

	/** 
	 * Template for subclasses
	 * @param depth 
	 * @param s 
	 * @param interp 
	 * @throws LogicProgramException 
	 */
	protected void beforeDfs(State s, ProofGraph pg, int depth) throws LogicProgramException {}
	
	@Override
	public Map<State, Double> prove(StateProofGraph pg, StatusLogger status) {
		Map<State,Double>vec = new HashMap<State,Double>();
		int i=0;
		backtrace.start();
		try {
			for (Entry e : dfs(pg,pg.getStartState(),0)) {
				/// wtf was this for? -katie
//				vec.put(e.state, 1.0/(i+1));
				vec.put(e.state, e.w);
				i++;
			}
		} catch (LogicProgramException e) {
			backtrace.rethrow(e);
		}
		return vec;
	}

}
