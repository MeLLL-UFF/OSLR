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
import java.util.Map;

import org.apache.logging.log4j.Logger;import org.apache.logging.log4j.LogManager;;

import edu.cmu.ml.proppr.prove.wam.CachingIdProofGraph;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.Outlink;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.StatusLogger;
import edu.cmu.ml.proppr.util.math.LongDense;
import edu.cmu.ml.proppr.util.math.SmoothFunction;

/**
 * prover using power iteration
 * @author "William Cohen <wcohen@cs.cmu.edu>"
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class IdPprProver extends Prover<CachingIdProofGraph> {
	private static final double SEED_WEIGHT = 1.0;
	private static final Logger log = LogManager.getLogger(IdPprProver.class);
	private static final boolean DEFAULT_TRACE=false;
	private static final boolean RESTART = true;
	private static final boolean TRUELOOP = true;
	protected boolean trace;
	protected LongDense.AbstractFloatVector params=null;
	
	public IdPprProver() { this(DEFAULT_TRACE); }
	public IdPprProver(boolean tr) {
		init(tr);
	}
	public IdPprProver(APROptions apr) { super(apr); init(DEFAULT_TRACE); }
	public IdPprProver(FeatureDictWeighter w, APROptions apr, boolean tr) {
		super(w, apr);
		init(tr);
	}
	private void init(boolean tr) {
		trace=tr;
	}
	
	@Override
	public String toString() { return "ippr:"+this.apr.maxDepth; }
	
	public Prover<CachingIdProofGraph> copy() {
		IdPprProver copy = new IdPprProver(weighter, this.apr, this.trace);
		copy.params = this.params;
		return copy;
	}
	@Override
	public Class<CachingIdProofGraph> getProofGraphClass() { return CachingIdProofGraph.class; }

	private LongDense.AbstractFloatVector getFrozenParams(CachingIdProofGraph pg) {
		if (params != null) return params;
		if (this.weighter.weights.size()==0) 
			params = new LongDense.UnitVector();
		else 
			params = pg.paramsAsVector(this.weighter.weights,this.weighter.squashingFunction.defaultValue()); // FIXME: default value should depend on f
		return params;
	}
	
	public void setMaxDepth(int i) {
		this.apr.maxDepth = i;
	}
	public void setTrace(boolean b) {
		this.trace = b;
	}

	@Override
	public Map<State, Double> prove(CachingIdProofGraph pg, StatusLogger status) 
	{
		LongDense.FloatVector p = proveVec(pg,status);
		if (apr.traceDepth!=0) {
			log.info("== proof graph: edges/nodes "+pg.edgeSize()+"/"+pg.nodeSize());
			log.info(pg.treeView(apr.traceDepth,apr.traceRoot,weighter,p));
		}
		return pg.asMap(p);
	}

	protected LongDense.FloatVector proveVec(CachingIdProofGraph pg, StatusLogger status)
	{
		LongDense.FloatVector startVec = new LongDense.FloatVector();
		startVec.set( pg.getRootId(), SEED_WEIGHT );
		LongDense.AbstractFloatVector params = getFrozenParams(pg);
		
		LongDense.FloatVector vec = startVec;

		LongDense.FloatVector nextVec = new LongDense.FloatVector();
		LongDense.FloatVector tmp;

		for (int i=0; i<this.apr.maxDepth; i++) {
			// vec = walkOnce(cg,vec,params,f);
			walkOnceBuffered(pg,vec,nextVec,params);
			// save vec as the next buffer, then point vec at the new result
			tmp = vec;
			tmp.clear();
			vec = nextVec;
			// now use the saved space as buffer next iteration
			nextVec = tmp;
			//log.info("ippr iter "+(i+1)+" size "+vec.size());
			if (log.isInfoEnabled() && status.due(1)) log.info(Thread.currentThread()+" depth "+(i+1)+" size "+vec.size());
		}

		return vec;
	}

	LongDense.FloatVector walkOnce(CachingIdProofGraph cg, LongDense.FloatVector vec,LongDense.AbstractFloatVector params) 
	{
		LongDense.FloatVector nextVec = new LongDense.FloatVector(vec.size());
		nextVec.set( cg.getRootId(), apr.alpha * SEED_WEIGHT );
		try {
			for (int uid=cg.getRootId(); uid<vec.size(); uid++) {
				double vu = vec.get(uid);
				if (vu >= 0.0) {
					double z = cg.getTotalWeightOfOutlinks(uid, params, this.weighter);
					int d = cg.getDegreeById(uid, this.weighter);
					for (int i=0; i<d; i++) {
						double wuv = cg.getIthWeightById(uid,i,params, this.weighter);
						int vid = cg.getIthNeighborById(uid,i,this.weighter);
						nextVec.inc(vid, vu*(1.0-apr.alpha)*(wuv/z));
					}
				}
			}
		} catch (LogicProgramException ex) {
				throw new IllegalStateException(ex);			
		}
		return nextVec;
	}

	void walkOnceBuffered(CachingIdProofGraph cg, 
												LongDense.FloatVector vec,LongDense.FloatVector nextVec,
												LongDense.AbstractFloatVector params) 
	{
		nextVec.clear();
		nextVec.set( cg.getRootId(), apr.alpha * SEED_WEIGHT );
		try {
			for (int uid=cg.getRootId(); uid<vec.size(); uid++) {
				double vu = vec.get(uid);
				if (vu >= 0.0) {
					double z = cg.getTotalWeightOfOutlinks(uid, params, this.weighter);
					int d = cg.getDegreeById(uid, this.weighter);
					for (int i=0; i<d; i++) {
						double wuv = cg.getIthWeightById(uid,i,params, this.weighter);
						int vid = cg.getIthNeighborById(uid,i,this.weighter);
						nextVec.inc(vid, vu*(1.0-apr.alpha)*(wuv/z));
					}
				}
			}
		} catch (LogicProgramException ex) {
				throw new IllegalStateException(ex);			
		}
	}
}
