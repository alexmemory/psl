/*
 * This file is part of the PSL software.
 * Copyright 2011-2015 University of Maryland
 * Copyright 2013-2018 The Regents of the University of California
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.linqs.psl.application.util;

import org.linqs.psl.model.atom.GroundAtom;
import org.linqs.psl.model.atom.RandomVariableAtom;
import org.linqs.psl.model.rule.GroundRule;
import org.linqs.psl.model.rule.UnweightedGroundRule;
import org.linqs.psl.model.rule.WeightedGroundRule;
import org.linqs.psl.model.rule.logical.WeightedGroundLogicalRule;

import java.util.ArrayList;
import java.util.List;

/**
 * Static utilities for common {@link GroundRule} tasks.
 */
public class GroundRules {

	/**
	 * Sums the total weighted incompatibility of an iterable container of
	 * {@link WeightedGroundRule GroundCompatibilityRules}.
	 *
	 * @param groundRules  the GroundCompatibilityRules
	 * @return the total weighted incompatibility
	 * @see WeightedGroundRule#getIncompatibility()
	 * @see WeightedGroundRule#getWeight()
	 */
	public static double getTotalWeightedIncompatibility(Iterable<WeightedGroundRule> groundRules) {
		double totalInc = 0.0;
		for (WeightedGroundRule groundRule : groundRules)
			totalInc += groundRule.getIncompatibility() * groundRule.getWeight();
		return totalInc;
	}

	/**
	 * Sums the total weighted compatibility (1 - incompatibility) of an iterable
	 * container of {@link WeightedGroundRule GroundCompatibilityRules}.
	 *
	 * WARNING: This method does not account for GroundCompatibilityRules that
	 * were not grounded because they are trivially satisfied.
	 *
	 * @param groundRules  the GroundCompatibilityRules
	 * @return the total weighted compatibility
	 * @see WeightedGroundRule#getIncompatibility()
	 * @see WeightedGroundRule#getWeight()
	 */
	public static double getTotalWeightedCompatibility(Iterable<WeightedGroundRule> groundRules) {
		double totalInc = 0.0;
		for (WeightedGroundRule groundRule : groundRules)
			totalInc += (1 - groundRule.getIncompatibility()) * groundRule.getWeight();
		return totalInc;
	}

	/**
	 * Computes the expected total weighted incompatibility of an iterable
	 * container of {@link WeightedGroundRule GroundCompatibilityRules}
	 * from independently rounding each {@link RandomVariableAtom} to 1.0 or 0.0
	 * with probability equal to its current truth value.
	 *
	 * WARNING: the result of this function is incorrect if the RandomVariableAtoms
	 * are subject to any {@link UnweightedGroundRule GroundConstraintRules}.
	 *
	 * @param groundRules  the GroundCompatibilityRules
	 * @return the expected total weighted incompatibility
	 * @see WeightedGroundRule#getIncompatibility()
	 * @see WeightedGroundRule#getWeight()
	 */
	public static double getExpectedTotalWeightedIncompatibility(Iterable<WeightedGroundRule> groundRules) {
		double totalInc = 0.0;
		List<RandomVariableAtom> atoms = new ArrayList<RandomVariableAtom>();
		for (WeightedGroundRule groundRule : groundRules) {
			double inc = 0.0;

			// Collects RandomVariableAtoms
			for (GroundAtom atom : groundRule.getAtoms())
				if (atom instanceof RandomVariableAtom)
					atoms.add((RandomVariableAtom) atom);

			// Collects truth values
			double[] truthValues = new double[atoms.size()];
			for (int i = 0; i < truthValues.length; i++)
				truthValues[i] = atoms.get(i).getValue();

			// Sums over settings
			for (int i = 0; i < Math.pow(2, atoms.size()); i++) {
				double assignmentProb = 1.0;

				// Sets assignment and computes probability
				for (int j = 0; j < atoms.size(); j++) {
					int assignment = ((i >> j) & 1);
					atoms.get(j).setValue(assignment);
					assignmentProb *= (assignment == 1) ? truthValues[j] : 1 - truthValues[j];
				}

				inc += assignmentProb * groundRule.getIncompatibility();
			}

			// Restores truth values
			for (int i = 0; i < atoms.size(); i++)
				atoms.get(i).setValue(truthValues[i]);

			// Clears atom list
			atoms.clear();

			// Weights and adds to total
			inc *= groundRule.getWeight();
			totalInc += inc;
		}
		return totalInc;
	}

	/**
	 * Computes the expected weighted compatibility (1 - incompatibility)
	 * of a collection of WeightedGroundRules.
	 * from independently rounding each {@link RandomVariableAtom} to 1.0 or 0.0
	 * with probability equal to its current truth value.
	 *
	 * WARNING: the result of this function is incorrect if the RandomVariableAtoms
	 * are subject to any {@link UnweightedGroundRule GroundConstraintRules}.
	 *
	 * WARNING: This method does not account for GroundCompatibilityRules that
	 * were not grounded because they are trivially satisfied.
	 */
	public static double getExpectedTotalWeightedCompatibility(Iterable<WeightedGroundRule> groundRules) {
		double totalInc = 0.0;
		for (WeightedGroundRule groundRule : groundRules) {
			totalInc += getExpectedWeightedCompatibility(groundRule);
		}

		return totalInc;
	}

	/**
	 * Computes the expected weighted compatibility (1 - incompatibility)
	 * of a WeightedGroundRule.
	 * from independently rounding each {@link RandomVariableAtom} to 1.0 or 0.0
	 * with probability equal to its current truth value.
	 *
	 * WARNING: the result of this function is incorrect if the RandomVariableAtoms
	 * are subject to any {@link UnweightedGroundRule GroundConstraintRules}.
	 *
	 * WARNING: This method does not account for GroundCompatibilityRules that
	 * were not grounded because they are trivially satisfied.
	 */
	public static double getExpectedWeightedCompatibility(WeightedGroundRule groundRule) {
		double inc = 0.0;
		List<RandomVariableAtom> atoms = new ArrayList<RandomVariableAtom>();

		// Collects RandomVariableAtoms
		for (GroundAtom atom : groundRule.getAtoms())
			if (atom instanceof RandomVariableAtom)
				atoms.add((RandomVariableAtom) atom);

		// Collects truth values
		double[] truthValues = new double[atoms.size()];
		for (int i = 0; i < truthValues.length; i++)
			truthValues[i] = atoms.get(i).getValue();

		// Sums over settings
		for (int i = 0; i < Math.pow(2, atoms.size()); i++) {
			double assignmentProb = 1.0;

			// Sets assignment and computes probability
			for (int j = 0; j < atoms.size(); j++) {
				int assignment = ((i >> j) & 1);
				atoms.get(j).setValue(assignment);
				assignmentProb *= (assignment == 1) ? truthValues[j] : 1 - truthValues[j];
			}

			inc += assignmentProb * (1 - groundRule.getIncompatibility());
		}

		// Restores truth values
		for (int i = 0; i < atoms.size(); i++)
			atoms.get(i).setValue(truthValues[i]);

		// Weights and returns
		return inc * groundRule.getWeight();
	}

	/** Compute the rounding probability function for a given truth value */
	public static double roundingProbability(Double truthValue) {
		return 0.25 + 0.5 * truthValue;
	}

	/**
	 * Computes the expected weighted compatibility (1 - incompatibility)
	 * of a WeightedGroundLogicalRule.
	 * from independently rounding each {@link RandomVariableAtom} to 1.0 or 0.0
	 * with probability equal to its current truth value.
	 *
	 * WARNING: the result of this function is incorrect if the RandomVariableAtoms
	 * are subject to any {@link UnweightedGroundRule UnweightedGroundRule}.
	 *
	 * WARNING: This method does not account for WeightedGroundLogicalRules that
	 * were not grounded because they are trivially satisfied.
	 */
	public static double getExpectedWeightedLogicalCompatibility(WeightedGroundLogicalRule groundRule) {
		// Collects GroundAtom values. Atoms in GroundRule are already negated
		List<Double> posRvValues = new ArrayList<Double>();
		List<Double> posObsValues = new ArrayList<Double>();
		List<Double> negRvValues = new ArrayList<Double>();
		List<Double> negObsValues = new ArrayList<Double>();
		for (GroundAtom atom : groundRule.getPositiveAtoms())
			if (atom instanceof RandomVariableAtom)
				negRvValues.add(atom.getValue());
			else
				negObsValues.add(atom.getValue());
		for (GroundAtom atom : groundRule.getNegativeAtoms())
			if (atom instanceof RandomVariableAtom)
				posRvValues.add(atom.getValue());
			else
				posObsValues.add(atom.getValue());

		// Sum of observed atoms
		double observedSum = 0;
		for (double posObsValue : posObsValues)
			observedSum += posObsValue;
		for (double negObsValue : negObsValues)
			observedSum += (1 - negObsValue);
		observedSum = Math.min(1, observedSum);

		// P(sum of RV >= 1)
		double rvSumLessThanOne = 1.0;
		for (double posRvValue: posRvValues)
			rvSumLessThanOne *= 1 - roundingProbability(posRvValue);
		for (double negRvValue: negRvValues)
			rvSumLessThanOne *= roundingProbability(negRvValue);
		double rvSumAtleastOne = 1.0 - rvSumLessThanOne;

		// 2 cases: If rvSumAtleastOne, then compatibility = 1, else = observedSum.
		double compat = rvSumAtleastOne + rvSumLessThanOne * observedSum;
		return groundRule.getWeight() * compat;
	}

	/**
	 * Computes the weighted probability WeightedGroundLogicalRule is satisfied.
	 * from independently rounding each {@link RandomVariableAtom} to 1.0 or 0.0
	 * with probability equal to its current truth value.
	 *
	 * WARNING: this function will throw an exception if any GroundAtoms are soft-valued.
	 *
	 * WARNING: the result of this function is incorrect if the RandomVariableAtoms
	 * are subject to any {@link UnweightedGroundRule UnweightedGroundRule}.
	 *
	 * WARNING: This method does not account for WeightedGroundLogicalRules that
	 * were not grounded because they are trivially satisfied.
	 */
	public static double getExpectedWeightedLogicalSatisfaction(WeightedGroundLogicalRule groundRule) {

		// Collects GroundAtoms. Atoms in GroundRule are already negated
		List<GroundAtom> posAtoms = new ArrayList<GroundAtom>();
		List<GroundAtom> negAtoms = new ArrayList<GroundAtom>();
		for (GroundAtom atom : groundRule.getPositiveAtoms())
			if (!(atom instanceof RandomVariableAtom) &&
				(atom.getValue() > 0) &&
				(atom.getValue() < 1))
				throw new IllegalArgumentException("Soft observed values are not supported");
			else
				negAtoms.add((GroundAtom) atom);
		for (GroundAtom atom : groundRule.getNegativeAtoms())
			if (!(atom instanceof RandomVariableAtom) &&
				(atom.getValue() > 0) &&
				(atom.getValue() < 1))
				throw new IllegalArgumentException("Soft observed values are not supported");
			else
				posAtoms.add((GroundAtom) atom);

		// Collects truth values
		double[] positiveValues = new double[posAtoms.size()];
		double[] negativeValues = new double[negAtoms.size()];
		for (int i = 0; i < positiveValues.length; i++) {
			if (posAtoms.get(i) instanceof RandomVariableAtom)
				positiveValues[i] = roundingProbability(posAtoms.get(i).getValue());
			else
				positiveValues[i] = posAtoms.get(i).getValue();
		}
		for (int i = 0; i < negativeValues.length; i++) {
			if (negAtoms.get(i) instanceof RandomVariableAtom)
				negativeValues[i] = roundingProbability(negAtoms.get(i).getValue());
			else
				negativeValues[i] = negAtoms.get(i).getValue();
		}

		// Product of rounding probabilities
		double unsat = 1.0;
		for (double positiveValue: positiveValues)
			unsat *= 1 - positiveValue;
		for (double negativeValue: negativeValues)
			unsat *= negativeValue;

		// Weights and returns
		return groundRule.getWeight() * (1 - unsat);
	}

	/**
	 * Computes the Euclidean norm of the infeasibilities of an iterable container
	 * of {@link UnweightedGroundRule GroundConstraintRules}.
	 *
	 * @param groundRules  the GroundConstraintRules
	 * @return the Euclidean norm of the infeasibilities
	 * @see UnweightedGroundRule#getInfeasibility()
	 */
	public static double getInfeasibilityNorm(Iterable<UnweightedGroundRule> groundRules) {
		double inf, norm = 0.0;
		for (UnweightedGroundRule groundRule : groundRules) {
			inf = groundRule.getInfeasibility();
			norm += inf * inf;
		}
		return Math.sqrt(norm);
	}
}
