package uhh_praktikum_fea.tools;


import org.jobimtext.api.struct.WebThesaurusDatastructure;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrDocument;


import uhh_praktikum_fea.webserver.TextEvaluator;



public class RMessageStats {

    private static WebThesaurusDatastructure dt;

    public static void main(String[] args) throws IOException, SolrServerException {

        //dt = new WebThesaurusDatastructure("resources/conf_web_deNews_trigram.xml");
        //dt.connect();

        PrintWriter stats = new PrintWriter("src/main/resources/R_Message.txt");

        // Query
        SolrClient client = new HttpSolrClient.Builder("http://ltdemos:8983/solr/fea-schema-less").build();

        SolrQuery query = new SolrQuery();
        query.setQuery("*");
        query.set("fl", "id,R_Message");
        query.setStart(0);
        query.setRows(999);

        QueryResponse response = client.query(query);

        SolrDocumentList queryResults = response.getResults();

        int i = 0;

        for (SolrDocument queryResult : queryResults)
        {
            i++;
            String r_message = queryResult.get("R_Message").toString();
            String answer = TextEvaluator.getEvaluation(r_message, dt);
            stats.println(answer);
            System.out.println(i);
        }

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        stats.println(dateFormat.format(date));
        stats.close();
    }
}
