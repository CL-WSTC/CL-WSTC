package approach;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import basemodel.*;

public class Seed_Update_first2period {
	
	public HashMap<String, Integer> word2IdVocabulary; // Vocabulary to get ID
    public HashSet<String> wordSet = new HashSet<>();
    public List<String[]> seedList = new ArrayList<>();
    public List<String[]> seedList_Update = new ArrayList<>();
    public List<String[]> wordprobmap = new ArrayList<>();
    public List<String[]> wordprobmap_cur;
    public List<String> seedNew_words= new ArrayList<>();
    public List<Integer> seedNew_id= new ArrayList<>();
    public List<String> seedDelete_words= new ArrayList<>();
    public List<Integer> seedDelete_id= new ArrayList<>();
    public double[][] wordEmbedding;
    public float standard,current,standard_record;
    public static int ntopics,time,K;
    public String dataname_t,modelname,expName,folderPath;
	public static String base;
    	
	public void script(String dataname,String base,String vecfile,int time) throws IOException{
		
		folderPath = "results_"+time+"/";
		modelname = "SeedBTM";
		dataname_t=dataname+'_'+time;
		expName =dataname_t+"_"+modelname;
		ntopics=getNum(base+dataname+"_numofKclass.txt");
		
		readGloveWE(vecfile);//wordEmbedding[][]
		LoadDoc(base+dataname_t+".txt");//wordSet
		LoadSeed(seedList,base+dataname_t+"_seed.txt");
		
		//Standard performance obtained for each category
		OutWordSimi(folderPath+"SeedUpdate_wordsimimax.txt");
		standard=performance_standard(folderPath+"SeedUpdate_wordsimimax.txt",
				base+dataname_t+".txt",base+dataname_t+"_seed.txt",time);
		System.out.println("Standard performance:"+standard);
		
		//Candidate Seed Word Selection
		String seeds_name=folderPath +"accept_"+time+"_"+modelname+ ".kbKnownSeed_prob";
//		String seeds_name=folderPath + expName + ".kbKnownSeed_prob";
		
		//Descending order list-topic_words+topic_id
		LoadSeed_new(seedNew_words,seedNew_id,seeds_name,0);//0--descending order, 1--ascending order
		doc_Update(base+dataname_t+"_seed.txt",-1,"",folderPath+"seed_update.txt");//Record the seed word update results
		
		//Seed Word Addition
		//Seed record--current_seed Performance record--current_performance matrix--wordprobamp
		String performance_name=folderPath+"SeedUpdate_performance.txt";
		BufferedWriter perw = new BufferedWriter(new FileWriter(performance_name));
		String outline="";
		System.out.println("Seed word addition:");	
		for(int i=0;i<seedNew_words.size();i++) {
			OutWordSimi_New(seedNew_id.get(i),seedNew_words.get(i),folderPath+"SeedNew_wordsimimax.txt");
			doc_Update(folderPath+"seed_update.txt",seedNew_id.get(i),
					seedNew_words.get(i),folderPath+"current_seed.txt");
			current=performance_standard(folderPath+"SeedNew_wordsimimax.txt",
        			base+dataname_t+".txt",folderPath+"current_seed.txt",time);        		
    		if(current>standard) {
    			doc_Update(folderPath+"current_seed.txt",-1,"",folderPath+"seed_update.txt");
    			wordprobmap=wordprobmap_cur;
    			standard=current;
    		}  		
    		outline+=seedNew_id.get(i)+" "+seedNew_words.get(i)+":"+current+"\n";	
        }
		perw.write(outline);perw.flush();
		doc_Update(folderPath+"seed_update.txt",-1,"",folderPath+"seed_addition.txt"); //Record
		
		standard_record=standard;//Record the performance before the deletion
		
		//Seed Word Deletion
//		String seeds_pro_name=folderPath + "accept_"+time+"_"+modelname+ ".seed_prob";
		String seeds_pro_name=folderPath + expName + ".seed_prob";
		LoadSeed_new(seedDelete_words,seedDelete_id,seeds_pro_name,1);
		
		String performance_delete_name=folderPath+"SeedDelete_performance.txt";
		BufferedWriter delw = new BufferedWriter(new FileWriter(performance_delete_name));
		LoadSeed(seedList_Update,folderPath+"seed_update.txt");
		String[] newArray;
		
		System.out.println("Seed word delete:");	
		for(int i=0;i<seedDelete_words.size();i++) {			
			outline=""; 
			String[] seedarray = seedList_Update.get(seedDelete_id.get(i));
			newArray = new String[seedarray.length-1];
			int flag=0;
			
			if(1==seedarray.length)
				continue;
			
			for(int k=0;k<seedarray.length-1; k++) {
				if(seedDelete_words.get(i).equals(seedarray[k]))
					flag=1;
        		if(1==flag)
        			newArray[k] = seedarray[k+1];
                else 
                	newArray[k] = seedarray[k];
            }
			OutWordSimi_Delete(seedDelete_id.get(i),newArray,folderPath+"SeedDelete_wordsimimax.txt");
			doc_Update(folderPath+"seed_update.txt",seedDelete_id.get(i),newArray,folderPath+"current_seed.txt");    
			//Performance
        	current=performance_standard(folderPath+"SeedDelete_wordsimimax.txt",
        			base+dataname_t+".txt",folderPath+"current_seed.txt",time);       
        	
//    		if(current>=standard) {// || (current<standard)&&(standard-current<0.001) 
//    			doc_Update(folderPath+"current_seed.txt",-1,"",folderPath+"seed_update.txt");
//    			wordprobmap=wordprobmap_cur;
//    			standard=current;
//    			seedList_Update = new ArrayList<>(); //
//    			LoadSeed(seedList_Update,folderPath+"current_seed.txt");
//    			//System.out.print("√");
//    		}
    		outline+=seedDelete_id.get(i)+" "+seedDelete_words.get(i)+":"+(current-standard_record)+"\n";
    		delw.write(outline);delw.flush();
		}

		//Setting of seed words for the next period
		doc_Update(folderPath+"seed_update.txt",-1,"",base+dataname+'_'+(time+1)+"_seed.txt");
		
		System.out.println("Finished!"); 
	}
	
	public void LoadSeed_new(List<String> words,List<Integer> id,String seeds_name,int order) {		
		HashMap<String, Double> oneLine = new HashMap<>();
        try {
        	BufferedReader br = new BufferedReader(new FileReader(seeds_name));
            for (String doc; (doc = br.readLine()) != null;) {
                if (doc.trim().length() == 0)
                    continue;
                String[] str = doc.trim().split(" ");
                oneLine.put(str[0], Double.parseDouble(str[1]));
            }
            List<Map.Entry<String, Double>> maplist = new ArrayList<>(oneLine.entrySet());
            if(0==order)//descending
            	Collections.sort(maplist, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));
            else//ascending
            	Collections.sort(maplist, (o1, o2) -> o1.getValue().compareTo(o2.getValue()));
            for (Map.Entry<String, Double> o1 : maplist) {
            	String[] str2 = o1.getKey().split(":");
            	words.add(str2[0]);
            	id.add(Integer.parseInt(str2[1]));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
	}

	private void doc_Update(String name, int category, String[] newArray, String name_new) throws IOException {
		BufferedReader fr = null;
        int lineId = -1;
        HashMap<Integer, String> map1 = new HashMap<Integer,String>();
        try {
        	fr = new BufferedReader(new FileReader(name));
        	for (String doc; (doc = fr.readLine()) != null;) {
                lineId++;
                map1.put(lineId, doc);
        	}
        }catch (Exception e) {
            e.printStackTrace();
        }
        
        BufferedWriter writer = new BufferedWriter(new FileWriter(name_new));
    	for(int topic_id = 0; topic_id < ntopics; topic_id++) {
    		if(category==topic_id) 
    			writer.write(String.join(" ", newArray)+"\n");
    		else
    			writer.write(map1.get(topic_id)+"\n");
    	}	
    	writer.flush(); writer.close();
	}
	
	public void doc_Update(String name,int category,String word,String name_new)throws IOException {
		BufferedReader fr = null;
        int lineId = -1;
        HashMap<Integer, String> map1 = new HashMap<Integer,String>();
        try {
        	fr = new BufferedReader(new FileReader(name));
        	for (String doc; (doc = fr.readLine()) != null;) {
                lineId++;
                map1.put(lineId, doc);
        	}
        }catch (Exception e) {
            e.printStackTrace();
        }
        
        BufferedWriter writer = new BufferedWriter(new FileWriter(name_new));
    	for(int topic_id = 0; topic_id < ntopics; topic_id++) {
    		if(category==topic_id)
    			writer.write(map1.get(topic_id)+" "+word+"\r\n");
    		else
    			writer.write(map1.get(topic_id)+"\n");
    	}
   	
    	writer.flush(); writer.close();
	}

	public float performance_standard(String wordsiminame,String docname,String seedname,int time) throws IOException {
		
		SimiBTM_Seed stbtm = null;
        try {
        	stbtm = new SimiBTM_Seed(docname, seedname, wordsiminame, ntopics,
             		50/ntopics + 1, 0.1, 100, 30,dataname_t+"_"+modelname,0.0001,time);
            stbtm.inference();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
		//Coherence
		float performance=getNum_float(folderPath+ expName + ".measure");

		return performance;
	}
	
	public float pro_mean(List<Float> probMax, double threshold) {
		float sum=0;
		int count=0;
		for(int i=0;i<probMax.size();i++) 
			if(probMax.get(i)>threshold) {
				count++;
				sum+=probMax.get(i);
			}
		
		return count>0?sum/count:0;
	}

	private void OutWordSimi_Delete(int category,String[] seedarray,String wordsiminame) {
        try {
            BufferedWriter docbw = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(wordsiminame), "utf-8"));
            
            String outline,seedword;double simivalue;int j=0;
            String arr[];
            wordprobmap_cur = new ArrayList<>();
            for (String word:wordSet) {
            	outline = word;
            	arr=new String[ntopics];
            	for(int i=0;i<ntopics;i++) {
            		List<Double> similist = new ArrayList<>();
            		if(category==i) {
            			for (int k = 0; k<seedarray.length; k++) {
                            seedword = seedarray[k];
                            simivalue = similarity(seedword, word);
                            similist.add(simivalue);
                        }
            			Collections.sort(similist);
            			simivalue = Math.max(similist.get(similist.size()-1),0);
            			outline+=","+simivalue;
            			arr[i]=""+simivalue;
            		}else {
            			outline+=","+wordprobmap.get(j)[i];
            			arr[i]=wordprobmap.get(j)[i];
            		}
            	}
            	wordprobmap_cur.add(arr);
            	outline += "\r\n";
                docbw.write(outline);
                docbw.flush();
                j++;
            }
            docbw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	private void OutWordSimi_New(int category,String wordNew,String wordsiminame) {
        try {
            BufferedWriter docbw = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(wordsiminame), "utf-8"));
            
            String outline;double simivalue;int j=0;
            String arr[];
            wordprobmap_cur = new ArrayList<>();
            for (String word:wordSet) {
            	outline = word;
            	arr=new String[ntopics];
            	for(int i=0;i<ntopics;i++) {
            		if(category==i) {
            			simivalue = Math.max(Double.parseDouble(wordprobmap.get(j)[i]),similarity(wordNew, word));
            			outline+=","+simivalue;
            			arr[i]=""+simivalue;
            		}else {
            			outline+=","+wordprobmap.get(j)[i];
            			arr[i]=wordprobmap.get(j)[i];
            		}
            	}
            	wordprobmap_cur.add(arr);
            	outline += "\r\n";
                docbw.write(outline);
                docbw.flush();
                j++;
            }
            docbw.close();
        } catch (IOException e) {
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
    
    private void LoadSeed(List<String[]> seedlist,String seedname) {
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
                seedlist.add(words);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void OutWordSimi(String wordsiminame) {
        try {
            BufferedWriter docbw = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(wordsiminame), "utf-8"));
            for (String word:wordSet) {
                String outline = GetBitermMaxSimi(word);
                docbw.write(outline);
                docbw.flush();
                String[] words = outline.trim().split(",");
                wordprobmap.add(Arrays.copyOfRange(words, 1, words.length));
            }
            docbw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String GetBitermMaxSimi(String word) {

        String[] seedarray;
        String seedword;
        double simivalue;
        String outline = word;    
        for (int i = 0; i < seedList.size(); i++) {
            seedarray = seedList.get(i); 
            simivalue = 0;
            List<Double> similist = new ArrayList<>(); 
//                for (int j = 0; j < seedarray.length; j++) {
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
        outline += "\r\n";
        return outline;
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
			simi=0.0001;
		return simi;
	}

	public float score_total(int[] js,int[] js2,int numofclass) {
		
		float acc_count=0;

		float[] truePositive=new float[numofclass];
		float[] falseNegative=new float[numofclass];
		float[] falsePositive=new float[numofclass];

		int totnum = js.length;
		for(int i=0;i<totnum;i++){
			if(js2[i]==js[i]){
				acc_count++;
				truePositive[js2[i]]++;
			}
			else{	
				falsePositive[js[i]]++;
				falseNegative[js2[i]]++;
			}
		}

		float count1=0;
		float count2=0;
		float[] precision = new float[numofclass];
		float[] recall = new float[numofclass];
		float[] f = new float[numofclass];
		float sum_precision=0,sum_recall=0;

		for(int i=0;i<numofclass;i++){
			float base = truePositive[i]+falsePositive[i]; 
			if(base>0) {
				precision[i]= truePositive[i]/base;
				sum_precision+=precision[i];
			}
			else{
				count1++;	
			}
			base = truePositive[i]+falseNegative[i]; 
			if(base>0) {
				recall[i] = truePositive[i]/base;
				sum_recall+=recall[i];
			}		
			else{
				count2++;
			}
			f[i] = 2*precision[i]*recall[i]/(precision[i]+recall[i]+Float.MIN_VALUE);
		}
		
		float MacroPrecision= sum_precision/(numofclass-count1);
		float MacroRecall=sum_recall/(numofclass-count2);	
		return 2*MacroPrecision*MacroRecall/(MacroPrecision+MacroRecall+Float.MIN_VALUE);		
	}
	
	public void LoadFloat(List<Float> list,String name) {   	
		File f = new File(name);
		if(!f.exists())
			return;
		
    	BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(name));
            for (String doc; (doc = br.readLine()) != null;) {
                if (doc.trim().length() == 0)
                    continue; 
                list.add(Float.parseFloat(doc)); 
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	public void Load(List<Integer> list,String name) {
    	
		File f = new File(name);
		if(!f.exists())
			return;
		
    	BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(name));
            for (String doc; (doc = br.readLine()) != null;) {
                if (doc.trim().length() == 0)
                    continue; 
                list.add(Integer.parseInt(doc)); 
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

	public int[] geneArray(List<Integer> list1) {
		int[] array=new int[list1.size()];
		int num=0;
		for(int i=0;i<list1.size();i++) {
			array[num++]=list1.get(i);
		}
		return array;
	}
	
	public void readGloveWE(String filename) {
		word2IdVocabulary = new HashMap<String,Integer>();
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
			
			System.out.println("number of dimensions: " + K);
			System.out.println("number of words: " + numWords);

			br = new BufferedReader(new FileReader(filename));
			wordEmbedding = new double[numWords][K];
			int i = 0;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				String[] info = line.split("\\s+");
				word2IdVocabulary.put(info[0], i);
				for (int j = 0; j < K; j++)
					wordEmbedding[i][j] = Double.parseDouble(info[j+1]);
				i++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static float getNum_float(String name) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(name));
		String str=br.readLine();
        return Float.parseFloat(str);
	}
	
    public static double getNum_double(String name) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(name));
		String str=br.readLine();
        return Double.parseDouble(str);
	}
    
	public static int getNum(String name) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(name));
		String str=br.readLine();
        return Integer.parseInt(str);
	}
}
