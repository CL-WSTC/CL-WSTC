package basemodel;

import java.io.*;
import java.util.*;

public class WordSimilarityAvgData_Extension {

    public HashSet<String> wordSet = new HashSet<>();
    public List<String[]> seedList = new ArrayList<>();
    public HashMap<String, Integer> word2IdVocabulary; // Vocabulary to get ID
    public HashMap<Integer, String> id2WordVocabulary; // Vocabulary to get word
    public double[][] wordEmbedding;
    int K; // Word vector dimension
    double hmin=0.0001;

    public void script(String docname,String seedname,String vecfile, String wordsiminame,int buffer_num) {
        LoadDoc(docname);
        LoadSeed(seedname);
        readGloveWE(vecfile);
        OutWordSimi(wordsiminame,buffer_num);
    }

    public static int getNum(String name) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(name));
		String str=br.readLine();
        return Integer.parseInt(str);
	}
    
    private void OutWordSimi(String wordsiminame,int buffer_num) {
        try {
            BufferedWriter docbw = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(wordsiminame), "utf-8"));

            for (String word:wordSet) {
                String outline = GetBitermMaxSimi(word,buffer_num);
                docbw.write(outline);
                docbw.flush();
            }
            docbw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String GetBitermMaxSimi(String word,int buffer_num) {

        String[] seedarray;
        String seedword;
        double simivalue;
        String outline = word;
        
        for (int i = 0; i < seedList.size(); i++) {
            seedarray = seedList.get(i);
            simivalue = 0;
            List<Double> similist = new ArrayList<>();
            for (int j = 0; j<seedarray.length; j++) {
                seedword = seedarray[j];
                simivalue = similarity(seedword, word);
                similist.add(simivalue);
            }
            Collections.sort(similist);

            simivalue = similist.get(similist.size()-1);

            simivalue = Math.max(simivalue,0);
            outline+=","+simivalue;
        }

        for(int i=0;i<buffer_num;i++)
        	outline +=","+Math.random();

        outline += "\r\n";
        return outline;
    }


    private void LoadSeed(String seedname) {
        BufferedReader br = null;
        int lineId = -1;
        try {
            br = new BufferedReader(new FileReader(seedname));
            for (String doc; (doc = br.readLine()) != null;) {
                lineId++;
                if (doc.trim().length() == 0) {
                    System.out.println("doc "+lineId+ " is empty'");
                    continue;
                }
                String[] words = doc.trim().split(" ");
                seedList.add(words);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void LoadDoc(String docname) {

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(docname));
            for (String doc; (doc = br.readLine()) != null;) {

                if (doc.trim().length() == 0)
                    continue;

                if(doc.indexOf(",")>=0)
                    doc = doc.substring(doc.indexOf(",")+1);

                String[] words = doc.trim().split("\\s+");
                for (int i = 0; i < words.length; i++) {
                    wordSet.add(words[i]);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void readGloveWE(String filename) {
		word2IdVocabulary = new HashMap<String,Integer>();
		id2WordVocabulary = new HashMap<Integer,String>();
		BufferedReader br;
		try
		{
			// find the number of dimensions and words
			br = new BufferedReader(new FileReader(filename));
			String line = br.readLine();
			line = line.trim();
			K = line.split("\\s+").length-1;
			int numWords = 1;
			while ((line = br.readLine()) != null) 
			{
				numWords++;
			}
			br.close();
			
//			System.out.println("number of dimensions: " + K);
//			System.out.println("number of words: " + numWords);

			br = new BufferedReader(new FileReader(filename));
			wordEmbedding = new double[numWords][K];
			int i = 0;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				String[] info = line.split("\\s+");
				word2IdVocabulary.put(info[0], i);
				id2WordVocabulary.put(i, info[0]);
				for (int j = 0; j < K; j++)
					wordEmbedding[i][j] = Double.parseDouble(info[j+1]);
				i++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    
	public double similarity(String word1,String word2) {
		int id1=0,id2=0;
		double simi=0;
		if (word2IdVocabulary.containsKey(word1)) {
			id1 = word2IdVocabulary.get(word1);
        }
		if (word2IdVocabulary.containsKey(word2)) {
			id2 = word2IdVocabulary.get(word2);
        }
		double a=0,b=0,c=0;
		for(int i=0;i<K;i++) {
			a+=wordEmbedding[id1][i]*wordEmbedding[id2][i];
			b+=wordEmbedding[id1][i]*wordEmbedding[id1][i];
			c+=wordEmbedding[id2][i]*wordEmbedding[id2][i];
		}
		simi=a/(Math.sqrt(b)*Math.sqrt(c));
		if(simi<=0)
			simi=hmin;
		return simi;
	}
}

