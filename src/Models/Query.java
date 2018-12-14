package Models;

import java.util.Map;
import java.util.PriorityQueue;
import java.util.SortedSet;

/**
 * Represents a query
 */
public class Query {

    public String num; // id of query
    public String title; // query's content
    // more data
    public String desc;
    public String narr;

    /**
     * the results of the query, in the from" docID, rank
     */
    public SortedSet<Map.Entry<String, Double>> result;
}
