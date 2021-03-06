/*******************************************************************************
 * Copyright (c) 2012 György Orosz, Attila Novák.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/
 * 
 * This file is part of PurePos.
 * 
 * PurePos is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * PurePos is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * Contributors:
 *     György Orosz - initial API and implementation
 ******************************************************************************/
package hu.ppke.itk.nlpg.purepos.decoder;

import hu.ppke.itk.nlpg.purepos.model.internal.CompiledModel;
import hu.ppke.itk.nlpg.purepos.model.internal.NGram;
import hu.ppke.itk.nlpg.purepos.morphology.IMorphologicalAnalyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

/**
 * Decoder that implements the Viterbi search method speed up with using beams.
 * 
 * @author György Orosz
 * 
 */
public class BeamedViterbi extends AbstractDecoder {
	public BeamedViterbi(CompiledModel<String, Integer> model,
			IMorphologicalAnalyzer morphologicalAnalyzer, double logTheta,
			double sufTheta, int maxGuessedTags) {
		super(model, morphologicalAnalyzer, logTheta, sufTheta, maxGuessedTags);

	}

	// protected Logger logger = Logger.getLogger(this.getClass());

	@Override
	public List<Pair<List<Integer>, Double>> decode(List<String> observations,
			int maxResultsNumber) {
		List<String> obs = prepareObservations(observations);
		NGram<Integer> startNGram = createInitialElement();

		List<Pair<List<Integer>, Double>> tagSeqList = beamedSearch(startNGram,
				obs, maxResultsNumber);
		List<Pair<List<Integer>, Double>> ret = cleanResults(tagSeqList);
		return ret;
	}

	//
	// public List<Integer> beamedSearch(final NGram<Integer> start,
	// final List<String> obs) {
	// return beamedSearch(start, obs, 1).get(0);
	//
	// }

	public List<Pair<List<Integer>, Double>> beamedSearch(
			final NGram<Integer> start,
			final List<String> observations,
			int resultsNumber) {
		HashMap<NGram<Integer>, Node> beam = new HashMap<NGram<Integer>, Node>();

		beam.put(start, startNode(start));
		boolean isFirst = true;
		int pos = 0;
		for (String obs : observations) {

			HashMap<NGram<Integer>, Node> newBeam = new HashMap<NGram<Integer>, Node>();

			Table<NGram<Integer>, Integer, Double> nextProbs = HashBasedTable
					.create();
			Map<NGram<Integer>, Double> obsProbs = new HashMap<NGram<Integer>, Double>();
			Set<NGram<Integer>> contexts = beam.keySet();

			Map<NGram<Integer>, Map<Integer, Pair<Double, Double>>> nexts = getNextProbs(
					contexts, obs, pos, isFirst);

			for (Map.Entry<NGram<Integer>, Map<Integer, Pair<Double, Double>>> nextsEntry : nexts
					.entrySet()) {
				NGram<Integer> context = nextsEntry.getKey();
				Map<Integer, Pair<Double, Double>> nextContextProbs = nextsEntry
						.getValue();
				for (Map.Entry<Integer, Pair<Double, Double>> entry : nextContextProbs
						.entrySet()) {
					Integer tag = entry.getKey();
					nextProbs.put(context, tag, entry.getValue().getLeft());
					obsProbs.put(context.add(tag), entry.getValue().getRight());
				}
			}


			for (Cell<NGram<Integer>, Integer, Double> cell : nextProbs
					.cellSet()) {
				Integer nextTag = cell.getColumnKey();
				NGram<Integer> context = cell.getRowKey();
				Double transVal = cell.getValue();
				NGram<Integer> newState = context.add(nextTag);
				Node from = beam.get(context);
				double newVal = transVal + beam.get(context).getWeight();
				update(newBeam, newState, newVal, from);
			}

			if (nextProbs.size() > 1)
				for (NGram<Integer> tagSeq : newBeam.keySet()) {
					Node node = newBeam.get(tagSeq);
					Double obsProb = obsProbs.get(tagSeq);
					node.setWeight(obsProb + node.getWeight());
				}

			beam = prune(newBeam);
			isFirst = false;
			++pos;
		}
		return findMax(beam, resultsNumber);
	}

	private List<Pair<List<Integer>, Double>> findMax(
			final HashMap<NGram<Integer>, Node> beam, int resultsNumber) {

		// Node max = Collections.max(beam.values());
		// Node act = max;
		// return decompose(max);

		SortedSet<Node> sortedKeys = new TreeSet<Node>(beam.values());

		List<Pair<List<Integer>, Double>> ret = new ArrayList<Pair<List<Integer>, Double>>();
		Node max;
		for (int i = 0; i < resultsNumber && !sortedKeys.isEmpty(); ++i) {
			max = sortedKeys.last();
			sortedKeys.remove(max);
			List<Integer> maxTagSeq = decompose(max);
			ret.add(Pair.of(maxTagSeq, max.weight));
		}
		return ret;

	}

	private HashMap<NGram<Integer>, Node> prune(final HashMap<NGram<Integer>, Node> beam) {
		HashMap<NGram<Integer>, Node> ret = new HashMap<NGram<Integer>, Node>();
		Node maxNode = Collections.max(beam.values());
		Double max = maxNode.getWeight();
		for (NGram<Integer> key : beam.keySet()) {
			Node actNode = beam.get(key);
			Double actVal = actNode.getWeight();
			if (!(actVal < max - logTheta)) {
				ret.put(key, actNode);
			}
		}
		return ret;
	}

	private void update(HashMap<NGram<Integer>, Node> beam,
			NGram<Integer> newState, Double newWeight, Node fromNode) {

		if (!beam.containsKey(newState)) {
			// logger.trace("\t\t\tAS: " + newNGram + " from " + context
			// + " with " + newValue);

			beam.put(newState, new Node(newState, newWeight, fromNode));

		} else if (beam.get(newState).getWeight() < newWeight) {
			// logger.trace("\t\t\tUS: " + old + " to " + newNGram + " from "
			// + context + " with " + newValue);
			beam.get(newState).setPrevious(fromNode);
			beam.get(newState).setWeight(newWeight);
		} else {
			// logger.trace("\t\t\tNU: " + old + " to " + newNGram + " from "
			// + context + " with " + newValue);
		}
	}

}
