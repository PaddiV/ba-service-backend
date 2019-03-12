package uhh_praktikum_fea.tools;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import uhh_praktikum_fea.webserver.ApplicationController;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MostCommonNounListTool {
    // Determines the number of nouns to be stored. The x most common nouns will be stored.
    private static int amount_nouns_to_store = 100;

    /**
     * Analyzes all answers currently stored in Solr and stores the x (amount_nouns_to_store) most common nouns among those for quicker text evaluations.
     */
    public static void main(String[] args) throws IOException, SolrServerException {
        SolrClient client = new HttpSolrClient.Builder(ApplicationController.solr_core_uri).build();
        // Delete old common noun entries in Solr.
        client.deleteByQuery("common_noun:*");

        // Get answers from Solr.
        SolrQuery query = new SolrQuery();
        query.setQuery("R_Message:*");
        query.set("fl", "R_Message");
        query.setRows(5000);
        QueryResponse response = client.query(query);
        SolrDocumentList queryResults = response.getResults();
        String answers = "";
        for (SolrDocument queryResult : queryResults) {
            // Concatenate all answers without the surrounding brackets.
            answers += queryResult.get("R_Message").toString().substring(1, queryResult.get("R_Message").toString().length() - 2);
        }

        List<String> nouns = new ArrayList<String>();
        List<String> nouns_tags = new ArrayList<String>();
        String sentences[];
        // Tokenizer does not recognize punctuation marks, if no space is between them and the next word.
        // Therefore an additional space is placed after each special character.
        answers = answers.replaceAll("(?<=[]\\[+&|!(){}\\[\\]^\"„~*?:/-[.][,]])", " ");
        // Matches when first char is a capital letter.
        Pattern non_noun_filter_pattern = Pattern.compile("^[A-ZÄÖÜ]");

        // Sentence detection.
        try (InputStream sentence_model_in = new FileInputStream("de-sent.bin")) {
            SentenceModel sentence_model = new SentenceModel(sentence_model_in);
            SentenceDetectorME sentenceDetector = new SentenceDetectorME(sentence_model);
            sentences = sentenceDetector.sentDetect(answers);
            // Tokenization per sentence.
            try (InputStream token_model_in = new FileInputStream("de-token.bin")) {
                TokenizerModel token_model = new TokenizerModel(token_model_in);
                Tokenizer tokenizer = new TokenizerME(token_model);
                // Load POS tagger.
                try (InputStream pos_model_in = new FileInputStream("de-pos-maxent.bin")) {
                    POSModel model = new POSModel(pos_model_in);
                    POSTaggerME tagger = new POSTaggerME(model);
                    for (String sentence : sentences) {
                        String tokens[] = tokenizer.tokenize(sentence);
                        // Remove first word of sentence to get rid of false positives during noun detection.
                        List<String> tokens_list = new ArrayList<String>();
                        for (String token: tokens) {
                            tokens_list.add(token);
                        }
                        tokens_list.remove(0);
                        tokens = tokens_list.toArray(new String[tokens_list.size()]);
                        // POS tagging all tokens
                        String tags[] = tagger.tag(tokens);
                        // Store all nouns (tag starting with 'N' and capital first letter).
                        int i = 0;
                        for (String tag : tags) {
                            Matcher regex_matcher = non_noun_filter_pattern.matcher(tokens[i]);
                            if (tag.charAt(0) == 'N' && regex_matcher.find()) {
                                // Not great, but the way the tagger works way more readable than the alternative.
                                nouns.add(tokens[i]);
                                nouns_tags.add(tag);
                                i++;
                            }
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                System.out.println("Uh-oh! File not found. Please make sure you have all necessary OpenNLP files for German sentence detection, tokenization and POS tagging (Maxent).");
            } catch (IOException e) {
                System.out.println("Uh-oh! An IO Exception occurred while attempting POS tagging.");
            }

            // Count noun_usages.
            Set<String> distinct_nouns = new HashSet<>(nouns);
            Map<String, Long> noun_usages = new HashMap<>();
            for (String noun: distinct_nouns) {
                noun_usages.put(noun, 0l);
            }
            for (String noun: nouns) {
                noun_usages.put(noun, noun_usages.get(noun) + 1);
            }
            // Sort to get most commonly used words first.
            noun_usages = MapUtility.sortByValue(noun_usages);

            // Upload results to Solr.
            SolrInputDocument doc = new SolrInputDocument();
            int i = 0;
            Iterator it = noun_usages.entrySet().iterator();
            while (it.hasNext() && i < amount_nouns_to_store) {
                Map.Entry pair = (Map.Entry)it.next();
                doc.setField("id", "noun-" + i++);
                doc.setField("common_noun", pair.getKey());
                doc.setField("usage_amount", pair.getValue());
                client.add(doc);
                it.remove(); // avoids a ConcurrentModificationException
            }
            client.commit();
        }
    }

    public static int getUsage(String word) throws IOException, SolrServerException {
        SolrClient client = new HttpSolrClient.Builder(ApplicationController.solr_core_uri).build();
        SolrQuery query = new SolrQuery();
        query.setQuery("common_noun:" + word);
        QueryResponse response = client.query(query);
        SolrDocumentList queryResults = response.getResults();
        if (queryResults.getNumFound() == 0) {
            return 0;
        }
        String result_string = queryResults.get(0).get("usage_amount").toString().replaceAll("[\\[\\]]", "");
        int result;
        try {
            result = Integer.parseInt(result_string);
        }
        catch (NumberFormatException e)
        {
            result = 0;
        }
        return result;
    }
}
