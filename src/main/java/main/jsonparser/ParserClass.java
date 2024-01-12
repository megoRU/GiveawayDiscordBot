package main.jsonparser;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ParserClass {

    private final static Logger LOGGER = Logger.getLogger(ParserClass.class.getName());

    public static final ConcurrentMap<String, String> russian = new ConcurrentHashMap<>();
    public static final ConcurrentMap<String, String> english = new ConcurrentHashMap<>();
    private static volatile ParserClass parserClass;

    private ParserClass() {
    }

    public static ParserClass getInstance() {
        if (parserClass == null) {
            synchronized (ParserClass.class) {
                if (parserClass == null) {
                    parserClass = new ParserClass();
                }
            }
        }
        return parserClass;
    }

    public String getTranslation(String key, String language) {
        try {
            if (language.equals("eng")) {
                return english.get(key);
            } else {
                return russian.get(key);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return "NO_FOUND_LOCALIZATION";
    }
}
