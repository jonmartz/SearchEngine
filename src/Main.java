import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    /*
    todo: things changed in indexing:
    1) stop words are now saved in index
    2) added getParsedSentence to indexer AND parser
    3) added document average calculation in writeDocs function
    */

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("GUI/View.fxml"));
        primaryStage.setTitle("Search Engine");
        primaryStage.setScene(new Scene(root, 830, 454));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
