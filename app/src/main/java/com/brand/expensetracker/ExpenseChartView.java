package com.brand.expensetracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;

public class ExpenseChartView extends View {
    private double[] values = new double[0];
    private String[] labels = new String[0];
    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);

    public ExpenseChartView(Context context) {
        super(context);
        grid.setColor(Color.rgb(38, 47, 74));
        grid.setStrokeWidth(dp(1));
        line.setColor(Color.rgb(74, 126, 255));
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(dp(3));
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeJoin(Paint.Join.ROUND);
        dot.setColor(Color.rgb(153, 238, 255));
        dot.setStyle(Paint.Style.FILL);
        text.setColor(Color.rgb(139, 151, 184));
        text.setTextSize(dp(11));
        text.setTextAlign(Paint.Align.CENTER);
        setMinimumHeight((int)dp(190));
    }

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }

    public void setData(String[] labels, double[] values) {
        this.labels = labels == null ? new String[0] : labels;
        this.values = values == null ? new double[0] : values;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float left = dp(14), right = w - dp(14), top = dp(18), bottom = h - dp(32);
        for (int i = 0; i < 4; i++) {
            float y = top + (bottom - top) * i / 3f;
            canvas.drawLine(left, y, right, y, grid);
        }
        if (values.length == 0) return;
        double max = 1;
        for (double v : values) max = Math.max(max, v);
        float step = values.length <= 1 ? 0 : (right - left) / (values.length - 1f);
        Path p = new Path();
        Path area = new Path();
        for (int i = 0; i < values.length; i++) {
            float x = left + step * i;
            float y = (float) (bottom - (values[i] / max) * (bottom - top));
            if (i == 0) { p.moveTo(x, y); area.moveTo(x, bottom); area.lineTo(x, y); }
            else { p.lineTo(x, y); area.lineTo(x, y); }
        }
        float endX = left + step * (values.length - 1);
        area.lineTo(endX, bottom);
        area.close();
        fill.setShader(new LinearGradient(0, top, 0, bottom,
                new int[]{Color.argb(105, 74, 126, 255), Color.argb(0, 74, 126, 255)}, null, Shader.TileMode.CLAMP));
        canvas.drawPath(area, fill);
        canvas.drawPath(p, line);
        for (int i = 0; i < values.length; i++) {
            float x = left + step * i;
            float y = (float) (bottom - (values[i] / max) * (bottom - top));
            canvas.drawCircle(x, y, dp(i == values.length - 1 ? 4.5f : 2.6f), dot);
            if (labels.length > i) canvas.drawText(labels[i], x, h - dp(9), text);
        }
    }
}
