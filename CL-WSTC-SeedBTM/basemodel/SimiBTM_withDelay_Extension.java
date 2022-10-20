package basemodel;

import util.FuncUtils;

import java.io.*;
import java.util.*;

public class SimiBTM_withDelay_Extension {

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
    public HashMap<Integer, String> id2WordVocabulary; // Vocabulary to get word
    public int vocabularySize; // The number of word types in the corpus
    int[][] wordId_of_corpus = null;
    public ArrayList<HashMap<Long,Integer>> biterm_of_corpus = new ArrayList<>();
    int[] doc_biterm_num ;
    ArrayList<Long> biterms = new ArrayList<>();

    int[] topic_of_biterms;
    int[][] topic_word_num;
    int[] num_of_topic_of_biterm;

    private HashMap<Long, Double> bitermSum = new HashMap<>();

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

    public double hmin = 0.2;

    double xmin = 0.3;
    
    public double[] rejectThreshold;
    public double kbKnown;
    public int knownNumTopics;
    public int time;
    
    public int seed_limit;
    public String vecfile;

    public SimiBTM_withDelay_Extension(String pathToCorpus, String pathToSeed, String pathToSimi, int inNumTopics,
                   double inAlpha, double inBeta, int inNumIterations, int inTopWords,
                   String inExpName,double hmin,double[] rejectThreshold,
                   int knownNumTopics,int time,String vecfile, int seed_limit)
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
        this.rejectThreshold=rejectThreshold; 
        this.knownNumTopics=knownNumTopics;
        this.time=time;
        this.seed_limit=seed_limit;
        this.vecfile=vecfile;
        
        System.out.println("Reading topic modeling corpus: " + pathToCorpus);

        //folderPath = "results/";
        folderPath = "results_"+time+"/";
        File dir = new File(folderPath);
        if (!dir.exists())
            dir.mkdir();

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

                    System.out.println(numDocuments);
                    continue;
                }
                String[] words = doc.trim().split(" ");
                ArrayList<Integer> wordIdList = new ArrayList<>();
                for (int i = 0; i < words.length ; i++) {
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
                    System.out.println(numDocuments);
                    continue;
                }
//                doc = doc.replaceAll(","," ");
                String[] words = doc.trim().split(",");
                word  = words[0];
                //System.out.println(word);
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

        System.out.println("Corpus size: " + numDocuments + " docs, "
                + numWordsInCorpus + " words");
        System.out.println("Vocabuary size: " + vocabularySize);
        System.out.println("Number of topics: " + numTopics);
        System.out.println("alpha: " + alpha);
        System.out.println("beta: " + beta);
        System.out.println("Number of sampling iterations: " + numIterations);
        System.out.println("Number of top topical words: " + topWords);

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
        System.out.println("Randomly initializing topic assignments using SeedBTM");
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

        int topicId;
        for(int bitermIndex=0;bitermIndex<this.biterms.size();bitermIndex++){
            Long itmeNum = this.biterms.get(bitermIndex);
            int wordid1 = (int)(itmeNum%1000000);
            int wordid2 = (int)(itmeNum/1000000);
            //seed
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
        writeDictionary();

        System.out.println("Running Gibbs sampling inference: ");

        long startTime = System.currentTimeMillis();

        for (int iter = 0; iter < this.numIterations; iter++) {

            if(iter%50 == 0)
                System.out.print(" " + (iter));

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
        System.out.println();

        System.out.println("Writing output from the last sample ...");
        write();

        System.out.println("Sampling completed for BTM!");

    }
    
    public void inference2() throws IOException{
    	writeDictionary();

        System.out.println("Running Gibbs sampling inference: ");

        long startTime = System.currentTimeMillis();

        for (int iter = 0; iter < this.numIterations; iter++) {

            if(iter%50 == 0)
                System.out.print(" " + (iter));

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
        
        System.out.println();

        iterTime =System.currentTimeMillis()-startTime;
        expName = orgExpName;

        writeSeedProb();
        writeKnownKBUpdate2();

        System.out.println("SeedBTM--Accept!");
    }

    public void writeParameters()
            throws IOException
    {
        BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
                + expName + ".paras"));
        writer.write("-model" + "\t" + "BTM");
        writer.write("\n-corpus" + "\t" + corpusPath);
        writer.write("\n-ntopics" + "\t" + numTopics);
        writer.write("\n-alpha" + "\t" + alpha);
        writer.write("\n-beta" + "\t" + beta);
        writer.write("\n-niters" + "\t" + numIterations);
        writer.write("\n-twords" + "\t" + topWords);
        writer.write("\n-name" + "\t" + expName);
        if (tAssignsFilePath.length() > 0)
            writer.write("\n-initFile" + "\t" + tAssignsFilePath);
        if (savestep > 0)
            writer.write("\n-sstep" + "\t" + savestep);

        writer.write("\n-initiation time" + "\t" + initTime);
        writer.write("\n-one iteration time" + "\t" + iterTime/numIterations);
        writer.write("\n-total time" + "\t" + (initTime+iterTime));
        
        writer.close();
    }

    public void writeDictionary()
            throws IOException
    {
        BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
                + expName + ".vocabulary"));
        for (int id = 0; id < vocabularySize; id++)
            writer.write(id2WordVocabulary.get(id) + " " + id + "\n");
        writer.close();
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

    public void writeTopicWordPros()
            throws IOException
    {
        BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
                + expName + ".phi"));

        for (int topic_id = 0; topic_id < this.numTopics; topic_id++) {
            for (int word_id = 0; word_id < vocabularySize; word_id++) {
                writer.write(((this.topic_word_num[word_id][topic_id] + beta) 
                		/ (this.num_of_topic_of_biterm[topic_id] * 2 + vocabularySize * beta))+" ");
            }

            writer.write("\n");
        }

        writer.close();
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
        BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
                + expName + ".theta"));
        
        BufferedWriter labelwriter = new BufferedWriter(new FileWriter(folderPath
                + expName + ".doclabel"));
        BufferedWriter maxWriter = new BufferedWriter(new FileWriter(folderPath
                + expName + ".probMax")); 
        BufferedWriter rejectWriter = new BufferedWriter(new FileWriter(folderPath
                + expName + ".rejectDocFlag"));

        double maxprob = 0;
        int maxtopic = 0; 
        int docIndex = 0;
        
        double max2prob = 0;
        double entropyprob = 0;
        int rejectDocFlag=0;
        
        for (HashMap<Long,Integer> line : this.biterm_of_corpus) {
            maxtopic = 0; maxprob = 0;max2prob = 0;
            entropyprob = 0;rejectDocFlag=0;
            
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
                writer.write(oneSum + " ");
                
                if (oneSum>maxprob) {
                	max2prob=maxprob;
                	maxprob=oneSum;
                	maxtopic = topic_id;  
            	} else if (oneSum>max2prob) {
            		max2prob=oneSum; 
            	}
                if(0!=oneSum) entropyprob += -oneSum * (Math.log(oneSum) / Math.log(2));
                
            }
            writer.write("\n");
            docIndex++;
            labelwriter.write(maxtopic+"\n");
            
            if(maxprob<rejectThreshold[maxtopic]) {
        		rejectDocFlag=1;
        	}
            
            //Reject, unknown topic document
            if(maxtopic>=knownNumTopics) {
            	rejectDocFlag=1;
            }
                      	
            rejectWriter.write(rejectDocFlag+"\n");
            maxWriter.write(maxprob+"\n");
        }
        
        writer.flush();
        labelwriter.flush(); 
        maxWriter.flush();
        rejectWriter.flush();
        writer.close();
        labelwriter.close();
        maxWriter.close();
        rejectWriter.close();
    }
    
    public void writeRejectAccept()
            throws IOException
    {
        BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath + "reject.txt"));
        BufferedWriter acceptwriter = new BufferedWriter(new FileWriter(folderPath + "acceptKnown.txt"));
        
        BufferedReader fr = null;
        int lineId = -1;
        List<Integer> flags = new ArrayList<>();
        try {
        	fr = new BufferedReader(new FileReader(folderPath+expName+".rejectDocFlag"));
        	for (String doc; (doc = fr.readLine()) != null;) {
                lineId++;
                flags.add(Integer.parseInt(doc));
        	}
        }catch (Exception e) {
            e.printStackTrace();
        }
        
    	BufferedReader br = null;
        try {
            int num = 0;
            br = new BufferedReader(new FileReader(corpusPath));
            for (String doc; (doc = br.readLine()) != null;) {
                if (doc.trim().length() == 0) 
                    continue;
                if (1==flags.get(num++))
                	writer.write(doc+"\n"); 
                else
                	acceptwriter.write(doc+"\n"); 
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        } 
        writer.close(); acceptwriter.close();
        

    }
    
    public void writeAcceptSeed()
            throws IOException
    {
    	String docanme_accept=folderPath+"acceptKnown.txt";
    	String wordsiminame = folderPath+"accept"+"_"+time+"_wordsimimax.txt";
    	
    	WordSimilarityAvgData wsd = new WordSimilarityAvgData();
		wsd.script(docanme_accept,seedPath,vecfile,wordsiminame);
		
		double threshold_acc[]=new double[numTopics];
		
		SimiBTM_withDelay_Extension stbtm = null;
        try {
        	stbtm = new SimiBTM_withDelay_Extension(docanme_accept, seedPath, wordsiminame,knownNumTopics,
             		50/knownNumTopics + 1, 0.1, 200, 30,"accept"+"_"+time+"_SeedBTM",0.0001,
             		threshold_acc,knownNumTopics,time,vecfile,seed_limit);
            stbtm.inference2();   
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
    public void writeKnownKBUpdate2()
            throws IOException
    {
    	BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
                + expName + ".kbKnownSeed"));
        BufferedWriter writer_prob = new BufferedWriter(new FileWriter(folderPath
                + expName + ".kbKnownSeed_prob"));
        
        for (int topic_id = 0; topic_id < this.knownNumTopics; topic_id++) {
        	int count=0;
        	HashMap<Integer, Double> oneLine = new HashMap<>();
            for (int word_id = 0; word_id < vocabularySize; word_id++) {
            	double prob=((this.topic_word_num[word_id][topic_id] + beta) /
            			(this.num_of_topic_of_biterm[topic_id] * 2 + vocabularySize * beta));
            	oneLine.put(word_id, prob);
            }
            List<Map.Entry<Integer, Double>> maplist = new ArrayList<>(oneLine.entrySet());
            Collections.sort(maplist, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));
            for (Map.Entry<Integer, Double> o1 : maplist) {
            	int id=o1.getKey();
            	if(o1.getValue()<=kbKnown || count>=seed_limit){
            		break;
            	}else {
            		List<Integer> wordlist = cate2wordlist.get(topic_id);
            		int flag=1;
            		for (int j = 0; j < wordlist.size(); j++) {
                        if(id==wordlist.get(j)) {
                        	flag=0;
                        	break;
                        }
                    }             		
            		if(1==flag) {
            			count++;
            			writer.write(" " + this.id2WordVocabulary.get(id));
                    	writer_prob.write(this.id2WordVocabulary.get(id)+":"+topic_id+" " +o1.getValue()+"\n");
                    }
            	}
            }
            writer.write("\n");
        }
        writer.flush();writer.close();
        writer_prob.flush();writer_prob.close();
    }
    
    public void writeSeedProb()
            throws IOException
    {
        BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
                + expName + ".seed_prob"));

        List<Double> probs = new ArrayList<Double>();
        
        for (int topic_id = 0; topic_id < this.knownNumTopics; topic_id++) {
        	List<Integer> wordlist = cate2wordlist.get(topic_id);
        	for (int j = 0; j < wordlist.size(); j++) {
        		int word_id=wordlist.get(j);
        		double prob=((this.topic_word_num[word_id][topic_id] + beta) /
            			(this.num_of_topic_of_biterm[topic_id] * 2 + vocabularySize * beta));
        		writer.write(this.id2WordVocabulary.get(word_id)+":"+topic_id+" " +prob+"\n");
        		probs.add(prob);
        	}
        }

        double[] probs_array = new double[probs.toArray().length];
        for (int i = 0; i < probs.toArray().length; i++) {  
        	probs_array[i] = (double) probs.toArray()[i]; 
        }
        
        kbKnown=getQuartiles(probs_array);//getMedianNum(probs_array);
        System.out.println("kbKnown:"+kbKnown);
        
        BufferedWriter writer_kbKnown = new BufferedWriter(new FileWriter(folderPath
                + expName + ".kbKnown"));
        writer_kbKnown.write(""+kbKnown);
        
        writer.flush();writer_kbKnown.flush();
        writer.close();writer_kbKnown.close();
    }
    
	public Double getMedianNum(double[] arr){
        if(arr == null || arr.length == 0){
            return (double) 0;
        }
        if(arr.length == 1){
            return (double) arr[0];
        }
        PriorityQueue<Double> minPQ = new PriorityQueue<>();
        int length = arr.length;
        int k = length/2+1;
 
        for(int i = 0;i<k;i++){
            minPQ.add(arr[i]);
        }
        for(int i = k;i<length;i++){
            if(minPQ.peek() < arr[i]){
                minPQ.poll();
                minPQ.add(arr[i]);
            }
        }
        if (length % 2 == 0){
            return (minPQ.poll() + minPQ.peek())/ 2.0;
        } else{
            return minPQ.peek().doubleValue();
        }
    }
	
    
    public Double getQuartiles(double[] arr) {
        double[] arr1 = new double[4];
        if (arr.length < 4) {
            for (int i = 0; i < arr.length; i++) {
                arr1[i] = arr[i];
            }
            for (int k = arr.length; k < 4; k++) {
                arr1[k] = arr1[k];
            }
            return arr1[0];
        }
        double[] tempArr = Arrays.copyOf(arr, arr.length);
        Arrays.sort(tempArr);
        
        double quartiles_1;
        int n = arr.length;
        double Q1 = (n+1) * 0.25D;
        //Q1
        if(Q1 % 2 == 0){
            quartiles_1 = tempArr[(int)Q1];
        }else{
            double Q1y = Q1-Math.floor(Q1);
            double Q1r;
            Q1r = (1D - Q1y) * tempArr[(int) Math.floor(Q1)-1] + Q1y * tempArr[(int) Math.ceil(Q1)-1];
            quartiles_1 = Q1r;
        }
        return quartiles_1;
    }
    

    public void write()
            throws IOException
    {
        writeTopTopicalWords();
        writeDocTopicPros();
//        writeTopicWordPros();
//        writeParameters();
        writeRejectAccept();
        writeSeedProb();//kbKnown
        writeAcceptSeed();// SeedBTM-Accept 
    }
}
