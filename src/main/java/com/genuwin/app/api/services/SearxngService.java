package com.genuwin.app.api.services;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

/**
 * Retrofit service interface for SearXNG API
 */
public interface SearxngService {
    
    /**
     * SearXNG search response structure
     */
    class SearxngResponse {
        public String query;
        public int number_of_results;
        public Result[] results;
        public String[] suggestions;
        public String[] answers;
        public String[] corrections;
        public Infobox[] infoboxes;
        
        public static class Result {
            public String title;
            public String content;
            public String url;
            public String pretty_url;
            public String engine;
            public String[] engines;
            public String[] positions;
            public double score;
            public String category;
            public String parsed_url[];
            public String template;
        }
        
        public static class Infobox {
            public String infobox;
            public String content;
            public String engine;
            public Url[] urls;
            
            public static class Url {
                public String title;
                public String url;
            }
        }
    }
    
    @GET("search/")
    Call<SearxngResponse> search(
            @Header("Authorization") String authorization,
            @Query("q") String query,
            @Query("format") String format,
            @Query("categories") String categories,
            @Query("engines") String engines,
            @Query("lang") String language,
            @Query("pageno") int pageNumber,
            @Query("time_range") String timeRange,
            @Query("safesearch") int safeSearch
    );
}
