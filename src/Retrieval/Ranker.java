package Retrieval;

import java.util.*;

/**
 * Responsible for ranking a set of documents for a given query.
 */
public class Ranker {

    /**
     * Get a list of relevant documents to query, ordered from most relevant (list[0]) to least relevant.
     * @param postings list of term postings. Each posting is in this form:
     *                 posting[0] = term, df, qf, positionsInQuery
     *                 posting[i>0] = docID, inTitle, tf, positionsInDoc
     * @param documents map of documents. Each document entry is in this form:
     *                  docName -> docLength, maxTf, city, language, date
     * @param K for BM25
     * @param b for BM25
     * @param docCount from doc index
     * @param avgDocLength from doc index
     * @return list of documents sorted by rank
     */
    public SortedSet<Map.Entry<String, Double>> getRankedDocuments(ArrayList<ArrayList<String[]>> postings,
                                                                   HashMap<String, String[]> documents,
                                                                   double K, double b, int docCount, double avgDocLength) {
        HashMap<String, Double> documentsMap = new HashMap<>();
        for (ArrayList<String[]> posting : postings) {
            String[] termData = posting.get(0);
            for (int i = 1; i < posting.size(); i++) {
                String[] doc = posting.get(i);
                String docID = doc[0];
                if (!documentsMap.containsKey(docID)) documentsMap.put(docID, 0.0);
                int tf = Integer.parseInt(doc[2]);
                int docLength = Integer.parseInt(documents.get(docID)[0]);
                int qf = Integer.parseInt(termData[2]);
                int df = Integer.parseInt(termData[1]);
                double value = BM25(tf, docCount, docLength, avgDocLength, qf, df, K, b);
                documentsMap.replace(docID, documentsMap.get(docID) + value);
            }
        }
        // sort docs by rank
        SortedSet<Map.Entry<String, Double>> rankedDocuments = new TreeSet<>(new RankComparator());
        rankedDocuments.addAll(documentsMap.entrySet());

        // get top 50 or less
        SortedSet<Map.Entry<String, Double>> topDocuments = new TreeSet<>(new RankComparator());
        int count = 0;
        for (Map.Entry<String, Double> document : rankedDocuments){
            topDocuments.add(document);
            count++;
            if (count == 50) break;
        }
        return topDocuments;
    }

    /**
     * Calculate the BM25 for the given term from the query and document
     * @param tf            term frequency in doc
     * @param docCount      number of docs in corpus
     * @param docLength     length of doc
     * @param avgDocLength  average doc length
     * @param qf            term frequency in query
     * @param df            term document frequency
     * @param K             give the upper bound
     * @param b             normalizer
     * @return              BM25 calculation
     */
    private double BM25(double tf, double docCount, double docLength, double avgDocLength, double qf, double df, double K, double b) {
        double numerator = qf * tf * (K + 1);
        double denominator = tf + K * (1 - b + b * docLength / avgDocLength);
        double log = Math.log((docCount + 1) / df);
        return numerator * log / denominator;
    }

    /**
     * Class for comparing ranks of docs and getting a sorted list.
     * The higher the rank, the higher the priority of the document
     */
    private class RankComparator implements Comparator<Map.Entry<String, Double>>
    {
        @Override
        public int compare(Map.Entry<String, Double> entry1, Map.Entry<String, Double> entry2) {
            if (entry1.getValue() < entry2.getValue()) return 1;
            if (entry1.getValue() > entry2.getValue()) return -1;
            return 0;
        }
    }
}
