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

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.junit.Test;

import edu.cmu.ml.proppr.prove.DprProver;
import edu.cmu.ml.proppr.prove.Prover;
import edu.cmu.ml.proppr.prove.wam.StateProofGraph;
import edu.cmu.ml.proppr.prove.wam.WamProgram;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.WamBaseProgram;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.StatusLogger;

public class EqualityTest {
	private static final String EQUALITY_PROGRAM="src/testcases/equalityTest.wam";

	@Test
	public void test() throws LogicProgramException, IOException {
		WamProgram program = WamBaseProgram.load(new File(EQUALITY_PROGRAM));
		Prover prover = new DprProver();
		ProofGraph moral = new StateProofGraph(Query.parse("moral(X)"),new APROptions(), program);
		Collection<Query> bobs = prover.solvedQueries(moral, new StatusLogger()).keySet();
//		Map<State,Double> ans = prover.prove(moral);
//		ArrayList<Query> bobs = new ArrayList<Query>();
//		for (Map.Entry<State,Double> e : ans.entrySet()) {
//			if (e.getKey().isCompleted()) bobs.add(moral.fill(e.getKey()));
//		}
		
		assertEquals(1,bobs.size());
		Query bob = bobs.iterator().next();
		assertEquals(1,bob.getRhs().length);
		assertEquals("Answer should be bob","bob",bob.getRhs()[0].getArg(0).getName());
	}

}
