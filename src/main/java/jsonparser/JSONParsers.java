package jsonparser;

import java.io.File;
import java.io.FileReader;
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

      File file = new File("src/main/java/translations/" + language + ".json");
      JSONParser parser = new JSONParser();
      Object obj = parser.parse(new FileReader(file));
      JSONObject jsonObject = (JSONObject) obj;
      return jsonObject.get(key).toString();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "NO_FOUND_LOCALIZATION";
  }

}