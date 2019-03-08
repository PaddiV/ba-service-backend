package uhh_praktikum_fea.tools;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;

import java.util.Arrays;


public class Stats {

    public String charts (String x_achse, String y_achse) throws FileNotFoundException, IOException, ParseException {


        InputStream messeges = getClass().getClassLoader().getResourceAsStream("R_Message.txt");

        InputStreamReader reader = new  InputStreamReader (messeges);
        BufferedReader buffer = new BufferedReader(reader);
        String s;
        JSONParser parser = new JSONParser();

        int counter = 0;

        double x = 0;
        double y = 0;


        Double[] x_list = new Double[999];
        Double[] y_list = new Double[999];


        while ((s = buffer.readLine()) != null) {
            JSONObject object = (JSONObject) parser.parse(s);

            x_list[counter] = toDouble(object, x_achse);
            y_list[counter] = toDouble(object, y_achse);
            counter++;
        }

        double[][] chart_array = new double[999][2];

        for (int i = 0; i < counter; ++i) {
            chart_array[i][0] = x_list[i];
            chart_array[i][1] = y_list[i];

        }
        return Arrays.deepToString(chart_array);

            //System.out.println("nouns_to_verbs_ratio: "+ (ntvr/counter));
            //System.out.println("nouns_used: "+ (nu/counter) );
            //System.out.println("lix_score: "+ (lix/counter) );
            //System.out.println("text_length: "+ (tl/counter));
            //System.out.println("avg_sentence_length: "+ (avg/counter));
    }
    private double toDouble ( JSONObject object, String axis){
        String str = object.get(axis).toString();
        Double d = new Double(str);
        return d.doubleValue();
    }
}