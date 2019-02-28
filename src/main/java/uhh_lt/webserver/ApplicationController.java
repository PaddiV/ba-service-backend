package uhh_lt.webserver;
//import net.sf.json.JSONArray;

import org.apache.pig.SortColInfo;
import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.jobimtext.api.struct.Order2;
import org.jobimtext.api.struct.WebThesaurusDatastructure;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.*;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.commons.lang.StringUtils;

import com.ibm.watson.developer_cloud.natural_language_understanding.v1.NaturalLanguageUnderstanding;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.*;
import com.ibm.watson.developer_cloud.service.security.IamOptions;

@RestController
@EnableAutoConfiguration
public class ApplicationController {

    private static WebThesaurusDatastructure dt;
    // Determines how many keywords Watson should return for given question.
    private static int amount_watson_keywords = 10;
    private static int amount_watson_concepts = 10;
    //TODO: REMOVE API KEY BEFORE COMMITTING
    private static String api_key = "";
    // Variables for answer complexity check.
    private static int min_text_length_to_match = 200;
    private static int max_text_length_to_match = 500;
    private static int avg_sentence_length_to_match = 15;
    private static int avg_sentence_length_allowed_variance = 5;
    private static int min_noun_usage_to_match = 40;
    private static int min_avg_noun_usage_to_match = 1000;

    @RequestMapping("/expansions")
    String home(@RequestParam(value = "word", defaultValue = "") String word, @RequestParam(value = "format", defaultValue = "text") String format) {

        word = word.replace("\r", " ").replace("\n", " ").trim();
        format = format.replace("\r", " ").replace("\n", " ").trim();

        if (format.compareTo("json") == 0) {
            return generateJSONResponse(word);
        } else {
            return generateTextResponse(word);
        }
    }

    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = RequestMethod.GET)
    @RequestMapping("/fea")
    String fea_home(@RequestParam(value = "question", defaultValue = "") String question, @RequestParam(value = "offset", defaultValue = "0") String offset, @RequestParam(value = "upper_limit", defaultValue = "0") String upper_limit) throws IOException, SolrServerException {
        int actual_offset;
        int actual_amount;
        int upper_boundary;

        try {
            actual_offset = Integer.parseInt(offset);
        } catch(NumberFormatException e) {
            actual_offset = 0;
            upper_limit = "0";
        }

        try {
            upper_boundary = Integer.parseInt(upper_limit);
        } catch(NumberFormatException e) {
            upper_boundary = 0;
        }

        if ((upper_boundary- actual_offset) < 0) {
            actual_amount = 0;
        } else {
            actual_amount = (upper_boundary - actual_offset + 1);
        }

        // Get necessary data determined by Watson.
        IamOptions options = new IamOptions.Builder()
                .apiKey(api_key)
                .build();

        NaturalLanguageUnderstanding naturalLanguageUnderstanding = new NaturalLanguageUnderstanding("2018-11-16", options);
        naturalLanguageUnderstanding.setEndPoint("https://gateway-lon.watsonplatform.net/natural-language-understanding/api/");

        KeywordsOptions keywords= new KeywordsOptions.Builder()
                .limit(amount_watson_keywords)
                .build();

        ConceptsOptions concepts= new ConceptsOptions.Builder()
                .limit(amount_watson_concepts)
                .build();

        Features features = new Features.Builder()
                .keywords(keywords)
                .concepts(concepts)
                .build();


        AnalyzeOptions parameters = new AnalyzeOptions.Builder()
                .text(question)
                .features(features)
                .build();

        AnalysisResults watson_response = naturalLanguageUnderstanding
                .analyze(parameters)
                .execute();

        // Prepare keywords determined by Watson for Solr query.
        List<KeywordsResult> response_keywords = watson_response.getKeywords();
        String keywords_query_string = "";
        String keywords_string = "";
        if (response_keywords.size() != 0) {
            for (KeywordsResult result: response_keywords) {
                keywords_query_string += ("*" + result.getText() + "* ");
                keywords_string += (result.getText()+" ");
            };
        } else {
            keywords_query_string = "*";
        }

        // Prepare concepts determined by Watson for Solr query.
        List<ConceptsResult> response_concepts = watson_response.getConcepts();
        String concepts_query_string = "";
        String concepts_string = "";
        if (response_concepts.size() !=0){
            for (ConceptsResult concepts_result: response_concepts) {
                concepts_query_string += ("*" + concepts_result.getText()+ "* ");
                concepts_string += (concepts_result.getText()+" ");
            }
        } else {
            concepts_query_string = "*";
        }

        SolrClient client = new HttpSolrClient.Builder("http://ltdemos:8983/solr/fea-schema-less").build();
        SolrQuery query = new SolrQuery();
        query.setQuery("T_Message:"+ question + " OR Keywords:(" + keywords_query_string + ")" + " OR Concepts:(" + concepts_query_string + ")");// + " OR Tags:( + TAGS + ")");
        query.set("fl", "id, T_Date, T_Subject, T_Message, R_Message, score");
        query.addSort("score", SolrQuery.ORDER.desc);
        query.setStart(actual_offset);
        query.setRows(actual_amount);
        org.json.JSONArray result = new org.json.JSONArray();
        QueryResponse response = client.query(query);
        SolrDocumentList queryResults = response.getResults();
        for (int i = 0; i < queryResults.size(); ++i) {
            org.json.JSONObject obj = new org.json.JSONObject();
            try {
                obj.put("id", queryResults.get(i).get("id"));
                obj.put("T_Date", queryResults.get(i).get("T_Date"));
                obj.put("T_Subject", queryResults.get(i).get("T_Subject"));
                obj.put("T_Message", queryResults.get(i).get("T_Message"));
                obj.put("R_Message", queryResults.get(i).get("R_Message"));
                obj.put("score", queryResults.get(i).get("score"));
            } catch (JSONException error) {
                System.out.println(error);
            }
            result.put(obj);
        }
        org.json.JSONObject totalresult = new org.json.JSONObject();
        try {
            totalresult.put("results_count", queryResults.getNumFound());
            totalresult.put("data", result);
            totalresult.put("Keywords", keywords_string);
            totalresult.put("Concepts", concepts_string);
            //totalresult.put("Tags", tags);
        } catch (JSONException error) {
            System.out.println(error);
        }
        return totalresult.toString();
    }

    /**
     * Checks the given text for complexity and returns a JSONObject with remarks and/or warnings.
     *
     * @param text text to be checked
     */
    @RequestMapping("/text_check")
    String text_check(@RequestParam(value = "text", defaultValue = "") String text) {

        // Split text into single words, remove first word, words after dots and lowercase words. Thus only (most) nouns should remain.
        String[] split_text = text.split(" ");
        List<String> nouns = new ArrayList<String>();
        Pattern pattern = Pattern.compile("[^a-zA-ZßäÄöÖüÜ0-9]$");
        for (int i = 1; i <= (split_text.length - 1); i++) {
            // Add uppercase words.
            if (Character.isUpperCase(split_text[i].charAt(0))) {
                // Remove everything but letters of the German alphabet, numbers and hyphens.
                String noun_to_add = split_text[i].replaceAll("[^a-zA-ZßäÄöÖüÜ0-9/-]", "");
                // Remove last char manually in case of it being a hyphen.
                Matcher matcher = pattern.matcher(noun_to_add);
                if (matcher.find()) {
                    noun_to_add = noun_to_add.substring(0, noun_to_add.length() - 1);
                }
                nouns.add(noun_to_add);
            }
            // Ignore words after a dot, question mark or exclamation mark.
            char last_char_of_word = split_text[i].charAt(split_text[i].length() - 1);
            if (last_char_of_word == '.' || last_char_of_word == '?' || last_char_of_word == '!') {
                i++;
            }
        }

        // Check usage of each noun and keep track of especially rarely used nouns for warnings.
        List<Long> nouns_usages = new ArrayList<Long>();
        List<String> problematic_nouns = new ArrayList<String>();
        for (String noun: nouns) {
            long noun_usage = dt.getTermCount(noun);
            nouns_usages.add(noun_usage);
            if (noun_usage < min_noun_usage_to_match) {
                problematic_nouns.add(noun);
            }
        }

        // Check if text is too short or too long.
        Rating text_length_rating;
        if (text.length() > min_text_length_to_match) {
            if (text.length() < max_text_length_to_match) {
                text_length_rating = Rating.OK;
            } else {
                text_length_rating = Rating.LONG;
            }
        } else {
            text_length_rating = Rating.SHORT;
        }

        // Check if the average word count per sentence is too short or too long.
        String[] split_sentences = text.split("[\\.\\?\\!]");
        Rating avg_sentence_length_rating;
        if (split_sentences.length != 0) {
            double avg_sentence_word_count = 0;
            for (String sentence : split_sentences) {
                avg_sentence_word_count = avg_sentence_word_count + StringUtils.countMatches(sentence.trim(), " ") + 1;
            }
            avg_sentence_word_count = avg_sentence_word_count / split_sentences.length;

            if (avg_sentence_word_count < (avg_sentence_length_to_match - avg_sentence_length_allowed_variance)) {
                avg_sentence_length_rating = Rating.SHORT;
            } else {
                if (avg_sentence_word_count > (avg_sentence_length_to_match + avg_sentence_length_allowed_variance)) {
                    avg_sentence_length_rating = Rating.LONG;
                } else {
                    avg_sentence_length_rating = Rating.OK;
                }
            }
        } else {
            avg_sentence_length_rating = Rating.NONE;
        }

        // Check if the overall usage of nouns is too low.
        Rating nouns_used_rating;
        if (nouns_usages.size() != 0) {
            Long avg_nouns_usage = 0l;
            for (Long usage: nouns_usages) {
                avg_nouns_usage = avg_nouns_usage + usage;
            }
            avg_nouns_usage = avg_nouns_usage / nouns_usages.size();

            if (avg_nouns_usage < min_avg_noun_usage_to_match) {
                nouns_used_rating = Rating.BAD;
            } else {
                nouns_used_rating = Rating.GOOD;
            }
        } else {
            nouns_used_rating = Rating.NONE;
        }

        JSONObject response = new JSONObject();
        response.put("text_length", text_length_rating.toString());
        response.put("avg_sentence_length", avg_sentence_length_rating.toString());
        response.put("nouns_used", nouns_used_rating.toString());
        response.put("problematic_nouns", problematic_nouns);

        return response.toString();
    }




    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = RequestMethod.GET)
    @RequestMapping("/costum_fea")
    String fea_costum(@RequestParam(value = "question", defaultValue = "") String question, @RequestParam(value = "offset", defaultValue = "0") String offset, @RequestParam(value = "upper_limit", defaultValue = "0") String upper_limit, @RequestParam(value = "keywords", defaultValue = "*") String keywords, @RequestParam(value = "tags", defaultValue = "*") String tags, @RequestParam(value = "concepts" , defaultValue = "*") String concepts ) throws IOException, SolrServerException {
        int actual_offset;
        int actual_amount;
        int upper_boundary;

        try {
            actual_offset = Integer.parseInt(offset);
        } catch(NumberFormatException e) {
            actual_offset = 0;
            upper_limit = "0";
        }

        try {
            upper_boundary = Integer.parseInt(upper_limit);
        } catch(NumberFormatException e) {
            upper_boundary = 0;
        }

        if ((upper_boundary- actual_offset) < 0) {
            actual_amount = 0;
        } else {
            actual_amount = (upper_boundary - actual_offset + 1);
        }
        SolrClient client = new HttpSolrClient.Builder("http://ltdemos:8983/solr/fea-schema-less").build();
        SolrQuery query = new SolrQuery();
        query.setQuery("T_Message:"+ question + " OR Keywords:(" + keywords + ")" + " OR Concepts:(" + concepts + ")");// + " OR Tags:( + TAGS + ")");
        query.set("fl", "id, T_Date, T_Subject, T_Message, R_Message, score");
        query.addSort("score", SolrQuery.ORDER.desc);
        query.setStart(actual_offset);
        query.setRows(actual_amount);
        org.json.JSONArray result = new org.json.JSONArray();
        QueryResponse response = client.query(query);
        SolrDocumentList queryResults = response.getResults();
        for (int i = 0; i < queryResults.size(); ++i) {
            org.json.JSONObject obj = new org.json.JSONObject();
            try {
                obj.put("id", queryResults.get(i).get("id"));
                obj.put("T_Date", queryResults.get(i).get("T_Date"));
                obj.put("T_Subject", queryResults.get(i).get("T_Subject"));
                obj.put("T_Message", queryResults.get(i).get("T_Message"));
                obj.put("R_Message", queryResults.get(i).get("R_Message"));
                obj.put("score", queryResults.get(i).get("score"));
            } catch (JSONException error) {
                System.out.println(error);
            }
            result.put(obj);
        }
        org.json.JSONObject totalresult = new org.json.JSONObject();
        try {
            totalresult.put("results_count", queryResults.getNumFound());
            totalresult.put("data", result);
        } catch (JSONException error) {
            System.out.println(error);
        }
        return totalresult.toString();
    }

        private String generateJSONResponse(String input) {
        JSONObject out = new JSONObject();
        out.put("input", input);
        JSONArray expansions = new JSONArray();
        for (Order2 exp : dt.getSimilarTerms(input)) {
            expansions.add(exp.key);
        }
        out.put("expansions", expansions);
        return out.toString();
    }

    private String generateTextResponse(String input) {
        StringBuilder output = new StringBuilder();
        output.append("input: " + input);
        output.append("\nexpansions:");
        for (Order2 exp : dt.getSimilarTerms(input)) {
            output.append("\n  - " + exp.key);
        }
        return output.toString();
    }

    /**
     * Runs the RESTful server.
     *
     * @param args execution arguments
     */
    public static void main(String[] args) {
        dt = new WebThesaurusDatastructure("resources/conf_web_deNews_trigram.xml");
        dt.connect();
        SpringApplication.run(ApplicationController.class, args);
    }

}