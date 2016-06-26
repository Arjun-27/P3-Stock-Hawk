package com.sam_chordas.android.stockhawk.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.ui.StockDetailsActivity;

/**
 * Created by Arjun on 19-Jun-2016 for StockHawk.
 */
public class StockWidgetProvider extends AppWidgetProvider {
    public static String WIDGET_BUTTON = "MY_PACKAGE_NAME.WIDGET_BUTTON";
    public static String ON_STOCK_CLICK = "STOCK_ITEM_CLICK";
    public static final String STOCK_NAME = "stockName";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (WIDGET_BUTTON.equals(intent.getAction())) {
            startServiceAndScheduleUpdate(context);
            //Toast.makeText(context, "Helloooo", Toast.LENGTH_SHORT).show();
            Log.d("Hellloo", "AADADAD");
        } else if(ON_STOCK_CLICK.equals(intent.getAction())) {
            context.startActivity(new Intent(context, StockDetailsActivity.class).putExtra("STOCK_NAME", intent.getExtras().getString(STOCK_NAME)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        try {
            for (int appWidgetId : appWidgetIds) {
                // Set up the intent that starts the StackViewService, which will
                // provide the views for this collection.
                Intent intent = new Intent(context, StockWidgetService.class);

                // Add the app widget ID to the intent extras.
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

                // Instantiate the RemoteViews object for the app widget layout.
                RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.stock_widget_provider_layout);
                rv.setRemoteAdapter(appWidgetId, R.id.stock_list, intent);
                rv.setEmptyView(R.id.stock_list, R.id.textEmpty_w);
                if (appWidgetId == R.xml.stock_widget_provider)
                    startServiceAndScheduleUpdate(context);
                //
                // Do additional processing specific to this app widget...
                //
                Intent intent1 = new Intent(WIDGET_BUTTON);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent1, PendingIntent.FLAG_UPDATE_CURRENT);
                rv.setOnClickPendingIntent(R.id.button, pendingIntent);

                Intent intent2 = new Intent(ON_STOCK_CLICK);
                PendingIntent onClickPendingIntent = PendingIntent.getBroadcast(context, 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT);
                rv.setPendingIntentTemplate(R.id.stock_list, onClickPendingIntent);

                Log.d("Updating", "Updated at " + System.currentTimeMillis());
                appWidgetManager.updateAppWidget(appWidgetId, rv);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    private void startServiceAndScheduleUpdate(Context context) {
        Intent intentS;

        if(Utils.isNetworkAvailable(context)) {
            intentS = new Intent(context, StockIntentService.class);
            intentS.putExtra("tag", "init");
            context.startService(intentS);
            Toast.makeText(context, R.string.str_updating, Toast.LENGTH_SHORT).show();
        } else {
            UpdateWidget.updateWidget(context);
            Toast.makeText(context, R.string.widget_no_net, Toast.LENGTH_SHORT).show();
        }
    }
}