package com.sam_chordas.android.stockhawk.widget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.TextView;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockTaskService;

import java.security.acl.LastOwnerException;
import java.util.ArrayList;

/**
 * Created by Arjun on 19-Jun-2016 for StockHawk.
 */
public class StockWidgetDataProvider implements RemoteViewsService.RemoteViewsFactory {

    private Context context;
    private Cursor cursor = null;
    private ArrayList<StockQuote> list;
    private boolean doUpdate;
    private int mAppWidgetId;

    public StockWidgetDataProvider(Context context, Intent intent) {
        this.context = context;
        //doUpdate = intent.getBooleanExtra("UPDATE", false);
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    @Override
    public void onCreate() {
        Log.d("Create", "In Provider");
    }

    @Override
    public void onDataSetChanged() {
        initCursor();
    }

    @Override
    public void onDestroy() {
        if(cursor != null && !cursor.isClosed())
            cursor.close();
    }

    @Override
    public int getCount() {
        return cursor != null ? cursor.getCount() : 0;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews view = new RemoteViews(context.getPackageName(), R.layout.stock_widget_item_layout);
        if(cursor != null) {
            cursor.moveToPosition(position);
            view.setTextViewText(R.id.stock_symbol_w, cursor.getString(1));
            view.setTextViewText(R.id.bid_price_w, cursor.getString(2));
            view.setTextViewText(R.id.change_w, cursor.getString(4) + " (" + cursor.getString(3) + ")");
            view.setInt(R.id.change_w, "setBackgroundResource", cursor.getString(3).contains("-") ? R.drawable.percent_change_pill_red : R.drawable.percent_change_pill_green);

            final Intent fillInIntent = new Intent();
            fillInIntent.setAction(StockWidgetProvider.ON_STOCK_CLICK);
            final Bundle bundle = new Bundle();
            bundle.putString(StockWidgetProvider.STOCK_NAME, cursor.getString(1));
            fillInIntent.putExtras(bundle);
            view.setOnClickFillInIntent(R.id.stockItem, fillInIntent);
        }

        return view;
    }

    private void initCursor() {
        if(cursor != null && !cursor.isClosed())
            cursor.close();
        cursor = context.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
        assert cursor != null;
        Log.d("COUNT", ""+cursor.getCount());
        //doUpdate = false;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
