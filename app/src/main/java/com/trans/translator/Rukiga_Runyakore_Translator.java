package com.trans.translator;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility helper that loads the Runyankore/Rukiga dictionary bundled in the assets and provides
 * simple lookups for both Runyankore/Rukiga to English as well as English to Runyankore/Rukiga.
 *
 * The dictionary content comes from the {@code rukiga_dictionary.txt} asset which is a text
 * version of the UNESCO Runyankore/Rukiga-English dictionary.
 */
public final class Rukiga_Runyakore_Translator {

    private static final String TAG = "RukigaTranslator";
    private static final String DICTIONARY_ASSET_NAME = "rukiga_dictionary.txt";

    private static final Pattern HEADWORD_TOKEN_PATTERN = Pattern.compile("[a-z'ʼ-]+\\d*");
    private static final Pattern WORD_BOUNDARY_TEMPLATE = Pattern.compile("\\b\\p{L}+\\b");

    private static final Map<String, List<String>> RUKIGA_TO_ENGLISH = new LinkedHashMap<>();
    private static final List<DictionaryEntry> ALL_ENTRIES = new ArrayList<>();

    private static volatile boolean isLoaded = false;
    private static volatile boolean isLoading = false;

    private Rukiga_Runyakore_Translator() {
        // Utility class.
    }

    /**
     * Starts loading the dictionary in a background thread. Subsequent calls are ignored once the
     * dictionary has been loaded.
     */
    public static void preload(@NonNull Context context) {
        if (isLoaded || isLoading) {
            return;
        }

        isLoading = true;
        final Context appContext = context.getApplicationContext();
        Thread loader = new Thread(() -> {
            try {
                ensureLoaded(appContext);
            } catch (IOException e) {
                Log.e(TAG, "Failed to preload Runyankore/Rukiga dictionary", e);
            } finally {
                isLoading = false;
            }
        }, "rukiga-dictionary-loader");
        loader.setDaemon(true);
        loader.start();
    }

    /**
     * Looks up an entry by Runyankore/Rukiga headword and returns the available English
     * definitions. If multiple senses exist, all of them are returned separated by blank lines.
     */
    @NonNull
    public static String translateRukigaToEnglish(@NonNull Context context, @NonNull String source) {
        if (source.trim().isEmpty()) {
            return "";
        }

        try {
            ensureLoaded(context.getApplicationContext());
        } catch (IOException e) {
            Log.e(TAG, "Dictionary lookup failed while loading asset", e);
            return "Dictionary not available";
        }

        String normalized = normalizeHeadword(source);
        if (normalized.isEmpty()) {
            return "Translation not found";
        }

        List<String> direct = RUKIGA_TO_ENGLISH.get(normalized);
        if (direct != null && !direct.isEmpty()) {
            return joinDefinitions(direct);
        }

        // Attempt prefix matches if exact lookup fails.
        List<String> suggestions = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : RUKIGA_TO_ENGLISH.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(normalized)) {
                suggestions.add(formatSuggestion(key, entry.getValue().get(0)));
                if (suggestions.size() >= 3) {
                    break;
                }
            }
        }

        if (!suggestions.isEmpty()) {
            return "Did you mean:\n" + joinDefinitions(suggestions);
        }

        return "Translation not found";
    }

    /**
     * Performs a reverse lookup by scanning the English definitions and returning up to five
     * candidate Runyankore/Rukiga headwords whose definitions mention the provided English term.
     */
    @NonNull
    public static List<DictionaryEntry> translateEnglishToRukiga(@NonNull Context context, @NonNull String source) {
        String trimmed = source.trim();
        if (trimmed.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            ensureLoaded(context.getApplicationContext());
        } catch (IOException e) {
            Log.e(TAG, "Dictionary reverse lookup failed while loading asset", e);
            return new ArrayList<>();
        }

        String search = trimmed.toLowerCase(Locale.ROOT);
        Pattern exactPattern = Pattern.compile("\\b" + Pattern.quote(search) + "\\b");

        List<DictionaryEntry> matches = new ArrayList<>();
        Set<String> seenHeadwords = new LinkedHashSet<>();

        for (DictionaryEntry entry : ALL_ENTRIES) {
            if (seenHeadwords.contains(entry.displayHeadword)) {
                continue;
            }

            if (search.length() >= 3) {
                if (!exactPattern.matcher(entry.definitionLower).find()) {
                    continue;
                }
            } else if (!entry.definitionLower.contains(search)) {
                continue;
            }

            matches.add(entry);
            seenHeadwords.add(entry.displayHeadword);

            if (matches.size() >= 5) {
                break;
            }
        }

        if (matches.isEmpty()) {
            // Try again using individual keywords to provide broader suggestions.
            List<String> keywords = extractMeaningfulKeywords(search);
            for (DictionaryEntry entry : ALL_ENTRIES) {
                if (seenHeadwords.contains(entry.displayHeadword)) {
                    continue;
                }

                if (definitionContainsAny(entry.definitionLower, keywords)) {
                    matches.add(entry);
                    seenHeadwords.add(entry.displayHeadword);
                    if (matches.size() >= 5) {
                        break;
                    }
                }
            }
        }

        return matches;
    }

    private static boolean definitionContainsAny(String definitionLower, List<String> keywords) {
        if (keywords.isEmpty()) {
            return false;
        }
        for (String keyword : keywords) {
            if (definitionLower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> extractMeaningfulKeywords(String search) {
        Matcher matcher = WORD_BOUNDARY_TEMPLATE.matcher(search);
        List<String> tokens = new ArrayList<>();
        while (matcher.find()) {
            String token = matcher.group().toLowerCase(Locale.ROOT);
            if (token.length() >= 3) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private static String joinDefinitions(List<String> definitions) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < definitions.size(); i++) {
            builder.append(definitions.get(i));
            if (i < definitions.size() - 1) {
                builder.append("\n\n");
            }
        }
        return builder.toString();
    }

    private static String formatSuggestion(String headword, String definition) {
        String sanitizedDefinition = definition.trim();
        if (sanitizedDefinition.length() > 220) {
            sanitizedDefinition = sanitizedDefinition.substring(0, 217) + "...";
        }
        return headword + " — " + sanitizedDefinition;
    }

    private static synchronized void ensureLoaded(@NonNull Context context) throws IOException {
        if (isLoaded) {
            return;
        }

        synchronized (RUKIGA_TO_ENGLISH) {
            if (isLoaded) {
                return;
            }
            loadDictionary(context);
            isLoaded = true;
        }
    }

    private static void loadDictionary(Context context) throws IOException {
        AssetManager assetManager = context.getAssets();

        try (InputStream inputStream = assetManager.open(DICTIONARY_ASSET_NAME);
             BufferedReader reader =
                     new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            String currentHeadword = null;
            StringBuilder definitionBuilder = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                if (looksLikeHeadword(trimmed)) {
                    // Save the previous entry.
                    if (currentHeadword != null && definitionBuilder.length() > 0) {
                        storeEntry(currentHeadword, definitionBuilder.toString());
                    }

                    HeadwordParts parts = extractHeadword(trimmed);
                    currentHeadword = parts.headword;
                    definitionBuilder = new StringBuilder(parts.definition);
                } else if (currentHeadword != null) {
                    if (definitionBuilder.length() > 0) {
                        definitionBuilder.append(' ');
                    }
                    definitionBuilder.append(trimmed);
                }
            }

            if (currentHeadword != null && definitionBuilder.length() > 0) {
                storeEntry(currentHeadword, definitionBuilder.toString());
            }
        }
    }

    private static boolean looksLikeHeadword(String line) {
        if (line.isEmpty()) {
            return false;
        }

        char first = line.charAt(0);
        if (!Character.isLowerCase(first)) {
            return false;
        }

        int spaceIndex = line.indexOf(' ');
        if (spaceIndex <= 0) {
            return false;
        }

        String token = line.substring(0, spaceIndex);
        return HEADWORD_TOKEN_PATTERN.matcher(token).matches();
    }

    private static HeadwordParts extractHeadword(String line) {
        String sanitized = line.replace("|", " ");
        sanitized = sanitized.replaceAll("\\s+", " ").trim();

        int firstSpace = sanitized.indexOf(' ');
        if (firstSpace == -1) {
            return new HeadwordParts(sanitized, "");
        }

        String headword = sanitized.substring(0, firstSpace);
        String definition = sanitized.substring(firstSpace + 1).trim();
        return new HeadwordParts(headword, definition);
    }

    private static void storeEntry(String rawHeadword, String rawDefinition) {
        String normalizedHeadword = normalizeHeadword(rawHeadword);
        if (normalizedHeadword.isEmpty()) {
            return;
        }

        String cleanedDefinition = cleanDefinition(rawDefinition);
        if (cleanedDefinition.isEmpty()) {
            return;
        }

        addDefinition(normalizedHeadword, cleanedDefinition);

        String baseHeadword = normalizedHeadword.replaceAll("\\d+$", "");
        if (!baseHeadword.equals(normalizedHeadword)) {
            addDefinition(baseHeadword, cleanedDefinition);
        }

        ALL_ENTRIES.add(new DictionaryEntry(rawHeadword.trim(), cleanedDefinition));
    }

    private static void addDefinition(String headword, String definition) {
        List<String> definitions =
                RUKIGA_TO_ENGLISH.computeIfAbsent(headword, key -> new ArrayList<>());
        if (!definitions.contains(definition)) {
            definitions.add(definition);
        }
    }

    private static String normalizeHeadword(String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        lower = lower.replace('ʼ', '\'');
        lower = lower.replaceAll("[^a-z'\\-\\d\\s]", "");
        lower = lower.replaceAll("\\s+", " ").trim();
        return lower;
    }

    private static String cleanDefinition(String definition) {
        String trimmed = definition.replace('ʼ', '\'').trim();
        trimmed = trimmed.replaceAll("\\s+", " ");
        return trimmed;
    }

    public static final class DictionaryEntry {
        public final String displayHeadword;
        public final String definition;
        public final String definitionLower;

        DictionaryEntry(String displayHeadword, String definition) {
            this.displayHeadword = displayHeadword;
            this.definition = definition;
            this.definitionLower = definition.toLowerCase(Locale.ROOT);
        }
    }

    private static final class HeadwordParts {
        final String headword;
        final String definition;

        HeadwordParts(String headword, String definition) {
            this.headword = headword;
            this.definition = definition;
        }
    }
}