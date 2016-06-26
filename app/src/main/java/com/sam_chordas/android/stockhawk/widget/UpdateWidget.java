package com.sam_chordas.android.stockhawk.widget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;

import java.util.Calendar;
import java.util.Locale;

/**
 * Created by Arjun on 26-Jun-2016 for StockHawk.
 */
public class UpdateWidget {

    public static void updateWidget(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int appWidgetIds[] = appWidgetManager.getAppWidgetIds(new ComponentName(context, StockWidgetProvider.class));
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.stock_list);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.stock_widget_provider_layout);

        Calendar cal = Calendar.getInstance();
        String date = getFormattedNo(cal.get(Calendar.DATE)) + "/" + getFormattedNo(cal.get(Calendar.MONTH)) + " " + getFormattedNo(cal.get(Calendar.HOUR_OF_DAY)) + ":" + (cal.get(Calendar.MINUTE) < 10 ? getFormattedNo(0) + getFormattedNo(cal.get(Calendar.MINUTE)) : getFormattedNo(cal.get(Calendar.MINUTE)));
        views.setTextViewText(R.id.textWidDate, date);
        appWidgetManager.updateAppWidget(appWidgetIds, views);
        Log.d("Updating", "Updated at " + date);
    }

    private static String getFormattedNo(int no) {
        return String.format(Locale.getDefault() ,"%d", no);
    }
}
