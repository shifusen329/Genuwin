package com.genuwin.app.homeassistant;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class ToolParser {
    private static final Gson gson = new Gson();

    public static ToolCall parse(String json) {
        try {
            JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
            String tool = jsonObject.get("tool").getAsString();
            String entityId = jsonObject.get("entity_id").getAsString();
            return new ToolCall(tool, entityId);
        } catch (Exception e) {
            return null;
        }
    }

    public static class ToolCall {
        public final String tool;
        public final String entityId;

        public ToolCall(String tool, String entityId) {
            this.tool = tool;
            this.entityId = entityId;
        }
    }
}
