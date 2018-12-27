package Models;
import java.util.ArrayList;

/**
 * Represents a query
 */
public class Query {

    public String num = "000"; // id of query
    public String title = ""; // query's content
    // more data
    public String desc = "";
    public String narr = "";

    /** the results of the query, in the from" docID, rank */
    public ArrayList<String> result;
}
