package main.service;

import main.jsonparser.ParserClass;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class LanguageService {

    private final static Logger LOGGER = LoggerFactory.getLogger(LanguageService.class.getName());

    public void languageParse() {
        try {
            List<String> listLanguages = new ArrayList<>();
            listLanguages.add("rus");
            listLanguages.add("eng");

            for (String listLanguage : listLanguages) {
                InputStream inputStream = new ClassPathResource("json/" + listLanguage + ".json").getInputStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                JSONObject jsonObject = (JSONObject) new JSONParser().parse(reader);

                for (Object o : jsonObject.keySet()) {
                    String key = (String) o;

                    if (listLanguage.equals("rus")) {
                        ParserClass.russian.put(key, String.valueOf(jsonObject.get(key)));
                    } else {
                        ParserClass.english.put(key, String.valueOf(jsonObject.get(key)));
                    }
                }
                reader.close();
                inputStream.close();
                reader.close();
            }
            System.out.println("setLanguages()");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
