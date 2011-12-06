package hu.ppke.itk.nlpg.purepos.cli;

import hu.ppke.itk.nlpg.docmodel.ISentence;
import hu.ppke.itk.nlpg.docmodel.IToken;
import hu.ppke.itk.nlpg.purepos.Tagger;
import hu.ppke.itk.nlpg.purepos.Trainer;
import hu.ppke.itk.nlpg.purepos.model.Model;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class SimpleTest implements Runnable {

	protected Model<String, Integer> model;
	protected Tagger tagger;
	protected Trainer trainer;
	protected final String trainingCorpusPath;

	public SimpleTest(String trainingCorpusPath) {
		this.trainingCorpusPath = trainingCorpusPath;
	}

	@Override
	public void run() {

		try {

			trainer = new Trainer(new File(trainingCorpusPath));
			model = trainer.trainModel(2, 2, 10, 10);
			tagger = new Tagger(model, Math.log(10), 20);

			BufferedReader is = new BufferedReader(new InputStreamReader(
					System.in));
			String inputLine;

			while ((inputLine = is.readLine()) != null) {

				ISentence s = tagger.tagSentence(inputLine);
				if (s != null) {
					for (IToken t : s) {
						System.out.print(t + " ");
					}
				}
				System.out.println();
			}
			is.close();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

	}

	public static void main(String[] args) {
		// MSZNY2011Demo demo = new MSZNY2011Demo("./res/testCorpus.txt");
		SimpleTest demo = new SimpleTest(args[0]);
		demo.run();
	}

}