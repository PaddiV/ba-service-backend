package uhh_praktikum_fea.tools;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

import java.io.IOException;

/**
 * Tool for updating or deleting data in the Solr index.
 */
public class SolrUpdatr {
    public static void main(String[] args) throws IOException, SolrServerException {
    }

    /**
     * Deletes data matching the 'queryToDelete' parameter.
     *
     * @param queryToDelete query string (matching datasets will be deleted)
     */
    public static void delete(String queryToDelete) throws IOException, SolrServerException {
        SolrClient client = new HttpSolrClient.Builder("http://ltdemos:8983/solr/fea-schema-less").build();
        client.deleteByQuery(queryToDelete);
        client.commit();
    }

    /**
     * Updates data matching the 'queryToUpdate' parameter according to the 'updateQuery'.
     *
     * @param queryToUpdate query string (matching datasets will be updated)
     * @param updateQuery updates to be executed
     */
    public static void update(String queryToUpdate, String updateQuery) throws IOException, SolrServerException {
        // TODO
    }
}
