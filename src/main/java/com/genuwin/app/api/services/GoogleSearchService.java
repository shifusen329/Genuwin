package com.genuwin.app.api.services;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Retrofit service interface for Google Custom Search API
 */
public interface GoogleSearchService {
    
    @GET(".")
    Call<GoogleSearchResponse> search(
        @Query("key") String apiKey,
        @Query("cx") String cseId,
        @Query("q") String query,
        @Query("num") int numResults
    );
    
    /**
     * Google Custom Search API response model
     */
    public static class GoogleSearchResponse {
        public SearchInformation searchInformation;
        public Item[] items;
        
        public static class SearchInformation {
            public String totalResults;
            public double searchTime;
        }
        
        public static class Item {
            public String title;
            public String link;
            public String snippet;
            public String displayLink;
            public String formattedUrl;
            public String htmlTitle;
            public String htmlSnippet;
            public PageMap pagemap;
            
            public static class PageMap {
                public MetaTag[] metatags;
                
                public static class MetaTag {
                    public String description;
                    public String title;
                }
            }
        }
    }
}
