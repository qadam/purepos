package hu.ppke.itk.nlpg.purepos.model.internal;

import hu.ppke.itk.nlpg.purepos.model.INGramFrequencyModel;
import hu.ppke.itk.nlpg.purepos.model.INGramProbabilityModel;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * N-gram model implementation which uses tries to store these elements.
 * (Similar to SRILM.)
 * 
 * Tries are stored in HashTables, for the sake of efficiency.
 * 
 * @author György Orosz
 * 
 * @param <C>
 *            context type
 * @param <W>
 *            word type
 */
public class IntegerTrieNGramModel<W> extends INGramFrequencyModel<Integer, W> {

	protected static class IntTrieNode<T> extends TrieNode<Integer, Integer, T> {
		IntTrieNode(Integer id) {
			super(id);
		}

		public IntTrieNode(Integer id, T word) {
			super(id, word);
		}

		@Override
		protected Integer zero() {
			return 0;
		}

		@Override
		protected Integer increment(Integer n) {
			return n + 1;
		}

		@Override
		protected TrieNode<Integer, Integer, T> createNode(Integer id) {
			return new IntTrieNode<T>(id);
		}

	};

	protected IntTrieNode<W> root;
	/*
	 * lambda1 is at position 1 and so on; lamda0 is seen to be used in Hunpos
	 * when calculating probs:
	 * 
	 * P(C| A B) = l3 * ML (C| A B) + l2 * ML (C | B) + l1 * ML (C) + l0
	 */
	protected ArrayList<Double> lambdas;

	public IntegerTrieNGramModel(int n) {
		super(n);
		root = new IntTrieNode<W>(IntVocabulary.getExtremalElement());
	}

	@Override
	public void addWord(List<Integer> context, W word) {
		// TODO: is there any space for performance improvement? arraylist or
		// linkedlist?
		ListIterator<Integer> iterator = context.listIterator(context.size());
		IntTrieNode<W> act = root;
		int i = 0;
		int size = n - 1;
		act.addWord(word);
		while (iterator.hasPrevious() && i < size) {
			act = (IntTrieNode<W>) act.addChild(iterator.previous());
			act.addWord(word);
			i++;
		}

	}

	@Override
	public List<Double> getWordFrequency(List<Integer> context, W word) {
		ArrayList<Double> ret = new ArrayList<Double>();

		ret.add(calculateFreq(root, word));
		if (!(context == null || context.size() == 0)) {
			ListIterator<Integer> it = context.listIterator(context.size());
			Integer previous;
			IntTrieNode<W> actNode = root;
			while (it.hasPrevious() && actNode != null) {
				previous = it.previous();
				if (actNode.hasChild(previous)) {
					actNode = (IntTrieNode<W>) actNode.getChild(previous);
					ret.add(calculateFreq(actNode, word));
				} else {
					ret.add(0.0);
					while (it.hasPrevious()) {
						ret.add(0.0);
					}
					actNode = null;
				}
			}

		}

		return ret;
	}

	public Double calculateFreq(IntTrieNode<W> node, W word) {
		if (node.hasWord(word)) {
			return (double) node.getWord(word) / (double) node.getNum();
		} else {
			return 0.0;
		}
	}

	@Override
	public int getTotalFrequency() {
		return root.getNum();
	}

	@Override
	public INGramProbabilityModel<Integer, W> createProbabilityModel() {
		// TODO Auto-generated method stub
		return null;
	}

	protected double calculateValue(TrieNode<Integer, Integer, W> node, W word) {
		double nodeFreq = node.getNum();
		double wFreq = node.getWord(word);
		if (nodeFreq == 1 || wFreq == -1)
			return -1;
		else
			// TODO: what if we would substract 0.5 instead of 1?
			return (wFreq - 1) / (nodeFreq - 1);

	}

	public Pair<Integer, Double> findMax(
			ArrayList<TrieNode<Integer, Integer, W>> list, W word) {

		Integer maxPos;
		Double maxVal;
		if (!(list == null || list.size() == 0)) {
			maxPos = 0;
			maxVal = calculateValue(list.get(0), word);
			for (int i = 1; i < list.size(); ++i) {
				double val = calculateValue(list.get(i), word);
				if (val >= maxVal) {
					// equality is important since we need the longest n-gram
					maxPos = i;
					maxVal = val;
				}

			}
		} else {
			maxPos = null;
			maxVal = null;
		}
		ImmutablePair<Integer, Double> ret = new ImmutablePair<Integer, Double>(
				maxPos, maxVal);
		return ret;
	}

	@Override
	protected void calculateNGramLamdas() {
		adjustLamdas();
		double sum = 0.0;
		for (Double e : lambdas) {
			sum += e;
		}
		if (sum > 0)
			for (int i = 0; i < lambdas.size(); ++i) {
				lambdas.set(i, lambdas.get(i) / sum);
			}
	}

	public void adjustLamdas() {
		lambdas = new ArrayList<Double>();
		for (int i = 0; i < n + 1; ++i) {
			lambdas.add(0.0);
		}
		ArrayList<TrieNode<Integer, Integer, W>> acc = new ArrayList<TrieNode<Integer, Integer, W>>();
		iterate(root, acc);
	}

	protected void iterate(TrieNode<Integer, Integer, W> node,
			ArrayList<TrieNode<Integer, Integer, W>> acc) {
		acc.add(node);
		if (node.getChildNodes() == null || node.getChildNodes().size() == 0) {
			for (W word : node.getWords().keySet()) {
				Pair<Integer, Double> max = findMax(acc, word);
				int index = max.getKey() + 1;
				if (max.getValue() != -1)
					lambdas.set(index, lambdas.get(index) + node.getWord(word));
			}
		} else {
			for (TrieNode<Integer, Integer, W> child : node.getChildNodes()
					.values()) {
				iterate(child, acc);

			}
		}
		acc.remove(acc.size() - 1);
	}
}