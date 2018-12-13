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
    public PriorityQueue<Map.Entry<String, Double>> getRankedDocuments(ArrayList<ArrayList<String[]>> postings,
                                                            HashMap<String, String[]> documents,
                                                            int K, double b, int docCount, int avgDocLength) {

        HashMap<String, Double> documentsMap = new HashMap<>();

        /*
        todo: for Adiel! Instructions:
        for posting in postings:
            save term data (df, qf, positionsInQuery) which is posting[0]
            for doc in posting (which is every posting[i>0])
                documentsMap[doc] += value from BM25 formula
        return rankedDocuments
        */

        // sort docs by rank
        PriorityQueue<Map.Entry<String, Double>> rankedDocuments = new PriorityQueue<>(new RankComparator());
        rankedDocuments.addAll(documentsMap.entrySet());
        return rankedDocuments;
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
