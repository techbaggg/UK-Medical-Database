package com.drgenz;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import android.util.Xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Streaming XML-to-SQLite importer for the NHSBSA dm+d main and supplementary releases. */
public final class UkDmDSqlite extends SQLiteOpenHelper {
    private static final String DB_NAME = "uk_dmd_local.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE = "dmd_records";
    private static final String[] RECORD_TAGS = {
            "AMP","AMPP","VMP","VMPP","ING","VTM","HISTORY","TF","TFG",
            "VTM_ING","BNF","ATC","DDD","VMP_BNF","VMP_ATC"
    };

    public UkDmDSqlite(Context c) { super(c, DB_NAME, null, DB_VERSION); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " (id INTEGER PRIMARY KEY AUTOINCREMENT, dataset TEXT, entity_type TEXT, code TEXT, name TEXT, description TEXT, fields TEXT)");
        db.execSQL("CREATE INDEX idx_dmd_name ON " + TABLE + "(name)");
        db.execSQL("CREATE INDEX idx_dmd_code ON " + TABLE + "(code)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE); onCreate(db);
    }

    public synchronized void replaceFromArchives(File mainZip, File supplementaryZip) throws Exception {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE, null, null);
            parseZip(db, mainZip, "main");
            parseZip(db, supplementaryZip, "supplementary");
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    private void parseZip(SQLiteDatabase db, File zip, String dataset) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().toLowerCase(Locale.ROOT);
                if (entry.isDirectory()) continue;
                if (name.endsWith(".xml") && !name.endsWith(".xsd")) {
                    parseXml(db, zis, dataset);
                } else if (name.endsWith(".zip")) {
                    // Nested BNF/GTIN archive: stage only this nested archive, then stream its XML entries.
                    File nested = File.createTempFile("dmd-nested-", ".zip", zip.getParentFile());
                    try (java.io.FileOutputStream out = new java.io.FileOutputStream(nested)) {
                        byte[] b = new byte[32768]; int n;
                        while ((n = zis.read(b)) != -1) out.write(b, 0, n);
                    }
                    try { parseZip(db, nested, dataset); } finally { //noinspection ResultOfMethodCallIgnored
                        nested.delete(); }
                }
                zis.closeEntry();
            }
        }
    }

    private void parseXml(SQLiteDatabase db, InputStream in, String dataset) throws Exception {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(in, "UTF-8");
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && isRecordTag(parser.getName())) {
                Map<String,String> fields = readRecord(parser);
                insert(db, dataset, parser.getName(), fields);
            }
            event = parser.next();
        }
    }

    private Map<String,String> readRecord(XmlPullParser p) throws Exception {
        String entity = p.getName();
        int depth = p.getDepth();
        Map<String,String> f = new HashMap<>();
        while (true) {
            int event = p.next();
            if (event == XmlPullParser.END_TAG && p.getDepth() == depth) break;
            if (event == XmlPullParser.START_TAG && p.getDepth() == depth + 1) {
                readFieldTree(p, f);
            }
            if (event == XmlPullParser.END_DOCUMENT) break;
        }
        f.put("_ENTITY", entity);
        return f;
    }

    /** Recursively flattens nested XML structures (for example AMPP/GTINDATA/GTIN) without DOM allocation. */
    private void readFieldTree(XmlPullParser p, Map<String,String> fields) throws Exception {
        String tag = p.getName();
        int depth = p.getDepth();
        StringBuilder value = new StringBuilder();
        boolean hasChild = false;
        while (true) {
            int event = p.next();
            if (event == XmlPullParser.TEXT || event == XmlPullParser.CDSECT) {
                String txt = p.getText();
                if (txt != null) value.append(txt.trim());
            } else if (event == XmlPullParser.START_TAG) {
                hasChild = true;
                readFieldTree(p, fields);
            } else if (event == XmlPullParser.END_TAG && p.getDepth() == depth) {
                break;
            } else if (event == XmlPullParser.END_DOCUMENT) break;
        }
        String v = value.toString().trim();
        if (!hasChild && !v.isEmpty()) {
            String old = fields.get(tag);
            fields.put(tag, old == null || old.isEmpty() ? v : old + "," + v);
        }
    }

    private boolean isRecordTag(String tag) {
        for (String t : RECORD_TAGS) if (t.equalsIgnoreCase(tag)) return true;
        return false;
    }

    private void insert(SQLiteDatabase db, String dataset, String type, Map<String,String> f) {
        String name = first(f, "NM", "DESC", "NAME");
        String description = first(f, "DESC", "NM", "NAME");
        String code = first(f, "APID","APPID","AMPPID","VPID","VPPID","ISID","VTMID","IDCURRENT","TFID","TFGID","ID","CODE");
        if (name.isEmpty()) name = type + " " + code;
        ContentValues v = new ContentValues();
        v.put("dataset", dataset); v.put("entity_type", type); v.put("code", code);
        v.put("name", name); v.put("description", description);
        v.put("fields", new JSONObject(f).toString());
        db.insert(TABLE, null, v);
    }

    private static String first(Map<String,String> f, String... keys) {
        for (String k : keys) { String v=f.get(k); if (v!=null && !v.trim().isEmpty()) return v.trim(); }
        return "";
    }

    public synchronized List<Map<String,String>> search(String query, int limit) {
        ArrayList<Map<String,String>> result = new ArrayList<>();
        String q = "%" + query.trim().replace("%","\\%").replace("_","\\_") + "%";
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT dataset,entity_type,code,name,description,fields FROM " + TABLE +
                        " WHERE name LIKE ? OR description LIKE ? OR code LIKE ? OR fields LIKE ? ORDER BY name LIMIT ?",
                new String[]{q,q,q,q,String.valueOf(limit)})) {
            while (c.moveToNext()) {
                Map<String,String> row = new HashMap<>();
                row.put("dataset",c.getString(0)); row.put("table",c.getString(1).toLowerCase(Locale.ROOT));
                row.put("code",c.getString(2)); row.put("name",c.getString(3));
                row.put("description",c.getString(4)); row.put("fields",c.getString(5));
                result.add(row);
            }
        }
        return result;
    }

    public boolean hasRecords() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + TABLE, null)) {
            return c.moveToFirst() && c.getLong(0) > 0;
        }
    }
}
