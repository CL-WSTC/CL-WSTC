package basemodel;

import util.FuncUtils;
import util.Measure;

import java.io.*;
import java.util.*;

/**
 * * BTM: A Java package for the short text topic models
 *
 * Implementation of the Biterm topic modeling, using collapsed Gibbs sampling, as described in:
 *
 * Xueqi Cheng, Xiaohui Yan, Yanyan Lan, and Jiafeng Guo. BTM: Topic Modeling over Short Texts.
 * In IEEE TRANSACTIONS ON KNOWLEDGE AND DATA ENGINEERING, 2014.
 *
 * @author: Jipeng Qiang on 18/6/6.
 */
public class SimiBTM_Seed {

    public double alpha; // Hyper-parameter alpha
    public double beta; // Hyper-parameter alpha
    public int numTopics; // Number of topics
    public int numIterations; // Number of Gibbs sampling iterations

    public int topWords; // Number of most probable words for each topic

    public double alphaSum; // alpha * numTopics
    public double betaSum; // beta * vocabularySize

    public int numDocuments; // Number of documents in the corpus
    public int numWordsInCorpus; // Number of words in the corpus

    public HashMap<String, Integer> word2IdVocabulary; // Vocabulary to get ID
    // given a word
    public HashMap<Integer, String> id2WordVocabulary; // Vocabulary to get word
    // given an ID
    public int vocabularySize; // The number of word types in the corpus

    int[][] wordId_of_corpus = null;
    public ArrayList<HashMap<Long,Integer>> biterm_of_corpus = new ArrayList<>();
    int[] doc_biterm_num ;
    ArrayList<Long> biterms = new ArrayList<>();

    int[] topic_of_biterms;

    int[][] topic_word_num;
    int[] num_of_topic_of_biterm;

    private HashMap<Long, Double> bitermSum = new HashMap<>();

    //public int[][] topicWordCount;
    // Total number of words assigned to a topic
    //public int[] sumTopicWordCount;

    // Double array used to sample a topic
    public double[] multiPros;

    // Path to the directory containing the corpus
    public String folderPath;
    // Path to the topic modeling corpus
    public String corpusPath;

    //seed
    public int seedvobSize = 5;
    ArrayList<ArrayList<Integer>> cate2wordlist = new ArrayList<>(); 
    double tao;
    public String seedPath;
    public ArrayList<HashSet<Long>>  biterm_of_seed = new ArrayList<>();
    public HashMap<Long, List<Integer>> biterm2cate = new HashMap<>();

    HashMap<Integer,double[]> wordsimimap = new HashMap<>();
    HashMap<Long,double[]> itemsimimap = new HashMap<>();

    public String expName = "BTMmodel";
    public String orgExpName = "BTMmodel";
    public String tAssignsFilePath = "";
    public int savestep = 0;

    public double initTime = 0;
    public double iterTime = 0;

    public int topseednum = 30;
    public double hmin = 0.2;

    double xmin = 0.3;

    public SimiBTM_Seed(String pathToCorpus, String pathToSeed, String pathToSimi, int inNumTopics,
                   double inAlpha, double inBeta, int inNumIterations, int inTopWords,
                   String inExpName,double hmin, int time)
            throws IOException
    {
        alpha = inAlpha;
        beta = inBeta;
        numTopics = inNumTopics;
        numIterations = inNumIterations;
        topWords = inTopWords;
        expName = inExpName;
        orgExpName = expName;
        corpusPath = pathToCorpus;
        seedPath = pathToSeed;
        this.hmin = hmin;
        //System.out.println("Reading topic modeling corpus: " + pathToCorpus);

        folderPath = "results_"+time+"/";
//        File dir = new File(folderPath);
//        if (!dir.exists())
//            dir.mkdir();

        word2IdVocabulary = new HashMap<String, Integer>();
        id2WordVocabulary = new HashMap<Integer, String>();
        //corpus = new ArrayList<List<Integer>>();
        ArrayList<int[]> tmpCorpus = new ArrayList<>();

        numDocuments = 0;
        numWordsInCorpus = 0;

        BufferedReader br = null;
        try {
            int indexWord = -1;
            br = new BufferedReader(new FileReader(pathToCorpus));
            for (String doc; (doc = br.readLine()) != null;) {

                if (doc.trim().length() == 0)
                    continue;

                String[] words = doc.trim().split("\\s+");
                //List<Integer> document = new ArrayList<Integer>();
                int [] document = new int[words.length];

                int ind = 0;
                for (String word : words) {
                    if (word2IdVocabulary.containsKey(word)) {
                        document[ind++] = word2IdVocabulary.get(word);

                    }
                    else {
                        indexWord += 1;
                        word2IdVocabulary.put(word, indexWord);
                        id2WordVocabulary.put(indexWord, word);

                        document[ind++] = indexWord;
                    }
                }

                numDocuments++;
                numWordsInCorpus += document.length;
                tmpCorpus.add(document);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //seed
        int lineId = -1;
        try {
            br = new BufferedReader(new FileReader(seedPath));
            for (String doc; (doc = br.readLine()) != null;) {
                lineId++;
                if (doc.trim().length() == 0) {

                    //System.out.println(numDocuments);
                    continue;
                }
                String[] words = doc.trim().split(" ");
                ArrayList<Integer> wordIdList = new ArrayList<>();
                for (int i = 0; i < words.length && i<topseednum; i++) {
                    if(word2IdVocabulary.containsKey(words[i]))
                        wordIdList.add(word2IdVocabulary.get(words[i]));
                }
                cate2wordlist.add(wordIdList);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        seedvobSize = vocabularySize;

        vocabularySize = word2IdVocabulary.size(); // vocabularySize = indexWord

        //topicWordCount = new int[numTopics][vocabularySize];

        //sumTopicWordCount = new int[numTopics];

        //pathToSimi
        String word;
        int wordid;
        lineId = -1;
        try {
            br = new BufferedReader(new FileReader(pathToSimi));
            for (String doc; (doc = br.readLine()) != null;) {
                lineId++;
                if (doc.trim().length() == 0) {
                    //System.out.println(numDocuments);
                    continue;
                }
//                doc = doc.replaceAll(","," ");
                String[] words = doc.trim().split(",");
                List<Integer> document = new ArrayList<Integer>();
                word  = words[0];
                wordid = word2IdVocabulary.get(word);
                double[] simiarray = new double[words.length-1];
                for (int j = 0; j < words.length-1; j++) {
                    String simistr = words[j+1];
                    simiarray[j] = Math.max(Double.valueOf(simistr),hmin);
                }
                wordsimimap.put(wordid,simiarray);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        multiPros = new double[numTopics];
        for (int i = 0; i < numTopics; i++) {
            multiPros[i] = 1.0 / numTopics;
        }

        alphaSum = numTopics * alpha;
        betaSum = vocabularySize * beta;

        this.doc_biterm_num = new int[tmpCorpus.size()];
        this.wordId_of_corpus = new int[tmpCorpus.size()][];
        for (int docIndex = 0; docIndex < this.wordId_of_corpus.length; docIndex++) {
            this.wordId_of_corpus[docIndex] = tmpCorpus.get(docIndex);
        }

//        System.out.println("Corpus size: " + numDocuments + " docs, "
//                + numWordsInCorpus + " words");
//        System.out.println("Vocabuary size: " + vocabularySize);
//        System.out.println("Number of topics: " + numTopics);
//        System.out.println("alpha: " + alpha);
//        System.out.println("beta: " + beta);
//        System.out.println("Number of sampling iterations: " + numIterations);
//        System.out.println("Number of top topical words: " + topWords);

        if (tAssignsFilePath.length() > 0)
            initialize();
        else
            initialize();
    }

    /**
     * Randomly initialize topic assignments
     */
    public void initialize()
            throws IOException
    {
        //System.out.println("Randomly initializing topic assignments using SeedBTM");
        HashSet<Long> itemset = new HashSet();
        long startTime = System.currentTimeMillis();

        int docIndex = 0;
        for(int[] doc:this.wordId_of_corpus){
            HashMap<Long, Integer> oneCop = new HashMap<>();
            for(int word1:doc){
                for(int word2:doc){
                    if(word1<word2){
                        Long itmeNum = (long)word1*1000000+word2;
                        if(!oneCop.containsKey(itmeNum)){
                            oneCop.put(itmeNum,0);
                        }
                        oneCop.put(itmeNum,oneCop.get(itmeNum)+1);
                        this.biterms.add(itmeNum);
                        itemset.add(itmeNum);
                        this.doc_biterm_num[docIndex] += 1;
                    }
                }
            }
            docIndex++;
            this.biterm_of_corpus.add(oneCop);
        }

        //calculate all regular item simi for seed topic
        for (Long item: itemset)
        {
            int word1 = (int)(item/1000000);
            int word2 = (int)(item%1000000);
            double[] simiword1 = wordsimimap.get(word1);
            double[] simiword2 = wordsimimap.get(word2);

            double[] simiitem = FuncUtils.meanarray(simiword1,simiword2);
//            double[] simiitem = FuncUtils.multipyarray(simiword1,simiword2);
//            double[] simiitem = FuncUtils.rootmultipyarray(simiword1,simiword2);
//            simiitem = FuncUtils.multipyarray(simiitem,simiitem1);

            itemsimimap.put(item,simiitem);
        }

        //seed
        //Initialize seed biterm
        for (int i = 0; i < cate2wordlist.size(); i++) {
            List<Integer> wordlist = cate2wordlist.get(i);
            HashSet<Long> bitermset = new HashSet<>();
            for (int j = 0; j < wordlist.size(); j++) {
                int word1 = wordlist.get(j);
                for (int k = 0; k < wordlist.size(); k++) {
                    int word2 = wordlist.get(k);
                    if(word1 < word2)
                    {
                        Long itmeNum = (long)word1*1000000+word2;
                        bitermset.add(itmeNum);
                        if (biterm2cate.containsKey(itmeNum))
                        {
                            biterm2cate.get(itmeNum).add(i);
                        }
                        else
                        {
                            List<Integer> catelist = new ArrayList<>();
                            catelist.add(i);
                            biterm2cate.put(itmeNum,catelist);
                        }
                    }
                }
            }
            biterm_of_seed.add(bitermset);
        }

        this.topic_of_biterms = new int[this.biterms.size()];

        this.topic_word_num = new int[this.vocabularySize][this.numTopics];
        this.num_of_topic_of_biterm = new int[this.numTopics];

        //seed
        List<Integer> seedlist;

        int topicId;
        for(int bitermIndex=0;bitermIndex<this.biterms.size();bitermIndex++){
            Long itmeNum = this.biterms.get(bitermIndex);
            int wordid1 = (int)(itmeNum%1000000);
            int wordid2 = (int)(itmeNum/1000000);
            //seed
            int buffer_x;

            buffer_x = 0;
//            double[] simiarray = itemsimimap.get(itmeNum);
            double[] simiarray = multiPros;
            topicId = FuncUtils.nextDiscrete(simiarray); // Sample a topic;

            this.topic_word_num[wordid1][topicId] += 1;
            this.topic_word_num[wordid2][topicId] += 1;

            this.num_of_topic_of_biterm[topicId] += 1;
            this.topic_of_biterms[bitermIndex] = topicId;

        }
        initTime =System.currentTimeMillis()-startTime;
    }

    public void inference()
            throws IOException
    {
        //System.out.println("Running Gibbs sampling inference: ");

        long startTime = System.currentTimeMillis();

        for (int iter = 0; iter < this.numIterations; iter++) {

//            if(iter%50 == 0)
//                System.out.print(" " + (iter));

            for(int bitermIndex = 0;bitermIndex<this.topic_of_biterms.length;bitermIndex++) {
                int oldTopicId = this.topic_of_biterms[bitermIndex];
                //seed
                Long itmeNum = this.biterms.get(bitermIndex);
                int word1 = (int)(itmeNum/1000000);
                int word2 = (int)(itmeNum%1000000);

                this.topic_word_num[word1][oldTopicId] -= 1;
                this.topic_word_num[word2][oldTopicId] -= 1;
                this.num_of_topic_of_biterm[oldTopicId] -= 1;

                int newTopicId = -1;

                // double[] p = new double[this.topic_num];

                for (int k = 0; k < this.numTopics; k++) {

                    multiPros[k] = (this.num_of_topic_of_biterm[k] + alpha)
                            * itemsimimap.get(itmeNum)[k]
//                            * wordsimimap.get(word1)[k]
//                            * wordsimimap.get(word2)[k]
                            * (this.topic_word_num[word1][k] + beta)
                            * (this.topic_word_num[word2][k] + beta)
                            / Math.pow(this.num_of_topic_of_biterm[k] * 2 + this.vocabularySize * beta, 2);
                }

                newTopicId = FuncUtils.nextDiscrete(multiPros);

                this.topic_word_num[word1][newTopicId] += 1;
                this.topic_word_num[word2][newTopicId] += 1;
                this.num_of_topic_of_biterm[newTopicId] += 1;

                this.topic_of_biterms[bitermIndex] = newTopicId;
            }
        }

        iterTime =System.currentTimeMillis()-startTime;

        expName = orgExpName;
        //System.out.println();

        //System.out.println("Writing output from the last sample ...");
       
        writeDocTopicPros();
        //System.out.println("Sampling completed for BTM!");

        writeRejectTopicMeasure();
        
//        writeTopTopicalWords();
    }
    
    public void writeRejectTopicMeasure() throws IOException{
    	
    	//topic coherence
        BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
                + expName + ".measure"));
        
    	
        int[] top_words; 
        double topic_umass=0;
        
        for (int topic_id = 0; topic_id < this.numTopics; topic_id++) {
        	top_words=new int[topWords];
        	HashMap<Integer, Double> oneLine = new HashMap<>();
             for (int word_id = 0; word_id < this.vocabularySize; word_id++) {
                 oneLine.put(word_id, ((double) this.topic_word_num[word_id][topic_id]) / this.num_of_topic_of_biterm[topic_id] / 2);
             }
             List<Map.Entry<Integer, Double>> maplist = new ArrayList<>(oneLine.entrySet());
             Collections.sort(maplist, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));

             int count = 0;
             for (Map.Entry<Integer, Double> o1 : maplist) {
                 top_words[count++]=o1.getKey();
                 if (count >= topWords) {
                     break;
                 }
             }
             topic_umass+=Measure.c_umass(top_words, wordId_of_corpus);
        }
        writer.write(topic_umass/this.numTopics+"\n");           
        writer.flush();writer.close();
    }
    

    public void inference(String labelname,String probMaxname)
            throws IOException
    {
        //System.out.println("Running Gibbs sampling inference: ");

        long startTime = System.currentTimeMillis();

        for (int iter = 0; iter < this.numIterations; iter++) {

//            if(iter%50 == 0)
//                System.out.print(" " + (iter));

            for(int bitermIndex = 0;bitermIndex<this.topic_of_biterms.length;bitermIndex++) {
                int oldTopicId = this.topic_of_biterms[bitermIndex];
                //seed
                Long itmeNum = this.biterms.get(bitermIndex);
                int word1 = (int)(itmeNum/1000000);
                int word2 = (int)(itmeNum%1000000);

                this.topic_word_num[word1][oldTopicId] -= 1;
                this.topic_word_num[word2][oldTopicId] -= 1;
                this.num_of_topic_of_biterm[oldTopicId] -= 1;

                int newTopicId = -1;

                // double[] p = new double[this.topic_num];

                for (int k = 0; k < this.numTopics; k++) {

                    multiPros[k] = (this.num_of_topic_of_biterm[k] + alpha)
                            * itemsimimap.get(itmeNum)[k]
//                            * wordsimimap.get(word1)[k]
//                            * wordsimimap.get(word2)[k]
                            * (this.topic_word_num[word1][k] + beta)
                            * (this.topic_word_num[word2][k] + beta)
                            / Math.pow(this.num_of_topic_of_biterm[k] * 2 + this.vocabularySize * beta, 2);
                }

                newTopicId = FuncUtils.nextDiscrete(multiPros);

                this.topic_word_num[word1][newTopicId] += 1;
                this.topic_word_num[word2][newTopicId] += 1;
                this.num_of_topic_of_biterm[newTopicId] += 1;

                this.topic_of_biterms[bitermIndex] = newTopicId;
            }
        }

        iterTime =System.currentTimeMillis()-startTime;

        expName = orgExpName;
        //System.out.println();

        //System.out.println("Writing output from the last sample ...");
       
        writeDocTopicPros(labelname,probMaxname);
        //System.out.println("Sampling completed for BTM!");
    }
    
    private double getSum(Long biterm){
        if(!bitermSum.containsKey(biterm)) {
            double sum = 0;
            int word1 = (int)(biterm/1000000);
            int word2 = (int)(biterm%1000000);
            for (int topic_id = 0; topic_id < this.numTopics; topic_id++) {

                sum += (this.num_of_topic_of_biterm[topic_id] + alpha)
                        * itemsimimap.get(biterm)[topic_id]
//                        * wordsimimap.get(word1)[topic_id]
//                        * wordsimimap.get(word2)[topic_id]
                        * (this.topic_word_num[word1][topic_id] + beta)
                        * (this.topic_word_num[word2][topic_id] + beta)
                        / Math.pow(this.num_of_topic_of_biterm[topic_id] * 2 + this.vocabularySize * beta, 2);
            }
            bitermSum.put(biterm,sum);
        }
        return bitermSum.get(biterm);
    }
    
    public void writeDocTopicPros()
            throws IOException
    {
        BufferedWriter labelwriter = new BufferedWriter(new FileWriter(folderPath+"seedUpdate.doclabel"));
        BufferedWriter maxWriter = new BufferedWriter(new FileWriter(folderPath+"seedUpdate.probMax"));

        double maxprob = 0;
        int maxtopic = 0;
        int docIndex = 0;

        for (HashMap<Long,Integer> line : this.biterm_of_corpus) {
            double[] oneTheta = new double[this.numTopics];            
            maxprob = 0;
            maxtopic = 0;
            for(int topic_id = 0; topic_id<this.numTopics;topic_id++) {
                double oneSum=0;
                for (Long biterm : line.keySet()) {
                    int word1 = (int)(biterm/1000000);
                    int word2 = (int)(biterm%1000000);
                    oneSum+=(((double)line.get(biterm))/this.doc_biterm_num[docIndex])
                            *((
                            (this.num_of_topic_of_biterm[topic_id] + alpha)
                                    * itemsimimap.get(biterm)[topic_id]
//                                    * wordsimimap.get(word1)[topic_id]
//                                    * wordsimimap.get(word2)[topic_id]
                                    * (this.topic_word_num[word1][topic_id] + beta)
                                    * (this.topic_word_num[word2][topic_id] + beta)
                                    / Math.pow(this.num_of_topic_of_biterm[topic_id]*2 + this.vocabularySize * beta, 2)
                    )/(getSum(biterm)));
                }              
                if(oneSum>maxprob)
                {
                    maxprob = oneSum;
                    maxtopic = topic_id;
                }
            }
            docIndex++;
            labelwriter.write(maxtopic+"\n");
            maxWriter.write(maxprob+"\n");
        }     
        labelwriter.flush();
        labelwriter.close(); 
        //System.out.println("Sampling completed for BTM!");
    }
   
    public void writeDocTopicPros(String labelname,String probMaxname)
            throws IOException
    {
        BufferedWriter labelwriter = new BufferedWriter(new FileWriter(labelname));
        BufferedWriter maxWriter = new BufferedWriter(new FileWriter(probMaxname));//概率最高值
        
        double maxprob = 0;
        int maxtopic = 0;
        int docIndex = 0;

        for (HashMap<Long,Integer> line : this.biterm_of_corpus) {
            double[] oneTheta = new double[this.numTopics];
            maxprob = 0;
            maxtopic = 0;
            for(int topic_id = 0; topic_id<this.numTopics;topic_id++) {
                double oneSum=0;
                for (Long biterm : line.keySet()) {
                    int word1 = (int)(biterm/1000000);
                    int word2 = (int)(biterm%1000000);
                    oneSum+=(((double)line.get(biterm))/this.doc_biterm_num[docIndex])
                            *((
                            (this.num_of_topic_of_biterm[topic_id] + alpha)
                                    * itemsimimap.get(biterm)[topic_id]
//                                    * wordsimimap.get(word1)[topic_id]
//                                    * wordsimimap.get(word2)[topic_id]
                                    * (this.topic_word_num[word1][topic_id] + beta)
                                    * (this.topic_word_num[word2][topic_id] + beta)
                                    / Math.pow(this.num_of_topic_of_biterm[topic_id]*2 + this.vocabularySize * beta, 2)
                    )/(getSum(biterm)));
                }                
                if(oneSum>maxprob)
                {
                    maxprob = oneSum;
                    maxtopic = topic_id;
                }
            }
            docIndex++;
            maxWriter.write(maxprob+"\n");
            labelwriter.write(maxtopic+"\n");
        }     
        labelwriter.flush();maxWriter.flush();
        labelwriter.close();maxWriter.close();       
        //System.out.println("Sampling completed for BTM!");
    }

    private void writeTopTopicalWords() throws IOException {

        BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
                + expName + ".topWords"));

        for (int topic_id = 0; topic_id < this.numTopics; topic_id++) {
            //writer.write("Topic" + new Integer(topic_id) + ":");
            HashMap<Integer, Double> oneLine = new HashMap<>();
            for (int word_id = 0; word_id < this.vocabularySize; word_id++) {
                //
                Double prob = 0.0;

                prob = ((double) this.topic_word_num[word_id][topic_id]) / this.num_of_topic_of_biterm[topic_id] / 2;

                oneLine.put(word_id, prob);
            }
            List<Map.Entry<Integer, Double>> maplist = new ArrayList<>(oneLine.entrySet());

            Collections.sort(maplist, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));

            //writer.write("Topic:" + topic_id + "\n");
            int count = 0;
            for (Map.Entry<Integer, Double> o1 : maplist) {
                writer.write( this.id2WordVocabulary.get(o1.getKey()) + " ") ;
                count++;
                if (count >= topWords) {
                    break;
                }
            }
            writer.write("\n");

        }
        writer.close();
    }

}
