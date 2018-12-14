package Indexing;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Class responsible of reading a file from corpus and returning the list of Docs in file.
 */
public class ReadFile {

    /**
     * Split the file by the <tag> and get the list of strings
     * @param path of file
     * @param tag to split with
     * @return list of strings
     */
    public static ArrayList<String> read(String path, String tag) throws IOException {

        ArrayList<String> docsInFile = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(path), StandardCharsets.UTF_8));
        StringBuilder stringBuilder = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
            if (line.contains("<" + tag + ">")) {
                stringBuilder.append(line + "\n");
                line = reader.readLine();
                while (line != null && !line.contains("</" + tag + ">")) {
                    stringBuilder.append(line + "\n");
                    line = reader.readLine();
                }
                if (line != null && line.contains("</" + tag + ">")){
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
                while (line != null && !line.contains("</top>")) {
                    if (line.contains("<title>")) stringBuilder.append("</num>");
                    if (line.contains("<desc>")) stringBuilder.append("</title>");
                    if (line.contains("<narr>")) stringBuilder.append("</desc>");
                    stringBuilder.append(line + "\n");
                    line = reader.readLine();
                }
                if (line != null && line.contains("</top>")){
                    stringBuilder.append("</narr>");
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