import Retrieval.Semantic;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Main extends Application {

    /*
    todo: things changed in indexing:
    1) stop words are now saved in index
    2) added getParsedSentence to indexer AND parser
    3) added document average calculation in writeDocs function
    4) added costume tag to ReadFile
    5) added entity find during merge
    */

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("GUI/View.fxml"));
        primaryStage.setTitle("Search Engine");
        primaryStage.setScene(new Scene(root, 830, 454));
        primaryStage.show();
    }

    public static void main(String[] args) throws IOException {
        String[] glovePath = {"C:","JavaProject","SearchEngine","glove.6B.50d.txt"};
        Semantic semantic = new Semantic(String.join("\\", glovePath));
        semantic.SemnticsFile(10);
        //launch(args);
    }
}
