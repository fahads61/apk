package com.brand.expensetracker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

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
    private static final int REQ_NOTIFICATIONS = 203;

    private static final int BG = Color.rgb(10, 14, 29);
    private static final int SURFACE = Color.rgb(18, 24, 45);
    private static final int SURFACE_2 = Color.rgb(24, 32, 58);
    private static final int BORDER = Color.rgb(42, 53, 84);
    private static final int TEXT = Color.rgb(245, 248, 255);
    private static final int MUTED = Color.rgb(145, 157, 190);
    private static final int BLUE = Color.rgb(76, 128, 255);
    private static final int CYAN = Color.rgb(137, 234, 255);
    private static final int GREEN = Color.rgb(86, 216, 159);
    private static final int RED = Color.rgb(255, 101, 124);

    private ExpenseDb db;
    private LinearLayout content;
    private final NumberFormat money = NumberFormat.getNumberInstance(new Locale("en", "PK"));
    private String currentPage = "home";
    private String dashboardScope = "All";
    private String addScope = "Brand";
    private String statsScope = "All";
    private String statsPeriod = "Daily";
    private String manageScope = "Brand";
    private String historyScope = "All";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        db = new ExpenseDb(this);
        money.setMaximumFractionDigits(0);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        if (Build.VERSION.SDK_INT >= 23) getWindow().getDecorView().setSystemUiVisibility(0);
        SummaryReceiver.schedule(this);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
        }
        showDashboard();
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }

    private GradientDrawable bordered(int color, int stroke, int radius) {
        GradientDrawable d = rounded(color, radius); d.setStroke(dp(1), stroke); return d;
    }

    private GradientDrawable heroBg() {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.rgb(28, 42, 79), Color.rgb(16, 23, 47), Color.rgb(31, 29, 66)});
        d.setCornerRadius(dp(24)); d.setStroke(dp(1), Color.rgb(51, 69, 115)); return d;
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(sp); t.setTextColor(TEXT);
        t.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setLineSpacing(0, 1.08f);
        return t;
    }

    private TextView muted(String value, int sp) { TextView t = text(value, sp, false); t.setTextColor(MUTED); return t; }

    private void buildShell(String title, String subtitle, String page) {
        currentPage = page;
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL); shell.setBackgroundColor(BG);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(18), dp(18), dp(18), dp(26));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        shell.addView(bottomNav(), new LinearLayout.LayoutParams(-1, dp(72)));
        setContentView(shell);

        LinearLayout top = new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titles = new LinearLayout(this); titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(text(title, 27, true));
        TextView sub = muted(subtitle, 13); LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1,-2); sp.setMargins(0,dp(4),0,0); titles.addView(sub, sp);
        top.addView(titles, new LinearLayout.LayoutParams(0,-2,1));
        TextView badge = text("● OFFLINE", 10, true); badge.setTextColor(CYAN); badge.setGravity(Gravity.CENTER); badge.setBackground(bordered(Color.rgb(15,39,55), Color.rgb(38,92,105), 14)); badge.setPadding(dp(10),dp(7),dp(10),dp(7));
        top.addView(badge);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1,-2); tp.setMargins(0,0,0,dp(18)); content.addView(top,tp);
    }

    private LinearLayout bottomNav() {
        LinearLayout nav = new LinearLayout(this); nav.setOrientation(LinearLayout.HORIZONTAL); nav.setGravity(Gravity.CENTER); nav.setPadding(dp(6),dp(6),dp(6),dp(8)); nav.setBackground(bordered(Color.rgb(12,17,34), BORDER, 0));
        addNav(nav,"⌂\nHome","home", v -> showDashboard());
        addNav(nav,"＋\nAdd","add", v -> showAdd());
        addNav(nav,"⌁\nStats","stats", v -> showStats());
        addNav(nav,"⚙\nManage","manage", v -> showManage());
        addNav(nav,"≡\nHistory","history", v -> showHistory());
        return nav;
    }

    private void addNav(LinearLayout nav, String label, String key, View.OnClickListener click) {
        TextView v = text(label, 11, key.equals(currentPage)); v.setGravity(Gravity.CENTER); v.setTextColor(key.equals(currentPage) ? CYAN : MUTED); v.setBackground(key.equals(currentPage) ? rounded(Color.rgb(24,39,70),16) : null); v.setPadding(dp(2),dp(6),dp(2),dp(6)); v.setOnClickListener(click);
        nav.addView(v,new LinearLayout.LayoutParams(0,-1,1));
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(16),dp(15),dp(16),dp(15)); c.setBackground(bordered(SURFACE,BORDER,20));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,0,0,dp(12)); content.addView(c,p); return c;
    }

    private LinearLayout cardInto(LinearLayout parent) {
        LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(14),dp(13),dp(14),dp(13)); c.setBackground(bordered(SURFACE,BORDER,18)); return c;
    }

    private Button action(String label, boolean primary) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(14); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD); b.setTextColor(primary ? Color.WHITE : TEXT); b.setBackground(primary ? rounded(BLUE,16) : bordered(SURFACE_2,BORDER,16)); b.setMinHeight(dp(50)); b.setPadding(dp(12),0,dp(12),0); return b;
    }

    private TextView section(String title, String right) {
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = text(title,18,true); row.addView(t,new LinearLayout.LayoutParams(0,-2,1));
        TextView r = muted(right == null ? "" : right,12); r.setGravity(Gravity.END); row.addView(r);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,dp(8),0,dp(10)); content.addView(row,p); return r;
    }

    private EditText input(String hint) {
        EditText e = new EditText(this); e.setHint(hint); e.setHintTextColor(Color.rgb(104,116,148)); e.setTextColor(TEXT); e.setTextSize(15); e.setSingleLine(true); e.setPadding(dp(14),dp(12),dp(14),dp(12)); e.setBackground(bordered(Color.rgb(14,20,39),BORDER,14)); return e;
    }

    private Spinner spinner(String[] values) {
        Spinner s = new Spinner(this); s.setBackground(bordered(Color.rgb(14,20,39),BORDER,14)); s.setPadding(dp(8),0,dp(8),0); s.setAdapter(new DarkSpinnerAdapter(values)); return s;
    }

    private class DarkSpinnerAdapter extends ArrayAdapter<String> {
        DarkSpinnerAdapter(String[] items) { super(MainActivity.this, android.R.layout.simple_spinner_item, items); }
        @Override public View getView(int position, View convertView, ViewGroup parent) { return row(position,false); }
        @Override public View getDropDownView(int position, View convertView, ViewGroup parent) { return row(position,true); }
        private View row(int pos, boolean drop) { TextView v = text(getItem(pos),15,false); v.setPadding(dp(14),dp(13),dp(14),dp(13)); v.setBackgroundColor(drop ? SURFACE_2 : Color.TRANSPARENT); return v; }
    }

    private void addLabeled(LinearLayout parent, String label, View view) {
        TextView l = muted(label.toUpperCase(Locale.ENGLISH),11); l.setTypeface(Typeface.DEFAULT,Typeface.BOLD); parent.addView(l);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, view instanceof Spinner ? dp(54) : -2); p.setMargins(0,dp(7),0,dp(14)); parent.addView(view,p);
    }

    private LinearLayout chipRow(String[] labels, String selected, ChipClick click) {
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        for (String label : labels) {
            TextView v = text(label,13,label.equals(selected)); v.setGravity(Gravity.CENTER); v.setTextColor(label.equals(selected)?Color.WHITE:MUTED); v.setBackground(label.equals(selected)?rounded(BLUE,14):bordered(SURFACE,BORDER,14)); v.setPadding(dp(10),dp(10),dp(10),dp(10)); v.setOnClickListener(x -> click.onClick(label));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,-2,1); p.setMargins(0,0,dp(7),0); row.addView(v,p);
        }
        return row;
    }

    private interface ChipClick { void onClick(String value); }

    private String rs(double value) { return "Rs " + money.format(value); }

    private void showDashboard() {
        buildShell("Expense OS", "Brand + personal spending in one private app", "home");
        LinearLayout scopes = chipRow(new String[]{"All","Brand","Personal"},dashboardScope, value -> { dashboardScope=value; showDashboard(); });
        LinearLayout.LayoutParams scp = new LinearLayout.LayoutParams(-1,-2); scp.setMargins(0,0,0,dp(14)); content.addView(scopes,scp);

        long now=System.currentTimeMillis(), monthStart=ExpenseDb.startOfMonth(now), nextMonth=ExpenseDb.addMonths(monthStart,1), prevMonth=ExpenseDb.addMonths(monthStart,-1);
        double month=db.total(dashboardScope,monthStart,nextMonth), prev=db.total(dashboardScope,prevMonth,monthStart);
        LinearLayout hero=new LinearLayout(this); hero.setOrientation(LinearLayout.VERTICAL); hero.setPadding(dp(20),dp(20),dp(20),dp(18)); hero.setBackground(heroBg());
        TextView lab=muted("THIS MONTH",11); lab.setTypeface(Typeface.DEFAULT,Typeface.BOLD); hero.addView(lab);
        TextView amount=text(rs(month),34,true); LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2); ap.setMargins(0,dp(7),0,dp(7)); hero.addView(amount,ap);
        String change=prev<=0?"Start tracking to see month-on-month trend":String.format(Locale.ENGLISH,"%s%.0f%% vs last month",month>=prev?"▲ ":"▼ ",Math.abs((month-prev)/prev*100));
        TextView trend=muted(change,12); trend.setTextColor(month>=prev?GREEN:CYAN); hero.addView(trend);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2); hp.setMargins(0,0,0,dp(12)); content.addView(hero,hp);

        LinearLayout metrics=new LinearLayout(this); metrics.setOrientation(LinearLayout.HORIZONTAL);
        long day=ExpenseDb.startOfDay(now); double today=db.total(dashboardScope,day,ExpenseDb.addDays(day,1)); double seven=db.total(dashboardScope,ExpenseDb.addDays(day,-6),ExpenseDb.addDays(day,1));
        metric(metrics,"TODAY",rs(today),CYAN); metric(metrics,"7 DAYS",rs(seven),GREEN);
        LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,-2); mp.setMargins(0,0,0,dp(16)); content.addView(metrics,mp);

        section("Spending pulse","Last 7 days");
        LinearLayout chartCard=card(); ExpenseChartView chart=new ExpenseChartView(this); chart.setData(db.dailyLabels(7),db.dailySeries(dashboardScope,7)); chartCard.addView(chart,new LinearLayout.LayoutParams(-1,dp(205)));

        String monthLabel=new SimpleDateFormat("yyyy-MM",Locale.ENGLISH).format(new Date()); int[] ops=db.latestMonthlyOrders(monthLabel);
        section("Orders & returns","Manual business snapshot");
        LinearLayout opsCard=card();
        LinearLayout opRow=new LinearLayout(this); opRow.setOrientation(LinearLayout.HORIZONTAL);
        miniStat(opRow,"ORDERS",String.valueOf(ops[0]),BLUE); miniStat(opRow,"RETURNS",String.valueOf(ops[1]),RED); double rr=ops[0]>0?(ops[1]*100.0/ops[0]):0; miniStat(opRow,"RETURN RATE",String.format(Locale.ENGLISH,"%.1f%%",rr),CYAN); opsCard.addView(opRow);
        Button upd=action(ops[0]==0?"Add this month's orders":"Update operations report",false); LinearLayout.LayoutParams up=new LinearLayout.LayoutParams(-1,-2); up.setMargins(0,dp(12),0,0); opsCard.addView(upd,up); upd.setOnClickListener(v->showOperationsDialog(0,null,null,0,0,0,""));

        section("Top categories","This month");
        Map<String,Double> cats=db.totalsByCategory(dashboardScope,monthStart,nextMonth);
        if(cats.isEmpty()){ LinearLayout e=card(); e.addView(muted("No expenses this month yet. Tap Add to record your first one.",14)); }
        else { int i=0; for(Map.Entry<String,Double> en:cats.entrySet()){ categoryBar(en.getKey(),en.getValue(),month); if(++i>=5)break; } }

        section("Recent expenses","Latest 5");
        Cursor c=db.recentExpenses(dashboardScope,5); if(!c.moveToFirst()){ LinearLayout e=card();e.addView(muted("Your latest expenses will appear here.",14)); } else { do { addExpenseSummaryCard(c,false); } while(c.moveToNext()); } c.close();
    }

    private void metric(LinearLayout parent,String label,String value,int accent){
        LinearLayout c=cardInto(parent); TextView l=muted(label,10); l.setTypeface(Typeface.DEFAULT,Typeface.BOLD); c.addView(l); TextView v=text(value,18,true); v.setTextColor(accent); LinearLayout.LayoutParams vp=new LinearLayout.LayoutParams(-1,-2);vp.setMargins(0,dp(6),0,0);c.addView(v,vp); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(0,0,dp(7),0);parent.addView(c,p);
    }

    private void miniStat(LinearLayout parent,String label,String value,int accent){ LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(4),dp(4),dp(4),dp(4));TextView l=muted(label,9);l.setGravity(Gravity.CENTER);box.addView(l);TextView v=text(value,18,true);v.setTextColor(accent);v.setGravity(Gravity.CENTER);box.addView(v);parent.addView(box,new LinearLayout.LayoutParams(0,-2,1)); }

    private void categoryBar(String name,double value,double total){
        LinearLayout c=card(); LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);TextView n=text(name,14,true);row.addView(n,new LinearLayout.LayoutParams(0,-2,1));TextView a=text(rs(value),14,true);a.setTextColor(CYAN);row.addView(a);c.addView(row);
        View track=new View(this);track.setBackground(rounded(Color.rgb(31,40,67),5));LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,dp(7));tp.setMargins(0,dp(11),0,0);c.addView(track,tp);
        View fill=new View(this);fill.setBackground(rounded(BLUE,5));int width=(int)(Math.max(0.04,Math.min(1,total<=0?0:value/total))*280);LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(dp(width),dp(7));fp.setMargins(0,-dp(7),0,0);c.addView(fill,fp);
    }

    private void showAdd(){
        buildShell("Add expense","Date is automatic. Choose Brand or Personal.","add");
        LinearLayout scopes=chipRow(new String[]{"Brand","Personal"},addScope,v->{addScope=v;showAdd();});LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.setMargins(0,0,0,dp(14));content.addView(scopes,sp);
        LinearLayout form=card();
        TextView date=text(new SimpleDateFormat("EEE, dd MMM yyyy",Locale.ENGLISH).format(new Date()),14,true);date.setTextColor(CYAN);form.addView(date);
        String[] cats=db.getCategories(addScope); if(cats.length==0)cats=new String[]{"Miscellaneous"};
        Spinner category=spinner(cats); Spinner sub=spinner(db.getSubcategories(addScope,cats[0]));
        addLabeled(form,"Category",category); addLabeled(form,"Subcategory / item",sub);
        category.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?>p,View v,int pos,long id){String cat=String.valueOf(category.getSelectedItem());sub.setAdapter(new DarkSpinnerAdapter(db.getSubcategories(addScope,cat)));}public void onNothingSelected(AdapterView<?>p){}});
        EditText desc=input("e.g. 10kg Kaju, TikTok campaign, boxes"); addLabeled(form,"Description",desc);
        EditText amount=input("0");amount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);addLabeled(form,"Amount (PKR)",amount);
        Button save=action("Save expense",true);form.addView(save);save.setOnClickListener(v->{String val=amount.getText().toString().trim();if(val.isEmpty()){amount.setError("Amount required");return;}double d;try{d=Double.parseDouble(val);}catch(Exception ex){amount.setError("Invalid amount");return;}if(d<=0){amount.setError("Enter amount greater than 0");return;}long ts=System.currentTimeMillis();String dt=new SimpleDateFormat("yyyy-MM-dd",Locale.ENGLISH).format(new Date(ts));db.addExpense(ts,dt,addScope,String.valueOf(category.getSelectedItem()),String.valueOf(sub.getSelectedItem()),desc.getText().toString().trim(),d);Toast.makeText(this,"Expense saved",Toast.LENGTH_SHORT).show();showDashboard();});
        Button manage=action("Can't find an item? Manage categories",false);LinearLayout.LayoutParams m=new LinearLayout.LayoutParams(-1,-2);m.setMargins(0,dp(10),0,0);form.addView(manage,m);manage.setOnClickListener(v->{manageScope=addScope;showManage();});
        if(addScope.equals("Brand")){section("Manufacturing tip",null);LinearLayout tip=card();tip.addView(muted("For ingredients/meva, choose Manufacturing → Kaju, Badam, Pista, Chia Seeds etc. You can add any missing ingredient from Manage.",13));}
    }

    private void showStats(){
        buildShell("Stats","Daily, weekly and monthly expense intelligence","stats");
        LinearLayout scope=chipRow(new String[]{"All","Brand","Personal"},statsScope,v->{statsScope=v;showStats();});LinearLayout.LayoutParams p1=new LinearLayout.LayoutParams(-1,-2);p1.setMargins(0,0,0,dp(10));content.addView(scope,p1);
        LinearLayout period=chipRow(new String[]{"Daily","Weekly","Monthly"},statsPeriod,v->{statsPeriod=v;showStats();});LinearLayout.LayoutParams p2=new LinearLayout.LayoutParams(-1,-2);p2.setMargins(0,0,0,dp(14));content.addView(period,p2);
        double[] vals;String[] labels;long start,end=System.currentTimeMillis()+1;
        if(statsPeriod.equals("Weekly")){vals=db.weeklySeries(statsScope,8);labels=db.weeklyLabels(8);start=ExpenseDb.addDays(ExpenseDb.startOfDay(System.currentTimeMillis()),-55);}else if(statsPeriod.equals("Monthly")){vals=db.monthlySeries(statsScope,6);labels=db.monthlyLabels(6);start=ExpenseDb.addMonths(ExpenseDb.startOfMonth(System.currentTimeMillis()),-5);end=ExpenseDb.addMonths(ExpenseDb.startOfMonth(System.currentTimeMillis()),1);}else{vals=db.dailySeries(statsScope,7);labels=db.dailyLabels(7);start=ExpenseDb.addDays(ExpenseDb.startOfDay(System.currentTimeMillis()),-6);end=ExpenseDb.addDays(ExpenseDb.startOfDay(System.currentTimeMillis()),1);}
        double sum=0,max=0;for(double v:vals){sum+=v;max=Math.max(max,v);}LinearLayout metrics=new LinearLayout(this);metrics.setOrientation(LinearLayout.HORIZONTAL);metric(metrics,"TOTAL",rs(sum),CYAN);metric(metrics,"AVERAGE",rs(vals.length==0?0:sum/vals.length),GREEN);content.addView(metrics,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout hi=card();TextView l=muted("HIGHEST PERIOD",10);l.setTypeface(Typeface.DEFAULT,Typeface.BOLD);hi.addView(l);TextView hv=text(rs(max),24,true);hv.setTextColor(BLUE);hi.addView(hv);
        section("Trend",statsPeriod);LinearLayout chart=card();ExpenseChartView cv=new ExpenseChartView(this);cv.setData(labels,vals);chart.addView(cv,new LinearLayout.LayoutParams(-1,dp(230)));
        section("Category breakdown",statsPeriod+" view");Map<String,Double> map=db.totalsByCategory(statsScope,start,end);if(map.isEmpty()){LinearLayout e=card();e.addView(muted("No data in this period.",14));}else{int i=0;for(Map.Entry<String,Double> en:map.entrySet()){categoryBar(en.getKey(),en.getValue(),sum);if(++i>=8)break;}}
    }

    private void showManage(){
        buildShell("Management","CRUD categories, ingredients, reports & reminders","manage");
        LinearLayout scopes=chipRow(new String[]{"Brand","Personal"},manageScope,v->{manageScope=v;showManage();});LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.setMargins(0,0,0,dp(14));content.addView(scopes,sp);
        if(manageScope.equals("Brand")){
            section("Manufacturing ingredients","Quick management");LinearLayout ing=card();ing.addView(text("Meva / raw items",16,true));TextView desc=muted("Kaju, Badam, Pista, Chia Seeds and any custom ingredient you create.",13);LinearLayout.LayoutParams d=new LinearLayout.LayoutParams(-1,-2);d.setMargins(0,dp(5),0,dp(12));ing.addView(desc,d);Button b=action("Manage manufacturing ingredients",true);ing.addView(b);b.setOnClickListener(v->{long id=db.getCategoryId("Brand","Manufacturing");if(id>0)showSubcategoryManager(id,"Manufacturing");});
            section("Orders & returns","Weekly / monthly");LinearLayout op=card();Button add=action("+ Add operations report",true);op.addView(add);add.setOnClickListener(v->showOperationsDialog(0,null,null,0,0,0,""));Cursor r=db.periodReports(4);while(r.moveToNext()){long id=r.getLong(0);String type=r.getString(2),label=r.getString(3);int orders=r.getInt(4),returns=r.getInt(5);double cost=r.getDouble(6);String notes=r.getString(7);LinearLayout rr=new LinearLayout(this);rr.setOrientation(LinearLayout.HORIZONTAL);rr.setPadding(0,dp(11),0,0);TextView txt=muted(type+" • "+label+"\n"+orders+" orders • "+returns+" returns • "+rs(cost),12);rr.addView(txt,new LinearLayout.LayoutParams(0,-2,1));Button edit=action("Edit",false);edit.setMinHeight(dp(38));rr.addView(edit,new LinearLayout.LayoutParams(dp(72),dp(42)));edit.setOnClickListener(v->showOperationsDialog(id,type,label,orders,returns,cost,notes));op.addView(rr);}r.close();
        }
        section("Categories & subcategories",manageScope);
        Button addCat=action("+ Create category",true);LinearLayout.LayoutParams acp=new LinearLayout.LayoutParams(-1,-2);acp.setMargins(0,0,0,dp(12));content.addView(addCat,acp);addCat.setOnClickListener(v->promptCategoryAdd());
        Cursor c=db.categoryRows(manageScope);while(c.moveToNext()){long id=c.getLong(0);String name=c.getString(1);LinearLayout item=card();LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);LinearLayout title=new LinearLayout(this);title.setOrientation(LinearLayout.VERTICAL);title.addView(text(name,15,true));title.addView(muted(subCount(id)+" subcategories",11));row.addView(title,new LinearLayout.LayoutParams(0,-2,1));TextView manage=text("Manage  ›",12,true);manage.setTextColor(CYAN);manage.setPadding(dp(10),dp(8),0,dp(8));row.addView(manage);manage.setOnClickListener(v->showCategoryActions(id,name));item.addView(row);}c.close();
        section("Summary notifications","Local • 8:00 PM");toggleSetting("Daily summary","daily_reminder");toggleSetting("Weekly summary (Sunday)","weekly_reminder");toggleSetting("Month-end summary","monthly_reminder");
        section("Data safety","Stored only on this phone");LinearLayout backup=card();backup.addView(muted("Create a JSON backup before uninstalling the app or resetting/changing your phone.",13));Button bk=action("Backup / Restore",false);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);bp.setMargins(0,dp(12),0,0);backup.addView(bk,bp);bk.setOnClickListener(v->showBackupMenu());
    }

    private int subCount(long catId){Cursor c=db.subcategoryRows(catId);int n=c.getCount();c.close();return n;}

    private void toggleSetting(String title,String key){LinearLayout c=card();LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.addView(text(title,14,true),new LinearLayout.LayoutParams(0,-2,1));boolean on=db.getBoolSetting(key,true);Button b=action(on?"ON":"OFF",on);b.setMinHeight(dp(40));row.addView(b,new LinearLayout.LayoutParams(dp(78),dp(42)));c.addView(row);b.setOnClickListener(v->{db.setSetting(key,on?"0":"1");SummaryReceiver.schedule(this);showManage();});}

    private void showCategoryActions(long id,String name){
        new AlertDialog.Builder(this).setTitle(name).setItems(new String[]{"Manage subcategories","Rename category","Delete category"},(d,which)->{if(which==0)showSubcategoryManager(id,name);else if(which==1)promptRenameCategory(id,name);else confirmDeleteCategory(id,name);}).setNegativeButton("Close",null).show();
    }

    private void promptCategoryAdd(){EditText e=dialogInput("Category name");new AlertDialog.Builder(this).setTitle("Create "+manageScope+" category").setView(e).setNegativeButton("Cancel",null).setPositiveButton("Create",(d,w)->{String n=e.getText().toString().trim();if(!n.isEmpty()){long id=db.addCategory(manageScope,n);if(id==-1)Toast.makeText(this,"Category already exists",Toast.LENGTH_SHORT).show();showManage();}}).show();}
    private void promptRenameCategory(long id,String old){EditText e=dialogInput("Category name");e.setText(old);new AlertDialog.Builder(this).setTitle("Rename category").setView(e).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{String n=e.getText().toString().trim();if(!n.isEmpty())db.renameCategory(id,n);showManage();}).show();}
    private void confirmDeleteCategory(long id,String name){new AlertDialog.Builder(this).setTitle("Delete "+name+"?").setMessage("The category and its subcategory list will be removed. Existing expense records stay safe with their original text.").setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->{db.deleteCategory(id);showManage();}).show();}

    private EditText dialogInput(String hint){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(Color.rgb(112,124,154));e.setTextColor(TEXT);e.setBackground(bordered(Color.rgb(14,20,39),BORDER,12));e.setPadding(dp(14),dp(12),dp(14),dp(12));e.setSingleLine(true);return e;}

    private void showSubcategoryManager(long catId,String catName){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(8),dp(8),dp(8),dp(8));
        Button add=action("+ Add subcategory / item",true);box.addView(add);add.setOnClickListener(v->promptAddSub(catId,catName));
        Cursor c=db.subcategoryRows(catId);while(c.moveToNext()){long id=c.getLong(0);String name=c.getString(1);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(4),dp(7),dp(4),0);TextView n=text(name,14,false);row.addView(n,new LinearLayout.LayoutParams(0,-2,1));Button edit=new Button(this);edit.setText("Edit");edit.setAllCaps(false);row.addView(edit,new LinearLayout.LayoutParams(dp(74),dp(44)));Button del=new Button(this);del.setText("×");row.addView(del,new LinearLayout.LayoutParams(dp(52),dp(44)));edit.setOnClickListener(v->promptRenameSub(catId,catName,id,name));del.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Delete "+name+"?").setNegativeButton("Cancel",null).setPositiveButton("Delete",(x,y)->{db.deleteSubcategory(id);showSubcategoryManager(catId,catName);}).show());box.addView(row);}c.close();
        ScrollView scroll=new ScrollView(this);scroll.addView(box);new AlertDialog.Builder(this).setTitle(catName+" items").setView(scroll).setNegativeButton("Close",null).show();
    }
    private void promptAddSub(long catId,String catName){EditText e=dialogInput("e.g. Chia Seeds");new AlertDialog.Builder(this).setTitle("Add item to "+catName).setView(e).setNegativeButton("Cancel",null).setPositiveButton("Add",(d,w)->{String n=e.getText().toString().trim();if(!n.isEmpty())db.addSubcategory(catId,n);showSubcategoryManager(catId,catName);}).show();}
    private void promptRenameSub(long catId,String catName,long id,String old){EditText e=dialogInput("Item name");e.setText(old);new AlertDialog.Builder(this).setTitle("Rename item").setView(e).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{String n=e.getText().toString().trim();if(!n.isEmpty())db.renameSubcategory(id,n);showSubcategoryManager(catId,catName);}).show();}

    private void showOperationsDialog(long id,String currentType,String currentLabel,int currentOrders,int currentReturns,double currentCost,String currentNotes){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(4),dp(18),dp(2));Spinner type=spinner(new String[]{"Monthly","Weekly"});if("Weekly".equals(currentType))type.setSelection(1);EditText label=dialogInput("Period label");if(currentLabel!=null)label.setText(currentLabel);else label.setText(new SimpleDateFormat("yyyy-MM",Locale.ENGLISH).format(new Date()));EditText orders=dialogInput("Total orders");orders.setInputType(InputType.TYPE_CLASS_NUMBER);if(currentOrders>0)orders.setText(String.valueOf(currentOrders));EditText returns=dialogInput("Returned orders");returns.setInputType(InputType.TYPE_CLASS_NUMBER);if(currentReturns>0)returns.setText(String.valueOf(currentReturns));EditText cost=dialogInput("Return cost (PKR) - optional");cost.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);if(currentCost>0)cost.setText(String.valueOf(currentCost));EditText notes=dialogInput("Notes - optional");if(currentNotes!=null)notes.setText(currentNotes);
        addDialogField(box,"Period type",type);addDialogField(box,"Label",label);addDialogField(box,"Total orders",orders);addDialogField(box,"Returns",returns);addDialogField(box,"Return cost",cost);addDialogField(box,"Notes",notes);
        AlertDialog dlg=new AlertDialog.Builder(this).setTitle(id==0?"Add operations report":"Edit operations report").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        dlg.setOnShowListener(x->dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{int o=parseInt(orders.getText().toString()),r=parseInt(returns.getText().toString());double rc=parseDouble(cost.getText().toString());String l=label.getText().toString().trim();if(l.isEmpty()){label.setError("Label required");return;}if(r>o&&o>0){returns.setError("Returns cannot exceed orders");return;}if(id==0)db.addPeriodReport(String.valueOf(type.getSelectedItem()),l,o,r,rc,notes.getText().toString().trim());else db.updatePeriodReport(id,String.valueOf(type.getSelectedItem()),l,o,r,rc,notes.getText().toString().trim());dlg.dismiss();Toast.makeText(this,"Operations report saved",Toast.LENGTH_SHORT).show();if(currentPage.equals("manage"))showManage();else showDashboard();}));
        dlg.show();
    }

    private void addDialogField(LinearLayout p,String label,View v){TextView l=new TextView(this);l.setText(label);l.setTextColor(MUTED);l.setTextSize(12);p.addView(l);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,v instanceof Spinner?dp(52):-2);lp.setMargins(0,dp(3),0,dp(10));p.addView(v,lp);}
    private int parseInt(String s){try{return Integer.parseInt(s.trim());}catch(Exception e){return 0;}}private double parseDouble(String s){try{return Double.parseDouble(s.trim());}catch(Exception e){return 0;}}

    private void showHistory(){
        buildShell("History","Edit or delete any saved expense","history");LinearLayout scopes=chipRow(new String[]{"All","Brand","Personal"},historyScope,v->{historyScope=v;showHistory();});LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.setMargins(0,0,0,dp(14));content.addView(scopes,sp);
        Cursor c=db.allExpenses(historyScope);if(!c.moveToFirst()){LinearLayout e=card();e.addView(muted("No expenses found.",14));}else{do{addExpenseSummaryCard(c,true);}while(c.moveToNext());}c.close();
    }

    private void addExpenseSummaryCard(Cursor c,boolean actions){
        long id=c.getLong(0);String date=c.getString(2),scope=c.getString(3),cat=c.getString(4),sub=c.getString(5),desc=c.getString(6);double amt=c.getDouble(7);LinearLayout item=card();LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);TextView title=text(cat+(sub.isEmpty()?"":" • "+sub),14,true);row.addView(title,new LinearLayout.LayoutParams(0,-2,1));TextView amount=text(rs(amt),15,true);amount.setTextColor(scope.equals("Brand")?CYAN:GREEN);row.addView(amount);item.addView(row);TextView meta=muted(scope+"  •  "+date+(desc.isEmpty()?"":"  •  "+desc),11);LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,-2);mp.setMargins(0,dp(7),0,0);item.addView(meta,mp);if(actions){LinearLayout btns=new LinearLayout(this);btns.setOrientation(LinearLayout.HORIZONTAL);Button edit=action("Edit",false);Button del=action("Delete",false);LinearLayout.LayoutParams p1=new LinearLayout.LayoutParams(0,dp(44),1);p1.setMargins(0,dp(10),dp(5),0);btns.addView(edit,p1);LinearLayout.LayoutParams p2=new LinearLayout.LayoutParams(0,dp(44),1);p2.setMargins(dp(5),dp(10),0,0);btns.addView(del,p2);item.addView(btns);edit.setOnClickListener(v->editExpenseDialog(id,scope,cat,sub,desc,amt));del.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Delete expense?").setMessage(rs(amt)+" • "+cat).setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->{db.deleteExpense(id);showHistory();}).show());}}

    private void editExpenseDialog(long id,String scope,String cat,String sub,String desc,double amt){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(4),dp(18),dp(2));Spinner sc=spinner(new String[]{"Brand","Personal"});if(scope.equals("Personal"))sc.setSelection(1);Spinner category=spinner(db.getCategories(scope));setSpinnerTo(category,cat);Spinner subcat=spinner(db.getSubcategories(scope,cat));setSpinnerTo(subcat,sub);EditText d=dialogInput("Description");d.setText(desc);EditText a=dialogInput("Amount");a.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);a.setText(String.valueOf(amt));addDialogField(box,"Scope",sc);addDialogField(box,"Category",category);addDialogField(box,"Subcategory",subcat);addDialogField(box,"Description",d);addDialogField(box,"Amount",a);
        sc.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?>p,View v,int pos,long x){String s=String.valueOf(sc.getSelectedItem());String[] cats=db.getCategories(s);category.setAdapter(new DarkSpinnerAdapter(cats));if(cats.length>0)subcat.setAdapter(new DarkSpinnerAdapter(db.getSubcategories(s,cats[0])));}public void onNothingSelected(AdapterView<?>p){}});category.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?>p,View v,int pos,long x){String s=String.valueOf(sc.getSelectedItem()),ct=String.valueOf(category.getSelectedItem());subcat.setAdapter(new DarkSpinnerAdapter(db.getSubcategories(s,ct)));}public void onNothingSelected(AdapterView<?>p){}});
        AlertDialog dlg=new AlertDialog.Builder(this).setTitle("Edit expense").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();dlg.setOnShowListener(x->dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{double val=parseDouble(a.getText().toString());if(val<=0){a.setError("Amount required");return;}db.updateExpense(id,String.valueOf(sc.getSelectedItem()),String.valueOf(category.getSelectedItem()),String.valueOf(subcat.getSelectedItem()),d.getText().toString().trim(),val);dlg.dismiss();showHistory();}));dlg.show();
    }

    private void setSpinnerTo(Spinner s,String value){for(int i=0;i<s.getCount();i++){if(String.valueOf(s.getItemAtPosition(i)).equals(value)){s.setSelection(i);return;}}}

    private void showBackupMenu(){new AlertDialog.Builder(this).setTitle("Local backup").setMessage("Create a backup before uninstalling or changing phones. Restore replaces the current app data with your backup.").setPositiveButton("Create backup",(d,w)->createBackup()).setNeutralButton("Restore backup",(d,w)->restoreBackup()).setNegativeButton("Cancel",null).show();}
    private void createBackup(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"expense-os-backup.json");startActivityForResult(i,REQ_BACKUP);}
    private void restoreBackup(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/json");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,REQ_RESTORE);}

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();try{if(requestCode==REQ_BACKUP){String payload=db.exportAll().toString(2);OutputStream out=getContentResolver().openOutputStream(uri,"wt");if(out==null)throw new Exception("Cannot open file");out.write(payload.getBytes(StandardCharsets.UTF_8));out.close();Toast.makeText(this,"Backup saved",Toast.LENGTH_LONG).show();}else if(requestCode==REQ_RESTORE){BufferedReader br=new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri),StandardCharsets.UTF_8));StringBuilder sb=new StringBuilder();String line;while((line=br.readLine())!=null)sb.append(line);br.close();String raw=sb.toString().trim();if(raw.startsWith("[")){int n=db.importLegacy(new JSONArray(raw));Toast.makeText(this,n+" legacy expenses restored",Toast.LENGTH_LONG).show();}else{int n=db.importAll(new JSONObject(raw),true);Toast.makeText(this,n+" expenses restored",Toast.LENGTH_LONG).show();}showDashboard();}}catch(Exception ex){Toast.makeText(this,"Backup error: "+ex.getMessage(),Toast.LENGTH_LONG).show();}}

    @Override public void onBackPressed(){if(!currentPage.equals("home"))showDashboard();else super.onBackPressed();}
}
