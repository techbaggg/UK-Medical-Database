package com.drgenz;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.*;
import java.util.*;

/**
 * UK Medical Database.
 * Direct launch with no login required.
 * Fully offline NHS dm+d (Dictionary of Medicines and Devices) reference browser.
 */
public class MainActivity extends Activity {
    int teal, navy;
    LinearLayout root, body; TextView title;
    static final int REQ_GOOGLE_SPEECH = 9301;
    int screenKind = 0;            // 0 main screen, 1 sub-screen
    Object backCb;                 // back callback (Android 13+), registered only on sub-screens

    static final String CONTACT_NAME = "Jagdish Krishanlal Arora";
    static final String CONTACT_MOBILE_SHOWN = "091-8446411131";
    static final String CONTACT_MOBILE_DIAL = "+918446411131";
    static final String CONTACT_EMAIL = "atlantisjagdish@gmail.com";
    static final String CONTACT_WEBSITE = "https://amazon.com/author/jagdisharora";
    static final String CONTACT_WEBSITE_INFO = "https://drgenz.info";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        try {
            teal = getColor(R.color.teal); navy = getColor(R.color.navy);
            askWarmTts();
            UkDmDManager.schedule(this);
            showHome();
        } catch (Throwable t) {
            android.util.Log.e("DrGenZ", "Error in UK onCreate", t);
            showHome();
        }
    }

    // ---------------- Main Screen ----------------
    void showHome() {
        shell("UK Medical Database"); setKind(0);

        // Header Branding with Logo
        LinearLayout brandBox = new LinearLayout(this);
        brandBox.setOrientation(LinearLayout.VERTICAL);
        brandBox.setGravity(Gravity.CENTER_HORIZONTAL);
        brandBox.setPadding(0, dp(4), 0, dp(8));

        ImageView logoView = new ImageView(this);
        logoView.setImageResource(R.drawable.drjk_logo);
        int logoSize = dp(72);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(logoSize, logoSize);
        logoLp.gravity = Gravity.CENTER_HORIZONTAL;
        logoView.setLayoutParams(logoLp);
        brandBox.addView(logoView);

        TextView headerTitle = tv("UK Medical Database", 22);
        headerTitle.setTypeface(null, 1);
        headerTitle.setGravity(Gravity.CENTER);
        headerTitle.setTextColor(teal);
        brandBox.addView(headerTitle);

        TextView subTitle = tv("Inbuilt NHS dm+d Medicines & Devices Reference", 14);
        subTitle.setGravity(Gravity.CENTER);
        subTitle.setTextColor(getColor(R.color.muted));
        brandBox.addView(subTitle);

        body.addView(brandBox);

        // Primary Search Navigation Tile
        int h = dp(isPhone() ? 120 : 138);
        LinearLayout row1 = new LinearLayout(this); row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.addView(menuTile("🇬🇧", "UK Medicines Database (NHS dm+d)", 0xFF283593, 0xFFFFFFFF, v -> ukMedicineDatabaseSearch()), new LinearLayout.LayoutParams(-1, h));
        body.addView(row1);

        // Help & About Combined Tile
        LinearLayout row2 = new LinearLayout(this); row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.addView(menuTile("ℹ️", "Help & About", 0xFF00838F, 0xFFFFFFFF, v -> helpAndAboutScreen()), new LinearLayout.LayoutParams(-1, dp(isPhone() ? 84 : 96)));
        body.addView(row2);

        // Feature overview
        body.addView(section("Search Features"));
        body.addView(tv("• UK NHS dm+d (Dictionary of Medicines and Devices) official terminology\n"
                + "• Virtual Medicinal Products (VMP) & Actual Medicinal Products (AMP)\n"
                + "• Packs (VMPP, AMPP), Ingredients & Virtual Therapeutic Moieties (VTM)\n"
                + "• Search by brand name, proprietary name, or active chemical substance\n"
                + "• Official NHS SNOMED CT identifiers and mapping codes\n"
                + "• Spoken or typed queries with Voice Assistant", 14));

        body.addView(section("Offline NHS Clinical Terminology"));
        body.addView(tv("All NHS dm+d records are pre-compiled and bundled inside the application. No internet connection is needed for catalogue lookups (internet is only required if using Google Voice Recognition).", 13));

        body.addView(section("Important Clinical Disclaimer"));
        body.addView(tv("The UK dm+d is a medicine terminology and identification resource, not a complete source of prescribing advice, dosage, side-effects, or contraindications. Missing data must never be interpreted as an absence of risk. Always check official BNF prescribing guidelines and consult a registered medical practitioner.", 13));

        body.addView(section("Developer & Contact Information"));
        body.addView(mainContactBox());
    }

    View mainContactBox() {
        LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_HORIZONTAL); c.setPadding(0, dp(8), 0, dp(12));

        TextView dev = tv("Built by: " + CONTACT_NAME, 15);
        dev.setTypeface(null, 1);
        dev.setGravity(Gravity.CENTER);
        c.addView(dev);

        c.addView(linkLine("Mobile: " + CONTACT_MOBILE_SHOWN, new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + CONTACT_MOBILE_DIAL))));
        c.addView(linkLine("Email: " + CONTACT_EMAIL, new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + CONTACT_EMAIL))
                .putExtra(Intent.EXTRA_SUBJECT, "UK Medical Database Inquiry")));
        c.addView(linkLine("Website: DrGenz.info", new Intent(Intent.ACTION_VIEW, Uri.parse(CONTACT_WEBSITE_INFO))));
        c.addView(linkLine("Author: " + CONTACT_WEBSITE, new Intent(Intent.ACTION_VIEW, Uri.parse(CONTACT_WEBSITE))));

        TextView copy = tv("© 2026 " + CONTACT_NAME + ". All rights reserved.", 13);
        copy.setGravity(Gravity.CENTER);
        copy.setPadding(0, dp(4), 0, dp(2));
        c.addView(copy);
        return c;
    }

    TextView linkLine(String text, final Intent intent) {
        TextView t = tv(text, 14); t.setGravity(Gravity.CENTER); t.setTextColor(0xFF00695C);
        t.setPaintFlags(t.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        t.setMinHeight(dp(isTvDevice() ? 60 : 48)); t.setFocusable(true);
        t.setOnClickListener(v -> { try { startActivity(intent); } catch (ActivityNotFoundException ex) { toast("No app found to open link"); } });
        return t;
    }

    // ---------------- Help & About Screen ----------------
    void helpAndAboutScreen() {
        shell("Help & About");
        body.addView(section("About UK Medical Database"));
        body.addView(tv("UK Medical Database is an offline clinical reference tool containing the official NHSBSA dm+d (Dictionary of Medicines and Devices) catalogue.", 14));

        body.addView(section("NHS dm+d Content"));
        body.addView(tv("• Actual Medicinal Product (AMP): Manufactured medicinal product available in the UK.\n"
                + "• Actual Medicinal Product Pack (AMPP): Specific pack size of an AMP.\n"
                + "• Virtual Medicinal Product (VMP): Generic title representing active ingredient and strength.\n"
                + "• Virtual Medicinal Product Pack (VMPP): Generic packaging specification.\n"
                + "• Ingredients & Virtual Therapeutic Moieties (VTM): Fundamental active ingredients.", 14));

        body.addView(section("Searching & Voice Assistant"));
        body.addView(tv("• Type at least 3 letters of a product or generic name and tap 'Search' or 'Type and Ask'.\n"
                + "• Tap the 🎙 Voice Assistant button to ask by voice hands-free.\n"
                + "• Select any result to view its NHS identifier code and complete entry details read aloud.", 14));

        body.addView(section("Important Disclaimer"));
        body.addView(tv("The catalogue does not supply complete clinical dosage, side-effect, contraindication or interaction information. It is provided for reference and educational use only.", 14));

        body.addView(section("Developer & Contact Information"));
        body.addView(mainContactBox());

        Button back = btn("Back to Main Screen"); body.addView(back); back.setOnClickListener(v -> showHome());
    }

    // ---------------- UK Medicines Database Search Screen ----------------
    EditText ukMedicineQueryBox;
    LinearLayout ukMedicineResults;

    void ukMedicineDatabaseSearch() {
        shell("UK Medicines Database");
        body.addView(section("UK NHS Medicines & Devices"));
        body.addView(tv("Search the offline NHS dm+d reference dictionary.", 14));
        body.addView(tv("Internet is required only for voice recognition.", 13));

        final EditText q = input("Search UK medicine / brand / ingredient (3+ letters)");
        ukMedicineQueryBox = q;
        body.addView(q);

        Button searchBtn = btn("Search Catalogue");
        body.addView(searchBtn);

        Button mic = btn("🎙 Voice Assistant: ask about a UK medicine");
        body.addView(mic);
        mic.setOnClickListener(v -> startVoiceInput("Which UK medicine would you like to look up?"));

        ukMedicineResults = new LinearLayout(this);
        ukMedicineResults.setOrientation(LinearLayout.VERTICAL);
        body.addView(ukMedicineResults);

        searchBtn.setOnClickListener(v -> ukMedicineSearch(q.getText().toString(), false));
        q.setOnEditorActionListener((v, action, event) -> { ukMedicineSearch(q.getText().toString(), false); return true; });

        Button back = btn("Back to Main Screen"); body.addView(back); back.setOnClickListener(v -> showHome());
    }

    void ukMedicineSearch(final String rawTerm, final boolean speakResults) {
        if (ukMedicineResults == null) return;
        final String term = rawTerm == null ? "" : rawTerm.trim();
        if (term.length() < 3) {
            toast("Please enter or say at least 3 letters.");
            return;
        }
        ukMedicineResults.removeAllViews();
        ukMedicineResults.addView(tv("Searching UK NHS dm+d catalogue…", 13));

        catalogExec.execute(() -> {
            List<Map<String, String>> hits = UkDmDManager.search(MainActivity.this, term, 60);
            postUi(() -> {
                if (isFinishing() || isDestroyed() || ukMedicineResults == null) return;
                ukMedicineResults.removeAllViews();
                if (hits.isEmpty()) {
                    String noMatch = "No matching UK dm+d entries found for \"" + term + "\".";
                    ukMedicineResults.addView(tv(noMatch, 14));
                    if (speakResults) askSay(noMatch);
                    return;
                }
                List<Map<String, String>> orderedHits = MedOrder.tabletsFirst(hits, h -> h.getOrDefault("name", "") + " " + h.getOrDefault("description", ""));
                ukMedicineResults.addView(tv("Found " + orderedHits.size() + " matches. Showing 10 at a time; tap Show More for additional records.", 12));
                List<java.util.function.Supplier<View>> resultRows = new ArrayList<>();
                for (Map<String, String> hit : orderedHits) {
                    final String name = hit.getOrDefault("name", "");
                    final String desc = hit.getOrDefault("description", "");
                    final String table = hit.getOrDefault("table", "");
                    final String code = hit.getOrDefault("code", "");
                    final String detail = "Entry type: " + UkDmDManager.tableLabel(table)
                            + "\nNHS Code: " + code
                            + (desc.isEmpty() ? "" : "\nDescription: " + desc)
                            + "\n\nNote: The NHS dm+d is a product-identification resource. Always check approved prescribing product information for complete clinical guidance.";
                    View.OnClickListener open = x -> {
                        AlertDialog dlg = new AlertDialog.Builder(MainActivity.this)
                                .setTitle(name).setMessage(detail).setPositiveButton("Close", null)
                                .setNeutralButton("Speak details", null).setOnDismissListener(d -> askStopAudio()).create();
                        dlg.show();
                        dlg.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> askSay(name + ". " + desc + ". Entry type: " + UkDmDManager.tableLabel(table)));
                    };
                    resultRows.add(() -> compactRow(name,
                            UkDmDManager.tableLabel(table) + (code.isEmpty() ? "" : " · " + code),
                            desc, open, miniAction("ℹ", "Details", open)));
                }
                addRowsPaged(ukMedicineResults, resultRows);
            });
        });
    }

    // ---------------- Voice Assistant ----------------
    void startVoiceInput(String prompt) {
        try {
            Intent intent = new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, Locale.UK);
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, prompt == null ? "Ask about a medicine" : prompt);
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            startActivityForResult(intent, REQ_GOOGLE_SPEECH);
        } catch (ActivityNotFoundException ex) { toast("Voice recognition is not available on this device."); }
    }

    @Override protected void onActivityResult(int r, int code, Intent data) {
        super.onActivityResult(r, code, data);
        if (r == REQ_GOOGLE_SPEECH && code == RESULT_OK && data != null) {
            ArrayList<String> values = data.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS);
            if (values != null && !values.isEmpty()) {
                String spoken = values.get(0);
                if (ukMedicineQueryBox != null) ukMedicineQueryBox.setText(spoken);
                ukMedicineSearch(spoken, true);
            }
        }
    }

    // ---------------- Voice Output (TTS) ----------------
    android.speech.tts.TextToSpeech askTts; boolean askTtsReady = false, askTtsFailed = false; String askPendingSpeech = null;

    void askWarmTts() {
        try {
            if (askTts != null || askTtsFailed) return;
            askTts = new android.speech.tts.TextToSpeech(getApplicationContext(), status -> {
                final android.speech.tts.TextToSpeech engine = askTts;
                if (engine == null) { askTtsReady = false; return; }
                askTtsReady = (status == android.speech.tts.TextToSpeech.SUCCESS);
                if (askTtsReady) {
                    int r = engine.setLanguage(Locale.UK);
                    if (r == android.speech.tts.TextToSpeech.LANG_MISSING_DATA || r == android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED)
                        engine.setLanguage(Locale.US);
                    engine.setSpeechRate(1.05f); engine.setPitch(1.0f);
                    if (askPendingSpeech != null) { String t = askPendingSpeech; askPendingSpeech = null; askSpeakNow(t); }
                } else { askTtsFailed = true; askPendingSpeech = null; }
            });
        } catch (Throwable ex) { askTtsFailed = true; }
    }

    void askSay(String text) {
        if (text == null || text.trim().isEmpty() || askTtsFailed) return;
        try {
            if (askTts == null) { askPendingSpeech = text; askWarmTts(); return; }
            if (!askTtsReady) { askPendingSpeech = text; return; }
            askSpeakNow(text);
        } catch (Throwable ex) { askTtsFailed = true; }
    }

    void askSpeakNow(String text) {
        try {
            ArrayList<String> parts = new ArrayList<>();
            String seg = text.trim(); int max = 3500, pos = 0;
            while (pos < seg.length()) {
                int end = Math.min(seg.length(), pos + max);
                if (end < seg.length()) { int sp = seg.lastIndexOf(' ', end); if (sp > pos) end = sp; }
                parts.add(seg.substring(pos, end)); pos = end;
            }
            if (parts.isEmpty()) return;
            askTts.stop();
            for (int n = 0; n < parts.size(); n++)
                askTts.speak(parts.get(n), n == 0 ? android.speech.tts.TextToSpeech.QUEUE_FLUSH : android.speech.tts.TextToSpeech.QUEUE_ADD, null, "uktts" + n);
        } catch (Throwable ex) { }
    }

    void askStopAudio() { askPendingSpeech = null; try { if (askTts != null) askTts.stop(); } catch (Throwable ignored) { } }

    // ---------------- Layout & View Utilities ----------------
    int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    boolean isPhone() { return getResources().getConfiguration().smallestScreenWidthDp < 600; }
    boolean isTvDevice() {
        return (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_TYPE_MASK) == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION;
    }

    TextView tv(String text, int sizeSp) {
        TextView t = new TextView(this); t.setText(text); t.setTextSize(sizeSp);
        t.setTextColor(getColor(R.color.ink)); t.setPadding(0, dp(4), 0, dp(4));
        return t;
    }

    TextView section(String title) {
        TextView t = new TextView(this); t.setText(title); t.setTextSize(17);
        t.setTextColor(teal); t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setPadding(0, dp(14), 0, dp(4)); return t;
    }

    Button btn(String text) {
        Button b = new Button(this); b.setText(text); b.setTextSize(15);
        b.setTextColor(Color.WHITE); b.setBackgroundColor(teal);
        GradientDrawable g = new GradientDrawable(); g.setColor(teal); g.setCornerRadius(dp(8));
        b.setBackground(g); b.setPadding(dp(12), dp(8), dp(12), dp(8));
        return b;
    }

    EditText input(String hint) {
        EditText e = new EditText(this); e.setHint(hint); e.setTextSize(15);
        e.setPadding(dp(10), dp(10), dp(10), dp(10));
        GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.WHITE);
        bg.setStroke(dp(1), getColor(R.color.border)); bg.setCornerRadius(dp(8));
        e.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(6), 0, dp(6)); e.setLayoutParams(lp);
        return e;
    }

    void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }

    void setKind(int kind) {
        screenKind = kind;
        syncBack();
    }

    void syncBack() {
        if (Build.VERSION.SDK_INT < 33) return;
        boolean need = !(screenKind == 0);
        if (need && backCb == null) backCb = BackCompat.register(this, this::showHome);
        else if (!need && backCb != null) { BackCompat.unregister(this, backCb); backCb = null; }
    }

    @Override public void onBackPressed() {
        if (screenKind == 0) super.onBackPressed(); else showHome();
    }

    void shell(String name) {
        screenKind = 1; syncBack();
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(navy); root.setFocusableInTouchMode(true);
        LinearLayout head = new LinearLayout(this); head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(dp(8), dp(6), dp(8), dp(6)); head.setBackgroundColor(navy);
        title = tv(name, 19); title.setTextColor(Color.WHITE); title.setTypeface(null, 1); title.setGravity(Gravity.CENTER);
        title.setPadding(dp(4), dp(8), dp(4), dp(8)); head.addView(title);
        root.addView(head);
        ScrollView sv = new ScrollView(this); sv.setBackgroundColor(getColor(R.color.paper)); sv.setFillViewport(true);
        body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(8), dp(8), dp(8), dp(24)); sv.addView(body);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
        
        root.setOnApplyWindowInsetsListener((view, ins) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = ins.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                android.graphics.Insets ime = ins.getInsets(WindowInsets.Type.ime());
                view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
                return WindowInsets.CONSUMED;
            }
            return ins;
        });
        setContentView(root);
    }

    TextView miniAction(String glyph, String label, View.OnClickListener click) {
        TextView t = new TextView(this); t.setText(glyph + " " + label); t.setTextSize(10); t.setGravity(Gravity.CENTER); t.setTextColor(getColor(R.color.ink)); t.setPadding(dp(5), dp(2), dp(5), dp(2)); t.setFocusable(true); t.setMinHeight(dp(isTvDevice() ? 60 : 48)); t.setTextSize(isTvDevice() ? 16 : 10); t.setOnClickListener(click); return t;
    }

    View compactRow(String titleText, String line1, String line2, View.OnClickListener open, View... actions) {
        LinearLayout outer = new LinearLayout(this); outer.setOrientation(LinearLayout.VERTICAL); outer.setPadding(dp(4), dp(5), dp(4), dp(5));
        GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.WHITE); bg.setStroke(dp(1), getColor(R.color.border)); bg.setCornerRadius(dp(10)); outer.setBackground(bg);
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout info = new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL); info.setPadding(dp(7), 0, dp(4), 0);
        TextView a = tv(titleText, 17); a.setTypeface(null, 1); info.addView(a); info.addView(tv(line1, 13)); if (line2 != null && !line2.isEmpty()) info.addView(tv(line2, 12));
        row.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout acts = new LinearLayout(this); acts.setOrientation(LinearLayout.HORIZONTAL); acts.setGravity(Gravity.CENTER_VERTICAL);
        for (View v : actions) acts.addView(v, new LinearLayout.LayoutParams(dp(isTvDevice() ? 72 : 46), dp(isTvDevice() ? 64 : 48)));
        row.addView(acts); row.setFocusable(true); row.setOnClickListener(open); outer.addView(row); return outer;
    }

    View menuTile(String icon, String label, int color, int textColor, View.OnClickListener l) {
        LinearLayout t = new LinearLayout(this); t.setOrientation(LinearLayout.VERTICAL); t.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(16)); g.setStroke(dp(1), 0xFFDCE5EF);
        android.graphics.drawable.RippleDrawable rp = new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x220F172A), g, null);
        t.setBackground(new android.graphics.drawable.InsetDrawable(rp, dp(4), dp(4), dp(4), dp(4))); t.setElevation(dp(1));
        TextView ic = new TextView(this); ic.setText(icon); ic.setTextSize(30); ic.setGravity(Gravity.CENTER);
        TextView tx = new TextView(this); tx.setText(label); tx.setTextSize(15); tx.setTypeface(null, android.graphics.Typeface.BOLD); tx.setTextColor(textColor); tx.setGravity(Gravity.CENTER); tx.setPadding(dp(4), dp(2), dp(4), 0);
        if (isTvDevice()) { ic.setTextSize(44); tx.setTextSize(22); }
        t.addView(ic); t.addView(tx); t.setClickable(true); t.setFocusable(true); t.setOnClickListener(l);
        t.setOnFocusChangeListener((view, focused) -> {
            GradientDrawable state = new GradientDrawable();
            state.setColor(color); state.setCornerRadius(dp(16)); state.setStroke(dp(focused ? 4 : 1), focused ? Color.WHITE : 0xFFDCE5EF); t.setBackground(state);
        });
        return t;
    }

    // ---------------- Background Execution ----------------
    final java.util.concurrent.ExecutorService catalogExec = java.util.concurrent.Executors.newSingleThreadExecutor();

    void postUi(Runnable action) {
        runOnUiThread(action);
    }

    void addRowsPaged(final LinearLayout list, final List<java.util.function.Supplier<View>> rows) {
        final int[] shown = {0};
        final Button more = btn("Show More");
        final Runnable add = () -> {
            list.removeView(more);
            int to = Math.min(rows.size(), shown[0] + 10);
            for (int i = shown[0]; i < to; i++) list.addView(rows.get(i).get());
            shown[0] = to;
            if (shown[0] < rows.size()) list.addView(more);
        };
        more.setOnClickListener(v -> add.run());
        add.run();
    }

    @Override protected void onStop() {
        super.onStop(); askStopAudio();
    }

    @Override protected void onDestroy() {
        try { catalogExec.shutdownNow(); } catch (Throwable ignored) { }
        if (askTts != null) { try { askTts.shutdown(); } catch (Throwable ignored) { } }
        super.onDestroy();
    }
}
