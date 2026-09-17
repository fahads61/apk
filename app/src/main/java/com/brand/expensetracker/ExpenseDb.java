package com.brand.expensetracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExpenseDb extends SQLiteOpenHelper {
    private static final String DB_NAME = "brand_expenses.db";
    private static final int DB_VERSION = 4;

    public ExpenseDb(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createCoreTables(db);
        seedDefaults(db);
    }

    private void createCoreTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS expenses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "created_at INTEGER NOT NULL," +
                "date_text TEXT NOT NULL," +
                "scope TEXT NOT NULL DEFAULT 'Brand'," +
                "category TEXT NOT NULL," +
                "subcategory TEXT NOT NULL DEFAULT ''," +
                "description TEXT NOT NULL DEFAULT ''," +
                "amount REAL NOT NULL" +
                ")");
        db.execSQL("CREATE TABLE IF NOT EXISTS categories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "scope TEXT NOT NULL," +
                "name TEXT NOT NULL," +
                "sort_order INTEGER NOT NULL DEFAULT 0," +
                "is_system INTEGER NOT NULL DEFAULT 0," +
                "UNIQUE(scope,name)" +
                ")");
        db.execSQL("CREATE TABLE IF NOT EXISTS subcategories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "category_id INTEGER NOT NULL," +
                "name TEXT NOT NULL," +
                "sort_order INTEGER NOT NULL DEFAULT 0," +
                "is_system INTEGER NOT NULL DEFAULT 0," +
                "UNIQUE(category_id,name)" +
                ")");
        db.execSQL("CREATE TABLE IF NOT EXISTS period_reports (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "created_at INTEGER NOT NULL," +
                "period_type TEXT NOT NULL," +
                "period_label TEXT NOT NULL," +
                "total_orders INTEGER NOT NULL DEFAULT 0," +
                "returned_orders INTEGER NOT NULL DEFAULT 0," +
                "return_cost REAL NOT NULL DEFAULT 0," +
                "notes TEXT NOT NULL DEFAULT ''," +
                "expense_id INTEGER NOT NULL DEFAULT 0" +
                ")");
        db.execSQL("CREATE TABLE IF NOT EXISTS settings (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            try { db.execSQL("ALTER TABLE expenses ADD COLUMN scope TEXT NOT NULL DEFAULT 'Brand'"); } catch (Exception ignored) { }
            try { db.execSQL("ALTER TABLE expenses ADD COLUMN subcategory TEXT NOT NULL DEFAULT ''"); } catch (Exception ignored) { }
        }
        createCoreTables(db);
        seedDefaults(db);
    }

    private void seedDefaults(SQLiteDatabase db) {
        String[][] brand = new String[][]{
                {"Packaging", "Boxes|Pouches|Labels|Stickers|Bottles / Jars"},
                {"Manufacturing", "Cashew / Kaju|Almond / Badam|Pistachio / Pista|Walnut / Akhrot|Chia Seeds|Dates / Khajoor|Honey|Spices|Other Ingredient"},
                {"Ads", "TikTok Ads|Meta Ads|Google Ads|Other Ads"},
                {"Design", "Graphic Design|Packaging Design|Branding"},
                {"Influencers", "Paid Collaboration|Barter / Gift|UGC Creator"},
                {"Marketing", "Campaign|PR|Giveaway|Promotion"},
                {"Website / Software", "Hosting|Domain|Shopify / Store|Apps / Tools|Development"},
                {"Returns / Refunds", "Customer Refund|Return Shipping|Damaged Order"},
                {"Shipping / Courier", "Courier|Fulfilment|COD Charges"},
                {"Content / Video", "Video Production|Photography|Editing|Studio"},
                {"Staff / Labor", "Salary|Daily Wage|Freelancer"},
                {"Office / Utilities", "Rent|Electricity|Internet|Furniture|Stationery"},
                {"Samples / Giveaways", "Samples|PR Packages|Gifts"},
                {"Miscellaneous", "Other"}
        };
        String[][] personal = new String[][]{
                {"Food", "Dining|Groceries|Coffee / Snacks"},
                {"Travel", "Fuel|Ride / Taxi|Tickets|Hotel"},
                {"Shopping", "Clothing|Electronics|General"},
                {"Bills", "Mobile|Internet|Electricity|Subscriptions"},
                {"Health", "Medicine|Doctor|Fitness"},
                {"Family", "Home|Gifts|Support"},
                {"Personal", "Self Care|Education|Entertainment"},
                {"Miscellaneous", "Other"}
        };
        seedScope(db, "Brand", brand);
        seedScope(db, "Personal", personal);
        putSettingIfMissing(db, "daily_reminder", "1");
        putSettingIfMissing(db, "weekly_reminder", "1");
        putSettingIfMissing(db, "monthly_reminder", "1");
    }

    private void seedScope(SQLiteDatabase db, String scope, String[][] rows) {
        for (int i = 0; i < rows.length; i++) {
            ContentValues v = new ContentValues();
            v.put("scope", scope);
            v.put("name", rows[i][0]);
            v.put("sort_order", i);
            v.put("is_system", 1);
            db.insertWithOnConflict("categories", null, v, SQLiteDatabase.CONFLICT_IGNORE);
            long catId = getCategoryId(db, scope, rows[i][0]);
            if (catId <= 0) continue;
            String[] subs = rows[i][1].split("\\|");
            for (int j = 0; j < subs.length; j++) {
                ContentValues sv = new ContentValues();
                sv.put("category_id", catId);
                sv.put("name", subs[j]);
                sv.put("sort_order", j);
                sv.put("is_system", 1);
                db.insertWithOnConflict("subcategories", null, sv, SQLiteDatabase.CONFLICT_IGNORE);
            }
        }
    }

    private void putSettingIfMissing(SQLiteDatabase db, String key, String value) {
        ContentValues v = new ContentValues();
        v.put("key", key);
        v.put("value", value);
        db.insertWithOnConflict("settings", null, v, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public long addExpense(long createdAt, String dateText, String scope, String category, String subcategory, String description, double amount) {
        ContentValues v = new ContentValues();
        v.put("created_at", createdAt);
        v.put("date_text", dateText);
        v.put("scope", scope);
        v.put("category", category);
        v.put("subcategory", subcategory == null ? "" : subcategory);
        v.put("description", description == null ? "" : description);
        v.put("amount", amount);
        return getWritableDatabase().insert("expenses", null, v);
    }

    public void updateExpense(long id, String scope, String category, String subcategory, String description, double amount) {
        ContentValues v = new ContentValues();
        v.put("scope", scope);
        v.put("category", category);
        v.put("subcategory", subcategory == null ? "" : subcategory);
        v.put("description", description == null ? "" : description);
        v.put("amount", amount);
        getWritableDatabase().update("expenses", v, "id=?", new String[]{String.valueOf(id)});
    }

    public Cursor allExpenses(String scope) {
        if (scope == null || scope.equals("All")) {
            return getReadableDatabase().rawQuery("SELECT id,created_at,date_text,scope,category,subcategory,description,amount FROM expenses ORDER BY created_at DESC,id DESC", null);
        }
        return getReadableDatabase().rawQuery("SELECT id,created_at,date_text,scope,category,subcategory,description,amount FROM expenses WHERE scope=? ORDER BY created_at DESC,id DESC", new String[]{scope});
    }

    public Cursor recentExpenses(String scope, int limit) {
        String lim = String.valueOf(Math.max(1, limit));
        if (scope == null || scope.equals("All")) {
            return getReadableDatabase().rawQuery("SELECT id,created_at,date_text,scope,category,subcategory,description,amount FROM expenses ORDER BY created_at DESC,id DESC LIMIT " + lim, null);
        }
        return getReadableDatabase().rawQuery("SELECT id,created_at,date_text,scope,category,subcategory,description,amount FROM expenses WHERE scope=? ORDER BY created_at DESC,id DESC LIMIT " + lim, new String[]{scope});
    }

    public void deleteExpense(long id) {
        getWritableDatabase().delete("expenses", "id=?", new String[]{String.valueOf(id)});
    }

    public double total(String scope, long start, long end) {
        String where = "created_at>=? AND created_at<?";
        List<String> args = new ArrayList<>();
        args.add(String.valueOf(start));
        args.add(String.valueOf(end));
        if (scope != null && !scope.equals("All")) {
            where += " AND scope=?";
            args.add(scope);
        }
        Cursor c = getReadableDatabase().rawQuery("SELECT COALESCE(SUM(amount),0) FROM expenses WHERE " + where, args.toArray(new String[0]));
        double out = 0;
        if (c.moveToFirst()) out = c.getDouble(0);
        c.close();
        return out;
    }

    public double totalAll(String scope) {
        String sql = "SELECT COALESCE(SUM(amount),0) FROM expenses";
        String[] args = null;
        if (scope != null && !scope.equals("All")) {
            sql += " WHERE scope=?";
            args = new String[]{scope};
        }
        Cursor c = getReadableDatabase().rawQuery(sql, args);
        double out = 0;
        if (c.moveToFirst()) out = c.getDouble(0);
        c.close();
        return out;
    }

    public Map<String, Double> totalsByCategory(String scope, long start, long end) {
        LinkedHashMap<String, Double> out = new LinkedHashMap<>();
        String where = "created_at>=? AND created_at<?";
        List<String> args = new ArrayList<>();
        args.add(String.valueOf(start));
        args.add(String.valueOf(end));
        if (scope != null && !scope.equals("All")) {
            where += " AND scope=?";
            args.add(scope);
        }
        Cursor c = getReadableDatabase().rawQuery("SELECT category,COALESCE(SUM(amount),0) AS t FROM expenses WHERE " + where + " GROUP BY category ORDER BY t DESC", args.toArray(new String[0]));
        while (c.moveToNext()) out.put(c.getString(0), c.getDouble(1));
        c.close();
        return out;
    }

    public String[] getCategories(String scope) {
        List<String> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT name FROM categories WHERE scope=? ORDER BY sort_order,name", new String[]{scope});
        while (c.moveToNext()) list.add(c.getString(0));
        c.close();
        return list.toArray(new String[0]);
    }

    public Cursor categoryRows(String scope) {
        return getReadableDatabase().rawQuery("SELECT id,name,is_system FROM categories WHERE scope=? ORDER BY sort_order,name", new String[]{scope});
    }

    public long addCategory(String scope, String name) {
        ContentValues v = new ContentValues();
        v.put("scope", scope);
        v.put("name", name.trim());
        v.put("sort_order", 999);
        v.put("is_system", 0);
        return getWritableDatabase().insertWithOnConflict("categories", null, v, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public boolean renameCategory(long id, String newName) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery("SELECT scope,name FROM categories WHERE id=?", new String[]{String.valueOf(id)});
        if (!c.moveToFirst()) { c.close(); return false; }
        String scope = c.getString(0);
        String old = c.getString(1);
        c.close();
        ContentValues v = new ContentValues();
        v.put("name", newName.trim());
        int changed = db.update("categories", v, "id=?", new String[]{String.valueOf(id)});
        if (changed > 0) {
            ContentValues ev = new ContentValues();
            ev.put("category", newName.trim());
            db.update("expenses", ev, "scope=? AND category=?", new String[]{scope, old});
        }
        return changed > 0;
    }

    public void deleteCategory(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("subcategories", "category_id=?", new String[]{String.valueOf(id)});
        db.delete("categories", "id=?", new String[]{String.valueOf(id)});
    }

    public long getCategoryId(String scope, String name) {
        return getCategoryId(getReadableDatabase(), scope, name);
    }

    private long getCategoryId(SQLiteDatabase db, String scope, String name) {
        Cursor c = db.rawQuery("SELECT id FROM categories WHERE scope=? AND name=? LIMIT 1", new String[]{scope, name});
        long id = -1;
        if (c.moveToFirst()) id = c.getLong(0);
        c.close();
        return id;
    }

    public String[] getSubcategories(String scope, String category) {
        long id = getCategoryId(scope, category);
        if (id <= 0) return new String[]{"General"};
        List<String> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT name FROM subcategories WHERE category_id=? ORDER BY sort_order,name", new String[]{String.valueOf(id)});
        while (c.moveToNext()) list.add(c.getString(0));
        c.close();
        if (list.isEmpty()) list.add("General");
        return list.toArray(new String[0]);
    }

    public Cursor subcategoryRows(long categoryId) {
        return getReadableDatabase().rawQuery("SELECT id,name,is_system FROM subcategories WHERE category_id=? ORDER BY sort_order,name", new String[]{String.valueOf(categoryId)});
    }

    public long addSubcategory(long categoryId, String name) {
        ContentValues v = new ContentValues();
        v.put("category_id", categoryId);
        v.put("name", name.trim());
        v.put("sort_order", 999);
        v.put("is_system", 0);
        return getWritableDatabase().insertWithOnConflict("subcategories", null, v, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void renameSubcategory(long id, String newName) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery("SELECT s.name,c.scope,c.name FROM subcategories s JOIN categories c ON c.id=s.category_id WHERE s.id=?", new String[]{String.valueOf(id)});
        if (!c.moveToFirst()) { c.close(); return; }
        String old = c.getString(0);
        String scope = c.getString(1);
        String category = c.getString(2);
        c.close();
        ContentValues v = new ContentValues();
        v.put("name", newName.trim());
        if (db.update("subcategories", v, "id=?", new String[]{String.valueOf(id)}) > 0) {
            ContentValues ev = new ContentValues();
            ev.put("subcategory", newName.trim());
            db.update("expenses", ev, "scope=? AND category=? AND subcategory=?", new String[]{scope, category, old});
        }
    }

    public void deleteSubcategory(long id) {
        getWritableDatabase().delete("subcategories", "id=?", new String[]{String.valueOf(id)});
    }

    public long addPeriodReport(String type, String label, int orders, int returns, double returnCost, String notes) {
        SQLiteDatabase db = getWritableDatabase();
        long now = System.currentTimeMillis();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(new Date(now));
        long expenseId = 0;
        if (returnCost > 0) {
            expenseId = addExpense(now, date, "Brand", "Returns / Refunds", "Operations Return Cost", "Operations report • " + label, returnCost);
        }
        ContentValues v = new ContentValues();
        v.put("created_at", now);
        v.put("period_type", type);
        v.put("period_label", label);
        v.put("total_orders", orders);
        v.put("returned_orders", returns);
        v.put("return_cost", returnCost);
        v.put("notes", notes == null ? "" : notes);
        v.put("expense_id", expenseId);
        return db.insert("period_reports", null, v);
    }

    public void updatePeriodReport(long id, String type, String label, int orders, int returns, double returnCost, String notes) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery("SELECT expense_id FROM period_reports WHERE id=?", new String[]{String.valueOf(id)});
        long expenseId = 0;
        if (c.moveToFirst()) expenseId = c.getLong(0);
        c.close();
        long now = System.currentTimeMillis();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(new Date(now));
        if (returnCost > 0) {
            if (expenseId > 0) updateExpense(expenseId, "Brand", "Returns / Refunds", "Operations Return Cost", "Operations report • " + label, returnCost);
            else expenseId = addExpense(now, date, "Brand", "Returns / Refunds", "Operations Return Cost", "Operations report • " + label, returnCost);
        } else if (expenseId > 0) {
            deleteExpense(expenseId);
            expenseId = 0;
        }
        ContentValues v = new ContentValues();
        v.put("period_type", type);
        v.put("period_label", label);
        v.put("total_orders", orders);
        v.put("returned_orders", returns);
        v.put("return_cost", returnCost);
        v.put("notes", notes == null ? "" : notes);
        v.put("expense_id", expenseId);
        db.update("period_reports", v, "id=?", new String[]{String.valueOf(id)});
    }

    public void deletePeriodReport(long id) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery("SELECT expense_id FROM period_reports WHERE id=?", new String[]{String.valueOf(id)});
        long expenseId = 0;
        if (c.moveToFirst()) expenseId = c.getLong(0);
        c.close();
        db.delete("period_reports", "id=?", new String[]{String.valueOf(id)});
        if (expenseId > 0) deleteExpense(expenseId);
    }

    public Cursor periodReports(int limit) {
        return getReadableDatabase().rawQuery("SELECT id,created_at,period_type,period_label,total_orders,returned_orders,return_cost,notes FROM period_reports ORDER BY created_at DESC,id DESC LIMIT " + Math.max(1, limit), null);
    }

    public int[] latestMonthlyOrders(String monthLabel) {
        Cursor c = getReadableDatabase().rawQuery("SELECT total_orders,returned_orders FROM period_reports WHERE period_type='Monthly' AND period_label=? ORDER BY created_at DESC LIMIT 1", new String[]{monthLabel});
        int[] out = new int[]{0,0};
        if (c.moveToFirst()) { out[0] = c.getInt(0); out[1] = c.getInt(1); }
        c.close();
        return out;
    }

    public void setSetting(String key, String value) {
        ContentValues v = new ContentValues();
        v.put("key", key);
        v.put("value", value);
        getWritableDatabase().insertWithOnConflict("settings", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public String getSetting(String key, String def) {
        Cursor c = getReadableDatabase().rawQuery("SELECT value FROM settings WHERE key=?", new String[]{key});
        String out = def;
        if (c.moveToFirst()) out = c.getString(0);
        c.close();
        return out;
    }

    public boolean getBoolSetting(String key, boolean def) {
        return "1".equals(getSetting(key, def ? "1" : "0"));
    }

    public static long startOfDay(long time) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        c.set(Calendar.HOUR_OF_DAY,0); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0);
        return c.getTimeInMillis();
    }

    public static long startOfMonth(long time) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        c.set(Calendar.DAY_OF_MONTH,1); c.set(Calendar.HOUR_OF_DAY,0); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0);
        return c.getTimeInMillis();
    }

    public static long addDays(long time, int days) {
        Calendar c = Calendar.getInstance(); c.setTimeInMillis(time); c.add(Calendar.DAY_OF_YEAR,days); return c.getTimeInMillis();
    }

    public static long addMonths(long time, int months) {
        Calendar c = Calendar.getInstance(); c.setTimeInMillis(time); c.add(Calendar.MONTH,months); return c.getTimeInMillis();
    }

    public double[] dailySeries(String scope, int days) {
        double[] vals = new double[days];
        long startToday = startOfDay(System.currentTimeMillis());
        for (int i = 0; i < days; i++) {
            long s = addDays(startToday, -(days - 1 - i));
            vals[i] = total(scope, s, addDays(s,1));
        }
        return vals;
    }

    public String[] dailyLabels(int days) {
        String[] out = new String[days];
        long startToday = startOfDay(System.currentTimeMillis());
        SimpleDateFormat f = new SimpleDateFormat("EEE", Locale.ENGLISH);
        for (int i = 0; i < days; i++) out[i] = f.format(new Date(addDays(startToday, -(days - 1 - i))));
        return out;
    }

    public double[] weeklySeries(String scope, int weeks) {
        double[] vals = new double[weeks];
        long today = startOfDay(System.currentTimeMillis());
        Calendar cal = Calendar.getInstance(); cal.setTimeInMillis(today);
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        int delta = (dow + 5) % 7;
        long thisMonday = addDays(today, -delta);
        for (int i=0;i<weeks;i++) {
            long s = addDays(thisMonday, -7*(weeks-1-i));
            vals[i] = total(scope, s, addDays(s,7));
        }
        return vals;
    }

    public String[] weeklyLabels(int weeks) {
        String[] out = new String[weeks];
        for (int i=0;i<weeks;i++) out[i] = "W" + (i+1);
        return out;
    }

    public double[] monthlySeries(String scope, int months) {
        double[] vals = new double[months];
        long thisMonth = startOfMonth(System.currentTimeMillis());
        for (int i=0;i<months;i++) {
            long s = addMonths(thisMonth, -(months-1-i));
            vals[i] = total(scope, s, addMonths(s,1));
        }
        return vals;
    }

    public String[] monthlyLabels(int months) {
        String[] out = new String[months];
        long thisMonth = startOfMonth(System.currentTimeMillis());
        SimpleDateFormat f = new SimpleDateFormat("MMM", Locale.ENGLISH);
        for (int i=0;i<months;i++) out[i] = f.format(new Date(addMonths(thisMonth, -(months-1-i))));
        return out;
    }

    public JSONObject exportAll() throws Exception {
        JSONObject root = new JSONObject();
        root.put("version", DB_VERSION);
        JSONArray expenses = new JSONArray();
        Cursor e = allExpenses("All");
        while (e.moveToNext()) {
            JSONObject o = new JSONObject();
            o.put("created_at", e.getLong(1)); o.put("date_text", e.getString(2)); o.put("scope", e.getString(3));
            o.put("category", e.getString(4)); o.put("subcategory", e.getString(5)); o.put("description", e.getString(6)); o.put("amount", e.getDouble(7));
            expenses.put(o);
        }
        e.close();
        root.put("expenses", expenses);

        JSONArray cats = new JSONArray();
        Cursor c = getReadableDatabase().rawQuery("SELECT id,scope,name,sort_order,is_system FROM categories ORDER BY id", null);
        while (c.moveToNext()) {
            JSONObject o = new JSONObject(); o.put("old_id",c.getLong(0)); o.put("scope",c.getString(1)); o.put("name",c.getString(2)); o.put("sort_order",c.getInt(3)); o.put("is_system",c.getInt(4)); cats.put(o);
        }
        c.close(); root.put("categories",cats);

        JSONArray subs = new JSONArray();
        c = getReadableDatabase().rawQuery("SELECT s.category_id,c.scope,c.name,s.name,s.sort_order,s.is_system FROM subcategories s JOIN categories c ON c.id=s.category_id ORDER BY s.id", null);
        while (c.moveToNext()) {
            JSONObject o = new JSONObject(); o.put("scope",c.getString(1)); o.put("category",c.getString(2)); o.put("name",c.getString(3)); o.put("sort_order",c.getInt(4)); o.put("is_system",c.getInt(5)); subs.put(o);
        }
        c.close(); root.put("subcategories",subs);

        JSONArray reports = new JSONArray();
        c = getReadableDatabase().rawQuery("SELECT created_at,period_type,period_label,total_orders,returned_orders,return_cost,notes FROM period_reports ORDER BY id",null);
        while(c.moveToNext()){
            JSONObject o=new JSONObject(); o.put("created_at",c.getLong(0)); o.put("period_type",c.getString(1)); o.put("period_label",c.getString(2)); o.put("total_orders",c.getInt(3)); o.put("returned_orders",c.getInt(4)); o.put("return_cost",c.getDouble(5)); o.put("notes",c.getString(6)); reports.put(o);
        }
        c.close(); root.put("reports",reports);
        return root;
    }

    public int importAll(JSONObject root, boolean replace) throws Exception {
        SQLiteDatabase db = getWritableDatabase();
        if (replace) {
            db.delete("period_reports",null,null); db.delete("expenses",null,null); db.delete("subcategories",null,null); db.delete("categories",null,null); seedDefaults(db);
        }
        JSONArray cats = root.optJSONArray("categories");
        if (cats != null) for(int i=0;i<cats.length();i++){ JSONObject o=cats.getJSONObject(i); addCategory(o.optString("scope","Brand"),o.optString("name","Other")); }
        JSONArray subs = root.optJSONArray("subcategories");
        if (subs != null) for(int i=0;i<subs.length();i++){ JSONObject o=subs.getJSONObject(i); long cid=getCategoryId(o.optString("scope","Brand"),o.optString("category","Miscellaneous")); if(cid>0)addSubcategory(cid,o.optString("name","General")); }
        JSONArray expenses = root.optJSONArray("expenses");
        int count=0;
        if(expenses!=null) for(int i=0;i<expenses.length();i++){ JSONObject o=expenses.getJSONObject(i); long id=addExpense(o.optLong("created_at",System.currentTimeMillis()),o.optString("date_text",""),o.optString("scope","Brand"),o.optString("category","Miscellaneous"),o.optString("subcategory",""),o.optString("description",""),o.optDouble("amount",0)); if(id!=-1)count++; }
        JSONArray reports=root.optJSONArray("reports");
        if(reports!=null) for(int i=0;i<reports.length();i++){ JSONObject o=reports.getJSONObject(i); ContentValues v=new ContentValues(); v.put("created_at",o.optLong("created_at",System.currentTimeMillis())); v.put("period_type",o.optString("period_type","Monthly")); v.put("period_label",o.optString("period_label","Imported")); v.put("total_orders",o.optInt("total_orders",0)); v.put("returned_orders",o.optInt("returned_orders",0)); v.put("return_cost",o.optDouble("return_cost",0)); v.put("notes",o.optString("notes","")); v.put("expense_id",0); db.insert("period_reports",null,v); }
        return count;
    }

    public int importLegacy(JSONArray a) throws Exception {
        int count=0;
        for(int i=0;i<a.length();i++){
            JSONObject o=a.getJSONObject(i);
            long id=addExpense(o.optLong("created_at",System.currentTimeMillis()),o.optString("date_text",""),"Brand",o.optString("category","Miscellaneous"),"",o.optString("description",""),o.optDouble("amount",0));
            if(id!=-1)count++;
        }
        return count;
    }
}
