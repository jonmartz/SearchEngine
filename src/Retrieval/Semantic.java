package Retrieval;

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

    public ArrayList<String> getSemnticsOfTerms(ArrayList<String> terms,int resultSizeForEach)throws IOException {
        ArrayList<String> semanticsWords = new ArrayList<>();
        RandomAccessFile reader = new RandomAccessFile(glovePath, "rw");
        HashMap<String,String> termsVectors = new HashMap<>();
        int count = terms.size();
        String line = reader.readLine();
        while (line != null && count != 0 ) {
            String termInGlove = line.split(" ")[0];
            if (terms.contains(termInGlove))
                termsVectors.put(termInGlove, line.substring(termInGlove.length() + 1, line.length()));
        }
        for(String term: terms){
            reader.seek(0);
            line = reader.readLine();
            PriorityQueue<vectorDistance> priorityVecDis= new PriorityQueue<>();
            while (line != null) {
                String otherTerm = line.split(" ")[0];
                String vector = line.substring(otherTerm.length()+1,line.length());
                /*
                getVectorDistance()
                vectorDistances.add
                sortedRankedDocs.addAll(rankedDocs.values());

                // get top (resultSize) or less
                ArrayList<String> topRankedDocNames = new ArrayList<>();
                int count = 0;
                for (Ranker.RankedDoc rankedDoc : sortedRankedDocs){
                    topRankedDocNames.add(rankedDoc.name);
                    count++;
                    if (count == resultSize) break;
                }
                return topRankedDocNames;

                tDistance
            */
            }
        }
    }


    private class vecDisComparator implements Comparator<vectorDistance>
    {
        @Override
        public int compare(vectorDistance vec1, vectorDistance vec2) {
            if (vec1.value < vec2.value) return 1;
            if (vec1.value < vec2.value) return -1;
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
