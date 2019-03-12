package uhh_praktikum_fea.webserver;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.apache.solr.client.solrj.SolrServerException;
import org.json.simple.JSONObject;
import uhh_praktikum_fea.tools.MostCommonNounListTool;

import org.json.JSONException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextEvaluator {
    private static int min_text_length_to_match = 1000;
    private static int max_text_length_to_match = 2500;
    private static int avg_sentence_length_to_match = 17;
    private static int avg_sentence_length_allowed_variance = 5;
    private static int min_noun_usage_to_match = 50;
    private static int min_avg_noun_usage_to_match = 30000;
    private static double noun_to_verb_ratio_to_match = 1.5;
    private static double noun_to_verb_ratio_to_match_variance = 0.4;

    /**
     * Accepts a text and returns understandability ratings based on the texts properties.
     *
     * @param text text to be evaluated
     * @param returnRawValues set false for Ratings, true for numeric values
     */
    public  String getEvaluation(String text, Boolean returnRawValues) throws IOException, SolrServerException {
        DTHelper dt = new DTHelper("conf_web_deNews_trigram.xml");
        double words_count = 0;
        double long_words_count = 0;
        double nouns_count, verbs_count;
        nouns_count = verbs_count = 0;
        // Matches when last char is not a letter or number.
        Pattern punctuation_mark_filter_pattern = Pattern.compile("[a-zA-ZßäÄöÖüÜ0-9]$");

        Rating noun_to_verb_ratio_rating = Rating.NONE;
        List<String> nouns = new ArrayList<String>();
        String sentences[] = new String[0];

        // Tokenizer does not recognize punctuation marks, if no space is between them and the next word.
        // Therefore we place an additional space after each special character.
        text = text.replaceAll("(?<=[]\\[+&|!(){}\\[\\]^\"~*?:/-[.][,]])", " ");
        // Remove quotation marks, which seem to break the method somehow.
        text = text.replaceAll("\"", "");

        // Sentence detection.
        try (InputStream sentence_model_in = getClass().getClassLoader().getResourceAsStream("de-sent.bin")) {
            SentenceModel sentence_model = new SentenceModel(sentence_model_in);
            SentenceDetectorME sentenceDetector = new SentenceDetectorME(sentence_model);
            sentences = sentenceDetector.sentDetect(text);
            // Tokenization per sentence.
            try (InputStream token_model_in = getClass().getClassLoader().getResourceAsStream("de-token.bin")) {
                TokenizerModel token_model = new TokenizerModel(token_model_in);
                Tokenizer tokenizer = new TokenizerME(token_model);
                // Load POS tagger
                try (InputStream pos_model_in = getClass().getClassLoader().getResourceAsStream("de-pos-maxent.bin")) {
                    POSModel model = new POSModel(pos_model_in);
                    POSTaggerME tagger = new POSTaggerME(model);
                    for (String sentence : sentences) {
                        String tokens[] = tokenizer.tokenize(sentence);
                        // Count total amount of words for LIX calculation.
                        String words[] = Arrays.stream(tokens).filter(token -> {
                            Matcher regex_matcher = punctuation_mark_filter_pattern.matcher(token);
                            return regex_matcher.find();
                        }).toArray(String[]::new);
                        List<String> utf8_tokens = new ArrayList<String>();
                        for (String token : tokens) {
                            String utf8_token = new String(token.getBytes(), "UTF-8");
                            utf8_tokens.add(utf8_token);
                        }
                        String[] utf8_tokens_arr = new String[utf8_tokens.size()];
                        utf8_tokens_arr = utf8_tokens.toArray(utf8_tokens_arr);
                        words_count = words_count + words.length;
                        long_words_count = long_words_count + Arrays.stream(words).filter(token ->
                                token.length() > 6).toArray(String[]::new).length;
                        // POS tagging all tokens
                        String tags[] = tagger.tag(utf8_tokens_arr);
                        // Count all nouns (tag starting with 'N') and verbs (tag starting with 'V').
                        int i = 0;
                        for (String tag : tags) {
                            if (tag.charAt(0) == 'N') {
                                nouns_count++;
                                // Not great, but the way the tagger works way more readable than the alternative.
                                nouns.add(tokens[i]);
                            } else if (tag.charAt(0) == 'V') {
                                verbs_count++;
                            }
                            i++;
                        }
                    }
                }
            }
            // Determine noun_to_verb_ratio and evaluate it.
            if (nouns_count != 0 && verbs_count != 0) {
                double noun_to_verb_ratio = nouns_count / verbs_count;
                if (noun_to_verb_ratio < noun_to_verb_ratio_to_match - noun_to_verb_ratio_to_match_variance) {
                    noun_to_verb_ratio_rating = Rating.BAD;
                } else if (noun_to_verb_ratio > noun_to_verb_ratio_to_match + noun_to_verb_ratio_to_match_variance) {
                    noun_to_verb_ratio_rating = Rating.BAD;
                } else {
                    noun_to_verb_ratio_rating = Rating.GOOD;
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Uh-oh! File not found. Please make sure you have all necessary OpenNLP files for German sentence detection, tokenization and POS tagging (Maxent).");
        } catch (IOException e) {
            System.out.println("Uh-oh! An IO Exception occurred while attempting POS tagging.");
        }

        // Check usage of each noun and keep track of especially rarely used nouns for warnings.
        List<Long> nouns_usages = new ArrayList<>();
        List<String> problematic_nouns = new ArrayList<>();
        // Convert to set to get rid of duplicates in order to improve performance with fewer network requests.
        Set<String> nouns_set = new HashSet<>(nouns);
        Map<String, Long> noun_to_usage_mapping = new HashMap<>();
        for (String noun : nouns_set) {
            long noun_usage;
            int local_common_nouns_usage = MostCommonNounListTool.getUsage(noun);
            if (local_common_nouns_usage == 0) {
                noun_usage = dt.getTermCount(noun);
            } else {
                noun_usage = local_common_nouns_usage;
            }
            noun_to_usage_mapping.put(noun, noun_usage);
            if (noun_usage < min_noun_usage_to_match) {
                problematic_nouns.add(noun);
            }
        }
        // Re-inflate from streamlined set for full text average noun usage calculated later.
        for (String noun : nouns) {
            long noun_usage = noun_to_usage_mapping.get(noun);
            nouns_usages.add(noun_usage);
        }

        // Check if the overall usage of nouns is too low.
        Rating nouns_used_rating;
        if (nouns_usages.size() != 0) {
            Long avg_nouns_usage = 0l;
            for (Long usage : nouns_usages) {
                avg_nouns_usage = avg_nouns_usage + usage;
            }
            avg_nouns_usage = avg_nouns_usage / nouns_usages.size();

            if (avg_nouns_usage < min_avg_noun_usage_to_match) {
                nouns_used_rating = Rating.BAD;
            } else {
                nouns_used_rating = Rating.GOOD;
            }
        } else {
            nouns_used_rating = Rating.NONE;
        }

        // Check if text is too short or too long.
        Rating text_length_rating;
        if (text.length() > min_text_length_to_match) {
            if (text.length() < max_text_length_to_match) {
                text_length_rating = Rating.OK;
            } else {
                text_length_rating = Rating.LONG;
            }
        } else {
            text_length_rating = Rating.SHORT;
        }

        // Check if the average word count per sentence is too short or too long.
        Rating avg_sentence_length_rating;
        double avg_sentence_word_count = 0;
        if (sentences.length != 0) {
            avg_sentence_word_count = words_count / sentences.length;
            if (avg_sentence_word_count < (avg_sentence_length_to_match - avg_sentence_length_allowed_variance)) {
                avg_sentence_length_rating = Rating.SHORT;
            } else {
                if (avg_sentence_word_count > (avg_sentence_length_to_match + avg_sentence_length_allowed_variance)) {
                    avg_sentence_length_rating = Rating.LONG;
                } else {
                    avg_sentence_length_rating = Rating.OK;
                }
            }
        } else {
            avg_sentence_length_rating = Rating.NONE;
        }

        // LIX calculation.
        Rating lix_score = Rating.NONE;
        double long_words_percentage = long_words_count / words_count * 100;
        int lix = (int) Math.rint(long_words_percentage + avg_sentence_word_count);
        if (lix <= 50) {
            lix_score = Rating.GOOD;
        } else if (lix <= 60) {
            lix_score = Rating.OK;
        } else if (lix > 60) {
            lix_score = Rating.BAD;
        }
        org.json.JSONObject totalresult = new org.json.JSONObject();
        JSONObject response = new JSONObject();
        if (returnRawValues) {
            Long avg_nouns_usage = 0l;
            for (Long usage : nouns_usages) {
                avg_nouns_usage = avg_nouns_usage + usage;
            }
            avg_nouns_usage = avg_nouns_usage / nouns_usages.size();
            response.put("text_length", text.length());
            response.put("avg_sentence_length", words_count / sentences.length);
            response.put("nouns_used", avg_nouns_usage.toString());
            response.put("problematic_nouns", problematic_nouns);
            response.put("nouns_to_verbs_ratio", nouns_count / verbs_count);
            response.put("lix_score", lix);
            try {
                totalresult.put("data", response);
                return totalresult.toString();
            } catch (JSONException error) {
                System.out.println(error);
            }
        } else {
            response.put("text_length", text_length_rating.toString());
            response.put("avg_sentence_length", avg_sentence_length_rating.toString());
            response.put("nouns_used", nouns_used_rating.toString());
            response.put("problematic_nouns", problematic_nouns);
            response.put("nouns_to_verbs_ratio", noun_to_verb_ratio_rating.toString());
            response.put("lix_score", lix_score.toString());
        }
        return response.toString();
    }
}
