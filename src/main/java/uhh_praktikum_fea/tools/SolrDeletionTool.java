package uhh_praktikum_fea.tools;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

import java.io.IOException;

/**
 * Tool for deleting data matching the 'queryToDelete' parameter in the Solr index.
 */
public class SolrDeletionTool {
    private static String queryToDelete = "";

    public static void main(String[] args) throws IOException, SolrServerException {
        SolrClient client = new HttpSolrClient.Builder("http://ltdemos:8983/solr/fea-schema-less").build();
        client.deleteByQuery(queryToDelete);
        client.commit();
    }
}
