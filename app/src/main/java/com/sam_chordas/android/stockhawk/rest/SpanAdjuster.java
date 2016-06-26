package com.sam_chordas.android.stockhawk.rest;

import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

/**
 * Created by Arjun on 24-Jun-2016 for StockHawk.
 */
public class SpanAdjuster extends MetricAffectingSpan {
    double shift = 0.05;

    public SpanAdjuster() { }

    public SpanAdjuster(double shift) {
        this.shift = shift;
    }

    @Override
    public void updateMeasureState(TextPaint p) {
        p.baselineShift -= (int) (p.ascent() * shift);
        p.setTextSize(p.getTextSize() * 2.2f);
    }

    @Override
    public void updateDrawState(TextPaint tp) {
        tp.baselineShift -= (int) (tp.ascent() * shift);
        tp.setTextSize(tp.getTextSize() * 2.2f);
    }
}
