package Retrieval;

import Indexing.Indexer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Responsible of retrieving the most relevant documents for a given query.
 */
public class Searcher {

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
    private HashSet<String> selectedDocuments;

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
        this.indexer = new Indexer(indexPath);
        this.ranker = new Ranker();
        this.selectedDocuments = getSelectedDocuments();
    }

    /**
     * Get the list of selected documents filetered by selected cities
     * @return set of document IDs
     */
    private HashSet<String> getSelectedDocuments() throws IOException {
        HashSet<String> selectedDocuments = new HashSet<>();
        for (String city : selectedCities){

            // Get the city postings
            long[] data = new long[3];
            city = getTermDataAndFixTermCase(city, data);
            long pointer = data[2]; // pointer to offset in postings file
            ArrayList<String[]> cityPostings = new ArrayList<>();
            searchAndAddTermPostings(city, pointer, cityPostings, false);

            // add cities to selected cities
            for (String[] cityPosting : cityPostings) selectedDocuments.add(cityPosting[0]);
        }
        return selectedDocuments;
    }

    /**
     * get a list of the relevant documents for a given query, sorted from highest to lowest rank
     * (list[0] is the most relevant document), filtered by selected cities.
     * @param query to get documents for
     * @param useStemming true to use stemming
     * @return list of relevant documents
     */
    public PriorityQueue<Map.Entry<String, Double>> getRankedDocuments(String query, boolean useStemming) throws IOException {

        // get terms from query
        HashSet<String> stopWords = getStopWords();
        LinkedList<String> parsedSentence = indexer.getParsedSentence(query, stopWords, useStemming);
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

        // get document data
        int[] docStats = new int[2]; // doc count (M in BM25) and average doc length (avdl)
        HashMap<String, String[]> documents = new HashMap<>();
        getDocumentData(documents, docStats);

        return ranker.getRankedDocuments(postings, documents, 1, 0.75, docStats[0], docStats[1]);
    }

    /**
     * Put all the documents from document index with their data in list, and save general doc stats
     * Each document entry will be: docName -> docLength, maxTf, city, language, date
     * @param documents list to add documents to
     * @param documentStats [0] = document count, [1] = average document length
     */
    private void getDocumentData(HashMap<String,String[]> documents, int[] documentStats) throws IOException {
        String inputPath = indexPath + "\\documents";
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(inputPath), StandardCharsets.UTF_8));
        String line = reader.readLine();
        String[] stats = line.split(",");
        documentStats[0] = Integer.parseInt(stats[0]); // doc count
        documentStats[1] = Integer.parseInt(stats[1]); // avg doc length
        while ((line = reader.readLine()) != null) {
            String[] strings = line.split("\\|");
            String documentName = strings[0];
            String[] docData = {strings[3], strings[4], strings[5], strings[6], strings[7]};
            documents.put(documentName,docData);
        }
        reader.close();
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
    private void searchAndAddTermPostings(String term, long pointer,ArrayList<String[]> termPostings
            , boolean filterByCities) throws IOException {

        RandomAccessFile reader = new RandomAccessFile(indexPath + "\\postings\\" + term.charAt(0), "rw");
        reader.seek(pointer);
        String line = reader.readLine(); // skip the first line cause it's the term's name
        while ((line = reader.readLine()) != null && line.length() > 1) {
            String[] strings = line.split("\\|");

            // posting like this: docID, inTitle, tf, positionsInDoc
            String docID = strings[0];

            // Add, if not filtering OR (filtering AND document is in the selected set)
            if (filterByCities && !selectedDocuments.contains(docID)) continue;
            String[] posting = {docID, strings[1], strings[2], strings[3]};
            termPostings.add(posting);
        }
    }

    /**
     * Get the term data from dictionary, and fix the term to upper / lower case if necessary
     * @param term to get data of
     * @param termData from dictionary
     * @return fixed term
     */
    private String getTermDataAndFixTermCase(String term, long[] termData) {
        dictionary.get(term);
        if (termData == null) { // then term appears in lower case in dictionary
            term = term.toLowerCase();
            dictionary.get(term);
        }
        if (termData == null) term = null; // term is not in dictionary!
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
