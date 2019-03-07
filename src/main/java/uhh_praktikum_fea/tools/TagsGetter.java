package uhh_praktikum_fea.tools;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class TagsGetter {
    public static void main(String[] args) throws IOException, SolrServerException {
        SolrClient client = new HttpSolrClient.Builder("http://ltdemos:8983/solr/fea-schema-less").build();
        SolrQuery query = new SolrQuery();
        query.setQuery("Tags:*");
        query.setFields("Tags");
        query.setStart(0);
        query.setRows(10000);
        QueryResponse response = client.query(query);
        SolrDocumentList results = response.getResults();
        Map<String, Integer> unique_tags = new HashMap<String, Integer>();
        boolean tag_already_stored = false;

        // Go through all results returned by the query and extract the individual tags.
        for (int i = 0; i < results.size(); ++i) {
            String tag_string = results.get(i).get("Tags").toString();
            // Remove square brackets at beginning and end of query results.
            tag_string = tag_string.replaceAll("[\\[\\]]", "").trim();
            String[] tags = tag_string.split(" ");

            // Check each tag against all tags already found.
            for (String tag: tags) {
                for (Map.Entry<String, Integer> tag_item: unique_tags.entrySet()) {
                    // If tag was already found before: increase the count for that tag by 1.
                    if (tag_item.getKey().equals(tag)) {
                        unique_tags.put(tag_item.getKey(), tag_item.getValue() + 1);
                        tag_already_stored = true;
                        break;
                    }
                }
                // If tag was not found before: add it and initialize the count with 1.
                if (!tag_already_stored) {
                    unique_tags.put(tag, 1);
                }
                tag_already_stored = false;
            }
        }
        // Sort by number of occurrences.
        Map<String, Integer> sorted_unique_tags = MapUtility.sortByValue(unique_tags);
        // Print result to file.
        try {
            PrintWriter out = new PrintWriter("sorted-unique-tags.txt");
            out.println(sorted_unique_tags);
            // Print current date and time.
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();
            out.println(dateFormat.format(date));
            out.close();
        } catch(FileNotFoundException e) {
            throw e;
        }
    }
}