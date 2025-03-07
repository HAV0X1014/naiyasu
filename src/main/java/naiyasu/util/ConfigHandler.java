package naiyasu.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static naiyasu.NaiMain.config;

public class ConfigHandler {
    public static String getString(String option) {
        JSONObject obj = new JSONObject(config);
        return obj.getJSONObject("Config").getString(option);
    }
    public static String[] getArray(String option) {
        JSONObject obj = new JSONObject(config);
        JSONArray jsonArray = obj.getJSONObject("Config").getJSONArray(option);

        List<String> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            list.add(jsonArray.getString(i));
        }
        return list.toArray(new String[0]);
    }
    public static boolean getBoolean(String option) {
        JSONObject obj = new JSONObject(config);
        return obj.getJSONObject("Config").getBoolean(option);
    }
}