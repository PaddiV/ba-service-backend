package uhh_praktikum_fea.tools;

import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.util.NamedList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.json.simple.parser.ParseException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import uhh_praktikum_fea.webserver.ApplicationController;


public class ScoreCalculator {
    public static void main(String[] args) throws IOException, SolrServerException, ParseException {

        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader("/informatik2/students/home/7ferrara/Desktop/mietrechtexport1000.json"));
        JSONObject jsonObject = (JSONObject) obj;
        JSONArray data = (JSONArray) jsonObject.get("data");
        SolrClient client = new HttpSolrClient.Builder(ApplicationController.solr_core_uri).build();
        SolrQuery query = new SolrQuery();
        query.setFields("score");
        query.addSort("score", SolrQuery.ORDER.desc);
        query.setStart(0);
        query.setRows(1);

        float [][] score_lenght = new float [999][2];
        float [][] rechner = new float [999][2];
        int counter = 0;
        for (Object o : data) {
            JSONObject jsonObject1 = (JSONObject) o;
            String tmessage = (String) jsonObject1.get("T_Message");
            float length = tmessage.length();

            // Eugen did this.
            query.setQuery(tmessage.substring(0, Math.min(6000, tmessage.length()-1)).replaceAll(":", " ").replaceAll("\\(", " ").replaceAll("\\)", " ")
                    .replaceAll("\\\\", " ").replaceAll("\"", " ").replaceAll("'", "").replaceAll("\\.", "")
                    .replaceAll("/", " ").replaceAll("\\[", " ").replaceAll("\\]", " ")
                    .replaceAll("-", " ").replaceAll("\\?", " ").replaceAll("\\!", "").replaceAll("\n", " ")
                    .replaceAll(",", " ").replaceAll("\\*", "x").replaceAll("\\+", " ").replaceAll("\\%", " ")
                    .replaceAll("=", " "));

            QueryResponse response = null;

            try {
                QueryRequest queryRequest = new QueryRequest(query);
                queryRequest.setMethod(SolrRequest.METHOD.POST);
                NamedList named_list = client.request(queryRequest);
                response = new QueryResponse(named_list, client);
            } catch (Exception e) {
                e.printStackTrace();
            }

            SolrDocumentList results = response.getResults();
            for (int i = 0; i < results.size(); ++i) {
                float score = (float) results.get(i).get("score");
                score_lenght[counter][0] = length;
                score_lenght[counter][1] = score;
                rechner[counter][0] = length;
                rechner[counter][1] = (score/length);
            }
            counter ++;
        }
        try {
            PrintWriter out = new PrintWriter("score.txt");
            out.println(Arrays.deepToString(score_lenght));

            PrintWriter aus = new PrintWriter("score_ratio.txt");
            aus.println(Arrays.deepToString(rechner));
            // Print current date and time.
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();

            out.println(dateFormat.format(date));
            out.close();

            aus.println(dateFormat.format(date));
            aus.close();
        } catch (FileNotFoundException e) {
            throw e;
        }
    }
}