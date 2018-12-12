package GUI;

import Indexing.Indexer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

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
    private String queriesFilePath;

    /**
     * Initializes the controller.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        statsVisible(false);
        termColumn.setCellValueFactory(new PropertyValueFactory<DictEntry, String>("term"));
        dfColumn.setCellValueFactory(new PropertyValueFactory<DictEntry, String>("df"));
        cfColumn.setCellValueFactory(new PropertyValueFactory<DictEntry, String>("cf"));
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

            String path;
            if (useStemming.isSelected()) path = indexPath + "\\WithStemming";
            else path = indexPath + "\\WithoutStemming";

            BufferedReader reader = new BufferedReader(new FileReader(new File(path + "\\documents")));
            String line = reader.readLine();
            double documentCount = Double.parseDouble(line.split("=")[1]);
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
            // Get index path
            String path = "";
            if (useStemming.isSelected()) path = this.indexPath + "\\WithStemming";
            else path = this.indexPath + "\\WithoutStemming";

            // In case index already exists
            if (Files.exists(Paths.get(path))) {
                String text = "Index already exists in folder. Do you want to delete it?";
                if (getResultFromWarning(text) == ButtonType.NO) return;
            }

            indexer = new Indexer(path);
            dictionary = null;

            // Modify GUI
            showComment(commentsBox,"GREEN", "Creating index...");
            statsVisible(false);
            dictionaryView.setItems(null);

            // Run indexer on thread so gui can work
            Thread thread = new Thread(new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    indexer.createInvertedIndex(corpusPath, useStemming.isSelected(), filesPerPosting);
                    indexingFinished();
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
        comments.setFill(Paint.valueOf(color));
        comments.setText(text);
        comments.setVisible(true);
    }

    /**
     * Modify the visibility of many GUI elements.
     * @param visibility true to show the elements, false to hide them
     */
    private void statsVisible(boolean visibility) {
        commentsBox.setVisible(false);
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
     * Save the query results in the desired folder. The file's name will be "QueryResults.txt"
     */
    public void saveResults() {
        //todo: implement
    }

    /**
     * Execute the search and ranking of documents relevant to query (or queries in query file),
     * and display results in GUI.
     */
    public void RUN() {
        //todo: implement
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
}
