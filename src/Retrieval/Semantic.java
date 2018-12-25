package Retrieval;

import Indexing.Indexer;
import javafx.collections.transformation.SortedList;

import javax.print.attribute.standard.JobPriority;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Semantic {
    private String path;
    private HashSet<String> stop_words;

    public Semantic(String Path,String stop_words_path) throws IOException {
        this.path = Path;
        this.stop_words = getStopWords(stop_words_path);
    }

    public void SemnticsFile(int resultSizeForEach)throws IOException {
        BufferedReader readerTerm = new BufferedReader(new InputStreamReader(
                new FileInputStream(path),StandardCharsets.UTF_8));
        OutputStreamWriter fstream = new OutputStreamWriter(
                new FileOutputStream("semantic_words.txt", true));
        BufferedWriter out = new BufferedWriter(fstream);
        String[] del = {"'",":","-",".","@", "!", ";", "+", "?", "\"" , "*", "(", ")", "<", ">", "{", "}", "=", "[", "]", "#", "|", "&", ",", "`"};
        String lineTerm = readerTerm.readLine();
        while (lineTerm != null) {
            boolean isStopWord = false ,isDel = false;
            String term = lineTerm.split(" ")[0];
            if(this.stop_words.contains(term)) isStopWord= true;
            for(String d:del)
                if(d.equals(term))isDel = true;
            if(isDel || isStopWord){
                lineTerm = readerTerm.readLine();
                continue;
            }
            ArrayList<Double> vectorTerm = getVector(lineTerm.substring(term.length() + 1, lineTerm.length()));
            BufferedReader readerOther = new BufferedReader(new InputStreamReader(
                    new FileInputStream(path),StandardCharsets.UTF_8));
            String lineOther = readerOther.readLine();
            PriorityQueue<vectorDistance> priorityVecDis = new PriorityQueue<>(new vecDisComparator());
            while (lineOther != null){
                String other = lineOther.split(" ")[0];
                isDel = false; isStopWord = false;
                if(this.stop_words.contains(other))isStopWord = true;
                for(String d:del)
                    if(d.equals(other))isDel = true;
                if(isDel || isStopWord){
                    lineOther = readerOther.readLine();
                    continue;
                }
                vectorDistance vecDis = new vectorDistance(other);
                if(vecDis.term.equals(term)) {
                    lineOther = readerOther.readLine();
                    continue;
                }
                ArrayList<Double> vectorOther = getVector(lineOther.substring(other.length() + 1, lineOther.length()));
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

    private ArrayList<Double> getVector(String vector) {
        String[] vecTerm = vector.split(" ");
        ArrayList<Double> vec = new ArrayList<>();
        for (String val : vecTerm)
            vec.add(Double.parseDouble(val));
        return vec;
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

    private HashSet<String> getStopWords(String stop_words_path) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(stop_words_path), StandardCharsets.UTF_8));
        HashSet<String> stopWords = new HashSet<>();
        String line;
        while ((line = reader.readLine()) != null) {
            stopWords.add(line.trim());
        }
        return stopWords;
    }





/*
    public ArrayList<String> getSemnticsOfTerms(ArrayList<String> terms,int resultSizeForEach)throws IOException {
        ArrayList<String> semanticsWords = new ArrayList<>();
        BufferedReader readerTerm = new BufferedReader(new InputStreamReader(
                new FileInputStream(path),StandardCharsets.UTF_8));
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
*/


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
