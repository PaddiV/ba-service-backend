package uhh_praktikum_fea.webserver;

import org.json.JSONException;
import org.jobimtext.api.struct.WebThesaurusDatastructure;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.web.SpringBootServletInitializer;
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
@SpringBootApplication
@EnableAutoConfiguration
public class ApplicationController extends SpringBootServletInitializer {

    //TODO: REMOVE CREDENTIALS BEFORE COMMITTING
    private static String user = "asdf";
    private static String password = "asdf";
    private static WebThesaurusDatastructure dt;
    // Determines how many keywords Watson should return for given question.
    private static int amount_watson_keywords = 10;
    private static int amount_watson_concepts = 10;
    //TODO: REMOVE API KEY BEFORE COMMITTING
    private static String api_key = "";
    // Holds the concepts of the question received by the last '/fea' request to work on when changing filters with 'custom_fea'.
    private String current_concepts_query_string = "";
    // Is the cutoff depending of the score
    private float score_cutoff = 0;

    /**
     * Accepts a question and returns similar questions and their answers found in Solr, as well as additional metadata.
     *
     * @param question question to search answers for
     * @param offset offset for Solr query (used for pagination)
     * @param upper_limit upper limit for Solr query (used for pagination)
     */

    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = RequestMethod.GET)
    @RequestMapping("/fea")
    String fea_home(@RequestParam(value = "question", defaultValue = "") String question, @RequestParam(value = "offset", defaultValue = "0") String offset, @RequestParam(value = "upper_limit", defaultValue = "0") String upper_limit, @RequestParam(value = "user", defaultValue = "") String logged_in_user, @RequestParam(value = "password", defaultValue = "") String password) throws IOException, SolrServerException {
        if (logged_in_user != user || password != password) {
            return "Incorrect login!";
        }
        //sets the current_question
        question = question.replace(":", " ");
        // erases the saved concepts
        current_concepts_query_string = "";
        // calculate the cutoff depending of the question-length
        score_cutoff = (float)(question.length() * 0.05);

        int actual_offset;
        int actual_amount;
        int upper_boundary;

        //catching false values
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

        // Creating a query to search the amount of answers with a score over the cutoff
        SolrQuery query_score = new SolrQuery();
        query_score.setQuery("T_Message:"+ question + " OR Keywords:(" + keywords_query_string + ")" + " OR Concepts:(" + current_concepts_query_string + ")");// + " OR Tags:(" + TAGS + ")");
        query_score.set("fl", "id, score");
        query_score.addSort("score", SolrQuery.ORDER.desc);
        query_score.setStart(0);
        query_score.setRows(5000);
        QueryResponse score_response = client.query(query_score);
        SolrDocumentList query_scoreResults = score_response.getResults();
        int counter = 0;
        for (SolrDocument query_scoreResult : query_scoreResults){
            Object score = query_scoreResult.get("score");
            if ((float) score < score_cutoff){ break;}
            counter++;
        }

        // Creating the query
        SolrQuery query = new SolrQuery();
        query.setQuery("T_Message:"+ question + " OR Keywords:(" + keywords_query_string + ")" + " OR Concepts:(" + current_concepts_query_string + ")");// + " OR Tags:(" + TAGS + ")");
        query.set("fl", "id, T_Date, T_Subject, T_Message, R_Message, score");
        query.addSort("score", SolrQuery.ORDER.desc);
        query.setStart(actual_offset);
        query.setRows(actual_amount);
        org.json.JSONArray result = new org.json.JSONArray();
        QueryResponse response = client.query(query);
        SolrDocumentList queryResults = response.getResults();

        for (SolrDocument queryResult: queryResults) {
            //adding the results with score over the cutoff to the total-result
            org.json.JSONObject obj = new org.json.JSONObject();
            if ((float)queryResult.get("score") < score_cutoff){break;}
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
        // adding count, keywords , concepts to the result
        org.json.JSONObject totalresult = new org.json.JSONObject();
        try {
            totalresult.put("results_count", counter);
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
     * @param tags used for limiting Solr query (filtering)
     */
    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = RequestMethod.GET)
    @RequestMapping("/custom_fea")
    String fea_custom(@RequestParam(value = "question", defaultValue = "") String question, @RequestParam(value = "offset", defaultValue = "0") String offset, @RequestParam(value = "upper_limit", defaultValue = "0") String upper_limit, @RequestParam(value = "keywords", defaultValue = "*") String keywords, @RequestParam(value = "tags", defaultValue = "*") String tags, @RequestParam(value = "user", defaultValue = "") String user, @RequestParam(value = "password", defaultValue = "") String password) throws IOException, SolrServerException {
        if (user != ApplicationController.user || password != ApplicationController.password) {
            return "Incorrect login!";
        }
        int actual_offset;
        int actual_amount;
        int upper_boundary;

        //catching false values
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
        // Creating a query to search the amount of answers with a score over the cutoff
        SolrQuery query_score = new SolrQuery();
        query_score.setQuery("T_Message:"+ question + " OR Keywords:(" + keywords + ")" + " OR Concepts:(*" + current_concepts_query_string + ")");// + " OR Tags:(" + TAGS + ")");
        query_score.set("fl", "id, score");
        query_score.addSort("score", SolrQuery.ORDER.desc);
        query_score.setStart(0);
        query_score.setRows(5000);
        QueryResponse score_response = client.query(query_score);
        SolrDocumentList query_scoreResults = score_response.getResults();
        int counter = 0;
        for (SolrDocument query_scoreResult : query_scoreResults) {
            Object score = query_scoreResult.get("score");
            if ((float) score < score_cutoff) {
                break;
            }
            counter++;
        }

        // Creating the query
        SolrQuery query = new SolrQuery();
        query.setQuery("T_Message:"+ question + " OR Keywords:(" + keywords + ")" + " OR Concepts:(*" + current_concepts_query_string + ")");// + " OR Tags:(" + TAGS + ")");
        query.set("fl", "id, T_Date, T_Subject, T_Message, R_Message, score");
        query.addSort("score", SolrQuery.ORDER.desc);
        query.setStart(actual_offset);
        query.setRows(actual_amount);
        org.json.JSONArray result = new org.json.JSONArray();
        QueryResponse response = client.query(query);
        SolrDocumentList queryResults = response.getResults();
        for (SolrDocument queryResult: queryResults) {
            //adding the results with score over the cutoff to the total-result
            org.json.JSONObject obj = new org.json.JSONObject();
            if ((float)queryResult.get("score") < score_cutoff){break;}
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
        // adding count to the result
        try {
            totalresult.put("results_count", counter);
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
    String text_check(@RequestParam(value = "text", defaultValue = "") String text, @RequestParam(value = "user", defaultValue = "") String user, @RequestParam(value = "password", defaultValue = "") String password) throws IOException, SolrServerException {
        if (user != user || password != password) {
            return "Incorrect login!";
        }
        return TextEvaluator.getEvaluation(text, dt, false);
    }

    /**
     * Returns a set of data for charts to be displayed in the application.
     */
    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = RequestMethod.GET)
    @RequestMapping("/chart_data")
    String chart_data(@RequestParam(value = "user", defaultValue = "") String user, @RequestParam(value = "password", defaultValue = "") String password) {
        if (user != ApplicationController.user || password != ApplicationController.password) {
            return "Incorrect login!";
        }
        return "Hello there! You found a construction site. Congrats!";
    }

    /**
     * Returns true when user and passphrase are correct, false otherwise.
     */
    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = RequestMethod.GET)
    @RequestMapping("/login")
    Boolean login(@RequestParam(value = "user", defaultValue = "") String user, @RequestParam(value = "password", defaultValue = "") String password) {
        System.out.println(user);
        System.out.println(password);
        if (user.equals(ApplicationController.user) && password.equals(ApplicationController.password)) {
            return true;
        }
        return false;
    }

    /**
     * Runs the RESTful server.
     *
     * @param args execution arguments
     */
    public static void main(String[] args) {
        dt = new WebThesaurusDatastructure("src/main/resources/conf_web_deNews_trigram.xml");
        dt.connect();
        SpringApplication.run(ApplicationController.class, args);
    }

}
