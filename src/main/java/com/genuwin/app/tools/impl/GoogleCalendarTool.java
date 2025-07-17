package com.genuwin.app.tools.impl;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.genuwin.app.tools.ToolDefinition;
import com.genuwin.app.tools.ToolExecutor;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GoogleCalendarTool implements ToolExecutor {
    private static final String TAG = "GoogleCalendarTool";

    private final Context context;
    private final OkHttpClient client;
    private String authToken;
    private String baseUrl;

    public GoogleCalendarTool(Context context) {
        this.context = context;
        this.client = new OkHttpClient();
        loadConfig();
    }

    private void loadConfig() {
        Properties properties = new Properties();
        try {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open("config.properties");
            properties.load(inputStream);
            this.authToken = properties.getProperty("AUDIO_API_KEY");
            this.baseUrl = properties.getProperty("GOOGLE_CALENDAR_BASE_URL");
            if (this.baseUrl == null || this.baseUrl.trim().isEmpty()) {
                Log.e(TAG, "GOOGLE_CALENDAR_BASE_URL not configured in config.properties");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load config", e);
        }
    }

    @Override
    public ToolDefinition getToolDefinition() {
        Map<String, ToolDefinition.ParameterDefinition> params = new HashMap<>();
        params.put("action", new ToolDefinition.ParameterDefinition("string", "The action to perform: list_events, create_event, update_event, delete_event, find_free_time", true));
        params.put("summary", new ToolDefinition.ParameterDefinition("string", "Event title", false));
        params.put("startTime", new ToolDefinition.ParameterDefinition("string", "Event start time (ISO string or relative keyword)", false));
        params.put("endTime", new ToolDefinition.ParameterDefinition("string", "Event end time (ISO string or relative keyword)", false));
        params.put("eventId", new ToolDefinition.ParameterDefinition("string", "ID of the event to update or delete", false));
        params.put("timeMin", new ToolDefinition.ParameterDefinition("string", "Start of time range (ISO string or relative keyword)", false));
        params.put("timeMax", new ToolDefinition.ParameterDefinition("string", "End of time range (ISO string or relative keyword)", false));
        params.put("duration", new ToolDefinition.ParameterDefinition("number", "Desired duration in minutes", false));
        return new ToolDefinition("google_calendar", "Manage Google Calendar events. Supports relative dates like 'today', 'this_week_start', 'this_week_end', 'weekend_start', 'weekend_end', 'this_month_start', 'this_month_end'.", params);
    }

    @Override
    public ValidationResult validateParameters(JsonObject parameters) {
        if (parameters == null || !parameters.has("action")) {
            return ValidationResult.invalid("Missing required parameter: action");
        }
        String action = parameters.get("action").getAsString();
        switch (action) {
            case "list_events":
                if (!parameters.has("timeMin") || !parameters.has("timeMax")) {
                    return ValidationResult.invalid("Missing required parameters for list_events: timeMin, timeMax");
                }
                break;
            case "create_event":
                if (!parameters.has("summary") || !parameters.has("startTime") || !parameters.has("endTime")) {
                    return ValidationResult.invalid("Missing required parameters for create_event: summary, startTime, endTime");
                }
                break;
            case "update_event":
                if (!parameters.has("eventId")) {
                    return ValidationResult.invalid("Missing required parameter for update_event: eventId");
                }
                break;
            case "delete_event":
                if (!parameters.has("eventId")) {
                    return ValidationResult.invalid("Missing required parameter for delete_event: eventId");
                }
                break;
            case "find_free_time":
                if (!parameters.has("timeMin") || !parameters.has("timeMax") || !parameters.has("duration")) {
                    return ValidationResult.invalid("Missing required parameters for find_free_time: timeMin, timeMax, duration");
                }
                break;
            default:
                return ValidationResult.invalid("Invalid action: " + action);
        }
        return ValidationResult.valid();
    }

    @Override
    public void execute(JsonObject parameters, ToolExecutionCallback callback) {
        if (authToken == null || authToken.isEmpty()) {
            callback.onError("Authorization token not found.");
            return;
        }

        String action = parameters.get("action").getAsString();
        String endpoint = "/" + action;

        // Handle relative dates
        if (parameters.has("timeMin")) {
            String timeMin = parameters.get("timeMin").getAsString();
            parameters.addProperty("timeMin", resolveDate(timeMin));
        }
        if (parameters.has("timeMax")) {
            String timeMax = parameters.get("timeMax").getAsString();
            parameters.addProperty("timeMax", resolveDate(timeMax));
        }
        if (parameters.has("startTime")) {
            String startTime = parameters.get("startTime").getAsString();
            parameters.addProperty("startTime", resolveDate(startTime));
        }
        if (parameters.has("endTime")) {
            String endTime = parameters.get("endTime").getAsString();
            parameters.addProperty("endTime", resolveDate(endTime));
        }

        new Thread(() -> {
            try {
                RequestBody body = RequestBody.create(
                    parameters.toString(),
                    MediaType.get("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(baseUrl + endpoint)
                        .post(body)
                        .addHeader("Authorization", "Bearer " + authToken)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        callback.onSuccess(responseBody);
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        Log.e(TAG, "API call failed with code " + response.code() + ": " + errorBody);
                        callback.onError("Failed to execute " + action + ": " + response.message());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error executing " + action, e);
                callback.onError("Error executing " + action + ": " + e.getMessage());
            }
        }).start();
    }

    private String resolveDate(String dateStr) {
        Calendar cal = Calendar.getInstance();
        switch (dateStr) {
            case "today":
                break;
            case "this_week_start":
                cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                break;
            case "this_week_end":
                cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                cal.add(Calendar.WEEK_OF_YEAR, 1);
                cal.add(Calendar.DAY_OF_YEAR, -1);
                break;
            case "weekend_start":
                cal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
                break;
            case "weekend_end":
                cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                cal.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case "this_month_start":
                cal.set(Calendar.DAY_OF_MONTH, 1);
                break;
            case "this_month_end":
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                break;
            default:
                return dateStr; // Assume it's already an ISO string
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(cal.getTime());
    }
}
