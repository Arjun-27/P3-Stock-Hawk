package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.afollestad.materialdialogs.MaterialDialog;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.rest.RecyclerViewItemClickListener;
import com.sam_chordas.android.stockhawk.rest.SpanAdjuster;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;

public class MyStocksActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private Intent mServiceIntent;
    private ItemTouchHelper mItemTouchHelper;
    private static final int CURSOR_LOADER_ID = 0;
    private static final String STOCK_NAME = "STOCK_NAME";
    private QuoteCursorAdapter mCursorAdapter;
    private Context mContext;
    private Cursor mCursor;

    private BroadcastReceiver receiver;
    private IntentFilter filter;
    private SwipeRefreshLayout refreshLayout;
    private TextView textEmpty;
    private int pos = 0;
    private RecyclerView recyclerView;
    private LinearLayoutManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;

        filter = new IntentFilter(MyStocksActivity.class.getName());
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(StockTaskService.ACTION_NO_STOCK)) {
                    Toast.makeText(MyStocksActivity.this, R.string.search_failed, Toast.LENGTH_SHORT).show();
                } else if(intent.getAction().equals(StockTaskService.UPDATE_COMPLETED)) {
                    if(refreshLayout != null && refreshLayout.isRefreshing()) {
                        refreshLayout.setRefreshing(false);
                    }
                }
            }
        };

        setContentView(R.layout.activity_my_stocks);
        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeToRefresh);

        textEmpty = (TextView) findViewById(R.id.textEmpty);

        makeEmptyText(getString(R.string.begin_str));
        textEmpty.setTypeface(Typeface.createFromAsset(mContext.getAssets(), "fonts/Roboto-Light.ttf"));
        textEmpty.setVisibility(View.INVISIBLE);
        // The intent service is for executing immediate pulls from the Yahoo API
        // GCMTaskService can only schedule tasks, they cannot execute immediately
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        manager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(manager);
        mServiceIntent = new Intent(this, StockIntentService.class);
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);
        refreshLayout.setNestedScrollingEnabled(true);
        mCursorAdapter = new QuoteCursorAdapter(this, null);
        recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        // TODO:
                        // do something on item click
                        startActivity(new Intent(MyStocksActivity.this, StockDetailsActivity.class).putExtra(STOCK_NAME, ((TextView)v.findViewById(R.id.stock_symbol)).getText()));
                    }
                }));
        recyclerView.setAdapter(mCursorAdapter);


        recyclerView.setOnScrollChangeListener(new RecyclerView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                pos = manager.findLastCompletelyVisibleItemPosition();
                if(manager.findFirstCompletelyVisibleItemPosition() == 0)
                    refreshLayout.setEnabled(true);
                else
                    refreshLayout.setEnabled(false);
            }
        });

        if (savedInstanceState == null) {
            // Run the initialize task service so that some stocks appear upon an empty database
            mServiceIntent.putExtra("tag", "init");
            if (Utils.isNetworkAvailable(MyStocksActivity.this)) {
                startService(mServiceIntent);
            } else {
                networkToast();
            }
        } else {
            pos = savedInstanceState.getInt("Position");
            recyclerView.smoothScrollToPosition(pos);
        }

        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(Utils.isNetworkAvailable(MyStocksActivity.this)) {
                    Intent intent = new Intent(MyStocksActivity.this, StockIntentService.class);
                    intent.putExtra("tag", "init");
                    startService(intent);
                } else {
                    if(refreshLayout.isRefreshing()) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MyStocksActivity.this, R.string.no_net, Toast.LENGTH_SHORT).show();
                                refreshLayout.setRefreshing(false);
                            }
                        }, 500);
                    }
                }
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.material_black_500)));
        fab.bringToFront();
        fab.attachToRecyclerView(recyclerView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Utils.isNetworkAvailable(MyStocksActivity.this)) {
                    new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
                            .content(R.string.content_test)
                            .inputType(InputType.TYPE_CLASS_TEXT)
                            .input(R.string.input_hint, R.string.input_prefill, false, new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(MaterialDialog dialog, CharSequence input) {
                                    // On FAB click, receive user input. Make sure the stock doesn't already exist
                                    // in the DB and proceed accordingly
                                    Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                            new String[]{QuoteColumns.SYMBOL}, QuoteColumns.SYMBOL + "= ?",
                                            new String[]{input.toString()}, null);
                                    if (c.getCount() != 0) {
                                        Toast toast =
                                                Toast.makeText(MyStocksActivity.this, R.string.res_saved,
                                                        Toast.LENGTH_LONG);
                                        toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                                        toast.show();
                                        return;
                                    } else {
                                        // Add the stock to DB
                                        mServiceIntent.putExtra("tag", "add");
                                        mServiceIntent.putExtra("symbol", input.toString());
                                        startService(mServiceIntent);
                                    }
                                }
                            })
                            .show();
                } else {
                    networkToast();
                }

            }
        });

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        mTitle = getTitle();
        if (Utils.isNetworkAvailable(MyStocksActivity.this)) {
            long period = 3600L;
            long flex = 10L;
            String periodicTag = "periodic";

            // create a periodic task to pull stocks once every hour after the app has been opened. This
            // is so Widget data stays up to date.
            PeriodicTask periodicTask = new PeriodicTask.Builder()
                    .setService(StockTaskService.class)
                    .setPeriod(period)
                    .setFlex(flex)
                    .setTag(periodicTag)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setRequiresCharging(false)
                    .build();
            // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
            // are updated.
            GcmNetworkManager.getInstance(this).schedule(periodicTask);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        filter.addAction(StockTaskService.ACTION_NO_STOCK);
        filter.addAction(StockTaskService.UPDATE_COMPLETED);
        registerReceiver(receiver, filter);
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
    }

    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    public void networkToast() {
        Toast.makeText(mContext, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    private void makeEmptyText(String text) {
        int i;
        for(i = 0; i < text.length(); i++) {
            if(text.charAt(i) == '+')
               break;
        }
        SpannableString string = new SpannableString(text);
        string.setSpan(new SpanAdjuster(0.5), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textEmpty.setText(string, TextView.BufferType.SPANNABLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_stocks, menu);
        restoreActionBar();
        return true;
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.d("PosSaved", ""+pos);
        savedInstanceState.putInt("Position", pos);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            if(Utils.isNetworkAvailable(MyStocksActivity.this)) {
                Intent intent = new Intent(MyStocksActivity.this, StockIntentService.class);
                intent.putExtra("tag", "init");
                startService(intent);
            }
            //return true;
        }

        if (id == R.id.action_change_units) {
            // this is for changing stock changes from percent value to dollar value
            Utils.showPercent = !Utils.showPercent;
            this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This narrows the return to only the stocks that are most current.
        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE, QuoteColumns.NAME,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursorAdapter.swapCursor(data);
        mCursor = data;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mCursor.getCount() == 0)
                    textEmpty.setVisibility(View.VISIBLE);
                else
                    textEmpty.setVisibility(View.INVISIBLE);
            }
        }, 250);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

}
