package aspect.sentiment.visualization;

import java.util.HashSet;
import java.util.Set;

public class AspectBasedSentimentScoreCard {
	AspectBasedSentimentScoreCard() {}
	public static int NEGATIVE = -1;
	public static int POSITIVE = 1;
	
	int sentiment;
	String subject;
	String stemmedSubject;
	Set<String> adjectives = new HashSet();
}

