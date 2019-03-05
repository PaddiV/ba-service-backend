package uhh_praktikum_fea.tools;

import java.io.FileNotFoundException;
import java.io.IOException;

import net.sf.corn.httpclient.HttpClient;
import net.sf.corn.httpclient.HttpResponse;
import org.json.simple.JSONArray;

import java.net.URI;
import java.net.URLEncoder;
import java.util.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Testclass {
    public static void main(String[] args) {

    Map<String, Integer> fragen = new HashMap<String, Integer>();

    fragen.put("Ich habe gewerblich Räume gemietet und hatte zuvor Kontakt mit einem Makler. Dieser hat mir zwar andere Räume im gleichen Gebäude gezeigt und mich mit dem Vermieter in Kontakt gebracht, von den später gemieteten Räumen habe ich jedoch direkt vom Vermieter erfahren. Ich kannte den Vermieter bereits vorher aus dem Internet. Als der Makler nun Provision verlangt hat, habe ich nicht gezahlt und einem entsprechenden Mahnbescheid widersprochen. Meine Frage ist, ob ich dem Makler nach dieser Schilderung Provision zahlen muss." , 322593);
    fragen.put("Muss ich Miete für den Zeitraum zwischen Kündigung des Mietvertrags und einer etwaigen Zwangsräumung zahlen? Wenn ja, wann verjähren diese Ansprüche?",322565);
    fragen.put("Habe ich als Eigentümer einen Anspruch darauf, dass mein Nachbar seine Fußbodenheizung in Stand hält, damit bei mir kein Wasserschaden entsteht? Dies ist in der Vergangenheit bereits mehrach vorgekommen.",322560);
    fragen.put("Ich habe einige Materialien bei einem Bekannten untergestellt. Wir haben vereinbart, dass er sich daran bedienen kann, wenn er mal etwas braucht. Nachdem die Sachen einige Monate bei ihm waren und ich diese abholen wollte, verlangte er plötzlich mehrere Hundert Euro Miete von mir. Wir hatten nie darüber gesprochen, dass die Nutzung seiner Garage kostepflichtig sein sollte. Muss ich diese Miete bezahlen und wenn ja in welcher Höhe?",322548);
    fragen.put("Nachdem wir ein Haus gekauft haben, möchten wir wegen Eingenbedarfs kündigen. Die Mietern haben unterschiedlich lange Verträge. Einmal seit 2001, einmal seit 2009 und einmal seit 2005. Welche Frist filt für die Kündigung?",322533);
    fragen.put("In meinem Mietvertrag steht, dass er mindestens sechs Monate gilt und mit einer Frist von drei Monaten gekündigt werden kann. Bedeutet das insgesamt eine Dauer von mindestens neun Monaten oder kann ich auch nach drei Monaten kündigen und somit insgesamt nur sechs Monate dort wohnen?",322532);
    fragen.put("Gilt der Grundsatz Kauf bricht nicht Miete eigentlich auch für gewerbliche Mietverträge? In meinem Fall geht es um eine sehr günstige Vermietung der Außenwand für Werbezwecke durch den Voreigentümer, die ich gern kündigen möchte.",322432);
    fragen.put("Wir wohnen zu zweit in einer Mietwohnung, in der zwei Sachen repariert werden müssen. Die Heizung verursacht laute Geräusche und ein Fenster ist undicht. Nachdem wir die Verwaltung unter Fristsetzung darum gebeten haben, diese Reparaturen vorzunehmen, war zwar ein Handwerker da, repariert wurde aber nichts. Wir haben eine Mietminderung erklärt und sollen jetzt nach Auffassung der Verwaltung die ganze Miete zahlen, da schon jemand mit der Reparatur beauftragt sei. Seit über einem Monat ist aber nichts passiert. Müssen wir die Miete vollständig bezahlen?",322430);
    fragen.put("Ich bin Mieter in einem Haus mit einem unbefristeten Mietvertrag. Das Haus soll nun verkauft werden und ich befürchte, dass mein Vermieter mich kündigen möchte. Kann er das und wenn ja, wie lange wäre die Kündigungsfrist in diesem Fall?",322367);
    fragen.put("Darf mein Vermieter mir untersagen ein Haustier zu halten?",322282);

    try {
            for (Map.Entry<String, Integer> entry : fragen.entrySet()) {
                HttpClient client = new HttpClient(new URI("http://localhost:8080/fea?question=" + URLEncoder.encode( entry.getKey(), "UTF-8") + "&offset=0&upper_limit=999"));
                HttpResponse response = client.sendData(HttpClient.HTTP_METHOD.GET);
                String jsonString = response.getData();

                JSONParser parser = new JSONParser();

                Object obj = parser.parse(jsonString);
                JSONObject jsonObject = (JSONObject) obj;
                JSONArray data = (JSONArray) jsonObject.get("data");


                int zaehler = 1;
                for (Object o : data) {

                   JSONObject jsonObject1 = (JSONObject) o;
                   long id = Long.parseLong((String) jsonObject1.get("id"));


                    if (id == (long) entry.getValue()) {
                        System.out.println(zaehler);
                        System.out.println(id);
                        //System.out.println(jsonObject1.get("score"));

                    }
                    zaehler++;
                }
            }


        } catch (Exception e){System.out.println(e);}

    }
}
