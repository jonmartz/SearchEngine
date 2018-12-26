package Retrieval;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Semantic {

    private HashMap<String, Double[]> gloVectors;

    public Semantic() throws IOException {

        // Build vector map
//        File file = new File(getClass().getClassLoader().getResource("GloVe").getFile());
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

    private class PotentialSynonymComparator implements Comparator<PotentialSynonym>
    {
        @Override
        public int compare(PotentialSynonym o1, PotentialSynonym o2) {
            return Double.compare(o1.vectorDistance, o2.vectorDistance);
        }
    }

    private class PotentialSynonym {
        public String term;
        public double vectorDistance;

        public PotentialSynonym(String term, double vectorDistance) {
            this.term = term;
            this.vectorDistance = vectorDistance;
        }
    }
}
