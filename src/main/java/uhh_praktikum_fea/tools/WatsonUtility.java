package uhh_praktikum_fea.tools;

import com.ibm.watson.developer_cloud.natural_language_understanding.v1.NaturalLanguageUnderstanding;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.*;
import com.ibm.watson.developer_cloud.service.security.IamOptions;

public class WatsonUtility {
    private static int amount_watson_categories = 10;
    private static int amount_watson_keywords = 10;
    private static int amount_watson_concepts = 10;
    //TODO: REMOVE API KEY BEFORE COMMITTING
    private static String api_key = "";
    private static String api_version = "2018-11-16";
    private static String ibm_watson_endpoint = "https://gateway-lon.watsonplatform.net/natural-language-understanding/api/";

    /**
     * Accepts a text and returns the response of the IBM Watson API including categories, sentiment, keywords and concepts.
     *
     * @param text_to_analyze text to be analyzed
     */
    public static AnalysisResults getWatsonRequestData(String text_to_analyze) {
        IamOptions options = new IamOptions.Builder()
                .apiKey(api_key)
                .build();

        NaturalLanguageUnderstanding naturalLanguageUnderstanding = new NaturalLanguageUnderstanding(api_version, options);
        naturalLanguageUnderstanding.setEndPoint(ibm_watson_endpoint);

        EntitiesOptions entities = new EntitiesOptions.Builder()
                .build();

        CategoriesOptions categories = new CategoriesOptions();
        categories.setLimit(amount_watson_categories);

        SentimentOptions sentiment = new SentimentOptions.Builder()
                .document(true)
                .build();

        KeywordsOptions keywords= new KeywordsOptions.Builder()
                .limit(amount_watson_keywords)
                .build();

        ConceptsOptions concepts= new ConceptsOptions.Builder()
                .limit(amount_watson_concepts)
                .build();

        Features features = new Features.Builder()
                .categories(categories)
                .entities(entities)
                .sentiment(sentiment)
                .keywords(keywords)
                .concepts(concepts)
                .build();

        AnalyzeOptions parameters = new AnalyzeOptions.Builder()
                .text(text_to_analyze)
                .features(features)
                .build();

        AnalysisResults ibm_response = naturalLanguageUnderstanding
                .analyze(parameters)
                .execute();

        return ibm_response;
    }
}
