package uhh_praktikum_fea.webserver;

import org.json.JSONException;
import org.jobimtext.api.struct.WebThesaurusDatastructure;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrDocument;

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
    // Holds the question received by the last '/fea' request to work on when changing filters with 'custom_fea'.
    private String current_question = "";
    // Holds the concepts of the question received by the last '/fea' request to work on when changing filters with 'custom_fea'.
    private String current_concepts_query_string = "";

    /**
     * Accepts a question and returns similar questions and their answers found in Solr, as well as additional metadata.
     *
     * @param question question to search answers for
     * @param offset offset for Solr query (used for pagination)
     * @param upper_limit upper limit for Solr query (used for pagination)
     */
    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = RequestMethod.GET)
    @RequestMapping("/fea")
    String fea_home(@RequestParam(value = "question", defaultValue = "") String question, @RequestParam(value = "offset", defaultValue = "0") String offset, @RequestParam(value = "upper_limit", defaultValue = "0") String upper_limit) throws IOException, SolrServerException {
        question = question.replace(":", " ");
        current_question = question;
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
                .text(current_question)
                .features(features)
                .build();

        AnalysisResults watson_response = naturalLanguageUnderstanding
                .analyze(parameters)
                .execute();

        // Prepare keywords determined by Watson for Solr query.
        List<KeywordsResult> response_keywords = watson_response.getKeywords();
        String keywords_query_string = "";
        ArrayList <String> keywords_array = new ArrayList<>();
        if (response_keywords.size() != 0) {
            for (KeywordsResult result: response_keywords) {
                keywords_query_string += ("*" + result.getText() + "* ");
                keywords_array.add(result.getText());
            }
        } else {
            keywords_query_string = "*";
        }

        // Prepare concepts determined by Watson for Solr query.
        List<ConceptsResult> response_concepts = watson_response.getConcepts();
        ArrayList <String> concepts_array = new ArrayList<>();
        if (response_concepts.size() !=0){
            for (ConceptsResult concepts_result: response_concepts) {
                current_concepts_query_string += ("*" + concepts_result.getText()+ "* ");
                concepts_array.add(concepts_result.getText());
            }
        } else {
            current_concepts_query_string = "*";
        }

        SolrClient client = new HttpSolrClient.Builder("http://ltdemos:8983/solr/fea-schema-less").build();
        SolrQuery query = new SolrQuery();
        query.setQuery("T_Message:"+ current_question + " OR Keywords:(" + keywords_query_string + ")" + " OR Concepts:(" + current_concepts_query_string + ")");// + " OR Tags:( + TAGS + ")");
        query.set("fl", "id, T_Date, T_Subject, T_Message, R_Message, score");
        query.addSort("score", SolrQuery.ORDER.desc);
        query.setStart(actual_offset);
        query.setRows(actual_amount);
        org.json.JSONArray result = new org.json.JSONArray();
        QueryResponse response = client.query(query);
        SolrDocumentList queryResults = response.getResults();
        for (SolrDocument queryResult: queryResults) {
            org.json.JSONObject obj = new org.json.JSONObject();
            try {
                obj.put("id", queryResult.get("id"));
                obj.put("T_Date", queryResult.get("T_Date"));
                obj.put("T_Subject", queryResult.get("T_Subject"));
                obj.put("T_Message", queryResult.get("T_Message"));
                obj.put("R_Message", queryResult.get("R_Message"));
                obj.put("score", queryResult.get("score"));
            } catch (JSONException error) {
                System.out.println(error);
            }
            result.put(obj);
        }
        org.json.JSONObject totalresult = new org.json.JSONObject();
        try {
            totalresult.put("results_count", queryResults.getNumFound());
            totalresult.put("data", result);
            totalresult.put("Keywords", keywords_array);
            totalresult.put("Concepts", concepts_array);
            //totalresult.put("Tags", tags);
        } catch (JSONException error) {
            System.out.println(error);
        }
        return totalresult.toString();
    }

    /**
     * Used for filtering the question query by given tags.
     *
     * @param offset offset for Solr query (used for pagination)
     * @param upper_limit upper limit for Solr query (used for pagination)
     * @param keywords used for limiting Solr query (filtering)
     */
    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = RequestMethod.GET)
    @RequestMapping("/costum_fea")
    String fea_costum( @RequestParam(value = "offset", defaultValue = "0") String offset, @RequestParam(value = "upper_limit", defaultValue = "0") String upper_limit, @RequestParam(value = "keywords", defaultValue = "*") String keywords, @RequestParam(value = "tags", defaultValue = "*") String tags) throws IOException, SolrServerException {
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
        query.setQuery("T_Message:"+ current_question + " OR Keywords:(" + keywords + ")" + " OR Concepts:(" + current_concepts_query_string + ")");// + " OR Tags:( + TAGS + ")");
        query.set("fl", "id, T_Date, T_Subject, T_Message, R_Message, score");
        query.addSort("score", SolrQuery.ORDER.desc);
        query.setStart(actual_offset);
        query.setRows(actual_amount);
        org.json.JSONArray result = new org.json.JSONArray();
        QueryResponse response = client.query(query);
        SolrDocumentList queryResults = response.getResults();
        for (SolrDocument queryResult: queryResults) {
            org.json.JSONObject obj = new org.json.JSONObject();
            try {
                obj.put("id", queryResult.get("id"));
                obj.put("T_Date", queryResult.get("T_Date"));
                obj.put("T_Subject", queryResult.get("T_Subject"));
                obj.put("T_Message", queryResult.get("T_Message"));
                obj.put("R_Message", queryResult.get("R_Message"));
                obj.put("score", queryResult.get("score"));
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

    /**
     * Checks the given text for complexity and returns a JSONObject with remarks and/or warnings.
     *
     * @param text text to be checked
     */
    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = RequestMethod.GET)
    @RequestMapping("/text_check")
    String text_check(@RequestParam(value = "text", defaultValue = "") String text) {
        return TextEvaluator.getEvaluation(text, dt);
    }

    /**
     * Returns a set of data for charts to be displayed in the application.
     */
    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = RequestMethod.GET)
    @RequestMapping("/chart_data")
    String chart_data() {
        return "Hello there! You found a construction site. Congrats!";
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
