package com.genuwin.app.homeassistant;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityManager {
    private final Map<String, List<String>> entities;

    public EntityManager(Context context) {
        this.entities = loadEntities(context);
    }

    private Map<String, List<String>> loadEntities(Context context) {
        try (InputStream is = context.getAssets().open("homeassistant-entities.json")) {
            Type type = new TypeToken<Map<String, List<String>>>() {}.getType();
            return new Gson().fromJson(new InputStreamReader(is), type);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<String> getEntityIds(String friendlyName) {
        return entities.get(friendlyName);
    }
    
    public List<String> getAllEntityNames() {
        if (entities == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(entities.keySet());
    }



    public String getTools() {
        // Deprecated - use getOpenAITools() instead
        return "";
    }
}
