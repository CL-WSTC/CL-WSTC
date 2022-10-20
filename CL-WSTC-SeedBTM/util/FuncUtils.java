package util;

import java.util.*;

public class FuncUtils
{
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValueDescending(Map<K, V> map)
    {
        List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>()
        {
            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2)
            {
                int compare = (o1.getValue()).compareTo(o2.getValue());
                return -compare;
            }
        });

        Map<K, V> result = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValueAscending(Map<K, V> map)
    {
        List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>()
        {
            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2)
            {
                int compare = (o1.getValue()).compareTo(o2.getValue());
                return compare;
            }
        });

        Map<K, V> result = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * Sample a value from a double array
     * 
     * @param probs
     * @return
     */
    public static int nextDiscrete(double[] probs)
    {
        double sum = 0.0;
        for (int i = 0; i < probs.length; i++)
            sum += probs[i];

        double r = MTRandom.nextDouble() * sum;

        sum = 0.0;
        for (int i = 0; i < probs.length; i++) {
            sum += probs[i];
            if (sum > r)
                return i;
        }
        return probs.length - 1;
    }

    public static int nextZ(int n)
    {
        double[] probs = new double[n];
        for (int i = 0; i < n; i++) {
            probs[i] = 1.0f/n;
        }

        double sum = 0.0;
        for (int i = 0; i < probs.length; i++)
            sum += probs[i];

        double r = MTRandom.nextDouble() * sum;

        sum = 0.0;
        for (int i = 0; i < probs.length; i++) {
            sum += probs[i];
            if (sum > r)
                return i;
        }
        return probs.length - 1;
    }

    public static int nextX()
    {
        double randback = Math.random();
        int buffer_x;
        if (randback > 0.5) {
            buffer_x = 1;
        } else {
            buffer_x = 0;
        }
        return buffer_x;
    }

    public static int nextX(double value)
    {
        double prob0 = 1-value;
        int buffer_x;

        double r = MTRandom.nextDouble();

        if(r>prob0)
            buffer_x = 1;
        else
            buffer_x = 0;

        return buffer_x;
    }

    public static int nextTopic(HashMap<Integer, Double> seedmap) {

        int size = seedmap.size();
        int[] topicarray = new int[size];
        double[] probarray = new double[size];
        int i=0;
        for (Map.Entry<Integer, Double> entry: seedmap.entrySet())
        {
            topicarray[i] = entry.getKey();
            probarray[i] = entry.getValue();
            i++;
        }
        int index = FuncUtils.nextDiscrete(probarray);
        return topicarray[index];
    }

    public static double mean(double[] m)
    {
        double sum = 0;
        for (int i = 0; i < m.length; i++)
            sum += m[i];
        return sum / m.length;
    }

    public static double[] meanarray(double[] m1,double[] m2)
    {
        if(m1.length != m2.length)
        {
            System.out.println("the length of these two array are not the same ");
            return null;
        }

        double[] m = new double[m1.length];
        double sum = 0;
        for (int i = 0; i < m1.length; i++)
        {
            m[i] = (m1[i] + m2[i])/2;
        }

        return m;
    }

    public static double[] multipyarray(double[] m1,double[] m2)
    {
        if(m1.length != m2.length)
        {
            System.out.println("the length of these two array are not the same ");
            return null;
        }

        double[] m = new double[m1.length];
        double sum = 0;
        for (int i = 0; i < m1.length; i++)
        {
            m[i] = m1[i]*m2[i];
        }

        return m;
    }

    public static double stddev(double[] m)
    {
        double mean = mean(m);
        double s = 0;
        for (int i = 0; i < m.length; i++)
            s += (m[i] - mean) * (m[i] - mean);
        return Math.sqrt(s / m.length);
    }

    public static double sum(double[] vector){
        double sum = 0.0;
        for ( int i = 0; i < vector.length; i++ )
            sum += vector[i];
        return sum;
    }

    public static double[] L1NormWithReusable(double[] vector){
        double sum = 0.0;

        for (double value : vector){
            sum += value;
        }

        if ( Double.compare(sum, 0.0) > 0 ){
            for ( int i = 0; i < vector.length; i++ ){
                vector[i] = vector[i] / sum;
            }
        } else {
            return null;
        }

        return vector;
    }

    public static float ComputeCosineSimilarity(float[] vector1, float[] vector2)
    {
        if (vector1.length != vector2.length)
            try {
                throw new Exception("DIFER LENGTH");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }


        float denom = (VectorLength(vector1) * VectorLength(vector2));
        if (denom == 0f)
            return 0f;
        else
            return (InnerProduct(vector1, vector2) / denom);

    }

    public static float computDist(float[] vector1, float[] vector2)
    {

        // return (float)Math.exp((1-ComputeCosineSimilarity(vector1,vector2))/0.3f);
        return 1-ComputeCosineSimilarity(vector1,vector2);

    }


    public static float InnerProduct(float[] vector1, float[] vector2)
    {

        if (vector1.length != vector2.length)
            try {
                throw new Exception("DIFFER LENGTH ARE NOT ALLOWED");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }


        float result = 0f;
        for (int i = 0; i < vector1.length; i++)
            result += vector1[i] * vector2[i];

        return result;
    }

    public static float VectorLength(float[] vector)
    {
        float sum = 0.0f;
        for (int i = 0; i < vector.length; i++)
            sum = sum + (vector[i] * vector[i]);

        return (float)Math.sqrt(sum);
    }

    public static <T> List<T> removeDuplicateHashSet(List<T> list){
        HashSet<T> hs = new HashSet<>(list);
        list.clear();
        list.addAll(hs);
        return list;
    }

    public static double[] addarray(double[] docsimi, double[] wordid) {

        for(int i=0; i<docsimi.length; i++){
            docsimi[i] = docsimi[i] + wordid[i];
        }
        return docsimi;
    }

    public static double[] divide(double[] docsimi, int size) {

        for(int i=0; i<docsimi.length; i++){
            docsimi[i] = docsimi[i]/size;
        }
        return docsimi;
    }
}
