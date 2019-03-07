package uhh_praktikum_fea.tools;

import com.ibm.watson.developer_cloud.natural_language_understanding.v1.NaturalLanguageUnderstanding;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.*;
import com.ibm.watson.developer_cloud.service.security.IamOptions;

public class WatsonUtility {
    private static int amount_watson_categories = 10;
    private static int amount_watson_keywords = 10;
    private static int amount_watson_concepts = 10;
    //TODO: REMOVE API KEY BEFORE COMMITTING
    private static String api_key = "hRBUii6eLTRZWjvq5yKVUXNv_Yk1-QPv-VB7ZEORbLEi";

    public static AnalysisResults getWatsonRequestData(String text_to_analyze) {
        IamOptions options = new IamOptions.Builder()
                .apiKey(api_key)
                .build();

        NaturalLanguageUnderstanding naturalLanguageUnderstanding = new NaturalLanguageUnderstanding("2018-11-16", options);
        naturalLanguageUnderstanding.setEndPoint("https://gateway-lon.watsonplatform.net/natural-language-understanding/api/");

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
