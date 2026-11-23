package com.drgenz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Ordering rule used by every list of medicines: tablets and capsules first, everything else (syrups, suspensions, injections,
 * creams ...) after them. The order inside each group is whatever the list had (best match, price ...), because the sort is stable.
 * Pure Java (unit-tested).
 */
public final class MedOrder {
    private MedOrder() { }

    private static final Pattern ABBREVIATION = Pattern.compile("\\b(tab|tabs|cap|caps)\\b");

    /** 0 for a tablet or a capsule (the text says tablet, capsule, caplet, tab or cap), 1 for everything else. */
    public static int formRank(String text) {
        if (text == null) return 1;
        String t = text.toLowerCase(Locale.ROOT);
        if (t.contains("tablet") || t.contains("capsule") || t.contains("caplet")) return 0;
        return ABBREVIATION.matcher(t).find() ? 0 : 1;
    }

    /** Puts tablets and capsules first, keeping the order inside each group. {@code text} gives what to look at (name, form ...). */
    public static <T> List<T> tabletsFirst(List<T> items, final Function<T, String> text) {
        List<T> r = new ArrayList<>(items);
        Collections.sort(r, new Comparator<T>() {            // List.sort / Collections.sort are stable
            @Override public int compare(T a, T b) { return Integer.compare(formRank(text.apply(a)), formRank(text.apply(b))); }
        });
        return r;
    }
}
