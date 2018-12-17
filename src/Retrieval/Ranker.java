package Retrieval;

import java.awt.List;
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
                                                                       double K, double b, int docCount, double avgDocLength,int resultSize) {

        HashMap<String, double[]> documentsMapBef = getMapOfDocumentsBeforeRank(postings, documents, K, b, docCount, avgDocLength);
        HashMap<String, Double> documentsMap = new HashMap<>();
        //todo: calculate formula of rank
        for (Map.Entry<String, double[]> document : documentsMapBef.entrySet()) {
            double ranked = 1;
            double[] rankBef = document.getValue();
            rankBef[2] = 1 - rankBef[2];
            for(double val : rankBef)
                ranked = ranked*val;
            documentsMap.put(document.getKey(),ranked);
        }
        // sort docs by rank
        SortedSet<Map.Entry<String, Double>> rankedDocuments = new TreeSet<>(new RankComparator());
        rankedDocuments.addAll(documentsMap.entrySet());
//gfdfgdfgd
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

    private HashMap<String, double[]> getMapOfDocumentsBeforeRank(ArrayList<ArrayList<String[]>> postings,
                                                                  HashMap<String, String[]> documents,
                                                                  double K, double b, int docCount, double avgDocLength){
        HashMap<String, double[]> documentsMap = new HashMap<>();
        for (ArrayList<String[]> posting : postings) {
            String[] termData = posting.get(0);
            for (int i = 1; i < posting.size(); i++) {
                String[] doc = posting.get(i);
        //BM25
                double BM25 = getBM25(termData[1], docCount, documents.get(doc[0])[0], avgDocLength,
                        termData[2] , doc[2], K, b);
                double [] values = {0.0,0.0,0.0};
                if (!documentsMap.containsKey(doc[0])) {
                    documentsMap.put(doc[0],values);
                }
                else
                    values = documentsMap.get(doc[0]);
                values[0] = documentsMap.get(doc[0])[0] + BM25;
        //in Titele
                if(doc[1] == "t")
                    values[1] = documentsMap.get(doc[0])[1] + 1;
                else
                    values[1] = documentsMap.get(doc[0])[1] + 0.5;
        //position in text: increase value if the position of term in doc in first
                values[2] = documentsMap.get(doc[0])[2] * getPositionIntext(doc[3],documents.get(doc[0])[0]);
                //update all score doc values
                documentsMap.replace(doc[0],values);
            }
        }
        return documentsMap;
    }

    /**
     * Calculate the BM25 for the given term from the query and document
     * @param tfInStr            term frequency in doc
     * @param docCount      number of docs in corpus
     * @param docLengthInStr     length of doc
     * @param avgDocLength  average doc length
     * @param qfInStr            term frequency in query
     * @param dfInStr            term document frequency
     * @param K             give the upper bound
     * @param b             normalizer
     * @return              BM25 calculation
     */
    private double getBM25(String tfInStr, int docCount, String docLengthInStr, double avgDocLength,
                           String qfInStr, String dfInStr, double K, double b) {
        int tf = Integer.parseInt(tfInStr);
        int docLength = Integer.parseInt(docLengthInStr);
        int qf = Integer.parseInt(qfInStr);
        int df = Integer.parseInt(dfInStr);
        return getBM25Formula(tf, docCount, docLength, avgDocLength, qf, df, K, b);
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
    private double getBM25Formula(int tf, int docCount, int docLength, double avgDocLength,
                                  int qf, int df, double K, double b) {
        double numerator = qf * df * (K + 1);
        double denominator = df + (K * (1 - b + (b * (Math.abs(docLength) / avgDocLength))));
        double inLog = Math.log10((docCount + 1) / tf);
        return (double) ((numerator * inLog) / denominator);
    }

    /**
     *
     * @param positionsInDoc
     * @param DocLength
     * @return
     */
    private double getPositionIntext(String positionsInDoc,String DocLength){
        String positionInDocB = positionsInDoc.substring(1,positionsInDoc.length());
        String[] positions = positionInDocB.split(" ");
        double positionDivideDocLength = 1;
        int docLength = Integer.parseInt(DocLength);
        for(String positionStr : positions){
            int position = Integer.parseInt(positionStr);
            positionDivideDocLength = positionDivideDocLength *(position/docLength);
        }
        return positionDivideDocLength;

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
