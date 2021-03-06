package jsonparser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import startbot.BotStart;

public class JSONParsers {

  public String getLocale(String key, String guildIdLong) {
    try {
      String language = "eng";
      if (BotStart.getMapLanguages().get(guildIdLong) != null) {
        language = BotStart.getMapLanguages().get(guildIdLong);
      }
      InputStream inputStream = getClass().getResourceAsStream("/json/" + language + ".json");
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      JSONParser parser = new JSONParser();
      Object obj = parser.parse(reader);
      JSONObject jsonObject = (JSONObject) obj;
      return jsonObject.get(key).toString();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "NO_FOUND_LOCALIZATION";
  }
}