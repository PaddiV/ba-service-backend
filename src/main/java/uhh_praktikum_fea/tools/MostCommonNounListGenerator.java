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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
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
            // Concatenate all answers without the surrounding brackets.
            answers += queryResult.get("R_Message").toString().substring(1, queryResult.get("R_Message").toString().length() - 2);
        }

        List<String> nouns = new ArrayList<String>();
        List<String> nouns_tags = new ArrayList<String>();
        String sentences[];
        // Tokenizer does not recognize punctuation marks, if no space is between them and the next word.
        // Therefore we place an additional space after each special character.
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
                // Load POS tagger
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

            System.out.println(nouns);
            System.out.println(nouns_tags);

            Set<String> distinct_nouns = new HashSet<>(nouns);
            Map<String, Long> noun_usages = new HashMap<>();
            for (String noun: distinct_nouns) {
                noun_usages.put(noun, 0l);
            }
            for (String noun: nouns) {
                noun_usages.put(noun, noun_usages.get(noun) + 1);
            }
            System.out.println(noun_usages.keySet());
            System.out.println(noun_usages.values());
            System.out.println(noun_usages);
        }
    }

    private static int getUsage(String word) {
        return 0;
    }
}