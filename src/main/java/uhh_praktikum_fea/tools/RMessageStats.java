package uhh_praktikum_fea.tools;


import org.jobimtext.api.struct.WebThesaurusDatastructure;

import java.io.IOException;
import java.io.PrintWriter;


import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrDocument;

import org.json.JSONException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


import uhh_praktikum_fea.webserver.ApplicationController;
import uhh_praktikum_fea.webserver.DTHelper;
import uhh_praktikum_fea.webserver.TextEvaluator;



public class RMessageStats {

    private static DTHelper dt;

    public static void main(String[] args) throws IOException, SolrServerException, ParseException {

        int start = 0;
        int rows = 3;

        dt = new DTHelper("conf_web_deNews_trigram.xml");

        PrintWriter stats = new PrintWriter("R_Message.json");
        // Query
        SolrClient client = new HttpSolrClient.Builder(ApplicationController.solr_core_uri).build();

        SolrQuery query = new SolrQuery();
        query.setQuery("R_Message:*");
        query.set("fl", "id,R_Message");
        query.setStart(start);
        query.setRows(rows);

        QueryResponse response = client.query(query);

        SolrDocumentList queryResults = response.getResults();

        int i = 0;

        org.json.JSONArray result = new org.json.JSONArray();

        JSONParser parser = new JSONParser();


        TextEvaluator evaluator = new TextEvaluator();
        for (SolrDocument queryResult : queryResults)
        {
            i++;
            String r_message = queryResult.get("R_Message").toString();
            String sub = r_message.substring(1, r_message.length()-1);
            String answer = evaluator.getEvaluation(sub, dt, true);
            JSONObject json = (JSONObject) parser.parse(answer);

            result.put(json);

            System.out.println(i);
        }
        stats.println(result);
        stats.close();
    }
}
