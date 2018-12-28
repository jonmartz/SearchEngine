package Retrieval;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Class responsible for the semantic improvements.
 */
public class Semantic {

    private HashMap<String, String[]> synonyms = new HashMap<>();

    /**
     * Constructor. Builds the synonyms HashMap using the synonyms file.
     */
    public Semantic() throws IOException {

        String path = "src\\Retrieval\\synonyms";
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(path), StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null){
            String[] strings = line.split(": ");
            synonyms.put(strings[0], strings[1].split(" "));
        }
    }

    /**
     * Get a string with the closest terms to term, ordered from closest to farthest.
     * If term is not in synonyms map, returns an empty string.
     * @param term to get synonyms of
     * @param synonymsCount size of result list
     * @return string with the synonyms delimited by " "
     */
    public String getSynonyms(String term, int synonymsCount){
        String[] synonymsOfTerm = synonyms.get(term);
        if (synonymsOfTerm == null) return "";
        ArrayList<String> someSynonymsOfTerm = new ArrayList<>();
        for (int i = 0; i < Math.min(5, synonymsCount); i++){
            someSynonymsOfTerm.add(synonymsOfTerm[i]);
        }
        return String.join(" ", someSynonymsOfTerm);
    }
}
