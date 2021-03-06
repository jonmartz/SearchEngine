package Indexing;

import Models.Doc;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Is responsible for parsing
 */
public class Parse {

    /**
     * month data
     */
    private HashMap<String, String> months;
    /**
     * prefixes to remove
     */
    private HashSet<Character> stopPrefixes;
    /**
     * suffixes to remove
     */
    private HashSet<Character> stopSuffixes;
    /**
     * stem dictionary, for not using the stemmer for words that we already found what their stem is.
     * Why? Because stemming takes a very long time, and holding the stems in memory is not a problem
     * considering the time it will save.
     */
    private ConcurrentHashMap<String, String> stem_collection;
    /**
     * token list from doc
     */
    private LinkedList<String> tokens;
    /**
     * term list from doc (tokens after parsing)
     */
    private LinkedList<String> terms;
    /**
     * stop-words set
     */
    private HashSet stop_words;
    /**
     * the stemmer
     */
    private PorterStemmer stemmer = new PorterStemmer();
    /**
     * map with city info
     */
    private HashMap<String, String[]> cities_dictionary;
    /**
     *
     * true to stem tokens
     */
    private boolean use_stemming;
    /**
     * cities than have been found in docs
     */
    private ConcurrentHashMap<String, String[]> cityIndex;

    /**
     * Constructor. Creates months, prefixes and suffixes sets.
     * @param stop_words set
     */
    public Parse(HashSet stop_words, HashMap cities_dictionary, ConcurrentHashMap cityIndex, HashMap months,
                 ConcurrentHashMap stem_collection, HashSet stopSuffixes, HashSet stopPrefixes, boolean use_stemming) {
        this.stop_words = stop_words;
        this.cities_dictionary = cities_dictionary;
        this.cityIndex = cityIndex;
        this.use_stemming = use_stemming;
        this.stem_collection = stem_collection;
        this.months = months;
        this.stopPrefixes = stopPrefixes;
        this.stopSuffixes = stopSuffixes;

    }

    /**
     * Indexing.Parse all the documents from file in file_path. Gets a Models.Doc array from Indexing.ReadFile where each Models.Doc.lines
     * is a list of the lines in doc, but each Models.Doc.terms is still null.
     * @param docString String of doc to be parsed (contains all tags between <DOC> and </DOC> including that tag)
     * @return array of Docs where each Models.Doc.terms is the list of the doc's terms
     */
    public Doc getParsedDoc(String docString) {

        Doc doc = new Doc();
        tokens = new LinkedList<>();
        terms = new LinkedList<>();

        // Get structure from XML text and set doc's fields
        Document docStructure = Jsoup.parse(docString, "", Parser.xmlParser());
        setDocDetails(doc, docStructure);

        // tokenize lines and get terms from tokens
        String[] lines = docStructure.select("TEXT").text().split("\n");
        docStructure = null;
        for (String line : lines) tokenize(line);
        setTerms(doc);
        return doc;
    }

    /**
     * Get all doc details from XML structure and set them to doc's fields
     * @param doc to set fields to
     * @param docStructure to get details from
     */
    private void setDocDetails(Doc doc, Document docStructure) {
        doc.name = docStructure.select("DOCNO").text();
        Elements FTags = docStructure.select("F");
        String city = "";
        String language = "";
        for (Element tag : FTags){
            if (tag.attr("P").equals("104")) city = tag.text();
            if (tag.attr("P").equals("105")){
                String[] languageTag = tag.text().split(" ");
                if (languageTag.length > 0) language = languageTag[0].trim();
            }
        }
        if (city.length() > 0 && Character.isAlphabetic(city.charAt(0))) setDocCity(doc, city);
        if (language.length() > 0 && language.length() > 2) doc.language = language.toUpperCase();

        // get title
        String title = docStructure.select("TI").text();
        if (title == null) title = docStructure.select("<HEADLINE>").text();
        if (title != null) setDocTitle(doc, title, city.length() > 0);

        // get date
        String date = docStructure.select("DATE1").text();
        if (date.length() > 0){
            // date in from "DAY MONTH YEAR..."
            String[] words = date.trim().split(" ");
            if (words.length > 2) {
                String monthNumber = months.get(words[1].toLowerCase());
                date = words[2] + "-" + monthNumber + "-" + add_zero(words[0]);
            }
            else date = "";
        } else{
            date = docStructure.select("DATE").select("P").text();
            if (date.length() > 0) {
                // date in form "MONTH DAY, YEAR,..."
                String[] words = date.trim().split(" ");
                if (words.length > 2) {
                    String year = words[2];
                    if (year.charAt(year.length()-1) == ',') year = year.substring(0,year.length()-1);
                    String day = words[1];
                    if (day.charAt(day.length()-1) == ',') day = day.substring(0,day.length()-1);
                    String monthNumber = months.get(words[0].toLowerCase());
                    date = year + "-" + monthNumber + "-" + add_zero(day);
                }
                else date = "";
            }
        }
        if (date.length() > 0) doc.date = date;
    }

    /**
     * Set the city found in the tag <F P=104> to doc. If first word is not a city from the cities dictionary,
     * tries with two words, and so on (maximum 5 words).
     * @param doc to set city to
     */
    private void setDocCity(Doc doc, String tagContents) {
        String[] words = tagContents.toUpperCase().trim().split(" ");
        if (words.length == 0) return;

        String city = words[0];
        String[] cityData = null;
        int i = 1;
        while (i < words.length && i < 5){
            cityData = cities_dictionary.get(city);
            if (cityData != null){
                cityIndex.put(city, cityData);
                doc.city = city;
                terms.add(city);
                return;
            }
            String next = words[i];
            if (next.length() > 0) city = city + " " + next;
            i++;
        }
        city = cleanString(words[0]);
        if (city.length() > 0 && Character.isAlphabetic(city.charAt(0))) {
            cityData = new String[3];
            cityData[0] = "";
            cityData[1] = "";
            cityData[2] = "";
            cityIndex.put(city, cityData);
            doc.city = city;
            terms.add(city);
        }
    }

    /**
     * Save the Models.Doc's title, adding the words from the title to the tokens list.
     * @param line of title
     * @param doc to save title to
     * @param gotCity to know if we need to skip the first term in terms (if its the city)
     */
    private void setDocTitle(Doc doc, String line, boolean gotCity) {
        tokenize(line);
        setTerms(doc);
        boolean skippedCity = false;
        for (String term : terms){
            if (gotCity && !skippedCity) skippedCity = true;
            else doc.title.add(term);
        }
    }

    /**
     * Cleans the string from all unnecessary symbols
     * @param string to clean
     * @return clean string
     */
    private String cleanString(String string) {
        while (string.length() > 0
                && !(Character.isDigit(string.charAt(0))
                || Character.isAlphabetic(string.charAt(0)))){
            string = string.substring(1);
        }
        while (string.length() > 0 && string.charAt(string.length()-1) != '%'
                && !(Character.isDigit(string.charAt(string.length()-1))
                || Character.isAlphabetic(string.charAt(string.length()-1)))){
            string = string.substring(0,string.length()-1);
        }
        int len = string.length();
        if (len > 1 && string.toLowerCase().charAt(len-1) == 's' && string.charAt(len-2) == '\'')
            string = string.substring(0,len-2);
        return string;
    }

    /**
     * Add tokens without symbols to token list
     * @param line to tokenize
     */
    private void tokenize(String line) {
        StringBuilder stringBuilder = new StringBuilder();
        line = line.trim().concat(" "); // Add one space at end to ensure last token is taken
        int length = line.length();
        for (int i = 0; i < length; i++) {
            char character = line.charAt(i);
            switch (character) {
                case '!': break;
                case '@': break;
                case ';': break;
                case '+': break;
                case '?': break;
                case '"': break;
                case '*': break;
                case '(': break;
                case ')': break;
                case '<': break;
                case '>': break;
                case '{': break;
                case '}': break;
                case '=': break;
                case '[': break;
                case ']': break;
                case '#': break;
                case '|': break;
                case '&': break;
                case ',': break;
                case '`': break;
                case ' ': {
                    if (stringBuilder.length() > 0) {
                        try {
                            char firstChar = stringBuilder.charAt(0);
                            while (stopPrefixes.contains(firstChar)) {
                                stringBuilder.deleteCharAt(0);
                                firstChar = stringBuilder.charAt(0);
                            }
                            char lastChar = stringBuilder.charAt(stringBuilder.length() - 1);
                            while (stopSuffixes.contains(lastChar)) {
                                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                                lastChar = stringBuilder.charAt(stringBuilder.length() - 1);
                            }
                            String token = stringBuilder.toString().toLowerCase();
                            if (token.length() > 0) {
                                // Do not stop at the following stop words!
                                if (token.equals("between")
                                        || token.equals("and")
                                        || token.equals("m")
                                        || token.equals("am")
                                        || !stop_words.contains(token)) {
                                    tokens.add(stringBuilder.toString());
                                }
                            }
                        } catch (NullPointerException | StringIndexOutOfBoundsException ignored) {
                        }
                        stringBuilder = new StringBuilder();
                    }
                }
                break;
                default:
                    stringBuilder.append(character);
            }
        }
    }

    /**
     * Get terms from tokens and set them in doc
     * @param doc to set terms to
     */
    private void setTerms(Doc doc) {
        try {
            while (true) {
                String term = getTerm(tokens.remove());
                term = cleanString(term); // need to clean again just in case
                if (term.length() > 0 && !stop_words.contains(term.toLowerCase())) {
                    //
//                    System.out.println(term);
                    //
                    terms.add(term);
                }
            }
        } catch (NoSuchElementException | IndexOutOfBoundsException ignored) {}
        doc.terms = terms;
    }

    /**
     * Receives a token and transforms it into a term. That is, checks whether it belongs to one
     * of the token types defined by the rules (number, date, dollar, etc.), and if it doesn't,
     * it stems the token (in case useStemming is true).
     * May have to look forward up to three tokens into the list of tokens.
     * @param token to transform into a term
     * @return the term (the token after transformation)
     */
    private String getTerm(String token) throws IndexOutOfBoundsException {
        String term = "";
        if (token.contains("-") || token.toLowerCase().contains("between")) {
            term = process_range_or_expression(token);
        } else {
            if (token.contains(":")) {
                term = process_hour(token);
            } else {
                if (token.contains("$")) {
                    term = process_dollars(token);
                } else {
                    if (months.containsKey(token.toLowerCase())) {
                        term = process_year_month(token);
                        if (term.length() == 0) {
                            term = process_day_month(token);
                        }
                    } else {
                        if (Character.isDigit(token.charAt(0))) {
                            term = process_percentage(token);
                            if (term.length() == 0) {
                                term = process_day_month(token);
                            }
                            if (term.length() == 0) {
                                term = process_dollars(token);
                            }
                            if (term.length() == 0) {
                                term = process_hour(token);
                            }
                            if (term.length() == 0) {
                                term = process_number(token, false);
                            }
                        }
                    }
                }
            }
        }
        if (term.length() > 0) return term;
        // In case term type is not of any case from above:
        boolean upper = false;
        if (Character.isUpperCase(token.charAt(0))) upper = true;
        if (use_stemming) {
            token = stemmer.stem(token.toLowerCase());
        }
        if (upper) token = token.toUpperCase();
        return token;
    }

    /**
     * check whether token is of type: NUMBER
     * and if it is, return the according term.
     * @param token to process into term
     * @param is_dollar if process_dollar called this function
     * @return term
     */
    private String process_number(String token, boolean is_dollar){
        String next_token = "";
        try {
            if (token.contains("/")) {
                String[] nums = token.split("/");
                Float.parseFloat(nums[0]);
                Float.parseFloat(nums[1]);
                return token;
            } else {
                float number = Float.parseFloat(token);
                String fraction = "";
                try {
                    next_token = tokens.remove().toLowerCase();
                    // Add fraction
                    if (next_token.contains("/")) {
                        String[] nums = next_token.split("/");
                        Float.parseFloat(nums[0]);
                        Float.parseFloat(nums[1]);
                        fraction = " " + next_token;
                    }
                    else if (!Character.isDigit(next_token.charAt(0))){
                        long factor = 1L;
                        switch (next_token) {
                            case "hundred":
                                factor = 100L; break;
                            case "thousand":
                                factor = 1000L; break;
                            case "million":
                                factor = 1000000L; break;
                            case "billion":
                                factor = 1000000000L; break;
                            case "trillion":
                                factor = 1000000000000L; break;
                            default:
                                tokens.addFirst(next_token);
                        }
                        number = number * factor;
                    } else tokens.addFirst(next_token);
                } catch (NoSuchElementException | IndexOutOfBoundsException ignored) {
                    if (next_token.length() != 0) tokens.addFirst(next_token);
                }
                // Convert to standard form
                String letter = "";
                if (!is_dollar) {
                    if (number < 1000L) {}
                    else if (number < 1000000L) {
                        number = number / 1000L;
                        letter = "K";
                    } else if (number < 1000000000L) {
                        number = number / 1000000L;
                        letter = "M";
                    } else {
                        number = number / 1000000000L;
                        letter = "B";
                    }
                } else if (number >= 1000000L) {
                    number = number / 1000000L;
                    letter = " M";
                }
                if (number == Math.round(number)) return (int)number + letter + fraction;
                return number + letter + fraction;
            }
        }catch (NumberFormatException | IndexOutOfBoundsException e) {
            return "";
        }
    }

    /**
     * check whether token is of type: PERCENTAGE
     * and if it is, return the according term.
     * @param token to process into term
     * @return term
     */
    private String process_percentage(String token) {
        try {
            if (token.endsWith("%")) {
                Float.parseFloat(token.substring(0, token.length() - 1));
                return token;
            }else {
                Float.parseFloat(token);
            }
            String next_token = tokens.getFirst().toLowerCase();
            if (next_token.equals("percent") || next_token.equals("percentage")) {
                tokens.removeFirst();
                return token + "%";
            }
            else return "";
        }catch (NumberFormatException | NoSuchElementException | IndexOutOfBoundsException e) {
            return "";
        }
    }

    /**
     * check whether token is of type: DOLLARS
     * and if it is, return the according term.
     * @param token to process into term
     * @return term
     */
    private String process_dollars(String token) {
        try {
            // in case of $price
            if (token.startsWith("$")) {
                Float.parseFloat(token.substring(1, token.length()));
                token = token.substring(1, token.length());
                return process_number(token, true) + " Dollars";
            }
            String next_token = tokens.getFirst().toLowerCase();
            // in case of "price dollars"
            if (next_token.equals("dollars")) {
                Float.parseFloat(token);
                String stringNumber = process_number(token, true);
                tokens.removeFirst(); // remove "Dollars"
                return stringNumber + " Dollars";
                // In case of "Price m/bn Dollars"
            } else if ((next_token.equals("m") || next_token.equals("bn"))
                    && tokens.get(1).toLowerCase().equals("dollars")) {
                float number = Float.parseFloat(token);
                long factor = 1000000L;
                if (next_token.equals("bn")) factor = 1000000000L;
                number *= factor;
                String stringNumber = process_number(Float.toString(number), true);
                tokens.removeFirst(); // remove "m" or "bn"
                tokens.removeFirst(); // remove "Dollars"
                return stringNumber + " Dollars";
            }
            else {
                String next_next_token = tokens.get(1).toLowerCase();
                // In case number has fraction and then dollar:
                if (next_next_token.equals("dollars")) {
                    String stringNumber = process_number(token, true);
                    if (stringNumber.length() != 0) {
                        tokens.removeFirst(); // remove "Dollars"
                        return stringNumber + " Dollars";
                    }
                }
                // In case number is followed by "U.S. Dollars" (the last '.' in "U.S." was previously removed):
                String next_next_next_token = tokens.get(2).toLowerCase();
                if (next_next_token.equals("u.s") && next_next_next_token.equals("dollars")) {
                    String stringNumber = process_number(token, true);
                    tokens.removeFirst();
                    tokens.removeFirst();
                    return stringNumber + " Dollars";
                }
                return "";
            }
        } catch(NumberFormatException | NoSuchElementException | IndexOutOfBoundsException e){
            return "";
        }
    }

    /**
     * check whether token is of type: DAY-MONTH
     * and if it is, return the according term.
     * @param token to process into term
     * @return term
     */
    private String process_day_month(String token) {
        try {
            token = token.toLowerCase();
            String next_token = tokens.getFirst().toLowerCase();
            String day;
            String month;
            // if month then day
            int value = Integer.parseInt(token);
            if (months.containsKey(token) && 1 <= value && value <= 31) {
                month = token;
                day = next_token;
            }
            // else if day then month
            else if (months.containsKey(next_token) && 1 <= value && value <= 31) {
                month = next_token;
                day = token;
            }
            else return "";
            terms.add(month.toUpperCase());
            terms.add(day);
            tokens.removeFirst();
            return months.get(month) + "-" + add_zero(day);
        }catch(NumberFormatException | NoSuchElementException | IndexOutOfBoundsException e) {
            return "";
        }
    }

    /**
     * Add a zero on positionInFile of token, if token is a number between 1 and 9
     * @param token to add a zero to
     * @return token after adding a zero
     */
    private String add_zero(String token) {
        try {
            int number = Integer.parseInt(token);
            if (number < 10) return "0" + token;
            return token;
        }catch (NumberFormatException e) {
            return token;
        }
    }

    /**
     * check whether token is of type: YEAR-MONTH
     * and if it is, return the according term.
     * @param token to process into term
     * @return term
     */
    private String process_year_month(String token) {
        try {
            String next_token = tokens.getFirst().toLowerCase();
            if (months.containsKey(token.toLowerCase())) {
                // next_token is year
                Integer.parseInt(next_token);
                terms.add(token.toUpperCase());
                terms.add(next_token);
                tokens.removeFirst();
                return next_token + "-" + months.get(token.toLowerCase());
            } else return "";
        } catch(NumberFormatException | NoSuchElementException | IndexOutOfBoundsException e) {
            return "";
        }
    }

    /**
     * check whether token is of type: RANGE or EXPRESSION
     * and if it is, return the according term.
     * @param token to process into term
     * @return term
     */
    private String process_range_or_expression(String token) {
        String next_token = "";
        String next_next_token = "";
        String next_next_next_token = "";
        try {
            if (token.contains("-")) {
                boolean is_expression = true;
                for (String word : token.split("-")) {
                    if (word.length() == 0) is_expression = false;
                    else tokens.addFirst(word);
                }
                if (is_expression) return token;
                return "";
            } else if (token.toLowerCase().equals("between")) {
                next_token = tokens.removeFirst();
                String firstNumber = process_number(next_token, false);
                if (firstNumber.length() == 0) throw new NumberFormatException();
                next_next_token = tokens.removeFirst();
                if (!next_next_token.equals("and")) throw new NumberFormatException();
                next_next_next_token = tokens.removeFirst();
                String secondNumber = process_number(next_next_next_token, false);
                if (secondNumber.length() == 0) throw new NumberFormatException();
                String[] expression = {"between", firstNumber, "and", secondNumber};
                return String.join(" ", expression);
            }
            return "";
        } catch (NumberFormatException | NoSuchElementException | IndexOutOfBoundsException e) {
            // Put next terms back into tokens queue, in case token is not an expression
            if (next_next_next_token.length() != 0) tokens.addFirst(next_next_next_token);
            if (next_next_token.length() != 0) tokens.addFirst(next_next_token);
            if (next_token.length() != 0) tokens.addFirst(next_token);
            return "";
        }
    }

    private String process_hour(String token) {
        try {
            token = token.toLowerCase();
            String next_token = tokens.getFirst().toLowerCase(), hour = "";
            boolean isAM = false, amInToken = false, pmInToken = false;
            if (token.length() > 2) {
                amInToken = token.substring(token.length() - 2, token.length()).equals("am");
                pmInToken = token.substring(token.length() - 2, token.length()).equals("pm");
            }
            if ((token.length() > 2 && amInToken) || next_token.equals("am")) {
                if (amInToken)
                    token = token.substring(0, token.length() - 2);
                isAM = true;
            }
            else if ((token.length() > 2 && pmInToken) || next_token.equals("pm")) {
                if (pmInToken)
                    token = token.substring(0, token.length() - 2);
            }
            else {
                return "";
            }
            int hours, minutes;
            if (token.length() > 3 && token.contains(":")) {
                try {
                    hours = Integer.parseInt(token.split(":")[0]);
                    minutes = Integer.parseInt(token.split(":")[1]);
                    if (hours <= 12 && minutes <= 60) {
                        if (!isAM)
                            hours = hours + 12;
                        String min = Integer.toString(minutes);
                        if(min.equals("0"))
                            min += '0';
                        String hourS = Integer.toString(hours);
                        if(hours <10)
                            hourS = '0' + hourS;
                        token = hourS + ':' + min;
                        return token;
                    }
                } catch (NumberFormatException | NoSuchElementException | IndexOutOfBoundsException e) {
                    return "";
                }
            }
            try {
                hours = Integer.parseInt(token);
                if (hours <= 12)
                    if (!isAM)
                        hours = hours + 12;
                token = Integer.toString(hours) + ":00";
                return token;
            } catch (NumberFormatException | NoSuchElementException | IndexOutOfBoundsException e) {
                return "";
            }
        } catch (NumberFormatException | NoSuchElementException | IndexOutOfBoundsException e) {
            return "";
        }
    }

    //          ----- queries ----

    /**
     * Parse a sentence and get a list of all the terms
     * @param sentence to parse
     * @return list of terms
     */
    public LinkedList<String> getParsedSentence(String sentence){
        tokens = new LinkedList<>();
        terms = new LinkedList<>();
        tokenize(sentence);
        Doc doc = new Doc(); // only for holding terms
        setTerms(doc);
        return doc.terms;
    }
}
