package com.drgenz;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns a UK NHS dm+d medicine record into natural sentences for text-to-speech reading.
 */
public final class MedSpeech {
    private MedSpeech() { }

    private static String nz(String s) { return s == null ? "" : s.trim(); }

    private static final Pattern MG = Pattern.compile("(\\d)\\s?mg\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern MCG = Pattern.compile("(\\d)\\s?(?:mcg|µg|ug)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern GM = Pattern.compile("(\\d)\\s?(?:gm|g)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ML = Pattern.compile("(\\d)\\s?ml\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern IU = Pattern.compile("(\\d)\\s?iu\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PCT = Pattern.compile("(\\d)\\s?%");
    private static final Pattern WORD_ML = Pattern.compile("\\bml\\b", Pattern.CASE_INSENSITIVE);

    /** Expands units so "amoxicillin 500mg capsule" is pronounced clearly. */
    public static String speakable(String s) {
        if (s == null) return "";
        String t = s.trim();
        t = MCG.matcher(t).replaceAll("$1 micrograms");
        t = MG.matcher(t).replaceAll("$1 milligrams");
        t = GM.matcher(t).replaceAll("$1 grams");
        t = ML.matcher(t).replaceAll("$1 millilitres");
        t = IU.matcher(t).replaceAll("$1 international units");
        t = PCT.matcher(t).replaceAll("$1 percent");
        t = WORD_ML.matcher(t).replaceAll("millilitres");
        t = t.replace("&", " and ").replace("+", " plus ").replace("/", " and ")
                .replace("(", ", ").replace(")", ", ").replace("*", " ").replace("_", " ").replace(";", ", ");
        t = t.replaceAll("\\s+", " ").replaceAll("\\s+,", ",").replaceAll(",\\s*,", ",").replaceAll("^[,\\s]+|[,\\s]+$", "");
        return t;
    }
}
