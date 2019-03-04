package uhh_lt.webserver;

import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.util.NamedList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
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



public class Score_rechner {
    public static void main(String[] args) throws IOException, SolrServerException, ParseException {

        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader("/informatik2/students/home/7ferrara/Desktop/mietrechtexport1000.json"));
        JSONObject jsonObject = (JSONObject) obj;
        JSONArray data = (JSONArray) jsonObject.get("data");
        SolrClient client = new HttpSolrClient.Builder("http://ltdemos:8983/solr/fea-schema-less").build();

        SolrQuery query = new SolrQuery();

        query.setFields("score");
        query.addSort("score", SolrQuery.ORDER.desc);
        query.setStart(0);
        query.setRows(1);


        float [][] score_lenght = new float [999][2];
        float [][] rechner = new float [999][2];

        int zaehler = 0;
        for (Object o : data) {

            JSONObject jsonObject1 = (JSONObject) o;

            String tmessage = (String) jsonObject1.get("T_Message");
            System.out.println(tmessage);
            float laenge = tmessage.length();
            System.out.println(laenge);

            query.setQuery(tmessage.substring(0, Math.min(6000, tmessage.length()-1)).replaceAll(":", " ").replaceAll("\\(", " ").replaceAll("\\)", " ")
                    .replaceAll("\\\\", " ").replaceAll("\"", " ").replaceAll("'", "").replaceAll("\\.", "")
                    .replaceAll("/", " ").replaceAll("\\[", " ").replaceAll("\\]", " ")
                    .replaceAll("-", " ").replaceAll("\\?", " ").replaceAll("\\!", "").replaceAll("\n", " ")
                    .replaceAll(",", " ").replaceAll("\\*", "x").replaceAll("\\+", " ").replaceAll("\\%", " ")
                    .replaceAll("=", " "));


            QueryResponse response=null;
            try {
                QueryRequest queryRequest=new QueryRequest(query);
                queryRequest.setMethod(SolrRequest.METHOD.POST);
                NamedList nl=client.request(queryRequest);
                response=new QueryResponse(nl, client);
            } catch (Exception e) {
                e.printStackTrace();
            }



            SolrDocumentList results = response.getResults();
            for (int i = 0; i < results.size(); ++i) {
                float score = (float) results.get(i).get("score");
                score_lenght[zaehler][0] = laenge;
                score_lenght[zaehler][1] = score;
                rechner[zaehler][0] = laenge;
                rechner[zaehler][1] = (score/laenge);

            }
            zaehler ++;
        }
        try {
            PrintWriter out = new PrintWriter("score.txt");
            out.println(Arrays.deepToString(score_lenght));
            PrintWriter aus = new PrintWriter("verhältnis_score.txt");
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