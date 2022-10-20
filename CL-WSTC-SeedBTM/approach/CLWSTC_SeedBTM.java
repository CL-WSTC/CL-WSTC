package approach;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import basemodel.SimiBTM_withDelay;
import basemodel.WordSimilarityAvgData;

public class CLWSTC_SeedBTM {
	
	public static void main(String[] args) throws IOException {
		
		String base = "D:/Eclipse/eclipse-workspace/CL-WSTC/src/data/dataset/";
        String vecfile = "D:/Eclipse/eclipse-workspace/Continual-SeedBTM-1/src/data/glove/glove.twitter.27B.100d.txt";
        String dataname="agnews";//"HuffN8"
        		
        CLWSTC_SeedBTM total = new CLWSTC_SeedBTM();
        total.merge(base,dataname,vecfile);
	}
	
	public void merge(String base,String dataname,String vecfile) throws IOException{
		
		int time_start=1+0;
		int time_total=4; //start from 0
		
		int label_test=1; // Whether there is ground truth labels for completeness and evaluation
		
		for(int time=(time_start+0);time<=(time_start+time_total);time++) {
			System.out.println("Period:"+time);
			
			String dataname_t=dataname+'_'+time;
			String folderPath = "results_"+time+"/";
			String docname = base +dataname_t+".txt";
			String seedname = base +dataname_t+"_seed.txt";
			String wordsiminame = base +dataname_t+"_wordsimimax.txt";
			String modelname = "SeedBTM";
			
			//1.Semantic Similarity Scores for SeedBTM
			WordSimilarityAvgData wsd = new WordSimilarityAvgData();
			wsd.script(docname,seedname,vecfile,wordsiminame);
			System.out.println("Similarity completed!");
			
			//2.Adaptive Threshold Learning
			int ntopics=getNum(base+dataname+"_numofKclass.txt");
			double threshold[]=new double[ntopics];
			if(time>time_start) {
				Threshold_Learning up_th=new Threshold_Learning();
				up_th.script(dataname,base,vecfile,time);
			}
			String thresholdname=base+dataname_t+"_threshold.txt";
			threshold=getNum_double(thresholdname,ntopics);
			System.out.println("Adaptive threshold learning completed!");
			
			//3.Classification Decision
	        SimiBTM_withDelay stbtm = null;
	        int seed_limit=20;
	        try {
	            stbtm = new SimiBTM_withDelay(docname,seedname,wordsiminame,ntopics,
	            		50/ntopics + 1, 0.1,200,30,dataname_t+"_"+modelname,
	            		0.0001,threshold,time,vecfile, seed_limit);
	            stbtm.inference();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        System.out.println("WSTC completed!");
	        
	        //4.Accept Decision
	        List<Integer> rejectDocFlag = new ArrayList<>();
	        LoadLabel(rejectDocFlag,folderPath+dataname_t+"_"+modelname+".rejectDocFlag");//读入rejectDocFlag   
	        writeAccept(rejectDocFlag,folderPath+"Accept_label.txt",folderPath+dataname_t+"_"+modelname+".doclabel");
	        if(1==label_test)
	        	writeReject(rejectDocFlag,folderPath+"Accept_GT.txt",folderPath+"reject_label.txt",base+dataname_t+"_label.txt");
	    
	        if(time==(time_start+time_total))
	        	break;
	        
	        //5.Seed Word Updating       
	        if(time<(time_start+2)) {
	        	Seed_Update_first2period update = new Seed_Update_first2period();
	        	update.script(dataname,base,vecfile,time);
	        }else{ 
	        	Seed_Update update = new Seed_Update();
	        	update.script(dataname,base,vecfile,time);
	        }

	        //6.Delay
	        rewrite(time+1,base+"time.txt");     
	        String delayname=folderPath+"reject.txt"; 
	        AddDelaydoc(base+dataname+"_"+(time+1)+".txt",delayname);//Texts Delayed
	        if(1==label_test) { //ground truth labels for evaluation
	        	String docname_label=base+dataname+"_"+(time+1)+"_label.txt";
	        	String delayame_label=folderPath+"reject_label.txt";
	        	AddDelaydoc(docname_label,delayame_label);
	        }
	        System.out.println();
		}
	}
	
    public void writeAccept(List<Integer> reject,String docname,String docname2) {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(docname), "utf-8"));              
            List<Integer> label = new ArrayList<>();
	        LoadLabel(label,docname2);      
            for(int i=0;i<label.size();i++) {
            	if(1!=reject.get(i)) {		
            		writer.write(label.get(i)+ "\n");
            	}
            }
            writer.close(); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void writeReject(List<Integer> reject,String docname,String docname1,String docname2) {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(docname), "utf-8"));  
            BufferedWriter writer1 = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(docname1), "utf-8"));  
            List<Integer> label = new ArrayList<>();
	        LoadLabel(label,docname2);      
            for(int i=0;i<label.size();i++) {
            	if(1!=reject.get(i)) 	
            		writer.write(label.get(i)+ "\n");
            	else
            		writer1.write(label.get(i)+ "\n");
            }
            writer.close(); writer1.close(); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    	
	public void LoadLabel(List<Integer> label,String docname) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(docname));
            for (String doc; (doc = br.readLine()) != null;) {
                if (doc.trim().length() == 0)
                    continue; 
                label.add(Integer.parseInt(doc));         
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	public static void doc_Update(String name,int category,String word,String name_new)throws IOException {
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
    	for(int topic_id = 0; topic_id < 8; topic_id++) {
    		if(category==topic_id)
    			writer.write(map1.get(topic_id)+" "+word+"\r\n");
    		else
    			writer.write(map1.get(topic_id)+"\n");
    	}
   	
    	writer.flush(); writer.close();
	}
	
	public double[] getNum_double(String thresholdname,int ntopics ) {
		BufferedReader br = null;
        int lineId = -1;
        double threshold[]=new double[ntopics];
        try {
            br = new BufferedReader(new FileReader(thresholdname));
            for (String doc; (doc = br.readLine()) != null;) {
                lineId++;
                if (doc.trim().length() == 0) {
                    System.out.println("doc "+lineId+ " is empty'");
                    continue;
                }
                threshold[lineId]=Double.parseDouble(doc);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }    
        return threshold;
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
	
	public static void rewrite(int newtime,String timename)throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(timename));
		writer.write(newtime+"\n");
		writer.flush();writer.close();
	}
	
	public static void AddDelaydoc(String docname, String delayname) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(docname,true));
		
		BufferedReader br = null;
        int lineId = -1;
        try {
            br = new BufferedReader(new FileReader(delayname));
            for (String doc; (doc = br.readLine()) != null;) {
                lineId++;
                if (doc.trim().length() == 0) {
                    System.out.println("doc "+lineId+ " is empty'");
                    continue;
                }
                writer.write(doc+"\n");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }    
		writer.flush();writer.close();
	}
}
