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
    public ArrayList<String> getRankedDocuments(ArrayList<ArrayList<String[]>> postings,
                                                HashMap<String, String[]> documents, double K,
                                                double b, int docCount, double avgDocLength, int resultSize) {

        HashMap<String, RankedDoc> rankedDocs = new HashMap<>(); // to hold and rank all docs
        for (ArrayList<String[]> posting : postings) {

            // Get general term data
            String[] termData = posting.get(0);
            String term = termData[0];
            int qf = Integer.parseInt(termData[2]);
            int df = Integer.parseInt(termData[1]);
            long[] positionsInQuery = toLongArray(termData[3].trim().split(" "));

            // For all doc postings of term
            for (int i = 1; i < posting.size(); i++) {

                // get doc posting data
                String[] docPosting = posting.get(i);
                String docName = docPosting[0];
                String inTitle = docPosting[1];
                int tf = Integer.parseInt(docPosting[2]);
                int docLength = Integer.parseInt(documents.get(docName)[0]);
                long[] positionsInDoc = toLongArray(docPosting[3].trim().split(" "));

                // Calculate all rank factors of doc
                double value = 1;
                value *= getBM25Factor(tf, docCount, docLength, avgDocLength, qf, df, K, b);
//                value *= getPositionsInDocFactor(positionsInDoc, docLength, 1);
                if (inTitle.equals("t")) value *= 2; // being in title is important!

                // update rank of doc
                RankedDoc rankedDoc = rankedDocs.get(docName);
                if (rankedDoc == null) {
                    rankedDoc = new RankedDoc(docName);
                    rankedDocs.put(docName, rankedDoc);
                }
                rankedDoc.value += value;
                rankedDoc.intersection++;
                if (Character.isUpperCase(term.charAt(0))) rankedDoc.entities++;
            }
        }
        // sort docs by rank
        SortedSet<RankedDoc> sortedRankedDocs = new TreeSet<>(new RankComparator());
        sortedRankedDocs.addAll(rankedDocs.values());

        // get top (resultSize) or less
        ArrayList<String> topRankedDocNames = new ArrayList<>();
        int count = 0;
        for (RankedDoc rankedDoc : sortedRankedDocs){
            topRankedDocNames.add(rankedDoc.name);
            count++;
            if (count == resultSize) break;
        }
        return topRankedDocNames;
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
     * If doc1 has more terms from query than doc2, doc1 is better.
     * Else if doc1 has more UpperCase terms than doc2, doc1 is better.
     * Else if doc1 has more value than doc2, doc1 is better.
     */
    private class RankComparator implements Comparator<RankedDoc>
    {
        @Override
        public int compare(RankedDoc doc1, RankedDoc doc2) {
            if (doc1.intersection < doc2.intersection) return 1;
            if (doc1.intersection > doc2.intersection) return -1;
            if (doc1.entities > doc2.entities) return 1;
            if (doc1.entities < doc2.entities) return -1;
            if (doc1.value < doc2.value) return 1;
            if (doc1.value > doc2.value) return -1;
            return 0;
        }
    }

    /**
     * Calculate the positions in doc factor which is:
     * factor = 1 - (p1/docLen)*(p2/docLen)*...(pn/docLen)
     * @param positions of term in doc
     * @param docLength of doc
     * @param positionsToCount number of positions before stopping
     * @return factor
     */
    private double getPositionsInDocFactor(long[] positions, double docLength, int positionsToCount){
        double factor = 1;
        for(int i = 0; i < positionsToCount; i++) {
            long position = positions[i];
            factor *= (position/docLength);
        }
        return 1 - factor;
    }

    /**
     * Class for sorting the ranked docs
     */
    private class RankedDoc {
        /** name of doc */
        public String name = "";
        /** value from all sorts of formula like BM25 */
        public double value = 0.0;
        /** size of the intersection of Query and Doc */
        public int intersection = 0;
        /** number of terms that are in UpperCase in dictionary */
        public int entities = 0;

        public RankedDoc(String name) {
            this.name = name;
        }
    }
}
