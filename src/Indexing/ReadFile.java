package Indexing;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Class responsible of reading a file from corpus and returning the list of Docs in file.
 */
public class ReadFile {

    /**
     * Split the file by the <DOC> tag and get the list of strings
     * @param path of file
     * @return list of strings
     */
    public static ArrayList<String> read(String path) throws IOException {

        ArrayList<String> docsInFile = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(path), StandardCharsets.UTF_8));
        StringBuilder stringBuilder = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
            if (line.contains("<DOC>")) {
                stringBuilder.append(line + "\n");
                line = reader.readLine();
                while (line != null && !line.contains("</DOC>")) {
                    stringBuilder.append(line + "\n");
                    line = reader.readLine();
                }
                if (line != null && line.contains("</DOC>")){
                    stringBuilder.append(line);
                    docsInFile.add(stringBuilder.toString());
                    stringBuilder = new StringBuilder();
                }
            }
            line = reader.readLine();
        }
        reader.close();
        return docsInFile;
    }

    /**
     * Split a queries file by the tags, adding an end tag after each tag
     * @param path of file
     * @return list of strings
     */
    public static ArrayList<String> readQueriesFile(String path) throws IOException {

        ArrayList<String> queriesInFile = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(path), StandardCharsets.UTF_8));
        StringBuilder stringBuilder = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
            if (line.contains("<top>")) {
                stringBuilder.append(line + "\n");
                line = reader.readLine();
                boolean inNarr = false;
                while (line != null && !line.contains("</top>")) {
                    if (!line.isEmpty()) {
                        if (line.contains("<narr>")) inNarr = true;
                        line = line.replace("<title>", "</num><title>");
                        line = line.replace("<desc>", "</title><desc>");
                        line = line.replace("<narr>", "</desc><narr>");
                    } else if (inNarr) {
                        line = ";";
                    }
                    stringBuilder.append(line + "\n");
                    line = reader.readLine();
                }
                if (line != null && line.contains("</top>")){
                    line = line.replace("<top>", "</narr><top>");
                    stringBuilder.append(line);
                    queriesInFile.add(stringBuilder.toString());
                    stringBuilder = new StringBuilder();
                }
            }
            line = reader.readLine();
        }
        reader.close();
        return queriesInFile;
    }
}