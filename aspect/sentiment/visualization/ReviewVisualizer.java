package aspect.sentiment.visualization;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import porter.*;

/*
 * Aspect based Sentiment Analysis.
An alternate visualization of TripAdvisor Reviews. Uses Stanford Parser for POS Tagging & Dependency Parsing i.e
we isolate the Nouns & their related Adjectives.
Uses a Sentiment Lexicon from http://www.cs.uic.edu/~liub/FBS/sentiment-analysis.html to classify the Adjectives as
either Positive or Negative.
Each mention of a Noun ("Subject"), increments it's Weight (Positive/Negative).
This outputs an Excel file that drives the Visualization.
Visualization is achieved via graphics from http://raw.densitydesign.org/.
Sample input file, intermediate Excel file & the final output are attached.
 */
public class ReviewVisualizer {
	
    static TokenizerFactory<CoreLabel> tokenizerFactory =
            PTBTokenizer.factory(new CoreLabelTokenFactory(), "");
    static LexicalizedParser lp;
    static List<String>  positiveWords = null;
    static List<String>  negativeWords = null;
    static List<AspectBasedSentimentScoreCard> sentimentList = new ArrayList<AspectBasedSentimentScoreCard>();
    static TreebankLanguagePack tlp = null; // PennTreebankLanguagePack for English
    static GrammaticalStructureFactory gsf = null;
    
    static {
        String parserModel = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";
        String[] options = { "-maxLength", "80", "-retainTmpSubcategories" };
        lp = LexicalizedParser.loadModel(parserModel, options);    	
        tlp = lp.getOp().langpack(); //lp.treebankLanguagePack(); // PennTreebankLanguagePack for English
        gsf = tlp.grammaticalStructureFactory();
        
        try {
			positiveWords = Files.readAllLines(Paths.get("E:\\SRC\\StanfordParser\\src\\positive-words.txt"), Charset.defaultCharset());
			negativeWords = Files.readAllLines(Paths.get("E:\\SRC\\StanfordParser\\src\\negative-words.txt"), Charset.defaultCharset());			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public static void visualizeTripAdvisorReviews (String fileName) throws Exception {
		String fileAsString = readFile(fileName, Charset.defaultCharset());
		JSONObject obj = new JSONObject(fileAsString);
		JSONArray array = obj.getJSONArray("Reviews");
		
		int reviews = 0;
		for (int i = 0; i < array.length(); i++) {
			reviews++;
			System.out.println("Processing Review # "+ reviews);
			String review = array.getJSONObject(i).getString("Content");
			Reader reader = new StringReader(review);
			DocumentPreprocessor dp = new DocumentPreprocessor(reader);
			
			//process each sentence
			for (List<HasWord> sentence : dp) {
				   String sentenceString = Sentence.listToString(sentence);

				   Tokenizer<CoreLabel> tok =
						   tokenizerFactory.getTokenizer(new StringReader(sentenceString));
				   List<CoreLabel> rawWords = tok.tokenize();
				   Tree parse = lp.apply(rawWords);
				   List<TaggedWord>  taggedWords = parse.taggedYield();
				   List<TaggedWord> adjectives = getAdjectives(taggedWords);
				   List<TaggedWord> nouns = getNouns(taggedWords);

				   GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
				   List<TypedDependency> tdl = gs.typedDependenciesCCprocessed(); 
		
				   for (TypedDependency td : tdl) {			    		
					   for (TaggedWord tw: adjectives) {
						   if ( td.reln().getShortName().equals("nsubj") && td.gov().originalText().contains(tw.value())) {
							   //check that dependent is a noun
							   if ( isDependentANoun(td.dep().word(), nouns) ) {				    			
								   AspectBasedSentimentScoreCard sc = new AspectBasedSentimentScoreCard();
								   if (positiveWords.contains(td.gov().originalText()))
									   sc.sentiment = AspectBasedSentimentScoreCard.POSITIVE;
								   else
									   sc.sentiment = AspectBasedSentimentScoreCard.NEGATIVE;
								   //sc.subject = td.dep().value().toUpperCase();
								   Stemmer stemmer = new Stemmer();
								   String subject = td.dep().value().toLowerCase();
								   stemmer.add(subject.toCharArray(), subject.length());
								   stemmer.stem();
								   sc.stemmedSubject = stemmer.toString();			
								   sc.subject = stemmer.toString();	
								   sc.adjectives.add(tw.value());
								   sentimentList.add(sc);	
							   }
						   }
					   }
				   }
			}
		}
		printSentiment("Report");    	
    }
    
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String fileName = "E:\\SRC\\StanfordParser\\src\\100407.json";
		try {
			visualizeTripAdvisorReviews(fileName);
			} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	static boolean isDependentANoun(String word, List<TaggedWord> nouns) {
		for (TaggedWord tw: nouns) {
			if (tw.value().equals(word))
				return true;
		}
		return false;
	}
	
	static void printSentiment(String fileName) throws Exception {
		StringBuffer sbf = new StringBuffer();
		Set<String> subjects = new HashSet<String>();
		for (AspectBasedSentimentScoreCard sc : sentimentList) {
			System.out.println("Sentiment " + sc.sentiment + " Subject " + sc.subject + " Attributes " + sc.adjectives);
			subjects.add(sc.subject);
		}
		
		sbf.append("Polarity" + "," + "Subject" + "," + "Attributes" + "," + "Weight");
		sbf.append(System.getProperty("line.separator"));
		for (String sub: subjects) {
			Set<String> posAdjectives = new HashSet<String>();		
			int weight = 0;
			for (AspectBasedSentimentScoreCard sc : sentimentList) {
				if (sc.subject.equalsIgnoreCase(sub) && sc.sentiment == AspectBasedSentimentScoreCard.POSITIVE && sc.adjectives.size() > 0)
				{
					posAdjectives.addAll(sc.adjectives);
					++weight;
				}
			}
			if (posAdjectives.size() == 0)
				continue;
			String adjective = "\"";
			for (String str: posAdjectives) {
				adjective += str + " ";
			}
			adjective += "\"";
			sbf.append(AspectBasedSentimentScoreCard.POSITIVE + ", " + sub.toUpperCase() + " ," + adjective + " ," + weight);
			sbf.append(System.getProperty("line.separator"));			
		}

		//avoid copy paste!
		for (String sub: subjects) {
			Set<String> posAdjectives = new HashSet<String>();			
			int weight = 0;			
			for (AspectBasedSentimentScoreCard sc : sentimentList) {
				if (sc.subject.equalsIgnoreCase(sub) && sc.sentiment == AspectBasedSentimentScoreCard.NEGATIVE && sc.adjectives.size() > 0)
				{
					posAdjectives.addAll(sc.adjectives);
					++weight;
				}
			}
			if (posAdjectives.size() == 0)
				continue;
			String adjective = "\"";
			for (String str: posAdjectives) {
				adjective += str + " ";
			}
			adjective += "\"";			
			sbf.append(AspectBasedSentimentScoreCard.NEGATIVE + ", " + sub + " ," + adjective + "," + weight );
			sbf.append(System.getProperty("line.separator"));			
		}
		
		BufferedWriter bwr = new BufferedWriter(new FileWriter(new File("E:\\SRC\\StanfordParser\\src\\" + fileName + ".csv")));
		bwr.write(sbf.toString());
		bwr.flush();
		bwr.close();		
	}
	
	static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}
	
	private static List<TaggedWord> getAdjectives(List<TaggedWord>  taggedWords) {
		List<TaggedWord> verbs = new ArrayList<TaggedWord>();
		for (TaggedWord tw: taggedWords) {
			if (tw.tag().contains("JJ") && 
					(positiveWords.contains(tw.value()) || negativeWords.contains(tw.value()))
					) // || tw.tag().contains("VB")
				verbs.add(tw);
		}
		return verbs;
	}

	private static List<TaggedWord> getNouns(List<TaggedWord>  taggedWords) {
		List<TaggedWord> verbs = new ArrayList<TaggedWord>();
		for (TaggedWord tw: taggedWords) {
			if (tw.tag().contains("NN") ) // || tw.tag().contains("VB")
				verbs.add(tw);
		}
		return verbs;
	}
	
}
