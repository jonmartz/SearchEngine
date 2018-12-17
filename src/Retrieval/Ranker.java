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
     * @param K for getBM25Factor
     * @param b for getBM25Factor
     * @param docCount from doc index
     * @param avgDocLength from doc index
     * @return list of documents sorted by rank
     */
    public SortedSet<Map.Entry<String, Double>> getRankedDocuments(ArrayList<ArrayList<String[]>> postings,
                                                                   HashMap<String, String[]> documents,
                                                                   double K, double b, int docCount, double avgDocLength,
                                                                   int resultSize) {
        HashMap<String, Double> documentsMap = new HashMap<>();
        for (ArrayList<String[]> posting : postings) {

            // Get general term data
            String[] termData = posting.get(0);
            int qf = Integer.parseInt(termData[2]);
            int df = Integer.parseInt(termData[1]);
            long[] positionsInQuery = toLongArray(termData[3].trim().split(" "));

            // For all doc postings of term
            for (int i = 1; i < posting.size(); i++) {

                // get doc posting data
                String[] docPosting = posting.get(i);
                String docID = docPosting[0];
                String inTitle = docPosting[1];
                int tf = Integer.parseInt(docPosting[2]);
                int docLength = Integer.parseInt(documents.get(docID)[0]);
                long[] positionsInDoc = toLongArray(docPosting[3].trim().split(" "));

                // Calculate all rank factors of doc
                double value = 1;
                value *= getBM25Factor(tf, docCount, docLength, avgDocLength, qf, df, K, b);
                value *= getPositionsInDocFactor(positionsInDoc, docLength);
                if (inTitle.equals("t")) value *= value; // being in title is important!

                // update rank of doc
                if (!documentsMap.containsKey(docID)) documentsMap.put(docID, 0.0);
                documentsMap.replace(docID, documentsMap.get(docID) + value);
            }
        }
        // sort docs by rank
        SortedSet<Map.Entry<String, Double>> rankedDocuments = new TreeSet<>(new RankComparator());
        rankedDocuments.addAll(documentsMap.entrySet());

        // get top (resultSize) or less
        SortedSet<Map.Entry<String, Double>> topDocuments = new TreeSet<>(new RankComparator());
        int count = 0;
        for (Map.Entry<String, Double> document : rankedDocuments){
            topDocuments.add(document);
            count++;
            if (count == resultSize) break;
        }
        return topDocuments;
    }

    /**
     * Parse all strings in stringArray and add them to longArray
     * @param stringArray to parse
     * @return array of longs
     */
    private long[] toLongArray(String[] stringArray) {
        long[] longArray = new long[stringArray.length];
        for (int i = 0; i < stringArray.length; i++) longArray[i] = Long.parseLong(stringArray[i]);
        return longArray;
    }

    /**
     * Calculate the BM25 factor for the given term from the query and document
     * @param tf            term frequency in doc
     * @param docCount      number of docs in corpus
     * @param docLength     length of doc
     * @param avgDocLength  average doc length
     * @param qf            term frequency in query
     * @param df            term document frequency
     * @param K             give the upper bound
     * @param b             normalizer
     * @return              BM25 value
     */
    private double getBM25Factor(double tf, double docCount, double docLength, double avgDocLength, double qf, double df, double K, double b) {
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

    /**
     * Calculate the positions in doc factor which is:
     * factor = 1 - (p1/docLen)*(p2/docLen)*...(pn/docLen)
     * @param positions of term in doc
     * @param docLength of doc
     * @return factor
     */
    private double getPositionsInDocFactor(long[] positions, int docLength){
        double factor = 1;
        for(long position : positions) factor *= (position/docLength);
        return 1 - factor;
    }
}
