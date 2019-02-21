package uhh_lt.webserver;
//import net.sf.json.JSONArray;

import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.jobimtext.api.struct.Order2;
import org.jobimtext.api.struct.WebThesaurusDatastructure;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

@RestController
@EnableAutoConfiguration
public class ApplicationController {


    private static WebThesaurusDatastructure dt;


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

    @RequestMapping("/fea")
    String hallo(@RequestParam(value = "question", defaultValue = "") String question ,@RequestParam(value = "offset", defaultValue = "0") Integer offset) throws IOException, SolrServerException
    {

        SolrClient client = new HttpSolrClient.Builder("http://ltdemos:8983/solr/fea-schema-less").build();

        SolrQuery query = new SolrQuery();
        query.setQuery("T_Subject:"+ question);
        //query.addFilterQuery("T_Subject":question);
        query.setFields("id","T_Date","T_Subject","T_Message","R_Message");
        query.setStart(offset);
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
            }
            catch (JSONException error)
                {System.out.println(error);}

            result.put(obj);

        }
        return result.toString();
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