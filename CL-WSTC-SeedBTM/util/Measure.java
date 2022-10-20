package util;

public class Measure {
    //The number of documents in which word appears
    public static int DocumentFrequency(int[][] documents, int word) {
		int count = 0;
		for (int i = 0; i < documents.length; i++) {
			for (int j = 0; j < documents[i].length; j++) {
				if (documents[i][j] == word) {
					count++;
					break;
				}

			}
		}
		return count;
	}

    //The number of documents in which word_i and word_j coexist
    public static int DocumentFrequency(int[][] documents, int word_i, int word_j) {
		int count = 0;
		for (int i = 0; i < documents.length; i++) {

			boolean exsit_i = false;
			boolean exsit_j = false;
			for (int j = 0; j < documents[i].length; j++) {
				if (documents[i][j] == word_i) {
					exsit_i = true;
					break;
				}
			}

			for (int j = 0; j < documents[i].length; j++) {
				if (documents[i][j] == word_j) {
					exsit_j = true;
					break;
				}
			}
			if (exsit_i && exsit_j)
				count++;
		}
		return count;
	}
    
    
    public static double NPMI(int[][] documents, int word_i, int word_j) {
		int count_i = DocumentFrequency(documents, word_i);
		int count_j = DocumentFrequency(documents, word_j);
		int co_occur = DocumentFrequency(documents, word_i, word_j);
		double p_i = (double) count_i / documents.length;
		double p_j = (double) count_j / documents.length;
		double p_i_j = (double) co_occur / documents.length;
		double e = 0.0000000001;
//		if (p_i_j == 0)
//			e = 0.0001;
		double npmi = Math.log((p_i_j + e) / (p_i * p_j)) / -Math.log((p_i_j + e));
		return npmi;
	}
    
    public static double npmi_coherence(int[] top_words, int[][] documents) {
		double topic_npmi = 0;
		for (int j = 1; j < top_words.length; j++) {
			for (int i = 0; i < j; i++)
				topic_npmi += NPMI(documents, top_words[i], top_words[j]);
		}
		return topic_npmi;
	}
    
    public static double PMI(int[][] documents, int word_i, int word_j) {
		int count_i = DocumentFrequency(documents, word_i);
		int count_j = DocumentFrequency(documents, word_j);
		int co_occur = DocumentFrequency(documents, word_i, word_j);
		double p_i = (double) count_i / documents.length;
		double p_j = (double) count_j / documents.length;
		double p_i_j = (double) co_occur / documents.length;
		double e = 0.0000000001;//1;
//		if (p_i_j == 0)
//			e = 0.0001;
		double pmi = Math.log((p_i_j + e) / (p_i * p_j));
		return pmi;
	}
    
    public static double pmi_coherence(int[] top_words, int[][] documents) {
		double topic_pmi = 0;
		for (int j = 1; j < top_words.length; j++) {
			for (int i = 0; i < j; i++)
				topic_pmi += PMI(documents, top_words[i], top_words[j]);
		}
		return topic_pmi;
	}
    
	public static double coherence(int[] top_words, int[][] documents) {
		double coherence_score = 0.0;
		for (int m = 1; m < top_words.length; m++) {
			for (int l = 0; l < m; l++) {
				if (top_words[m] != top_words[l])
					coherence_score += Math.log((double) (DocumentFrequency(documents, top_words[m], top_words[l]) + 1)
							/ DocumentFrequency(documents, top_words[l]));
				else
					coherence_score += Math.log((double) 2 / DocumentFrequency(documents, top_words[l]));
			}		
		}
		return coherence_score;
	}
	
	public static double c_uci(int[] top_words, int[][] documents) {
		double coherence_score = 0.0;
		for (int m = 0; m < top_words.length-1; m++) {
			for (int l = m+1; l < top_words.length; l++) {
				coherence_score+=PMI(documents, top_words[m], top_words[l]);
			}		
		}
		coherence_score=coherence_score*2/(top_words.length*(top_words.length-1)); 
		return coherence_score;
	}

	public static double c_umass(int[] top_words, int[][] documents) {
		double coherence_score = 0.0,p_j,p_i_j;
		double e = 0.0000000001;
		for (int m = 1; m < top_words.length; m++) {
			for (int l = 0; l < m; l++) {		
				p_j = (double) DocumentFrequency(documents, top_words[l]) / documents.length;
				p_i_j = (double) DocumentFrequency(documents, top_words[m], top_words[l])  / documents.length;
				coherence_score += Math.log((p_i_j + e) / (p_j));
			}		
		}
		coherence_score=coherence_score*2/(top_words.length*(top_words.length-1)); 
		return coherence_score;
	}
}
