package Retrieval;

import Indexing.Indexer;
import Models.Query;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Responsible of retrieving the most relevant documents for a given query.
 */
public class Searcher {

    /**
     * true if some cities were selected to filter the documents with
     */
    private final boolean useFilter;
    /**
     * Dictionary of terms in index
     */
    private ConcurrentHashMap<String, long[]> dictionary;
    /**
     * path of the index folder
     */
    private String indexPath;
    /**
     * cities to filter the documents with
     */
    private HashSet<String> selectedCities;
    /**
     * used to parse the query
     */
    private Indexer indexer;
    /**
     * for ranking the relevant docs
     */
    private Ranker ranker;
    /**
     * list of documents filtered by selected cities
     */
    private HashMap<String, String[]> selectedDocuments;
    /**
     * stop words set
     */
    private HashSet<String> stopWords;
    /**
     * number of docs in corpus
     */
    private int docCount;
    /**
     * average doc length in corpus
     */
    private int averageDocLength;

    /**
     * Constructor
     * @param dictionary of terms
     * @param indexPath path of index folder
     * @param selectedCities to filer documents with
     */
    public Searcher(ConcurrentHashMap<String, long[]> dictionary,
                    String indexPath, HashSet<String> selectedCities) throws IOException {
        this.dictionary = dictionary;
        this.indexPath = indexPath;
        this.selectedCities = selectedCities;
        this.useFilter = !selectedCities.isEmpty();
        this.indexer = new Indexer(indexPath);
        this.ranker = new Ranker();
        this.selectedDocuments = getSelectedDocuments();
        this.stopWords = getStopWords();
    }

    /**
     * Get the list of selected documents with their data from index, filtered by selected cities
     * @return map of documents with data from index, in the from:
     *         docID -> docLength, 
     */
    private HashMap<String, String[]> getSelectedDocuments() throws IOException {
        HashMap<String, String[]> selectedDocuments = new HashMap<>();

        // Get all documents that contain a city from the selected cities
        for (String city : selectedCities){
            // Get the city postings
            long[] data = new long[3];
            city = getTermDataAndFixTermCase(city, data);
            long pointer = data[2]; // pointer to offset in postings file
            ArrayList<String[]> cityPostings = new ArrayList<>();
            searchAndAddTermPostings(city, pointer, cityPostings, false);

            // get all filtered documents, still without their data.
            for (String[] cityPosting : cityPostings) selectedDocuments.put(cityPosting[0], null);
        }

        // Get the document data from document index
        String inputPath = indexPath + "\\documents";
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(inputPath), StandardCharsets.UTF_8));
        String line = reader.readLine();
        String[] stats = line.split(",");
        docCount = Integer.parseInt(stats[0]);
        averageDocLength = Integer.parseInt(stats[1]);
        // read index
        while ((line = reader.readLine()) != null) {
            String[] strings = line.split("\\|");
            String docID = strings[0];
            // the filtering part:
            if (useFilter && !selectedDocuments.containsKey(docID)) continue;
            String[] docData = {strings[3], strings[4], strings[5], strings[6], strings[7]};
            selectedDocuments.put(docID,docData);
        }
        reader.close();
        return selectedDocuments;
    }

    /**
     * get a list of the relevant documents for a given query, sorted from highest to lowest rank
     * (list[0] is the most relevant document), filtered by selected cities.
     * @param query to get documents for
     * @param useStemming true to use stemming
     * @return list of relevant documents
     */
    public SortedSet<Map.Entry<String, Double>> getResult(Query query, boolean useStemming) throws IOException {

        // get terms from query
        LinkedList<String> parsedSentence = indexer.getParsedSentence(query.title, stopWords, useStemming);
        HashMap<String, ArrayList<Integer>> terms = new HashMap<>();
        int position = 0;
        for (String term : parsedSentence){ // add positions
            ArrayList<Integer> positions = new ArrayList<>();
            if (terms.containsKey(term)) positions = terms.get(term);
            else terms.put(term, positions);
            positions.add(position++);
        }

        // get postings of terms
        ArrayList<ArrayList<String[]>> postings = new ArrayList<>();
        for (Map.Entry<String, ArrayList<Integer>> termEntry : terms.entrySet()){
            addPostings(termEntry, postings);
        }

        return ranker.getRankedDocuments(postings, selectedDocuments, 1, 0.75, docCount, averageDocLength);
    }

    /**
     * Add the term data and all its postings from index to postings list.
     * The added posting will be like this:
     * posting[0] = term, df, qf, positionsInQuery
     * posting[i>0] = docID, inTitle, tf, positionsInDoc
     * @param termEntry to add posting of
     * @param postings to add the posting to
     */
    private void addPostings(Map.Entry<String, ArrayList<Integer>> termEntry,
                             ArrayList<ArrayList<String[]>> postings) throws IOException {

        String term = termEntry.getKey();
        ArrayList<Integer> positions = termEntry.getValue();
        String qf = String.valueOf(positions.size());
        String positionsString = "";
        for (int position : positions) positionsString += " " + String.valueOf(position);

        // Get term's dictionary entry
        long[] termData = new long[3];
        term = getTermDataAndFixTermCase(term, termData);
        if (term == null) return; // term not in dictionary!
        String df = String.valueOf(termData[0]);

        // put the term's data in the dictionary (term, df, qf, positionsInQuery)
        ArrayList<String[]> termPostings = new ArrayList<>();
        String[] data = {term, df, qf, positionsString};
        termPostings.add(data);

        // add postings
        long pointer = termData[2]; // pointer to offset in postings file
        boolean filterByCities = !selectedCities.isEmpty();
        searchAndAddTermPostings(term, pointer, termPostings, filterByCities);
        postings.add(termPostings);
    }

    /**
     * Add all postings of term to the list. A posting is added like: docID, inTitle, tf, positions.
     * @param term to get postings of
     * @param pointer to find the beginning of term's postings in file
     * @param termPostings list to add the postings to
     * @param filterByCities true to not add the docs that are not in the set of selected docs
     */
    private void searchAndAddTermPostings(String term, long pointer,ArrayList<String[]> termPostings,
                                          boolean filterByCities) throws IOException {

        RandomAccessFile reader = new RandomAccessFile(indexPath + "\\postings\\" + term.charAt(0), "rw");
        reader.seek(pointer);
        String line = reader.readLine(); // skip the first line cause it's the term's name
        while ((line = reader.readLine()) != null && line.length() > 1) {
            String[] strings = line.split("\\|");

            // posting like this: docID, inTitle, tf, positionsInDoc
            String docID = strings[0];

            // Add, if not filtering OR (filtering AND document is in the selected set)
            if (filterByCities && !selectedDocuments.containsKey(docID)) continue;
            String[] posting = {docID, strings[1], strings[2], strings[3]};
            termPostings.add(posting);
        }
    }

    /**
     * Get the term data from dictionary, and fix the term to upper / lower case if necessary
     * @param term to get data of
     * @param termDataPointer term data to modify
     * @return fixed term
     */
    private String getTermDataAndFixTermCase(String term, long[] termDataPointer) {
        long[] termData = dictionary.get(term);
        if (termData == null) { // then term appears in lower case in dictionary
            term = term.toLowerCase();
            termData = dictionary.get(term);
        }
        if (termData == null) return null; // term is not in dictionary!
        else {
            termDataPointer[0] = termData[0];
            termDataPointer[1] = termData[1];
            termDataPointer[2] = termData[2];
        }
        return term;
    }

    /**
     * Get stop words from index
     * @return set of stop words
     */
    private HashSet<String> getStopWords() throws IOException {
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
