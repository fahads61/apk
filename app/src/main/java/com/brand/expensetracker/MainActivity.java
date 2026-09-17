package com.brand.expensetracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int REQ_BACKUP = 201;
    private static final int REQ_RESTORE = 202;
    private ExpenseDb db;
    private LinearLayout root;
    private final NumberFormat money = NumberFormat.getNumberInstance(new Locale("en", "PK"));

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        db = new ExpenseDb(this);
        money.setMaximumFractionDigits(2);
        showDashboard();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(Color.rgb(25, 28, 36));
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setMinHeight(dp(50));
        return b;
    }

    private LinearLayout base(String title, String subtitle) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(20), dp(18), dp(28));
        root.setBackgroundColor(Color.rgb(247, 248, 250));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -1));
        setContentView(scroll);

        TextView h = text(title, 28, true);
        root.addView(h);
        TextView s = text(subtitle, 14, false);
        s.setTextColor(Color.rgb(105, 110, 122));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.setMargins(0, dp(6), 0, dp(18));
        root.addView(s, sp);
        return root;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(14), dp(16), dp(14));
        c.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 0, 0, dp(12));
        root.addView(c, p);
        return c;
    }

    private void showDashboard() {
        base("Brand Expenses", "Offline expense tracker • data stays on this phone");

        LinearLayout totalCard = card();
        TextView lab = text("TOTAL EXPENSES", 12, true);
        lab.setTextColor(Color.rgb(104, 109, 120));
        totalCard.addView(lab);
        TextView total = text("Rs " + money.format(db.total()), 30, true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
        tp.setMargins(0, dp(8), 0, 0);
        totalCard.addView(total, tp);

        Button add = button("+ Add Expense");
        add.setOnClickListener(v -> showAdd());
        root.addView(add);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.setMargins(0, dp(8), 0, dp(18));
        root.addView(row, rp);

        Button history = button("History");
        Button backup = button("Backup / Restore");
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, -2, 1);
        half.setMargins(0, 0, dp(4), 0);
        row.addView(history, half);
        LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, -2, 1);
        half2.setMargins(dp(4), 0, 0, 0);
        row.addView(backup, half2);
        history.setOnClickListener(v -> showHistory());
        backup.setOnClickListener(v -> showBackupMenu());

        TextView section = text("Category Summary", 20, true);
        LinearLayout.LayoutParams secp = new LinearLayout.LayoutParams(-1, -2);
        secp.setMargins(0, 0, 0, dp(10));
        root.addView(section, secp);

        for (Map.Entry<String, Double> e : db.totalsByCategory().entrySet()) {
            LinearLayout c = card();
            c.setOrientation(LinearLayout.HORIZONTAL);
            TextView name = text(e.getKey(), 15, false);
            TextView amt = text("Rs " + money.format(e.getValue()), 15, true);
            amt.setGravity(Gravity.END);
            c.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
            c.addView(amt, new LinearLayout.LayoutParams(-2, -2));
        }
    }

    private EditText field(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(16);
        e.setSingleLine(false);
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        e.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(6), 0, dp(14));
        root.addView(e, p);
        return e;
    }

    private void showAdd() {
        base("Add Expense", "Date will be saved automatically when you add the expense");

        TextView date = text("Today: " + new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(new Date()), 15, true);
        LinearLayout dc = card();
        dc.addView(date);

        TextView cl = text("Category", 14, true);
        root.addView(cl);
        Spinner category = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ExpenseDb.CATEGORIES);
        category.setAdapter(a);
        category.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, dp(52));
        cp.setMargins(0, dp(6), 0, dp(14));
        root.addView(category, cp);

        TextView dl = text("Description", 14, true);
        root.addView(dl);
        EditText desc = field("e.g. 500 boxes, campaign shoot, courier charges");

        TextView al = text("Amount (PKR)", 14, true);
        root.addView(al);
        EditText amount = field("0");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        Button save = button("Save Expense");
        root.addView(save);
        save.setOnClickListener(v -> {
            String d = desc.getText().toString().trim();
            String amt = amount.getText().toString().trim();
            if (amt.isEmpty()) {
                amount.setError("Amount required");
                return;
            }
            double val;
            try { val = Double.parseDouble(amt); }
            catch (Exception ex) { amount.setError("Invalid amount"); return; }
            if (val <= 0) { amount.setError("Enter amount greater than 0"); return; }
            long now = System.currentTimeMillis();
            String dt = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(new Date(now));
            db.add(now, dt, String.valueOf(category.getSelectedItem()), d, val);
            Toast.makeText(this, "Expense saved", Toast.LENGTH_SHORT).show();
            showDashboard();
        });

        Button cancel = button("Cancel");
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(8), 0, 0);
        root.addView(cancel, p);
        cancel.setOnClickListener(v -> showDashboard());
    }

    private void showHistory() {
        base("Expense History", "Tap Delete only when you want to remove an entry");
        Cursor c = db.all();
        if (!c.moveToFirst()) {
            LinearLayout empty = card();
            empty.addView(text("No expenses added yet.", 16, false));
        } else {
            do {
                long id = c.getLong(0);
                String date = c.getString(2);
                String cat = c.getString(3);
                String des = c.getString(4);
                double amt = c.getDouble(5);
                LinearLayout item = card();
                LinearLayout top = new LinearLayout(this);
                top.setOrientation(LinearLayout.HORIZONTAL);
                TextView ct = text(cat, 16, true);
                TextView av = text("Rs " + money.format(amt), 16, true);
                av.setGravity(Gravity.END);
                top.addView(ct, new LinearLayout.LayoutParams(0, -2, 1));
                top.addView(av);
                item.addView(top);
                TextView meta = text(date + (des.isEmpty() ? "" : "  •  " + des), 13, false);
                meta.setTextColor(Color.rgb(103, 108, 118));
                LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-1, -2);
                mp.setMargins(0, dp(7), 0, dp(8));
                item.addView(meta, mp);
                Button del = button("Delete");
                item.addView(del);
                del.setOnClickListener(v -> new AlertDialog.Builder(this)
                        .setTitle("Delete expense?")
                        .setMessage("This entry will be removed from this phone.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete", (d, w) -> { db.delete(id); showHistory(); })
                        .show());
            } while (c.moveToNext());
        }
        c.close();
        Button back = button("Back to Dashboard");
        root.addView(back);
        back.setOnClickListener(v -> showDashboard());
    }

    private void showBackupMenu() {
        new AlertDialog.Builder(this)
                .setTitle("Local Backup")
                .setMessage("Expenses already stay in the app on this phone. Create a backup file as extra protection before changing/resetting the phone or uninstalling the app.")
                .setPositiveButton("Create Backup", (d, w) -> createBackup())
                .setNeutralButton("Restore Backup", (d, w) -> restoreBackup())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createBackup() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE, "brand-expenses-backup.json");
        startActivityForResult(i, REQ_BACKUP);
    }

    private void restoreBackup() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("application/json");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i, REQ_RESTORE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            if (requestCode == REQ_BACKUP) {
                String payload = db.exportJson().toString(2);
                OutputStream out = getContentResolver().openOutputStream(uri, "wt");
                if (out == null) throw new Exception("Cannot open backup file");
                out.write(payload.getBytes(StandardCharsets.UTF_8));
                out.close();
                Toast.makeText(this, "Backup saved", Toast.LENGTH_LONG).show();
            } else if (requestCode == REQ_RESTORE) {
                BufferedReader br = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                JSONArray a = new JSONArray(sb.toString());
                int count = db.importJson(a);
                Toast.makeText(this, count + " expenses restored", Toast.LENGTH_LONG).show();
                showDashboard();
            }
        } catch (Exception ex) {
            Toast.makeText(this, "Backup error: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        showDashboard();
    }
}
