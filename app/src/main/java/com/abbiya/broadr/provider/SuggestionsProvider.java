package com.abbiya.broadr.provider;

import android.content.SearchRecentSuggestionsProvider;

/**
 * Created by seshachalam on 25/11/14.
 */
public class SuggestionsProvider extends SearchRecentSuggestionsProvider {
    public final static String AUTHORITY = SuggestionsProvider.class.getCanonicalName();
    public final static int MODE = DATABASE_MODE_QUERIES;

    public SuggestionsProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }
}
