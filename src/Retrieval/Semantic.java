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
    private ArrayList<String> glove;
    public Semantic(String Path,String stop_words_path) throws IOException {
        this.path = Path;
        this.stop_words = getStopWords(stop_words_path);
        glove = new ArrayList<>();
    }

    public Stack<String> semanticWords(String term,int resultSizeForEach) throws IOException {
        removeStopWordAndDelimiters();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(path), StandardCharsets.UTF_8));
        Stack<String> a = new Stack<>();
        String lineTerm = reader.readLine();
        boolean founded = false;
        ArrayList<Double> vectorTerm = new ArrayList<>();
        while(lineTerm != null){
            if(lineTerm.split(" ")[0].equals(term)) {
                vectorTerm = getVector(lineTerm.substring(term.length() + 1, lineTerm.length()),vectorTerm);
                break;
            }
            lineTerm =reader.readLine();
        }
        reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(path), StandardCharsets.UTF_8));
        lineTerm = reader.readLine();
        PriorityQueue<vectorDistance> priorityVecDis = new PriorityQueue<>(new vecDisComparator());
        while (lineTerm != null) {
            String other = lineTerm.split(" ")[0];
            if(lineTerm.split(" ")[0].equals(term)) {
                lineTerm = reader.readLine();
                continue;
            }
            ArrayList<Double> vectorOther = new ArrayList<>();
            vectorDistance vecDis = new vectorDistance(other);
            vectorOther = getVector(lineTerm.substring(other.length() + 1, lineTerm.length()),vectorOther);
            vecDis.value = getVectorDistance(vectorTerm,vectorOther);
            priorityVecDis.add(vecDis);
            if (priorityVecDis.size() > resultSizeForEach) priorityVecDis.remove();
            lineTerm = reader.readLine();
        }
        while(!priorityVecDis.isEmpty()) {
            System.out.print(priorityVecDis.peek().term + "(" + priorityVecDis.peek().value + ")");
           a.push(priorityVecDis.poll().term);
        }
        return  a;
    }

    public void removeStopWordAndDelimiters() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(path), StandardCharsets.UTF_8));
        String[] newGlove = {"C:","JavaProject","SearchEngine","newglove.txt"};
        OutputStreamWriter fstream = new OutputStreamWriter(
                new FileOutputStream(String.join("\\",newGlove), true));
        BufferedWriter out = new BufferedWriter(fstream);
        String line = reader.readLine();
        while (line != null) {
            boolean isStopWord = false, isDel = false;
            String term = line.split(" ")[0];
            if (this.stop_words.contains(term)) isStopWord = true;
            for (int i = 0; i < term.length(); i++)
                if (!Character.isAlphabetic(term.charAt(i)))
                    isDel = true;
            if (isDel || isStopWord) {
                line = reader.readLine();
                continue;
            }
            out.write(line);
            //glove.add(line);
            line = reader.readLine();
        }
        out.close();
    }

    public void SemanticsFile(int resultSizeForEach)throws IOException {
        OutputStreamWriter fstream = new OutputStreamWriter(
                new FileOutputStream("semantic_words.txt", true));
        BufferedWriter out = new BufferedWriter(fstream);
        ArrayList<Double> vectorTerm = new ArrayList<>();
        for(String lineTerm : glove) {
            String term = lineTerm.split(" ")[0];
            vectorTerm = getVector(lineTerm.substring(term.length() + 1, lineTerm.length()),vectorTerm);
            PriorityQueue<vectorDistance> priorityVecDis = new PriorityQueue<>(new vecDisComparator());
            ArrayList<Double> vectorOther = new ArrayList<>();
            for(String lineOther : glove) {
                String other = lineOther.split(" ")[0];
                vectorDistance vecDis = new vectorDistance(other);
                if(vecDis.term.equals(term)) continue;
                vectorOther = getVector(lineOther.substring(other.length() + 1, lineOther.length()),vectorOther);
                vecDis.value = getVectorDistance(vectorTerm,vectorOther);
                priorityVecDis.add(vecDis);
                if (priorityVecDis.size() > resultSizeForEach) priorityVecDis.remove();
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
        }
    }

    private ArrayList<Double> getVector(String vector,ArrayList<Double> vec) {
        String[] vecTerm = vector.split(" ");
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
