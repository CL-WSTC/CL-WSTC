package approach;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class Evaluator_Total {

	public static String folderPath,base,dataname ;
	public static int time;
	public List<Integer> GT1 = new ArrayList<>();
	public List<Integer> GT2 = new ArrayList<>();
	public List<Integer> GT3 = new ArrayList<>();
	public List<Integer> GT4 = new ArrayList<>();
	public List<Integer> GT5 = new ArrayList<>();
	public List<Integer> label1 = new ArrayList<>();
	public List<Integer> label2 = new ArrayList<>();
	public List<Integer> label3 = new ArrayList<>();
	public List<Integer> label4 = new ArrayList<>();
	public List<Integer> label5 = new ArrayList<>();
	
	public List<Integer> known_GT = new ArrayList<>();
	public List<Integer> known_label = new ArrayList<>();
	public List<Integer> unknown_GT = new ArrayList<>();
	public List<Integer> unknown_label = new ArrayList<>();
	
	public float Accuracy;
	public float MarcoF;
	public float MacroPrecision = 0;
	public float MacroRecall = 0;
	public float Acc_known;
	public float Acc_unknown;
			
	public static void main(String[] args) throws IOException {
		dataname = "agnews";//"HuffN8"
		
		base = "D:/Eclipse/eclipse-workspace/CL-WSTC/src/data/dataset/";
   	
		int numofclass=4;//Total Categories
		int numofKclass=4;

		Evaluator_Total e = new Evaluator_Total();
        e.script(numofclass,numofKclass);
        
        System.out.println("Evaluator completed!\n");
	}
	
	public void script(int numofclass,int numofKclass) throws IOException {
		
		Load(GT1,"results_1/Accept_GT.txt");
		Load(GT2,"results_2/Accept_GT.txt");
		Load(GT3,"results_3/Accept_GT.txt");
		Load(GT4,"results_4/Accept_GT.txt");
		Load(GT5,base+dataname+"_5_label.txt");
		Load(label1,"results_1/Accept_label.txt");
		Load(label2,"results_2/Accept_label.txt");
		Load(label3,"results_3/Accept_label.txt");
		Load(label4,"results_4/Accept_label.txt");
		Load(label5,"results_5/Agnews_5_SeedBTM.doclabel");
		
//		Load(GT1,"results_13/Accept_GT.txt");
//		Load(GT2,"results_14/Accept_GT.txt");
//		Load(GT3,"results_15/Accept_GT.txt");
//		Load(GT4,"results_16/Accept_GT.txt");
//		Load(GT5,base+dataname+"_17_label.txt");
//		Load(label1,"results_13/Accept_label.txt");
//		Load(label2,"results_14/Accept_label.txt");
//		Load(label3,"results_15/Accept_label.txt");
//		Load(label4,"results_16/Accept_label.txt");
//		Load(label5,"results_17/HuffN8_17_SeedBTM.doclabel");
		
		DecimalFormat f = new DecimalFormat("#0.0");
		DecimalFormat f2 = new DecimalFormat("#0.000");
		
		double a1=0,a2=0,a3=0,a4=0,a5=0;
		a1=score_total1(geneArray(label1), geneArray(GT1), numofclass); 
		a2=score_total1(geneArray(label2), geneArray(GT2), numofclass);
		a3=score_total1(geneArray(label3), geneArray(GT3), numofclass); 	
		a4=score_total1(geneArray(label4), geneArray(GT4), numofclass);
		a5=score_total1(geneArray(label5), geneArray(GT5), numofclass);
		System.out.println(f.format(a1*100)+","+f.format(a2*100)+","+f.format(a3*100)+","+f.format(a4*100)+","+f.format(a5*100));
		
		int b1=geneArray(label1).length,b2=geneArray(label2).length,b3=geneArray(label3).length;
		int b4=geneArray(label4).length,b5=geneArray(label5).length;
		System.out.println(b1+","+b2+","+b3+","+b4+","+b5);
		System.out.println(f2.format(((double)(b1*1+b2*2+b3*3+b4*4+b5*5)/(b1+b2+b3+b4+b5))));
		
//		//Single period evaluation
//		System.out.println("1:  ");score_total(geneArray(label1), geneArray(GT1), numofclass); 
//		System.out.println("2:  ");score_total(geneArray(label2), geneArray(GT2), numofclass);
//		System.out.println("3:  ");score_total(geneArray(label3), geneArray(GT3), numofclass);
//		System.out.println("4:  ");score_total(geneArray(label4), geneArray(GT4), numofclass);
//		System.out.println("5:  ");score_total(geneArray(label5), geneArray(GT5), numofclass);
		
		System.out.println("total:  ");
		score_total(geneArray(label1,label2,label3,label4,label5), 
				geneArray(GT1,GT2,GT3,GT4,GT5), numofclass);
				
		//Evaluate by category
		score_total3(geneArray(label1,label2,label3,label4,label5), 
				geneArray(GT1,GT2,GT3,GT4,GT5), numofclass);
			
	}
	
	public static int getNum(String name) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(name));
		String str=br.readLine();
        return Integer.parseInt(str);
	}
	
	public int[] geneArray(List<Integer> list1) {
		int[] array=new int[list1.size()];
		int num=0;
		for(int i=0;i<list1.size();i++) {
			array[num++]=list1.get(i);
		}
		return array;
	}
	
	public int[] geneArray(List<Integer> list1,List<Integer> list2,List<Integer> list3,
			List<Integer> list4,List<Integer> list5) {
		int[] array=new int[list1.size()+list2.size()+list3.size()+list4.size()+list5.size()];
		int num=0;
		for(int i=0;i<list1.size();i++) 
			array[num++]=list1.get(i);
		for(int i=0;i<list2.size();i++) 
			array[num++]=list2.get(i);
		for(int i=0;i<list3.size();i++) 
			array[num++]=list3.get(i);
		for(int i=0;i<list4.size();i++) 
			array[num++]=list4.get(i);
		for(int i=0;i<list5.size();i++) 
			array[num++]=list5.get(i);
		return array;
	}
	
	public static int Load(String name) {
    	int count=0;
    	BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(name));
            for (String doc; (doc = br.readLine()) != null;) {
                if (doc.trim().length() == 0)
                    continue; 
                count++;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return count;
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
	
	public void score_total(int[] js,int[] js2,int numofclass) {
		
		float acc_count=0;

		float[] truePositive=new float[numofclass+4];
		float[] falseNegative=new float[numofclass+4];
		float[] falsePositive=new float[numofclass+4];

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
		Accuracy=acc_count/totnum;

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
		
		System.out.println(totnum);
		
		MacroPrecision= sum_precision/(numofclass-count1);
		MacroRecall=sum_recall/(numofclass-count2);	
		MarcoF=2*MacroPrecision*MacroRecall/(MacroPrecision+MacroRecall+Float.MIN_VALUE);		
		
		System.out.println("Accuracy       :"+Accuracy*100);
		System.out.println("Macro-Precision:"+MacroPrecision*100);
		System.out.println("Macro-Recall   :"+MacroRecall*100);
		System.out.println("Marco-F1       :"+MarcoF*100);
	}
	
	public float score_total1(int[] js,int[] js2,int numofclass) {
		
		float acc_count=0;

		float[] truePositive=new float[numofclass+4];
		float[] falseNegative=new float[numofclass+4];
		float[] falsePositive=new float[numofclass+4];

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
		Accuracy=acc_count/totnum;

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

		MacroPrecision= sum_precision/(numofclass-count1);
		MacroRecall=sum_recall/(numofclass-count2);	
		MarcoF=2*MacroPrecision*MacroRecall/(MacroPrecision+MacroRecall+Float.MIN_VALUE);		
		
		return MarcoF;
	}
	
	public void score_total3(int[] js,int[] js2,int numofclass) {
		
		int count1[]=new int[numofclass];
		int count2[]=new int[numofclass];

		for(int i=0;i<js2.length;i++){
			count1[js2[i]]++;
			if(js2[i]==js[i]){
				count2[js2[i]]++;
			}
		}
		
		for(int i=0;i<numofclass;i++) {
			System.out.println(i+":"+count1[i]+" "+(float)count2[i]/count1[i]);
		}
	}
	
}
