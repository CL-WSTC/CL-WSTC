package approach_extension;

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
import java.util.PriorityQueue;

import basemodel.SimiBTM_Seed;

public class Threshold_Learning_Buffer {

	public HashMap<String, Integer> word2IdVocabulary; // Vocabulary to get ID
    public HashSet<String> wordSet = new HashSet<>();
    public List<String[]> seedList = new ArrayList<>();
    public List<String[]> wordprobmap = new ArrayList<>();
    public double[][] wordEmbedding;
    public static int ntopics,time,K;
    public String dataname_t,modelname,expName,folderPath;
		
	public void script(String dataname,String base,String vecfile,int time,int num_buffer) throws IOException{
		folderPath = "results_"+time+"/";
		File dir = new File(folderPath);
        if (!dir.exists())
            dir.mkdir();
		
		modelname = "SeedBTM";
		dataname_t=dataname+'_'+time;
		String docanme=base+dataname+'_'+(time-1)+".txt";
		String seedname=base+dataname_t+"_seed.txt";
		String GTname_stable="results_"+(time-1)+"/"+dataname+'_'+(time-1)+"_"+modelname+".doclabel";
		String GTname_accept="results_"+(time-1)+"/"+dataname+'_'+(time-1)+"_"+modelname+".rejectDocFlag";
		expName =dataname_t+"_"+modelname;
		ntopics=getNum(base+dataname+"_numofKclass.txt"); //The number of known topics
		
		//threshold of the previous period
		String thresholdname=base+dataname+'_'+(time-1)+"_threshold.txt";
		double threshold_before[]=new double[ntopics+num_buffer];
		getNum_double(thresholdname,threshold_before);
		
		readGloveWE(vecfile);//wordEmbedding[][]
		LoadDoc(docanme);//wordSet
		LoadSeed(seedList,seedname);//Seed words of the current period
		
		//the execution of SeedBTM 
		String wordsiminame=folderPath+"Threshold_wordsimimax.txt";
		OutWordSimi(wordsiminame);	
		SimiBTM_Seed stbtm = null;
        try {
        	stbtm = new SimiBTM_Seed(docanme, seedname, wordsiminame, ntopics,
             		50/ntopics + 1, 0.1, 100, 30,dataname_t+"_"+modelname,0.0001,time);
            stbtm.inference(folderPath+"Threshold.doclabel",folderPath+"Threshold.probMax");   
        } catch (IOException e) {
            e.printStackTrace();
        }
		
        double threshold[]=new double[ntopics];
        threshold=threshold_accept_unite(GTname_accept,GTname_stable,folderPath+"Threshold.probMax",
        		threshold_before,0.7,1,1,1,1);

        //Record of results
        String thresholdname_new=base+dataname+'_'+time+"_threshold.txt";
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(thresholdname_new), "utf-8"));           
            for(int i=0;i<ntopics;i++) {
            	writer.write(threshold[i]+ "\n");
            }
            for(int j=0;j<num_buffer;j++)
            	 writer.write("1\n");
            writer.close(); writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
		System.out.println("Threshold Finished!");
	}
	
	public void getNum_double(String thresholdname, double[] threshold_before2) {
		BufferedReader br = null;
        int lineId = -1;
        try {
            br = new BufferedReader(new FileReader(thresholdname));
            for (String doc; (doc = br.readLine()) != null;) {
                lineId++;
                if (doc.trim().length() == 0) {
                    System.out.println("doc "+lineId+ " is empty'");
                    continue;
                }
                threshold_before2[lineId]=Double.parseDouble(doc);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }    
		
	}

	public double[] threshold_stable_unite(String GTname,String labelname,String probMaxname,double threshold_before[],
			double alpha,double a1,double b1,double a2,double b2,double c2,double d2) {
		
		List<Integer> GT_pseudo = new ArrayList<>();
        List<Integer> label = new ArrayList<>();
        List<Double> probMax = new ArrayList<>();
        Load(GT_pseudo,GTname);
        Load(label,labelname);
        Load2(probMax,probMaxname);
       
        int iter_num=500;      
        double eps,eta=0.002;
        double threshold_new[]=new double[ntopics];

        for(int i=0;i<ntopics;i++) {
        	eps=-Math.log(1/threshold_before[i]-1);
        	for(int j=0;j<iter_num;j++) {
//            	System.out.println(1/(1+Math.pow (Math.E,-eps)));
        		eps=eps-eta*df_stable_unite(GT_pseudo,label,probMax,i,alpha,a1,b1,a2,b2,c2,d2,eps);     
        	}
        	threshold_new[i]=1/(1+Math.pow (Math.E,-eps));
        	System.out.println(threshold_new[i]);
        }
        System.out.println();
        return threshold_new;
	}
	
	public double df_stable_unite(List<Integer> GT,List<Integer> label,List<Double> probMax,int topic_id,
			double alpha,double a1,double b1,double a2,double b2,double c2,double d2,double threshold) {
		
		int n1=0,n2=0,n3=0,n4=0;
		
		for(int i=0;i<GT.size();i++) {
			if(topic_id==GT.get(i)) {
				if(GT.get(i)==label.get(i)) {
					if(probMax.get(i)>1/(1+Math.pow (Math.E,-threshold)))
						n1++;
					else
						n2++;
				}else {
					if(probMax.get(i)>1/(1+Math.pow (Math.E,-threshold)))
						n3++;
					else
						n4++;
				}
			}
		}
		return (((1-alpha)*a1-alpha*a2)*n1+(alpha*b1+(1-alpha)*b2)*n2
				-(1-alpha)*c2*n3-(1-alpha)*d2*n4)
				*1/(1+Math.pow (Math.E,-threshold))*(1-1/(1+Math.pow (Math.E,-threshold)));
	}
	
	public double[] threshold_accept_unite(String GTname,String labelname,String probMaxname,double threshold_before[],
			double alpha,double a1,double b1,double b2,double c2) {
		        
        List<Integer> GT_accept = new ArrayList<>();    
		List<Integer> GT_label = new ArrayList<>();    
        List<Double> probMax = new ArrayList<>();
        Load(GT_accept,GTname);
        Load(GT_label,labelname);
        Load2(probMax,probMaxname);
       
        int iter_num=500;      
        double eps,eta=0.002;
        double threshold_new[]=new double[ntopics];
        
        for(int i=0;i<ntopics;i++) {
        	eps=-Math.log(1/threshold_before[i]-1);
        	for(int j=0;j<iter_num;j++) {
//            	System.out.println(1/(1+Math.pow (Math.E,-eps)));
        		eps=eps-eta*df_accept_unite(GT_accept,GT_label,probMax,i,alpha,a1,b1,b2,c2,eps);      
        	}
        	threshold_new[i]=1/(1+Math.pow (Math.E,-eps));
        	System.out.println(threshold_new[i]);
        }
        System.out.println();
        return threshold_new;
	}

	public double df_accept_unite(List<Integer> GT,List<Integer> label,List<Double> probMax,int topic_id,
			double alpha,double a1,double b1,double b2,double c2,double threshold) {
		
		int n1=0,n2=0,n3=0,n4=0;
		
		for(int i=0;i<GT.size();i++) {
			if(topic_id==label.get(i)) {
				if(0==GT.get(i)) {
					if(probMax.get(i)>1/(1+Math.pow (Math.E,-threshold)))
						n1++;
					else
						n2++;
				}else {
					if(probMax.get(i)>1/(1+Math.pow (Math.E,-threshold)))
						n3++;
					else
						n4++;
				}
			}
		}
		return ((-alpha*a1)*n1+(alpha*b1+(1-alpha)*b2)*n2-(1-alpha)*c2*n3)
				*1/(1+Math.pow (Math.E,-threshold))*(1-1/(1+Math.pow (Math.E,-threshold)));
	}

	public double[] threshold_accept_unite(String GTname,String labelname,String probMaxname,double threshold_before[],
			double alpha,double a1,double b1,double a2,double b2,double c2,double d2) {
		        
        List<Integer> GT_accept = new ArrayList<>();    
		List<Integer> GT_label = new ArrayList<>();    
        List<Double> probMax = new ArrayList<>();
        Load(GT_accept,GTname);
        Load(GT_label,labelname);
        Load2(probMax,probMaxname);
       
        int iter_num=500;      
        double eps,eta=0.002;
        double threshold_new[]=new double[ntopics];
        
        for(int i=0;i<ntopics;i++) {
        	eps=-Math.log(1/threshold_before[i]-1);
        	for(int j=0;j<iter_num;j++) {
//            	System.out.println(1/(1+Math.pow (Math.E,-eps)));
        		eps=eps-eta*df_accept_unite(GT_accept,GT_label,probMax,i,alpha,a1,b1,a2,b2,c2,d2,eps);      
        	}
        	threshold_new[i]=1/(1+Math.pow (Math.E,-eps));
        	System.out.println(threshold_new[i]);
        }
        System.out.println();
        return threshold_new;
        
	}
	
	public double df_accept_unite(List<Integer> GT,List<Integer> label,List<Double> probMax,int topic_id,
			double alpha,double a1,double b1,double a2,double b2,double c2,double d2,double threshold) {
		
		int n1=0,n2=0,n3=0,n4=0;
		
		for(int i=0;i<GT.size();i++) {
			if(topic_id==label.get(i)) {
				if(0==GT.get(i)) {
					if(probMax.get(i)>1/(1+Math.pow (Math.E,-threshold)))
						n1++;
					else
						n2++;
				}else {
					if(probMax.get(i)>1/(1+Math.pow (Math.E,-threshold)))
						n3++;
					else
						n4++;
				}
			}
		}
		
		return (((1-alpha)*a1-alpha*a2)*n1+(alpha*b1+(1-alpha)*b2)*n2
				-(1-alpha)*c2*n3-(1-alpha)*d2*n4)
				*1/(1+Math.pow (Math.E,-threshold))*(1-1/(1+Math.pow (Math.E,-threshold)));
	}
	
	public double[] threshold_stable_acc(String GTname,String labelname,String probMaxname,double threshold_before[],
			double a,double b,double c,double d) {
				
		List<Integer> GT_pseudo = new ArrayList<>();
        List<Integer> label = new ArrayList<>();
        List<Double> probMax = new ArrayList<>();
        Load(GT_pseudo,GTname);
        Load(label,labelname);
        Load2(probMax,probMaxname);    
        int iter_num=500;      
        double eps,eta=0.002;
        double threshold_new[]=new double[ntopics];
        
        for(int i=0;i<ntopics;i++) {
        	eps=-Math.log(1/threshold_before[i]-1);
        	for(int j=0;j<iter_num;j++) {
//            	System.out.println(1/(1+Math.pow (Math.E,-eps)));
        		eps=eps-eta*df_stable_acc(GT_pseudo,label,probMax,i,a,b,c,d,eps); 
        	}
        	threshold_new[i]=1/(1+Math.pow (Math.E,-eps));
        	System.out.println(threshold_new[i]);
        }
        System.out.println();
        return threshold_new;
	}
	
	public double df_stable_acc(List<Integer> GT,List<Integer> label,List<Double> probMax,
			int topic_id,double a,double b,double c,double d,double threshold) {		
		int n1=0,n2=0,n3=0,n4=0;
		for(int i=0;i<GT.size();i++) {
			if(topic_id==GT.get(i)) {
				if(GT.get(i)==label.get(i)) {
					if(probMax.get(i)>1/(1+Math.pow (Math.E,-threshold)))
						n1++;
					else
						n2++;
				}else {
					if(probMax.get(i)>1/(1+Math.pow (Math.E,-threshold)))
						n3++;
					else
						n4++;
				}
			}
		}
//		System.out.println((float)n1/(n1+n2+n3+n4)+" "+(float)n2/(n1+n2+n3+n4)+" "+(float)n3/(n1+n2+n3+n4)+" "+(float)n4/(n1+n2+n3+n4));
		return (a*n1+b*n2-c*n3-d*n4)*1/(1+Math.pow (Math.E,-threshold))*(1-1/(1+Math.pow (Math.E,-threshold)));
	}
	
	public double[] threshold_accept_acc(String GTname_accept,String GTname_label,String probMaxname,double[] threshold_before,
			double a,double b,double c,double d) {
		
		List<Integer> GT_accept = new ArrayList<>();    
		List<Integer> GT_label = new ArrayList<>();    
        List<Double> probMax = new ArrayList<>();
        Load(GT_accept,GTname_accept);
        Load(GT_label,GTname_label);
        Load2(probMax,probMaxname);
       
        int iter_num=500;      
        double eps,eta=0.002;
        double threshold_new[]=new double[ntopics];
        
        for(int i=0;i<ntopics;i++) {
        	eps=-Math.log(1/threshold_before[i]-1);
        	for(int j=0;j<iter_num;j++) {
//            	System.out.println(1/(1+Math.pow (Math.E,-eps)));
        		eps=eps-eta*df_accept_acc(GT_accept,GT_label,probMax,i,a,b,c,d,eps);     
        	}
        	threshold_new[i]=1/(1+Math.pow (Math.E,-eps));
        	System.out.println(threshold_new[i]);
        }
        System.out.println();
        return threshold_new;
	}
		
	public double df_accept_acc(List<Integer> GT,List<Integer> GT_label,List<Double> probMax,
			int topic_id,double a,double b,double c,double d,double threshold) {		
		int n1=0,n2=0,n3=0,n4=0;
		for(int i=0;i<GT.size();i++) {
			if(topic_id==GT_label.get(i)) {
				if(0==GT.get(i)) {
					if(probMax.get(i)>1/(1+Math.pow (Math.E,-threshold)))
						n1++;
					else
						n2++;
				}else {
					if(probMax.get(i)>1/(1+Math.pow (Math.E,-threshold)))
						n3++;
					else
						n4++;
				}
			}			
		}
//		System.out.println((float)n1/(n1+n2+n3+n4)+" "+(float)n2/(n1+n2+n3+n4)+" "+(float)n3/(n1+n2+n3+n4)+" "+(float)n4/(n1+n2+n3+n4));
		return (a*n1+b*n2-c*n3-d*n4)*1/(1+Math.pow (Math.E,-threshold))*(1-1/(1+Math.pow (Math.E,-threshold)));
	}
	
	public double[] threshold_accept_open(String GTname_accept,String GTname_label,String probMaxname,double[] threshold_before,
			double a,double b) {
		
		List<Integer> GT_accept = new ArrayList<>();    
		List<Integer> GT_label = new ArrayList<>();    
        List<Double> probMax = new ArrayList<>();
        Load(GT_accept,GTname_accept);
        Load(GT_label,GTname_label);
        Load2(probMax,probMaxname);
       
        int iter_num=500;      
        double eps,eta=0.002;
        double threshold_new[]=new double[ntopics];
        
        for(int i=0;i<ntopics;i++) {
        	eps=-Math.log(1/threshold_before[i]-1);
        	for(int j=0;j<iter_num;j++) {
//            	System.out.println(1/(1+Math.pow (Math.E,-eps)));
        		eps=eps-eta*df_accept_open(GT_accept,GT_label,probMax,i,a,b,eps);    
        	}
        	threshold_new[i]=1/(1+Math.pow (Math.E,-eps));
        	System.out.println(threshold_new[i]);
        }
        return threshold_new;
	}

	public double df_accept_open(List<Integer> GT_accept,List<Integer> GT_label,List<Double> probMax,int topic_id,
			double a,double b,double threshold) {		
		int n1=0,n2=0;
		for(int i=0;i<GT_accept.size();i++) {
			if(topic_id==GT_label.get(i)) {
				if(0==GT_accept.get(i)) {
					if(probMax.get(i)>1/(1+Math.pow (Math.E,-threshold)))
						n1++;
					else
						n2++;
				}	
			}
			
		}
		return (-a*n1+b*n2)*1/(1+Math.pow (Math.E,-threshold))*(1-1/(1+Math.pow (Math.E,-threshold)));
	}
	
	public double[] threshold_stable_open(String GTname,String labelname,String probMaxname,double threshold_before[],
			double a,double b) {
				
		List<Integer> GT_pseudo = new ArrayList<>();
        List<Integer> label = new ArrayList<>();
        List<Double> probMax = new ArrayList<>();
        Load(GT_pseudo,GTname);
        Load(label,labelname);
        Load2(probMax,probMaxname);    
        int iter_num=500;      
        double eps,eta=0.002;
        double threshold_new[]=new double[ntopics];
        
        for(int i=0;i<ntopics;i++) {
        	eps=-Math.log(1/threshold_before[i]-1);
        	for(int j=0;j<iter_num;j++) {
//            	System.out.println(1/(1+Math.pow (Math.E,-eps)));
        		eps=eps-eta*df_stable_open(GT_pseudo,label,probMax,i,a,b,eps); 
        	}
        	threshold_new[i]=1/(1+Math.pow (Math.E,-eps));
        	System.out.println(threshold_new[i]);
        }
        return threshold_new;
	}
	
	public double df_stable_open(List<Integer> GT,List<Integer> label,List<Double> probMax,int topic_id,
			double a,double b,double threshold) {		
		int n1=0,n2=0;
		for(int i=0;i<GT.size();i++) {
			if(topic_id==GT.get(i)) {
				if(GT.get(i)==label.get(i)) {
					if(probMax.get(i)>1/(1+Math.pow (Math.E,-threshold)))
						n1++;
					else
						n2++;
				}
			}
		}
//		System.out.println((float)n1/(n1+n2+n3+n4)+" "+(float)n2/(n1+n2+n3+n4)+" "+(float)n3/(n1+n2+n3+n4)+" "+(float)n4/(n1+n2+n3+n4));
		return (-a*n1+b*n2)*1/(1+Math.pow (Math.E,-threshold))*(1-1/(1+Math.pow (Math.E,-threshold)));
	}
	
	public double[] threshold_accept_median(String GTname_accept,String GTname_label,String probMaxname,
			double threshold_before[]) {

		List<Integer> GT_accept = new ArrayList<>();
		List<Integer> GT_label = new ArrayList<>();
        List<Double> probMax = new ArrayList<>();
        Load(GT_accept,GTname_accept);
        Load(GT_label,GTname_label);
        Load2(probMax,probMaxname);
                
        double arr[];
        int count[]=new int[ntopics];
        double threshold_new[]=new double[ntopics];
        
        for(int i=0;i<GT_accept.size();i++) 
        	if(0==GT_accept.get(i)) 
        		count[GT_label.get(i)]++;
        
        int flag;
        for(int topic_id=0;topic_id<ntopics;topic_id++) {
        	arr=new double[count[topic_id]];
        	flag=0;
        	for(int i=0;i<GT_accept.size();i++) {
        		if(topic_id==GT_label.get(i) && 0==GT_accept.get(i)) 
            		arr[flag++]=probMax.get(i);
        	}
        	threshold_new[topic_id]=getMedianNum(arr);
        	System.out.println(threshold_new[topic_id]);
        }
        
        return threshold_new;
	}
	
	public double[] threshold_stable_median(String GTname_label,String labelname,String probMaxname,
			double threshold_before[]) {
		
		List<Integer> GT_label = new ArrayList<>();
        List<Integer> label = new ArrayList<>();
        List<Double> probMax = new ArrayList<>();
        Load(GT_label,GTname_label);
        Load(label,labelname);
        Load2(probMax,probMaxname);
        
        double arr[];
        int count[]=new int[ntopics];
        double threshold_new[]=new double[ntopics];
        
        for(int i=0;i<GT_label.size();i++) 
        	if(GT_label.get(i)==label.get(i)) 
        		count[label.get(i)]++;
       
        int flag;
        for(int topic_id=0;topic_id<ntopics;topic_id++) {
        	arr=new double[count[topic_id]];
        	flag=0;
        	for(int i=0;i<GT_label.size();i++) {
        		if(topic_id==GT_label.get(i) && GT_label.get(i)==label.get(i)) 
            		arr[flag++]=probMax.get(i);
        	}
        	threshold_new[topic_id]=getMedianNum(arr);
        	System.out.println(threshold_new[topic_id]);
        }
        
        return threshold_new;
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
	
	public void Load2(List<Double> list,String name) {
    	
		File f = new File(name);
		if(!f.exists())
			return;
		
    	BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(name));
            for (String doc; (doc = br.readLine()) != null;) {
                if (doc.trim().length() == 0)
                    continue; 
                list.add(Double.parseDouble(doc)); 
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

	public static int getNum(String name) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(name));
		String str=br.readLine();
        return Integer.parseInt(str);
	}
	
}
