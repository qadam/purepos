package hu.ppke.itk.nlpg.purepos.model.internal;

import hu.ppke.itk.nlpg.purepos.common.Statistics;
import hu.ppke.itk.nlpg.purepos.common.lemma.ILemmaTransformation;
import hu.ppke.itk.nlpg.purepos.model.ICombiner;
import hu.ppke.itk.nlpg.purepos.model.INGramModel;
import hu.ppke.itk.nlpg.purepos.model.SuffixTree;

import java.io.Serializable;
import java.util.List;

public abstract class AbstractRawModelData<W, T extends Comparable<T>>
		implements Serializable {

	private static final long serialVersionUID = 3166238007676012545L;

	public Statistics stat;
	public INGramModel<T, T> tagNGramModel;
	public INGramModel<T, W> stdEmissionNGramModel;
	public INGramModel<T, W> specEmissionNGramModel;
	public T eosTag;
	public SuffixTree<W, ILemmaTransformation<W, Integer>> lemmaSuffixTree;
	public SuffixTree<W, String> lemmaFreqTree;
	public LemmaUnigramModel<W> lemmaUnigramModel;
	public HashSuffixTree<T> lowerSuffixTree;
	public HashSuffixTree<T> upperSuffixTree;
	public List<Double> lemmaLambdas;
	public ICombiner combiner;

}
