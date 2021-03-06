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
package hu.ppke.itk.nlpg.purepos.common.lemma;

import org.apache.commons.lang3.tuple.Pair;

public class SuffixLemmaTransformation extends
		AbstractLemmaTransformation<Pair<String, Integer>> {

	private static final long serialVersionUID = 1160747425706872720L;

	public SuffixLemmaTransformation(String word, String lemma, Integer tag) {
		super(word, lemma, tag);
	}

	private static final int SHIFT = 100;

	@Override
	protected Pair<String, Integer> decode(String word, String stem, Integer tag) {
		int i;
		for (i = 0; i < word.length() && i < stem.length(); ++i) {
			if (word.charAt(i) != stem.charAt(i)) {
				break;
			}
		}
		String wordSuff = word.substring(i);
		int cutSize = wordSuff.length();
		String lemmaSuff = stem.substring(i);

		int code = SHIFT * tag + cutSize;

		return Pair.of(lemmaSuff, code);
	}

	@Override
	protected Pair<String, Integer> encode(String word,
			Pair<String, Integer> representation) {
		int tagCode = representation.getRight() / SHIFT;
		int cutSize = representation.getRight() % SHIFT;
		String add = representation.getLeft();
		String lemma = word.substring(0, word.length() - cutSize) + add;
		return Pair.of(lemma, tagCode);
	}

	@Override
	public int minimalCutLength() {
		return representation.getRight() % SHIFT;
	}

}
