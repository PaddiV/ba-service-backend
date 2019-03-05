package uhh_praktikum_fea.webserver;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.jobimtext.api.struct.WebThesaurusDatastructure;
import org.json.simple.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextEvaluator {
    private static int min_text_length_to_match = 200;
    private static int max_text_length_to_match = 500;
    private static int avg_sentence_length_to_match = 15;
    private static int avg_sentence_length_allowed_variance = 5;
    private static int min_noun_usage_to_match = 40;
    private static int min_avg_noun_usage_to_match = 1000;
    private static double noun_to_verb_ratio_to_match = 2.5;

    public static String getEvaluation(String text, WebThesaurusDatastructure dt) {
        double words_count = 0;
        double long_words_count = 0;
        // Matches when last char is not a letter or number.
        Pattern punctuation_mark_filter_pattern = Pattern.compile("[a-zA-ZßäÄöÖüÜ0-9]$");

        Rating noun_to_verb_ratio_rating = Rating.NONE;
        List<String> nouns = new ArrayList<String>();
        String sentences[] = new String[0];

        // Tokenizer does not recognize punctuation marks, if no space is between them and the next word.
        // Therefore we place an additional space after each special character.
        text = text.replaceAll("(?<=[]\\[+&|!(){}\\[\\]^\"~*?:/-[.][,]])", " ");

        // Sentence detection.
        try (InputStream sentence_model_in = new FileInputStream("de-sent.bin")) {
            double nouns_count, verbs_count;
            nouns_count = verbs_count = 0;
            SentenceModel sentence_model = new SentenceModel(sentence_model_in);
            SentenceDetectorME sentenceDetector = new SentenceDetectorME(sentence_model);
            sentences = sentenceDetector.sentDetect(text);
            // Tokenization per sentence.
            try (InputStream token_model_in = new FileInputStream("de-token.bin")) {
                TokenizerModel token_model = new TokenizerModel(token_model_in);
                Tokenizer tokenizer = new TokenizerME(token_model);
                // Load POS tagger
                try (InputStream pos_model_in = new FileInputStream("de-pos-maxent.bin")){
                    POSModel model = new POSModel(pos_model_in);
                    POSTaggerME tagger = new POSTaggerME(model);
                    for (String sentence: sentences) {
                        String tokens[] = tokenizer.tokenize(sentence);
                        // Count total amount of words for LIX calculation.
                        String words[] = Arrays.stream(tokens).filter(token -> {
                            Matcher regex_matcher = punctuation_mark_filter_pattern.matcher(token);
                            return regex_matcher.find();
                        }).toArray(String[]::new);
                        words_count = words_count + words.length;
                        long_words_count = long_words_count + Arrays.stream(words).filter(token ->
                                token.length() > 6).toArray(String[]::new).length;
                        // POS tagging all tokens

                        String tags[] = tagger.tag(tokens);
                        // Count all nouns (tag starting with 'N') and verbs (tag starting with 'V').
                        int i = 0;
                        for (String tag: tags) {
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
                if (noun_to_verb_ratio < noun_to_verb_ratio_to_match) {
                    noun_to_verb_ratio_rating = Rating.GOOD;
                } else {
                    noun_to_verb_ratio_rating = Rating.BAD;
                }
            }
        } catch(FileNotFoundException e) {
            System.out.println("Uh-oh! File not found. Please make sure you have all necessary OpenNLP files for German sentence detection, tokenization and POS tagging (Maxent).");
        } catch(IOException e) {
            System.out.println("Uh-oh! An IO Exception occurred while attempting POS tagging.");
        }

        // Check usage of each noun and keep track of especially rarely used nouns for warnings.
        List<Long> nouns_usages = new ArrayList<>();
        List<String> problematic_nouns = new ArrayList<>();
        for (String noun: nouns) {
            long noun_usage = dt.getTermCount(noun);
            nouns_usages.add(noun_usage);
            if (noun_usage < min_noun_usage_to_match && !problematic_nouns.contains(noun)){
                problematic_nouns.add(noun);
            }
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

        // Check if the overall usage of nouns is too low.
        Rating nouns_used_rating;
        if (nouns_usages.size() != 0) {
            Long avg_nouns_usage = 0l;
            for (Long usage: nouns_usages) {
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

        JSONObject response = new JSONObject();
        response.put("text_length", text_length_rating.toString());
        response.put("avg_sentence_length", avg_sentence_length_rating.toString());
        response.put("nouns_used", nouns_used_rating.toString());
        response.put("problematic_nouns", problematic_nouns);
        response.put("nouns_to_verbs_ratio", noun_to_verb_ratio_rating.toString());
        response.put("lix_score", lix_score.toString());

        return response.toString();
    }
}
