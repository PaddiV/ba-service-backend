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

        //creating a hash-map with the test-questions
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
        fragen.put("Ich bin aus meiner Wohung ausgezogen und habe von meiner Vermieterin eine Abrechnung erhalten, bei der die Vorauszahlungen und abgerechneten Monate nicht stimmen, und somit eine deutliche Nachzahlung verlangt wird. Die Vermieterin weigert sich zu akzeptieren, dass diese Rechnung falsch ist. Die Abrehcnung war so schlecht, dass sich nicht mehr nachvollziehen lässt, ob in den früheren Abrechnungen ein Fehler gemacht wurde, außerdem habe ich frühere Abrechnungen nicht mehr und die Vewrmierting rückt sie nicht raus. Der Zeitraum der Nebenkostenabrechnung  muss doch immer exakt dem Zeitraum der Vorauszahlungen entsprechen? Sind vergangene Abrechnungen nicht einfach abgeschlossen und die neue Abrechnung muss korrigiert werden?",315279);
        fragen.put("Meine ehemalige Freundin und ich haben uns die Miete immer geteilt. Als sie mal kein Geld hatte, habe ich die Miete für sie gezahlt, ich habe dann für zwei Jahre kein Geld mehr erhalten.  Inzwischen sind wir getrennt und sie weigert sich, mir die noch ausstehende Miete zurückzuzahlen.",315203);
        fragen.put("Wenn unsere Nachbarn ihre Pflanzen auf dem Balkon gießen, werden wir nass und unser Balkon dreckig. Die Hausverwaltung will uns nicht Hälfen. Können wir weniger Miete zahlen, um den Druck zu erhöhen? Beste GRÜßE!",315194);
        fragen.put("Ich bin so genervt. Der Vermieter rechnet die Kosten für die Gartenpflege ab, kann aber die geleisteten Stunden nicht belegen. Da könnte ja jeder kommen! Ich vermute, der Vermieter lässt hier Stunden abrechnen, die gar nicht an unserem Haus geleistet wurden!!! Muss der Vermieter beweisen, dass tatstächlich gearbeitet wurde?",315135);
        fragen.put("Als wir ins unsere Wohnung gezogen sind, haben wir renoviert, alles frisch gestrichen etc. Jetzt wollen wir wieder ausziehen, was müssen wir jetzt tun? Müssen wir wieder streichen? Ist eine Quote im Vertrag zulässig und wie wird diese festgesetzt?",315052);
        fragen.put("Ich habe eine Frage zur Kündigung meines Mietvertrages. Wir haben eine Klausel im Vertrag, welche die Kündigung für ein Jahr ausschließt. Heißt das, wir müssen ein Jahr abwarten, können dann erst kündigen und müssen dann nochmal während der Kündigungsfrist dort wohnen? Oder können wir zum Ende des einen Jahres direkt ausziehen, wenn wir rechtzeitig vorher kündigen?",322269);
        fragen.put("Wenn in meniem gewerblichen Mietvertrag ein Optionsrecht für ein Jahr steht, dass ich sechs Monate vor dem Vertragsende ausüben muss, gilt das jedes Jahr neu oder nur für das erste Jahr?",322263);
        fragen.put("Was muss ich beachten, wenn ich eine aktuell als normale Wohnung genutzte Eigentumswohnug in Zukunft kurzfristig als Ferienwohnung oder tageweise vermieten möchte? Ist das für zustimmungsbedürftig?",322255);
        fragen.put("Angenommen ich habe mit meinem Vermieter vereinbart, dass wir beide erst nach zwei Jahren kündigen können. Kann ich irgendwie den Vertrag beenden, wenn ich heriate und meine Frau und ich Kinder bekommen? Die Wohnung ist eigentlich nur für eine Person geeignet.",322247);
        fragen.put("Ich möchte meine Wohnung gern zum Teil vermieten und dabei möglichst kurze Kündigungsfristen vereinbaren? Welche Regeln gelten dafür und müssen die Räume möbliert sein?",322176);
        fragen.put("Mein Vater ist vor kurzem verstorben, nachdem er über dreißig Jahre in einer Mietwohnung gelebt hat. Da der Mietvertrag verschollen ist, möchte ich wissen, wer gesetzlich dazu verpflichtet ist, etwaige Reparaturkosten zu tragen. Außerdem fragen ich mich, wie lang die Kündiungsfrist in diesem Fall ist. ",322143);
        fragen.put("Ich streite mit einem Mieter über eine Nebenkostennachzahlung. Dieser hat sich an den Mieterbund gewendet und jener verlangt nun die Originale der Rechnungen per Post. Muss ich dem nachkommen oder kann ich auch eine Einsichtnahme bei mir anbieten? ",322128);
        fragen.put("Wir haben eine Wohnung gekauft. Unser Verkäufer hat dem Mieter gekündigt, da dieser keine Miete gezahlt hat. Gilt diese Kündigung für uns weiter und was passiert, wenn der Voreigentümer schon geklagt hat?",321981);
        fragen.put("Ich bewohne ein Haus direkt neben meinem Vermieter. In meinem Mietvertrag steht, dass ich den Vermieter um Zustimmung bitten muss, wenni ch Pflanzen wegreißen oder neu pflanzen möchte. Ich habe nun eine Hecke deutlich geschnitten und erwäge eine weitere, überwiegend bereits tote Hecke, ebenfalls zu stutzen. Mein Vermieter hat die Zustimmung verweigert und mir mit einer Kündigung gedroht. Welche Möglichkeiten habe ich? ",321891);
        fragen.put("Ich habe ein Haus mit Wohn- und Gewerbeeinheiten gekauft. Was muss ich den Mietern außer meiner Bankverbindung noch mitteilen?",321825);
        fragen.put("Wir streiten in unserer WEG mit dem Verwalter über nicht ordnungemäße Abrechnungen. Nun wurde gerichtlich entschieden, dass die Abrechnungen falsch waren. Allerdings hat das Gericht dem Verwalter nicht die Kosten auferlegt. Da in Frage steht, ob die WEG diese Kosten übernehmen soll, wüsste ich gern, ob nicht eigetnlcih der Verwalter diese Kosten tragen muss.",321814);
        fragen.put("Haftet der Vermieter meines Ladens auch für Schäden an Tür und Fenster nach einem Einbruch, wenn ich eine Hausratversicherung habe, die solche Schäden erfasst?",321793);
        fragen.put("Muss ich beim Auszug kahle Wände neu tapezieren?",321679);
        fragen.put("Darf ich als Ausländer einen Mietvertrag unterschreiben?",321672);
        fragen.put("Muss auch der Vermieter für den Zahlungsverzug Zinsen zahlen, wenn im Vertrag nur der Mieter zur Zahlung von Zinsen verpflichtet ist?",319428);
        fragen.put("Können Sie uns einen Anwalt empfehlen?",319275);

        try {
            for (Map.Entry<String, Integer> entry : fragen.entrySet()) {

                // searching for results in "T_Message" for every question
                HttpClient client = new HttpClient(new URI("http://localhost:8080/fea?question=" + URLEncoder.encode( entry.getKey(), "UTF-8") + "&offset=0&upper_limit=999&user=asdf&password=asdf"));
                HttpResponse response = client.sendData(HttpClient.HTTP_METHOD.GET);
                String jsonString = response.getData();
                JSONParser parser = new JSONParser();
                Object obj = parser.parse(jsonString);
                JSONObject jsonObject = (JSONObject) obj;
                JSONArray data = (JSONArray) jsonObject.get("data");

                //count in witch place the id of the test-question equals the original question
                int counter = 1;
                for (Object o : data) {

                   JSONObject jsonObject1 = (JSONObject) o;
                   long id = Long.parseLong((String) jsonObject1.get("id"));

                    if (id == (long) entry.getValue()) {

                        //print the place and the id of the question
                        System.out.println(counter);
                        System.out.println(id);
                    }
                    counter++;
                }
            }
        } catch (Exception e){System.out.println(e);}

    }
}
