package uhh_praktikum_fea.tools;

import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.*;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.json.simple.JSONObject;
import uhh_praktikum_fea.webserver.ApplicationController;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Store {
    public static void storing (String question, String answer) throws IOException, SolrServerException {

        SolrClient client = new HttpSolrClient.Builder(ApplicationController.solr_core_uri).build();
        SolrInputDocument doc = new SolrInputDocument();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();

        String t_date = dateFormat.format(date);
        String t_subject = " No Data";
        String t_message = question;
        String t_summary = "No Data";
        String r_posted = dateFormat.format(date);
        String r_message = answer;
        Long price = 0l;
        Long recommendation = 0l;
        String tags = "";

        int t_length = t_message.replace(" ","").replace("\n","").replace("\r","").length();
        int r_length = r_message.replace(" ","").replace("\n","").replace("\r","").length();

        // Get data analysed by Watson for given String.
        WatsonUtility watson_utility = new WatsonUtility();
        AnalysisResults watson_data = watson_utility.getWatsonRequestData(t_message);

        // Prepare categories determined by Watson for upload.
        List<CategoriesResult> response_categories = watson_data.getCategories();
        List<JSONObject> upload_categories = new ArrayList<JSONObject>();
        for (CategoriesResult result: response_categories) {
            JSONObject category_object = new JSONObject();
            category_object.put("category_label", result.getLabel());
            category_object.put("category_score", result.getScore());
            upload_categories.add(category_object);
        };

        // Prepare sentiment determined by Watson for upload.
        SentimentResult response_sentiment = watson_data.getSentiment();
        DocumentSentimentResults targeted_response_sentiment = response_sentiment.getDocument();
        JSONObject sentiment = new JSONObject();
        sentiment.put("sentiment_label", targeted_response_sentiment.getLabel());
        sentiment.put("sentiment_score", targeted_response_sentiment.getScore());

        // Prepare keywords determined by Watson for upload.
        List<KeywordsResult> response_keywords = watson_data.getKeywords();
        List<JSONObject> upload_keywords = new ArrayList<JSONObject>();
        for (KeywordsResult result: response_keywords) {
            JSONObject keyword_object = new JSONObject();
            keyword_object.put("keyword_text", result.getText());
            keyword_object.put("keyword_relevance", result.getRelevance());
            upload_keywords.add(keyword_object);
        };

        // Prepare entities determined by Watson for upload.
        List<EntitiesResult> response_entities = watson_data.getEntities();
        List<JSONObject> upload_entities = new ArrayList<JSONObject>();
                for (EntitiesResult result: response_entities) {
            JSONObject entity_object = new JSONObject();
            entity_object.put("entity_text", result.getText());
            entity_object.put("entity_relevance", result.getRelevance());
            entity_object.put("entity_type", result.getType());
            entity_object.put("entity_count", result.getCount());
            upload_entities.add(entity_object);
        };

        // Prepare concepts determined by Watson for upload.
        List<ConceptsResult> response_concepts = watson_data.getConcepts();
        List<JSONObject> upload_concepts = new ArrayList<JSONObject>();
                for (ConceptsResult result: response_concepts) {
            JSONObject concept_object = new JSONObject();
            concept_object.put("concept_text", result.getText());
            concept_object.put("concept_relevance", result.getRelevance());
            concept_object.put("concept_dbpedia_resource", result.getDbpediaResource());
            upload_concepts.add(concept_object);
        };

        doc.setField("T_Date", t_date);
        doc.setField("T_Subject", t_subject);
        doc.setField("T_Message", t_message);
        doc.setField("T_Summary", t_summary );
        doc.setField("R_posted", r_posted);
        doc.setField("R_Message", r_message);
        doc.setField("Price", price);
        doc.setField("Recommendations", recommendation);
        doc.setField("Tags", tags);
        doc.setField("Sentiment", sentiment);
        doc.setField("Keywords", upload_keywords);
        doc.setField("Entities", upload_entities);
        doc.setField("Categories", upload_categories);
        doc.setField("Concepts", upload_concepts);
        doc.setField("T_Length", t_length );
        doc.setField("R_Length", r_length);
        doc.setField("New", true);

        client.add(doc);
        client.commit();
    }
}
