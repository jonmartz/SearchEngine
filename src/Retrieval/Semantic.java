package Retrieval;

import Indexing.Indexer;
import javafx.collections.transformation.SortedList;

import javax.print.attribute.standard.JobPriority;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Semantic {
    private String glovePath;
    public Semantic(String glovePath){
        this.glovePath = glovePath;
    }

    public void SemnticsFile(int resultSizeForEach)throws IOException {
        String[] semanticPath = {"C:", "JavaProject", "SearchEngine","semantic_words.txt"};
        RandomAccessFile readerTerm = new RandomAccessFile(glovePath, "rw");

        OutputStreamWriter fstream = new OutputStreamWriter(
                new FileOutputStream(String.join("\\", semanticPath), true));
        BufferedWriter out = new BufferedWriter(fstream);
        String lineTerm = readerTerm.readLine();
        while (lineTerm != null) {
            String term = lineTerm.split(" ")[0];
            ArrayList<Double> vectorTerm = getVector(lineTerm.substring(term.length() + 1, lineTerm.length()));
            RandomAccessFile readerOther = new RandomAccessFile(glovePath, "rw");
            String lineOther = readerOther.readLine();
            PriorityQueue<vectorDistance> priorityVecDis = new PriorityQueue<>(new vecDisComparator());
            while (lineOther != null){
                String other = lineOther.split(" ")[0];
                vectorDistance vecDis = new vectorDistance(other);
                if(vecDis.term.equals(term)) {
                    lineOther = readerOther.readLine();
                    continue;
                }
                ArrayList<Double> vectorOther = getVector(lineTerm.substring(term.length() + 1, lineTerm.length()));
                vecDis.value = getVectorDistance(vectorTerm,vectorOther);
                priorityVecDis.add(vecDis);
                if (priorityVecDis.size() > resultSizeForEach) priorityVecDis.remove();
                lineOther = readerOther.readLine();
            }
            out.write(term +": ");
            System.out.print(term+ ": ");
            while(!priorityVecDis.isEmpty()) {
                if (priorityVecDis.size() == 1) {
                    System.out.print(priorityVecDis.peek().term + ".\n");
                    out.write(priorityVecDis.poll().term + ".\n");
                    break;
                }
                System.out.print(priorityVecDis.peek().term+ ", ");
                out.write(priorityVecDis.poll().term+", ");
            }
            lineTerm = readerTerm.readLine();
        }
    }
     /*   //found most close vector distance to term
        for(String term: terms){
            reader.seek(0);
            line = reader.readLine();
            PriorityQueue<vectorDistance> priorityVecDis= new PriorityQueue<>(new vecDisComparator());
            while (line != null) {
                vectorDistance vecDis = new vectorDistance(line.split(" ")[0]);
                if(vecDis.term.equals(term))
                    continue;
                String [] vecOther = line.substring(vecDis.term.length()+1,line.length()).split(" ");
                ArrayList<Double> vector=new ArrayList<>();
                for(String val:vecOther)
                    vector.add(Double.parseDouble(val));
                vecDis.value = getVectorDistance(termsVectors.get(term),vector);
                priorityVecDis.add(vecDis);
                if (priorityVecDis.size() > resultSizeForEach) priorityVecDis.remove();
                line = reader.readLine();
            }
            for(vectorDistance vecD : priorityVecDis)
                semanticsWords.add(vecD.term);
        }
        return semanticsWords;
    }
*/
    private ArrayList<Double> getVector(String vector) {
        String[] vecTerm = vector.split(" ");
        ArrayList<Double> vec = new ArrayList<>();
        for (String val : vecTerm)
            vec.add(Double.parseDouble(val));
        return vec;
    }

    public ArrayList<String> getSemnticsOfTerms(ArrayList<String> terms,int resultSizeForEach)throws IOException {
        ArrayList<String> semanticsWords = new ArrayList<>();
        RandomAccessFile reader = new RandomAccessFile(glovePath, "rw");
        HashMap<String,ArrayList<Double>> termsVectors = new HashMap<>();
        int count = terms.size();
        String line = reader.readLine();
        //found terms vector

        while (line != null && count != 0 ) {
            String termInGlove = line.split(" ")[0];
            if (terms.contains(termInGlove)) {
                String[] vecTerm = line.substring(termInGlove.length() + 1, line.length()).split(" ");
                ArrayList<Double> vector=new ArrayList<>();
                for(String val:vecTerm)
                   vector.add(Double.parseDouble(val));
                termsVectors.put(termInGlove,vector);
            }
            line = reader.readLine();
            count--;
        }

        //found most close vector distance to term
        for(String term: terms){
            reader.seek(0);
            line = reader.readLine();
            PriorityQueue<vectorDistance> priorityVecDis= new PriorityQueue<>(new vecDisComparator());
            while (line != null) {
                vectorDistance vecDis = new vectorDistance(line.split(" ")[0]);
                if(vecDis.term.equals(term))
                    continue;
                String [] vecOther = line.substring(vecDis.term.length()+1,line.length()).split(" ");
                ArrayList<Double> vector=new ArrayList<>();
                for(String val:vecOther)
                    vector.add(Double.parseDouble(val));
                vecDis.value = getVectorDistance(termsVectors.get(term),vector);
                priorityVecDis.add(vecDis);
                if (priorityVecDis.size() > resultSizeForEach) priorityVecDis.remove();
                line = reader.readLine();
            }
            for(vectorDistance vecD : priorityVecDis)
                semanticsWords.add(vecD.term);
        }
        return semanticsWords;
    }

    private double getVectorDistance(ArrayList<Double> vector1,ArrayList<Double> vector2){
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        for (int i = 0; i < vector1.size(); i++) {
            dotProduct += vector1.get(i) * vector2.get(i);
            norm1 += Math.pow(vector1.get(i), 2);
            norm2 += Math.pow(vector2.get(i), 2);
        }
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }


    private class vecDisComparator implements Comparator<vectorDistance>
    {
        @Override
        public int compare(vectorDistance vec1, vectorDistance vec2) {
            if (vec1.value < vec2.value) return -1;
            if (vec1.value > vec2.value) return 1;
            return 0;
        }
    }

    private class vectorDistance {
        public String term = "";
        public double value = 0.0;

        public vectorDistance(String term) {
            this.term = term;
        }
    }
}
