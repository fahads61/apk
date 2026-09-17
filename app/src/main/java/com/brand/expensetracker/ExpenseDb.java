package com.brand.expensetracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class ExpenseDb extends SQLiteOpenHelper {
    private static final String DB_NAME = "brand_expenses.db";
    private static final int DB_VERSION = 1;

    public static final String[] CATEGORIES = new String[]{
            "Packaging", "Manufacturing", "Raw Material", "Ads / Marketing",
            "Influencer", "Video / Content Creation", "Shipping / Courier",
            "Staff / Labor", "Website / Software", "Samples / Giveaways",
            "Returns / Refunds", "Office / Utilities", "Miscellaneous"
    };

    public ExpenseDb(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE expenses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "created_at INTEGER NOT NULL," +
                "date_text TEXT NOT NULL," +
                "category TEXT NOT NULL," +
                "description TEXT NOT NULL," +
                "amount REAL NOT NULL" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }

    public long add(long createdAt, String dateText, String category, String description, double amount) {
        ContentValues v = new ContentValues();
        v.put("created_at", createdAt);
        v.put("date_text", dateText);
        v.put("category", category);
        v.put("description", description);
        v.put("amount", amount);
        return getWritableDatabase().insert("expenses", null, v);
    }

    public Cursor all() {
        return getReadableDatabase().rawQuery(
                "SELECT id, created_at, date_text, category, description, amount FROM expenses ORDER BY created_at DESC, id DESC",
                null
        );
    }

    public void delete(long id) {
        getWritableDatabase().delete("expenses", "id=?", new String[]{String.valueOf(id)});
    }

    public double total() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COALESCE(SUM(amount),0) FROM expenses", null);
        double t = 0;
        if (c.moveToFirst()) t = c.getDouble(0);
        c.close();
        return t;
    }

    public Map<String, Double> totalsByCategory() {
        LinkedHashMap<String, Double> out = new LinkedHashMap<>();
        for (String cat : CATEGORIES) out.put(cat, 0.0);
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT category, COALESCE(SUM(amount),0) FROM expenses GROUP BY category ORDER BY category", null);
        while (c.moveToNext()) out.put(c.getString(0), c.getDouble(1));
        c.close();
        return out;
    }

    public JSONArray exportJson() throws Exception {
        JSONArray a = new JSONArray();
        Cursor c = all();
        while (c.moveToNext()) {
            JSONObject o = new JSONObject();
            o.put("created_at", c.getLong(1));
            o.put("date_text", c.getString(2));
            o.put("category", c.getString(3));
            o.put("description", c.getString(4));
            o.put("amount", c.getDouble(5));
            a.put(o);
        }
        c.close();
        return a;
    }

    public int importJson(JSONArray a) throws Exception {
        SQLiteDatabase db = getWritableDatabase();
        int count = 0;
        db.beginTransaction();
        try {
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                ContentValues v = new ContentValues();
                v.put("created_at", o.getLong("created_at"));
                v.put("date_text", o.getString("date_text"));
                v.put("category", o.getString("category"));
                v.put("description", o.optString("description", ""));
                v.put("amount", o.getDouble("amount"));
                if (db.insert("expenses", null, v) != -1) count++;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return count;
    }
}
