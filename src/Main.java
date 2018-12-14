import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

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

//        String x = "147\n" +
//                "147\n" +
//                "162\n" +
//                "160\n" +
//                "137\n" +
//                "139\n" +
//                "153\n" +
//                "166\n" +
//                "145\n" +
//                "155\n" +
//                "130\n" +
//                "156\n" +
//                "144\n" +
//                "154\n" +
//                "129\n" +
//                "157\n" +
//                "132\n" +
//                "158\n" +
//                "152\n" +
//                "144\n" +
//                "155\n" +
//                "128\n" +
//                "152\n" +
//                "145\n" +
//                "169\n" +
//                "170\n" +
//                "156\n" +
//                "150\n" +
//                "145\n" +
//                "156\n" +
//                "159\n" +
//                "162\n" +
//                "138\n" +
//                "145\n" +
//                "153\n" +
//                "130\n" +
//                "149\n" +
//                "167\n" +
//                "147\n" +
//                "155\n" +
//                "145\n" +
//                "137\n" +
//                "150\n" +
//                "167\n" +
//                "167\n" +
//                "162\n" +
//                "143\n" +
//                "140\n" +
//                "159\n" +
//                "136\n" +
//                "143\n" +
//                "144\n" +
//                "149\n" +
//                "143\n" +
//                "156\n" +
//                "142\n" +
//                "144\n" +
//                "147\n" +
//                "143\n" +
//                "161\n" +
//                "158\n" +
//                "128\n" +
//                "125\n" +
//                "151\n" +
//                "143\n" +
//                "137\n" +
//                "137\n" +
//                "151\n" +
//                "170\n" +
//                "174\n" +
//                "154\n" +
//                "145\n" +
//                "136\n" +
//                "151\n" +
//                "141\n" +
//                "154\n" +
//                "148\n" +
//                "151\n" +
//                "176\n" +
//                "170\n" +
//                "162\n" +
//                "134\n" +
//                "142\n" +
//                "133\n" +
//                "158\n" +
//                "150\n" +
//                "129\n" +
//                "135\n" +
//                "160\n" +
//                "172\n" +
//                "138\n" +
//                "171\n" +
//                "154\n" +
//                "152\n" +
//                "166\n" +
//                "161\n" +
//                "142\n" +
//                "164\n" +
//                "142\n" +
//                "146\n" +
//                "156\n" +
//                "142\n" +
//                "152\n" +
//                "168\n" +
//                "135\n" +
//                "165\n" +
//                "160\n" +
//                "152\n" +
//                "145\n" +
//                "177\n" +
//                "139\n" +
//                "136\n" +
//                "155\n" +
//                "155\n" +
//                "153\n" +
//                "134\n" +
//                "148\n" +
//                "150\n" +
//                "150\n" +
//                "174\n" +
//                "146\n" +
//                "145\n" +
//                "150\n" +
//                "147\n" +
//                "122\n" +
//                "131\n" +
//                "129\n" +
//                "148\n" +
//                "144\n" +
//                "159\n" +
//                "130\n" +
//                "143\n" +
//                "146\n" +
//                "144\n" +
//                "150\n" +
//                "160\n" +
//                "155\n" +
//                "174\n" +
//                "151\n" +
//                "163\n" +
//                "148\n" +
//                "151\n" +
//                "149\n" +
//                "144\n" +
//                "136\n" +
//                "146\n" +
//                "158\n" +
//                "143\n" +
//                "163\n" +
//                "154\n";
//        String[] list = x.split("\n");
//        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
//        for (String i : list){
//            if (map.containsKey(i)) map.put(i,map.get(i)+1);
//            else  map.put(i,1);
//        }
//        for (Map.Entry<String, Integer> entry : map.entrySet()) System.out.println(entry.getKey() + "\t" + entry.getValue());
    }
}
