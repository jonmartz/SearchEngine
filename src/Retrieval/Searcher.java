package Retrieval;

import Indexing.Indexer;
import Indexing.Parse;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Responsible of retrieving the most relevant documents for a given query.
 */
public class Searcher {

    /**
     * Dictionary of terms in index
     */
    private final ConcurrentHashMap<String, long[]> dictionary;
    /**
     * path of the index folder
     */
    private final String indexPath;
    /**
     * cities to filter the documents with
     */
    private final HashSet<String> selectedCities;
    /**
     * used to parse the query
     */
    private final Indexer indexer;
    /**
     * for ranking the relevant docs
     */
    private final Ranker ranker;

    /**
     * Constructor
     * @param dictionary of terms
     * @param indexPath path of index folder
     * @param selectedCities to filer documents with
     */
    public Searcher(ConcurrentHashMap<String, long[]> dictionary, String indexPath, HashSet<String> selectedCities) {
        this.dictionary = dictionary;
        this.indexPath = indexPath;
        this.selectedCities = selectedCities;
        this.indexer = new Indexer(indexPath);
        this.ranker = new Ranker();
    }

    /**
     * get a list of the relevant documents for a given query, ordered from highest to lowest rank
     * (list[0] is the most relevant document).
     * @param query to get documents for
     * @param useStemming true to use stemming
     * @return list of relevant documents
     */
    public ArrayList<String> getRankedDocuments(String query, boolean useStemming) throws IOException {
        ArrayList<String> rankedDocuments = new ArrayList<>();

        // get terms from query
        HashSet<String> stopWords = getStopWords();
        LinkedList<String> terms = indexer.getParsedSentence(query, stopWords, useStemming);

        // get postings of terms
        HashMap<String, HashMap<String, String[]>> postings = new HashMap<>();
        for (String term : terms) getPostings(term, postings);

        return rankedDocuments;
    }

    /**
     * Add all term postings from index to postings list
     * @param term to the postings of
     * @param postings list to add postings to
     */
    private void getPostings(String term, HashMap<String, HashMap<String, String[]>> postings) {
        // todo: implement
    }

    /**
     * Get stop words from index
     * @return set of stop words
     */
    private HashSet<String> getStopWords() throws IOException {
        // set input
        String inputPath = indexPath + "\\stopWords";
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(inputPath), StandardCharsets.UTF_8));

        HashSet<String> stopWords = new HashSet<>();
        String line;
        while ((line = reader.readLine()) != null) {
            stopWords.add(line.trim());
        }
        reader.close();
        return stopWords;
    }
}
