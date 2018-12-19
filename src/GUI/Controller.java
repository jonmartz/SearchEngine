package GUI;

import Indexing.Indexer;
import Indexing.ReadFile;
import Models.Query;
import Retrieval.Searcher;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Controls the interaction between the user, the GUI (View) and the Index (Model).
 */
public class Controller implements Initializable {

    @FXML
    public Text corpusPathOKText; // shows when corpus indexPath has been selected
    public Text indexPathOKText; // shows when index indexPath has been selected
    public CheckBox useStemming; // to use stemming
    public Button createIndexButton; // to create index
    public Text commentsBox; // shows all kinds of comments
    public ChoiceBox languageChoicebox; // to choose language. Fills up after creating index / loading dictionary
    public Text docCountValue; // docs in corpus
    public Text termCountValue; // terms in dictionary
    public Text totalTimeValue; // indexing time
    public Button dictionaryViewButton; // to view dictionary
    public Button resetButton; // to remove index
    // The rest are self explanatory
    public Text docCountText;
    public Text termCountText;
    public Text totalTimeText;
    public TableView dictionaryView;
    public TableColumn termColumn;
    public TableColumn dfColumn;
    public TableColumn cfColumn;
    public Button loadDictionaryButton;
    public Button corpusPathButton;
    public Button indexPathButton;
    public GridPane indexStatsPane;

    // queries part
    @FXML
    public BorderPane queryPane;
    public TextField queryTextField;
    public RadioButton queryTextCheckBox;
    public RadioButton queryFileCheckBox;
    public Button queryFileButton;
    public Button RUNButton;
    public CheckBox semanticsCheckBox;
    public Text commentsQueryBox;
    public Button saveResultsButton;
    public MenuButton citiesMenu;
    public TableView entitiesTable;
    public TableColumn entitiesRankCol;
    public TableColumn entitiesEntityCol;
    public Text entitiesDocIDText;
    public TableView queryResultsTable;
    public TableColumn queryResultsRankCol;
    public TableColumn queryResultsDocCol;
    public TableColumn buttonColumn;
    public ComboBox querySelectChoiceBox;
    public TextField KTextField;
    public TextField bTextField;
    public TextField searchTermPostingsTextField;
    public Button searchTermPostingsButton;
    public TextField resultSizeTextField;
    public TextField searchDocumentTextField;
    public Button searchDocumentButton;

    /**
     * indicate whether the loaded dictionary is from an index that was built using stemming or not
     */
    private boolean usedStemming = false;
    /**
     * indexPath of the corpus directory
     */
    private String corpusPath;
    /**
     * indexPath of the index directory
     */
    private String indexPath;
    /**
     * Indexing.Indexer to make index
     */
    private Indexer indexer;
    /**
     * Number of files to write in every temporal posting
     */
    private int filesPerPosting = 1;
    /**
     * for measuring indexing time
     */
    private long startingTime;
    /**
     * dictionary to hold in memory
     */
    private ConcurrentHashMap<String, long[]> dictionary;
    /**
     * indexPath of the queries file
     */
    private String queriesFilePath = "";
    /**
     * Query for which the results are being displayed
     */
    private Query query;
    /**
     * List of queries from file
     */
    private ArrayList<Query> queries;

    // parameters for BM25:
    private double K;
    private double b;

    // for our own use
    private String termToSearchPostings;

    /**
     * Initializes the controller.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        statsVisible(false);
        querySelectChoiceBox.setVisible(false);

        // dictionary table
        termColumn.setCellValueFactory(new PropertyValueFactory<DictEntry, String>("term"));
        dfColumn.setCellValueFactory(new PropertyValueFactory<DictEntry, String>("df"));
        cfColumn.setCellValueFactory(new PropertyValueFactory<DictEntry, String>("cf"));

        // query results table
        queryResultsDocCol.setCellValueFactory(new PropertyValueFactory<ResultEntry, String>("docID"));
        queryResultsRankCol.setCellValueFactory(new PropertyValueFactory<ResultEntry, String>("rank"));
        buttonColumn.setCellValueFactory(new PropertyValueFactory<>("null")); // just for setting up buttons
        buttonColumn.setCellFactory(getButtonCallback());

        setKforBM25();
        setBforBM25();
    }

    /**
     * Opens a "browse" window for the user to choose a directory.
     * @param title of browse window
     * @return indexPath of directory chosen
     */
    private String getDirectoryPath(String title) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);
        File file = directoryChooser.showDialog(null);
        if (file != null) return file.getAbsolutePath();
        return null;
    }

    /**
     * Gets the indexPath of the corpus directory
     */
    public void getCorpusPath() {
        String path = getDirectoryPath("Select corpus directory");
        if (path != null){
            corpusPath = path;
            corpusPathButton.setTooltip(new Tooltip(corpusPath));
            corpusPathOKText.setVisible(true);
            checkAllFields();
        }
    }

    /**
     * Gets the indexPath of the index directory
     */
    public void getIndexPath() {
        String path  = getDirectoryPath("Select index directory");
        if (path != null){
            indexPath = path;
            indexPathButton.setTooltip(new Tooltip(indexPath));
            indexPathOKText.setVisible(true);
            checkAllFields();
            loadDictionaryButton.setDisable(false);
        }
    }

    /**
     * Checks if the index and corpus folders have been chosen and if they do
     * it enables the "Create index" button
     */
    private void checkAllFields() {
        if (corpusPath != null && indexPath != null) createIndexButton.setDisable(false);
    }

    /**
     * Loads into memory the dictionary from the index that's in the index indexPath. If there's no such index
     * then a message is displayed.
     * If "use stemming" is checked, will load the dicitonary from the "withStemming" indexPath, else from
     * the "withoutStemming" indexPath.
     */
    public void loadDictionary() {
        try{
            showComment(commentsBox,"GREEN", "Loading dictionary...");
            statsVisible(false);
            dictionaryView.setItems(null);

            String path = getIndexFullPath();

            BufferedReader reader = new BufferedReader(new FileReader(new File(path + "\\documents")));
            String line = reader.readLine();
            double documentCount = Double.parseDouble(line.split(",")[0]);
            reader.close();

            dictionary = new ConcurrentHashMap<>();
            reader = new BufferedReader(new FileReader(new File(path + "\\dictionary")));
            while ((line = reader.readLine()) != null){
                String[] termEntry = line.split("\\|");
                String term = termEntry[0];
                long[] termData = new long[3];
                termData[0] = Long.valueOf(termEntry[1]);
                termData[1] = Long.valueOf(termEntry[2]);
                termData[2] = Long.valueOf(termEntry[3]);
                dictionary.put(term,termData);
            }
            reader.close();

            setLanguages(path);
            setCities(path);
            showComment(commentsBox,"GREEN","Finished!");
            DecimalFormat formatter = new DecimalFormat("#,###");
            docCountValue.setText(formatter.format(documentCount));
            termCountValue.setText(formatter.format(dictionary.size()));
            statsVisible(true);
            totalTimeValue.setVisible(false);
            totalTimeText.setVisible(false);

        } catch (Exception e) {
//            e.printStackTrace();
            dictionary = null;
            showComment(commentsBox,"RED", e.getMessage());
        }
    }

    /**
     * Get the full path of index, taking into account if useStemming is true / false
     * @return full path
     */
    private String getIndexFullPath() {
        if (useStemming.isSelected()) {
            usedStemming = true;
            return indexPath + "\\WithStemming";
        }
        else{
            usedStemming = false;
            return indexPath + "\\WithoutStemming";
        }
    }

    /**
     * Add languages from the language index to the list of available languages.
     */
    private void setLanguages(String path) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(new File(path + "\\languages")));
        String line = "";
        languageChoicebox.getItems().clear();
        ObservableList items = languageChoicebox.getItems();
        while ((line = reader.readLine()) != null) items.add(line);
    }

    /**
     * Creates the index of corpus from corpus that in index indexPath, using the stop-words
     * from the corpus indexPath. If there's already a completed index in the indexPath, it replaces it.
     * If "use stemming" is checked, will create the index in the "withStemming" indexPath, else from
     * the "withoutStemming" indexPath.
     */
    public void createIndex() {
        try{
            String path = getIndexFullPath();

            // In case index already exists
            if (Files.exists(Paths.get(path))) {
                String text = "Index already exists in folder. Do you want to delete it?";
                if (getResultFromWarning(text) == ButtonType.NO) return;
            }

            indexer = new Indexer(path);
            dictionary = null;

            // Modify GUI
            statsVisible(false);
            showComment(commentsBox,"GREEN", "Creating index...");
            createIndexButton.setDisable(true);
            loadDictionaryButton.setDisable(true);
            dictionaryView.setItems(null);

            // Run indexer on thread so gui can work
            Thread thread = new Thread(new Task<Void>() {
                @Override
                protected Void call() {
                    try {
                        indexer.createInvertedIndex(corpusPath, useStemming.isSelected(), filesPerPosting);
                        indexingFinished();
                    }
                    catch (Exception e) {
                        indexer = null;
                        showComment(commentsBox,"RED", e.getMessage());
                        e.printStackTrace();
                    }
                    return null;
                }
            });
            startingTime = System.currentTimeMillis();
            thread.start();
        } catch (Exception e) {
            indexer = null;
            showComment(commentsBox,"RED", e.getMessage());
        }
    }

    /**
     * Continuation of the "createIndex" method that the thread triggers after finishing.
     */
    private void indexingFinished() {
        double totalTime = (System.currentTimeMillis() - startingTime)/1000;
        createIndexButton.setDisable(false);
        loadDictionaryButton.setDisable(false);
        dictionary = indexer.getDictionary();
        languageChoicebox.setItems(FXCollections.observableArrayList(indexer.getLanguages()));
        setCities(indexer.getCities());
        showComment(commentsBox,"GREEN","Finished!");
        DecimalFormat formatter = new DecimalFormat("#,###");
        docCountValue.setText(formatter.format(indexer.documentCount));
        termCountValue.setText(formatter.format(indexer.dictionarySize));
        totalTimeValue.setText(formatter.format(totalTime) + " seconds");
        statsVisible(true);
    }

    /**
     * Show warning and ask for user's confirmation
     * @param text of warning
     * @return user's answer
     */
    private ButtonType getResultFromWarning(String text){
        Alert alert = new Alert(Alert.AlertType.WARNING, text, ButtonType.YES, ButtonType.NO);
        alert.showAndWait();
        return alert.getResult();
    }

    /**
     * Display a comment in the comment's box
     * @param color of comment
     * @param text of comment
     */
    private void showComment(Text comments, String color, String text) {
        if (color.equals("RED")) text = "ERROR: " + text;
        comments.setFill(Paint.valueOf(color));
        comments.setText(text);
        comments.setVisible(true);
    }

    /**
     * Modify the visibility of many GUI elements.
     * @param visibility true to show the elements, false to hide them
     */
    private void statsVisible(boolean visibility) {
//        commentsBox.setVisible(false);
        docCountValue.setVisible(visibility);
        termCountValue.setVisible(visibility);
        totalTimeValue.setVisible(visibility);
        docCountText.setVisible(visibility);
        termCountText.setVisible(visibility);
        totalTimeText.setVisible(visibility);
        dictionaryViewButton.setVisible(visibility);
        resetButton.setVisible(visibility);
        dictionaryView.setVisible(false);
        dictionaryViewButton.setDisable(false);

        // queries
        queryPane.setVisible(visibility);
    }

    /**
     * Is called when the user types into the query text field
     */
    public void queryTyped() {
        if (queryTextField.getText().isEmpty()) RUNButton.setDisable(true);
        else RUNButton.setDisable(false);
    }

    /**
     * Called when a query is selected from the drop down menu
     */
    public void querySelected() {
        String queryNum = querySelectChoiceBox.getValue().toString().split(":")[0];
        for (Query query : queries) {
            if (query.num.equals(queryNum)) {
                this.query = query;
                displayQueryResult();
            }
        }
    }

    /**
     * Called when the user presses the stemming button. If the loaded dictionary is not from
     * an index built with/without stemming, makes part of the GUI invisible.
     */
    public void pressedStemming() {
        if ((usedStemming && !useStemming.isSelected()) || (!usedStemming && useStemming.isSelected())) {
            statsVisible(false);
            dictionaryView.setVisible(false);
            queryPane.setVisible(false);
        } else if (dictionary != null){
            statsVisible(true);
            if (dictionaryView.getItems() != null) dictionaryView.setVisible(true);
            queryPane.setVisible(true);
        }
    }

    /**
     * Set the K parameter for the BM25 formula. is called after something is inserted in textbox
     */
    public void setKforBM25() {
        try{
            K = Double.parseDouble(KTextField.getText());
        } catch (NumberFormatException e) {
            showComment(commentsBox, "RED", "K must be a double");
        }
    }

    /**
     * Set the b parameter for the BM25 formula. is called after something is inserted in textbox
     */
    public void setBforBM25() {
        try{
            b = Double.parseDouble(bTextField.getText());
        } catch (NumberFormatException e) {
            showComment(commentsBox, "RED", "b must be a double");
        }
    }

    /**
     * Print in console the postings of term to search
     */
    public void searchTermPostings() {
        try {
            String path = getIndexFullPath();
            int resultSize = Integer.parseInt(resultSizeTextField.getText());
            Searcher searcher = new Searcher(dictionary, path, getSelectedCities(), K, b, resultSize);
            Indexer tempIndexer = new Indexer(path);
            HashSet<String> stopWords = searcher.getStopWords();
            String text = searchTermPostingsTextField.getText();
            LinkedList<String> parsedSentence = tempIndexer.getParsedSentence(text, stopWords, useStemming.isSelected());
            HashMap<String, ArrayList<Integer>> terms = new HashMap<>();
            ArrayList<Integer> positions = new ArrayList<>();
            positions.add(0);
            for (String term : parsedSentence) terms.put(term, positions);
            ArrayList<ArrayList<String[]>> postings = new ArrayList<>();
            for (Map.Entry<String, ArrayList<Integer>> termEntry : terms.entrySet())
                searcher.addPostings(termEntry, postings);
            for (ArrayList<String[]> posting : postings){
                for (String[] doc : posting){
                    System.out.println(String.join(" ", doc));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Prints a document's text and title to the console
     */
    public void searchDocument() throws IOException {
        int resultSize = Integer.parseInt(resultSizeTextField.getText());
        Searcher searcher = new Searcher(dictionary, getIndexFullPath(), getSelectedCities(), K, b, resultSize);
        String docID = searchDocumentTextField.getText();
        System.out.println(searcher.getDocString(corpusPath, docID));
    }

    /**
     * Class that represents an entry from the index dictionary. Its purpose is only to make the TableView
     * for displaying the dictionary in the GUI.
     */
    public static class DictEntry{

        private String term;
        private long df;
        private long cf;

        /**
         * Constructor.
         * @param term name of entry
         * @param Df of entry (document frequency)
         * @param Cf of entry (sum of the tf from every document)
         */
        private DictEntry(String term, long Df, long Cf) {
            this.term = term;
            this.df = Df;
            this.cf = Cf;
        }

        /**
         * Getter
         * @return term's name
         */
        public String getTerm() { return term; }

        /**
         * Getter
         * @return Df of term
         */
        public long getDf() { return df; }

        /**
         * Getter
         * @return Cf of term
         */
        public long getCf() { return cf; }
    }

    /**
     * Display the dictionary as a TableView in the GUI. The table can be sorted by term name / Df / Cf
     * in increasing or decreasing order.
     */
    public void viewDictionary() {
        dictionaryViewButton.setDisable(true);
        ObservableList<DictEntry> items = FXCollections.observableArrayList();
        for (Map.Entry<String, long[]> entry : dictionary.entrySet()){
            long[] data = entry.getValue();
            DictEntry dictEntry = new DictEntry(entry.getKey(), data[0], data[1]);
            items.add(dictEntry);
        }
        dictionaryView.setItems(items);
        dictionaryView.getSortOrder().add(termColumn);
        dictionaryView.setVisible(true);
    }

    /**
     * Erase he index in index indexPath (both with and without stemming)
     * and remove the dictionary from memory.
     */
    public void resetIndex() {
//        String indexPath;
//        if (useStemming.isSelected()) indexPath = indexPath + "\\WithStemming";
//        else indexPath = indexPath + "\\WithoutStemming";
        try {
//            Path directory = Paths.get(indexPath);
            Path directory = Paths.get(indexPath);

            // In case index already exists
            if (Files.exists(directory)) {
                String text = "Index already exists in folder. Do you want to delete it?";
                if (getResultFromWarning(text) == ButtonType.NO) return;
                else {
                    Indexer.removeDir(directory);
                    new File(indexPath).mkdirs();
                    indexer = null;
                    statsVisible(false);
                    commentsBox.setVisible(false);
                }
            }
        } catch (Exception e) {
            commentsBox.setFill(Paint.valueOf("RED"));
            commentsBox.setText("Couldn't delete index!");
        }
    }

    //                                             -------- QUERIES --------

    /**
     * Get the indexPath of the queries file
     */
    public void getQueryFilePath() {
        String path = getFilePath("Select queries file");
        if (path != null){
            queriesFilePath = path;
            queryFileButton.setTooltip(new Tooltip(queriesFilePath));
            RUNButton.setDisable(false);
        }
    }

    /**
     * Opens a "browse" window for the user to choose a file.
     * @param title of browse window
     * @return indexPath of file chosen
     */
    private String getFilePath(String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        File file = fileChooser.showOpenDialog(null);
        if (file != null) return file.getAbsolutePath();
        return null;
    }


    /**
     * Save the query results in the desired folder. The file's name will be "results.txt"
     */
    public void saveResults() throws IOException {
        String path = getDirectoryPath("Select folder to save results in") + "\\results.txt";

        // In case index already exists
        if (Files.exists(Paths.get(path))) {
            String text = "\"results.txt\" already exists in folder. Do you want to rewrite it?";
            if (getResultFromWarning(text) == ButtonType.NO) return;
        }
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path, false)));
        String newLine = System.getProperty("line.separator");
        for (Query query : queries) {
            for (String docName : query.result) {
                String[] line = {query.num, "0", docName, "0", "0.0", "a"};
                out.write(String.join(" ", line) + newLine);
            }
        }
        out.close();
    }

    /**
     * Execute the search and ranking of documents relevant to query (or queries in query file),
     * and display results in GUI.
     */
    public void RUN() {
        try {
            int resultSize = Integer.parseInt(resultSizeTextField.getText());
            Searcher searcher = new Searcher(dictionary, getIndexFullPath(), getSelectedCities(), K, b, resultSize);
            queries = new ArrayList<>();

            // if the query entering method is by entering it in the text field (single query)
            if (queryTextCheckBox.isSelected()) {
                querySelectChoiceBox.setVisible(false);
                query = new Query();
                query.num = "000";
                query.title = queryTextField.getText();
                query.result = searcher.getResult(query, useStemming.isSelected());
                queries.add(query);
                displayQueryResult();
            }

            //  if the query entering method is by selecting a query file (multiple queries)
            else {
                addQueries();
                for (Query query : queries)
                    query.result = searcher.getResult(query, useStemming.isSelected());
                setQueriesChoiceBox();
                querySelectChoiceBox.setVisible(true);
            }

            saveResultsButton.setDisable(false);

        }catch (Exception e) {
            showComment(commentsQueryBox,"RED", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Parse the queries file and get the list of queries and add them to query list, and also get results
     */
    private void addQueries() throws IOException {
        ArrayList<String> queryStrings = ReadFile.readQueriesFile(queriesFilePath);
        for (String queryString : queryStrings) {
            Query query = new Query();
            Document queryStructure = Jsoup.parse(queryString, "", Parser.xmlParser());
            query.num = queryStructure.select("num").text().split(" ")[1];
            query.title = queryStructure.select("title").text();
            query.desc = queryStructure.select("desc").text();
            query.narr = queryStructure.select("narr").text();
            queries.add(query);
        }
    }

    /**
     * Display the query results for the current query in the query results table
     */
    private void displayQueryResult() {
        ObservableList<ResultEntry> items = FXCollections.observableArrayList();
        int rank = 1;
        for (String rankedDocName : query.result){
            items.add(new ResultEntry(rankedDocName, String.valueOf(rank++)));
        }
        queryResultsTable.setItems(items);
        queryResultsTable.getSortOrder().add(queryResultsRankCol);
    }

    /**
     * Is called when the query text box is clicked. Switches to query text method
     */
    public void queryTextChecked() {
        if (queryTextCheckBox.isSelected()) {
            queryTextCheckBox.setDisable(true);
            queryTextField.setDisable(false);

            queryFileCheckBox.setDisable(false);
            queryFileButton.setDisable(true);

            queryFileCheckBox.fire();

            if (queryTextField.getText().isEmpty()) RUNButton.setDisable(true);
            else RUNButton.setDisable(false);
        }
    }

    /**
     * Is called when the query file box is clicked. Switches to query file method
     */
    public void queryFileChecked() {
        if (queryFileCheckBox.isSelected()) {
            queryTextCheckBox.setDisable(false);
            queryTextField.setDisable(true);

            queryFileCheckBox.setDisable(true);
            queryFileButton.setDisable(false);

            queryTextCheckBox.fire();

            if (queriesFilePath.isEmpty()) RUNButton.setDisable(true);
            else RUNButton.setDisable(false);
        }
    }

    /**
     * Get all countries from city index FILE and add them to the country menu.
     */
    public void setCities(String path) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(new File(path + "\\cities")));
        String line = "";
        citiesMenu.getItems().clear();
        ObservableList items = citiesMenu.getItems();
        while ((line = reader.readLine()) != null) {
            String city = line.split("\\|")[0].trim();
            if (!city.isEmpty()) items.add(new CheckMenuItem(city));
        }
    }

    /**
     * Add all countries in the set to the country menu.
     */
    private void setCities(SortedSet<String> cities) {
        citiesMenu.getItems().clear();
        ObservableList items = citiesMenu.getItems();
        for (String city : cities) items.add(new CheckMenuItem(city));
    }

    /**
     * Add all queries to the select query box
     */
    private void setQueriesChoiceBox() {
        querySelectChoiceBox.getItems().clear();
        ObservableList items = querySelectChoiceBox.getItems();
        for (Query query : queries) items.add(query.num + ": " + query.title);
    }

    /**
     * Get a list of all the cities selected in the cities menu
     * @return list of selected cities
     */
    public HashSet<String> getSelectedCities(){
        HashSet<String> selectedCities = new HashSet<>();
        for (MenuItem menuItem : citiesMenu.getItems()){
            CheckMenuItem checkMenuItem = (CheckMenuItem)menuItem;
            if (checkMenuItem.isSelected()) selectedCities.add(checkMenuItem.getText());
        }
        return selectedCities;
    }

    /**
     * Function used to set the button column correctly (taken from stackoverflow)
     * @return Callback
     */
    private Callback<TableColumn<ResultEntry, String>, TableCell<ResultEntry, String>> getButtonCallback() {

        return new Callback<TableColumn<ResultEntry, String>, TableCell<ResultEntry, String>>() {
            @Override
            public TableCell call(final TableColumn<ResultEntry, String> param) {
                final TableCell<ResultEntry, String> cell = new TableCell<ResultEntry, String>() {

                    final Button button = new Button("show");

                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                            setText(null);
                        } else {
                            button.setOnAction(event -> {
                                ResultEntry resultEntry = getTableView().getItems().get(getIndex());
                                showEntities(resultEntry.docID);
                            });
                            setGraphic(button);
                            setText(null);
                        }
                    }
                };
                return cell;
            }
        };
    }

    /**
     * Display the dominant entities of doc in the entities table
     * @param docID of doc
     */
    private void showEntities(String docID) {
        // todo: implement
    }

    /**
     * Class for displaying the query results in the table
     */
    public class ResultEntry {

        // data holders
        private final String docID;
        private final int rank;

        /**
         * Constructor
         * @param docID of result
         * @param rank  of result
         */
        public ResultEntry(String docID, String rank) {
            this.docID = docID;
            this.rank = Integer.parseInt(rank);
        }

        /**
         * getter
         * @return value
         */
        public String getDocID() {
            return docID;
        }

        /**
         * getter
         * @return value
         */
        public int getRank() {
            return rank;
        }
    }
}
