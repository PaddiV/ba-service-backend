package uhh_praktikum_fea.tools;


import org.jobimtext.api.struct.WebThesaurusDatastructure;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import java.io.FileReader;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrDocument;

import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


import uhh_praktikum_fea.webserver.TextEvaluator;



public class RMessageStats {

    private static WebThesaurusDatastructure dt;

    public static void main(String[] args) throws IOException, SolrServerException, ParseException, JSONException {

        //dt = new WebThesaurusDatastructure("resources/conf_web_deNews_trigram.xml");
        //dt.connect();

        int start = 0;
        int rows = 3;

        dt = new WebThesaurusDatastructure("src/main/resources/conf_web_deNews_trigram.xml");
        dt.connect();

        PrintWriter stats = new PrintWriter("R_Message.json");
        stats.println("{  data : [");
        // Query
        SolrClient client = new HttpSolrClient.Builder("http://ltdemos:8983/solr/fea-schema-less").build();

        SolrQuery query = new SolrQuery();
        query.setQuery("*");
        query.set("fl", "id,R_Message");
        query.setStart(start);
        query.setRows(rows);

        QueryResponse response = client.query(query);

        SolrDocumentList queryResults = response.getResults();

        int i = 0;

        org.json.JSONArray result = new org.json.JSONArray();

        JSONParser parser = new JSONParser();


        for (SolrDocument queryResult : queryResults)
        {
            org.json.JSONObject obj = new org.json.JSONObject();
            i++;
            String r_message = queryResult.get("R_Message").toString();
            String answer = TextEvaluator.getEvaluation(r_message, dt, true);
            JSONObject json = (JSONObject) parser.parse(answer);

            result.put(json);

            System.out.println(i);
        }

    }
}
