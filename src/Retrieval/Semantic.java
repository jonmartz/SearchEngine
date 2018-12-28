package Retrieval;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Class responsible for finding synonyms for a word, by comparing the distance of word vectors
 * from the GloVe database.
 */
public class Semantic {

    /**
     * Contains all the word vectors from GloVe, with 50 dimensions
     */
    private HashMap<String, Double[]> gloVectors;

    /**
     * Constructor. Loads the list of word vectors from disk to memory
     */
    public Semantic() throws IOException {

        // Build vector map
        String path = "src\\Retrieval\\GloVe";
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(path), StandardCharsets.UTF_8));
        gloVectors = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null){
            String[] strings = line.split(" ");
            String term = strings[0];
            Double[] values = new Double[50];
            for (int i = 1; i < strings.length; i++)
                values[i-1] = Double.parseDouble(strings[i]);
            gloVectors.put(term, values);
        }
    }

    /**
     * Get a string with the closest terms to term, ordered from closest to farthest
     * @param term to get synonyms of
     * @param synonymsCount size of result list
     * @return string with the synonyms delimited by " "
     */
    public String getSynonyms(String term, int synonymsCount){
        Double[] termVectorValues = gloVectors.get(term);
        if (termVectorValues == null) return "";
        PriorityQueue<PotentialSynonym> synonyms = new PriorityQueue<>(new PotentialSynonymComparator());

        // Compare with all other terms
        for (Map.Entry<String, Double[]> vectorEntry : gloVectors.entrySet()){
            String otherTerm = vectorEntry.getKey();
            if(otherTerm.equals(term)) continue;
            Double[] otherTermVectorValues = vectorEntry.getValue();
            Double vectorDistance = getVectorDistance(termVectorValues,otherTermVectorValues);
            PotentialSynonym PotentialSynonym = new PotentialSynonym(otherTerm, vectorDistance);
            synonyms.add(PotentialSynonym);
            if (synonyms.size() > synonymsCount) synonyms.remove();
        }
        // order the synonyms from closest to farthest
        LinkedList<String> orderedSynonyms = new LinkedList<>();
        while(!synonyms.isEmpty()) {
            orderedSynonyms.addFirst(synonyms.remove().term);
        }
        return String.join(" ", orderedSynonyms);
    }

    /**
     * Get the value of the cosine of the angle between two word vectors.
     * @param vector1 to compare
     * @param vector2 to compare
     * @return cosine value
     */
    private double getVectorDistance(Double[] vector1,Double[] vector2){
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += Math.pow(vector1[i], 2);
            norm2 += Math.pow(vector2[i], 2);
        }
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * For ordering the synonyms in the priority queue. The least close synonym will be in
     * the head of the queue to easily remove it from the queue.
     */
    private class PotentialSynonymComparator implements Comparator<PotentialSynonym>
    {
        @Override
        public int compare(PotentialSynonym o1, PotentialSynonym o2) {
            return Double.compare(o1.vectorDistance, o2.vectorDistance);
        }
    }

    /**
     * Class that represents a potential synonym for a word.
     */
    private class PotentialSynonym {
        public String term; // name of term
        public double vectorDistance; // from this term to the term we're looking synonyms for

        /**
         * Constructor
         * @param term of synonym
         * @param vectorDistance of synonym from target term
         */
        public PotentialSynonym(String term, double vectorDistance) {
            this.term = term;
            this.vectorDistance = vectorDistance;
        }
    }

//    public static void main (String[] args) throws IOException {
////        Semantic semantic = new Semantic(); // 326,578 terms
////        String path = "C:\\synonyms\\synonyms";
////        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path, true)));
////        int i = 1;
////        for (String term : semantic.gloVectors.keySet()){
////            String line = term + ": " + semantic.getSynonyms(term,5);
////            System.out.println(i++);
////            out.write(line + "\n");
////        }
////        out.close();
//
////        String path = "C:\\synonyms\\synonyms";
////        BufferedReader reader = new BufferedReader(new InputStreamReader(
////                new FileInputStream(path), StandardCharsets.UTF_8));
////        String line;
////        int i = 0;
////        while ((line = reader.readLine()) != null){
////            int j = 0;
////            for (String word : line.split(" ")) {
////                j++;
////                if (j == 1){
////                    continue;
//////                    word = word.replace(":", "");
////                }
////                boolean found = false;
////                for (Character c : word.toCharArray()) {
////                    if (!((c >= 'a' && c <= 'z') ||
////                            (c >= 'A' && c <= 'Z') ||
////                            (c >= '0' && c <= '9'))) {
////                        i++;
////                        System.out.println(line);
////                        found = true;
////                        break;
////                    }
////                }
//////                break;
////                if (found) break;
////            }
////        }
////        System.out.println(i);
//
//        String inPath = "C:\\synonyms\\synonyms";
//        String outPath = "C:\\synonyms\\synonymsClean";
//        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outPath, false)));
//        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(inPath)));
//        String line;
//        int i = 0;
//        while ((line = in.readLine()) != null){
//            boolean found = false;
//            int j = 0;
//            for (String word : line.split(" ")) {
//                j++;
//                if (j == 1){
////                    continue;
//                    word = word.replace(":", "");
//                }
//                for (Character c : word.toCharArray()) {
//                    if (!((c >= 'a' && c <= 'z') ||
//                            (c >= 'A' && c <= 'Z'))) {
//                        i++;
//                        found = true;
//                        break;
//                    }
//                }
////                break;
//                if (found) break;
//            }
//            if (found) continue;
//            out.write(line + "\n");
//        }
//        System.out.println(i);
//        out.close();
//        in.close();
//    }
}
