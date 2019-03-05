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
import uhh_praktikum_fea.webserver.Rating;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MostCommonNounListGenerator {
    public static void main(String[] args) throws IOException, SolrServerException {
        // Query
        SolrClient client = new HttpSolrClient.Builder("http://ltdemos:8983/solr/fea-schema-less").build();
        SolrQuery suche = new SolrQuery();
        suche.setQuery("*");
        suche.set("fl", "R_Message");
        suche.setStart(0);
        suche.setRows(999);
        QueryResponse response = client.query(suche);
        SolrDocumentList queryResults = response.getResults();
        String answers = "";
        for (SolrDocument queryResult : queryResults) {
            answers += queryResult.get("R_Message").toString();
        }


        List<String> nouns = new ArrayList<String>();
        String sentences[] = new String[0];

        // Tokenizer does not recognize punctuation marks, if no space is between them and the next word.
        // Therefore we place an additional space after each special character.
        answers = answers.replaceAll("(?<=[]\\[+&|!(){}\\[\\]^\"~*?:/-[.][,]])", " ");

        // Matches when last char is not a letter or number.
        Pattern punctuation_mark_filter_pattern = Pattern.compile("[a-zA-ZßäÄöÖüÜ0-9]$");

        // Sentence detection.
        try (InputStream sentence_model_in = new FileInputStream("de-sent.bin")) {
            double nouns_count, verbs_count;
            nouns_count = verbs_count = 0;
            SentenceModel sentence_model = new SentenceModel(sentence_model_in);
            SentenceDetectorME sentenceDetector = new SentenceDetectorME(sentence_model);
            sentences = sentenceDetector.sentDetect(answers);
            // Tokenization per sentence.
            try (InputStream token_model_in = new FileInputStream("de-token.bin")) {
                TokenizerModel token_model = new TokenizerModel(token_model_in);
                Tokenizer tokenizer = new TokenizerME(token_model);
                // Load POS tagger
                try (InputStream pos_model_in = new FileInputStream("de-pos-maxent.bin")) {
                    POSModel model = new POSModel(pos_model_in);
                    POSTaggerME tagger = new POSTaggerME(model);
                    for (String sentence : sentences) {
                        String tokens[] = tokenizer.tokenize(sentence);
                        // POS tagging all tokens
                        String tags[] = tagger.tag(tokens);
                        // Count all nouns (tag starting with 'N') and verbs (tag starting with 'V').
                        int i = 0;
                        for (String tag : tags) {
                            if (tag.charAt(0) == 'N') {
                                nouns_count++;
                                // Not great, but the way the tagger works way more readable than the alternative.
                                nouns.add(tokens[i]);
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
            // Tokenize
            // (Remove everything but words)
            // Remove everything but nouns
            // Set daraus generieren
            // Daraus Map machen mit 0-Werten
            // Gesamtliste iterieren und Werte der Map entsprechend hochzählen
            // Häufigste x Worte
        }
    }

    private static int getUsage(String word) {
        return 0;
    }
}