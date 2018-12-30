package Retrieval;

import Indexing.Indexer;
import Indexing.ReadFile;
import Models.Doc;
import Models.Query;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Struct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Responsible of retrieving the most relevant documents for a given query.
 */
public class Searcher {

    /**
     * to use semantics or not
     */
    private final boolean useSemantics;
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
    private HashMap<String, String[]> documents;
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
    private double averageDocLength;
    /**
     * indicates how many docs to retrieve as result for a query
     */
    private int resultSize;

    // for BM25
    private double k;
    private double b;

    /**
     * for semantics
     */
    private Semantic semantic;

    /**
     * Constructor
     * @param dictionary of terms
     * @param indexPath path of index folder
     * @param selectedCities to filer documents with
     * @param k for BM25
     * @param b for BM25
     */
    public Searcher(ConcurrentHashMap<String, long[]> dictionary,
                    String indexPath, HashSet<String> selectedCities,
                    double k, double b, int resultSize, boolean useSemantics) throws IOException {
        this.dictionary = dictionary;
        this.indexPath = indexPath;
        this.selectedCities = selectedCities;
        this.indexer = new Indexer(indexPath);
        this.ranker = new Ranker();
        this.documents = getDocuments(!selectedCities.isEmpty());
        this.stopWords = getStopWords();
        this.k = k;
        this.b = b;
        this.resultSize = resultSize;
        this.useSemantics = useSemantics;
        if (useSemantics) semantic = new Semantic();
    }

    /**
     * Get the list of documents with their data, doc count and average doc length from index.
     * @param useFilter if true, get only documents that have a city from selected cities list.
     *                  else, get all documents.
     * @return map of documents with data from index, in the from:
     * docID -> docLength, maxTf, city, language, date
     */
    private HashMap<String, String[]> getDocuments(boolean useFilter) throws IOException {

        HashMap<String, String[]> documents = new HashMap<>();
        if (useFilter) documents = getFilteredDocuments();

        // Get the document data from document index
        String inputPath = indexPath + "\\documents";
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(inputPath), StandardCharsets.UTF_8));
        String line = reader.readLine();
        String[] stats = line.split(",");
        docCount = Integer.parseInt(stats[0]);
        averageDocLength = Double.parseDouble(stats[1]);

        // read document index
        while ((line = reader.readLine()) != null) {
            String[] strings = (line + "|").split("\\|");
            String docName = strings[0];
            String docID = strings[strings.length-1];
            // the filtering part:
            if (useFilter && !documents.containsKey(docID)) continue;
            //                  docLength   maxTf       city        language    date
            String[] docData = {strings[3], strings[4], strings[5], strings[6], strings[7], docName};
            documents.put(docID,docData);
        }
        reader.close();
        return documents;
    }

    /**
     * Get all documents that contain a city from the selected cities
     */
    private HashMap<String, String[]> getFilteredDocuments() throws IOException {
        HashMap<String, String[]> selectedDocuments = new HashMap<>();
        for (String city : selectedCities){
            // Get the city postings
            long[] data = new long[3];
            city = getTermDataAndFixTermCase(city, data);
            long pointer = data[2]; // pointer to offset in postings file
            ArrayList<String[]> cityPostings = new ArrayList<>();
            searchAndAddTermPostings(city, pointer, cityPostings, false);

            // get all doc names filtered by cities, still without their data.
            for (String[] cityPosting : cityPostings) selectedDocuments.put(cityPosting[0], null);
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
    public ArrayList<String> getResult(Query query, boolean useStemming, int synonymsCount) throws IOException {

        // Add synonymsMap in case of semantics
        String queryText = query.title;
        if (useSemantics) queryText = addSynonyms(queryText, synonymsCount);
        queryText += " " + query.desc;
//        queryText += " " + query.narr;

        // get terms from query
        LinkedList<String> parsedSentence = indexer.getParsedSentence(queryText, stopWords, useStemming);
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
        ArrayList<String> rankedDocIDs = ranker.getRankedDocuments(postings, documents, k, b,
                docCount, averageDocLength, resultSize);

        // translate doc IDs to doc names
        ArrayList<String> rankedDocNames = new ArrayList<>();
        for (String docID : rankedDocIDs){
            String[] docData = documents.get(docID);
            rankedDocNames.add(docData[docData.length-1]);
        }
        return rankedDocNames;
    }

    /**
     * Add a mapping for each term to it's synonymsMap, getting the synonymsMap from the Semantic class.
     * @param queryText to add synonymsMap to
     * @return query text with the added synonymsMap
     */
    private String addSynonyms(String queryText, int synonymsCount) {
        LinkedList<String> terms = indexer.getParsedSentence(queryText, stopWords, false);
        String newQueryText = "";
        for (String term : terms){
            ArrayList<String> termManyTimes = new ArrayList<>();
            for (int i = 0; i < synonymsCount; i++){
               termManyTimes.add(term);
            }
            String synonymsString = semantic.getSynonyms(term, synonymsCount);
            newQueryText += String.join(" ",termManyTimes) + " " + synonymsString + " ";
        }
//        System.out.println(newQueryText);
        return newQueryText;
    }

    /**
     * Add the term data and all its postings from index to postings list.
     * The added posting will be like this:
     * posting[0] = term, df, qf, positionsInQuery
     * posting[i>0] = docID, inTitle, tf, positionsInDoc
     * @param termEntry to add posting of
     * @param postings to add the posting to
     */
    public void addPostings(Map.Entry<String, ArrayList<Integer>> termEntry,
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
    private void searchAndAddTermPostings(String term, long pointer, ArrayList<String[]> termPostings,
                                          boolean filterByCities) throws IOException {

        BufferedRandomAccessFile reader = new BufferedRandomAccessFile(indexPath + "\\postings\\" + term.charAt(0), "rw");
        reader.seek(pointer);
        String line;
        while ((line = reader.getNextLine()) != null && line.length() > 1) {
            String[] strings = line.split("\\|");

            // posting like this: docID, inTitle, tf, positionsInDoc
            String docID = strings[0];

            // Add, if not filtering OR (filtering AND document is in the selected set)
            if (filterByCities && !documents.containsKey(docID)) continue;
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
        if (termData == null) { // then term appears in upper case in dictionary
            term = term.toUpperCase();
            termData = dictionary.get(term);
        }
        if (termData == null) return null; // then term is not in dictionary!
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
    public HashSet<String> getStopWords() throws IOException {
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

    /**
     * Get the document's structure
     * @param corpusPath of corpus
     * @param docName of doc to retrieve
     * @return doc's text
     */
    public String getDocString(String corpusPath, String docName) throws IOException {
        String inputPath = indexPath + "\\documents";
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(inputPath), StandardCharsets.UTF_8));
        String line = reader.readLine(); // skip first line
        String file = "";
        int docPosition = 0;
        while ((line = reader.readLine()) != null) {
            String[] strings = (line + "|").split("\\|");
            if (strings[0].equals(docName)){
                file = strings[1];
                docPosition = Integer.parseInt(strings[2]);
                break;
            }
        }
        reader.close();
        if (file.isEmpty()) return null;
        ArrayList<String> docsInFile = ReadFile.read(corpusPath + "\\" + file + "\\" + file);
//        return Jsoup.parse(docsInFile.get(docPosition), "", Parser.xmlParser());
        return docsInFile.get(docPosition);
    }
}
