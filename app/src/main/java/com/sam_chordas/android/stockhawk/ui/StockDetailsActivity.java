package com.sam_chordas.android.stockhawk.ui;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.VpnService;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.db.chart.model.LineSet;
import com.db.chart.view.AxisController;
import com.db.chart.view.ChartView;
import com.db.chart.view.LineChartView;
import com.db.chart.view.animation.Animation;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.CursorRecyclerViewAdapter;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by Arjun on 02-Jun-2016 for StockHawk.
 */
public class StockDetailsActivity extends AppCompatActivity {
    LineChartView chartView;
    LinearLayout modes;
    Calendar cal;
    ProgressWheel wheel;
    String stockName;
    String result = null;
    LineSet dataSet = null;
    boolean isLoading;

    private static final int UI_THREAD_SYNC_DURATION = 1000;
    private static final int GRAPH_ANIM_DURATION = 900;

    private float avg_high;
    private Paint paint;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph);

        chartView = (LineChartView) findViewById(R.id.linechart);
        wheel = (ProgressWheel) findViewById(R.id.progress_wheel);
        modes = (LinearLayout)findViewById(R.id.modes);
        cal = Calendar.getInstance();

        stockName = getIntent().getStringExtra("STOCK_NAME").toUpperCase(Locale.getDefault());
        setTitle(stockName);

        paint = new Paint();
        paint.setColor(Color.parseColor("#33FFFFFF"));
        avg_high = 0F;

        wheel.setVisibility(View.INVISIBLE);
        assert modes != null;
        //Default time mode
        if(savedInstanceState == null) {
            new GetStockData().execute();
        } else {
            if(savedInstanceState.getString("Result") != null)
                setStockData(savedInstanceState.getString("Result"));
            else
                new GetStockData().execute();
        }

        new GetGraphData().execute(-5, 1);
        toggleTextColors(R.id.btn_5D);

        modes.findViewById(R.id.btn_5D).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isLoading) {
                    new GetGraphData().execute(-5, 1);
                    toggleTextColors(v.getId());
                }
            }
        });

        modes.findViewById(R.id.btn_1M).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isLoading) {
                    new GetGraphData().execute(-30, 2);
                    toggleTextColors(v.getId());
                }
            }
        });

        modes.findViewById(R.id.btn_3M).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isLoading) {
                    new GetGraphData().execute(-90, 3);
                    toggleTextColors(v.getId());
                }
            }
        });

        modes.findViewById(R.id.btn_6M).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isLoading) {
                    new GetGraphData().execute(-180, 4);
                    toggleTextColors(v.getId());
                }
            }
        });

        modes.findViewById(R.id.btn_1Y).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isLoading) {
                    new GetGraphData().execute(-365, 5);
                    toggleTextColors(v.getId());
                }
            }
        });
        /*
        LineSet dataSet = new LineSet();
        dataSet.addPoint("YHOO", 75.24F);
        dataSet.addPoint("AAPL", 27.72F);
        dataSet.addPoint("GOOG", 85.28F);
        dataSet.addPoint("MSFT", 63.76F);

        dataSet.setColor(Color.parseColor("#53c1bd"))
                .setFill(Color.parseColor("#3d6c73"))
                .setThickness(9)
                .setGradientFill(new int[]{Color.parseColor("#364d5a"), Color.parseColor("#3f7178")}, null)
                .beginAt(0);
        */
    }

    private void toggleTextColors(int id) {
        for(int i = 0; i < modes.getChildCount(); i++) {
            if(modes.getChildAt(i).getId() != id)
                ((Button)modes.getChildAt(i)).setTextColor(Color.parseColor("#FFFFFF"));
            else
                ((Button)modes.getChildAt(i)).setTextColor(Color.parseColor("#53c1bd"));
        }
    }

    private class GetStockData extends AsyncTask<String, String, String> {
        private String BASE_URL = "https://query.yahooapis.com/v1/public/yql?q=";
        private String END_URL = "&format=json&diagnostics=false&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys";

        private OkHttpClient client;

        public void onPreExecute() {
            try {
                BASE_URL += URLEncoder.encode("select * from yahoo.finance.quotes where symbol in (\""+stockName+"\")", "UTF-8").replace(" ", "%20");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            client = new OkHttpClient();
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                Response response = client.newCall(new Request.Builder().url(BASE_URL + END_URL).build()).execute();
                JSONObject obj = new JSONObject(response.body().string()).getJSONObject("query").getJSONObject("results").getJSONObject("quote");
                result = obj.getString("Bid") + "|" + obj.getString("Change_PercentChange") + "|" + obj.getString("Open") + "|" + obj.getString("DaysHigh") + "|" + obj.getString("DaysLow") + "|" + obj.getString("AverageDailyVolume") + "|" + obj.getString("MarketCapitalization");
                return result;
            } catch (IOException e) {
                publishProgress(getString(R.string.server_not_found));
                e.printStackTrace();
            } catch (JSONException j) {
                publishProgress(getString(R.string.invalid_data));
                j.printStackTrace();
            }
            return null;
        }

        public void onProgressUpdate(String... args) {
            Toast.makeText(StockDetailsActivity.this, args[0], Toast.LENGTH_SHORT).show();
        }

        public void onPostExecute(String result) {
            setStockData(result);
        }
    }

    private void setStockData(String result) {
        if (result != null) {
            String data[] = result.split("\\|");
            ((TextView) findViewById(R.id.textBid)).setText(data[0].equals("null") ? "--" : data[0].format(Locale.getDefault(), getString(R.string.str_format), Double.parseDouble(data[0])));
            String change = "--", mktCap;
            if (data[1] != null) {
                String changeArr[] = data[1].replace("%", "").split(" \\- ");
                for (int i = 0; i < changeArr.length; i++)
                    changeArr[i] = changeArr[i].format(Locale.getDefault(), getString(R.string.str_format), Double.parseDouble(changeArr[i]));

                change = changeArr[0] + " (" + changeArr[1] + "%) ";
            }
            ((TextView) findViewById(R.id.textChange)).setText(data[1] == (null) ? "--" : change);
            ((TextView) findViewById(R.id.textChange)).setTextColor(data[1].contains("-") ? Color.RED : Color.GREEN);
            ((TextView) findViewById(R.id.textOpen)).setText(data[2] == (null) ? "--" : String.format(Locale.getDefault(), getString(R.string.str_format), Double.parseDouble(data[2])));
            ((TextView) findViewById(R.id.textHigh)).setText(data[3] == (null) ? "--" : String.format(Locale.getDefault(), getString(R.string.str_format), Double.parseDouble(data[3])));
            ((TextView) findViewById(R.id.textLow)).setText(data[4] == (null) ? "--" : String.format(Locale.getDefault(), getString(R.string.str_format), Double.parseDouble(data[4])));
            ((TextView) findViewById(R.id.textAvgVol)).setText(String.format(Locale.getDefault(), getString(R.string.str_format) + " " + getString(R.string.str_M), Double.parseDouble(data[5]) / 1000000.00));
            if (data[6].charAt(data[6].length() - 1) == 'B')
                mktCap = String.format(Locale.getDefault(), getString(R.string.str_format), Double.parseDouble(data[6].replace("B", ""))) + getString(R.string.str_B);
            else
                mktCap = String.format(Locale.getDefault(), getString(R.string.str_format), Double.parseDouble(data[6].replace("M", ""))) + getString(R.string.str_M);
            ((TextView) findViewById(R.id.textMktCap)).setText(data[6] == (null) ? "--" : mktCap);
        }
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString("Result", result);
    }

    private void setGraphData(final LineSet dataSet) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (dataSet != null) {
                    chartView.addData(dataSet);

                    int step = ((int) (2 * avg_high) + ((int) (2 * avg_high) % 5)) / 5;
//                  Log.d("STEP", (step*5)+", "+step);

                    chartView.setYLabels(AxisController.LabelPosition.NONE)
                            .setLabelsColor(Color.parseColor("#99FFFFFF"))
                            .setXAxis(false)
                            .setYAxis(false)
                            .setGrid(ChartView.GridType.FULL, paint)
                            .setAxisBorderValues(0, step * 5, step)
                            .setYLabels(AxisController.LabelPosition.OUTSIDE);

                    final Animation animation = new Animation();
                    animation.setDuration((int)(GRAPH_ANIM_DURATION/1.3));
                    //animation.setStartPoint(0.5f, 0f);
                    //animation.setEasing(new ExpoEase());

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            chartView.show(animation);
                        }
                    }, GRAPH_ANIM_DURATION / 3);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            isLoading = false;
                        }
                    }, (long) (1.5 * UI_THREAD_SYNC_DURATION));

                } else {
                    Toast.makeText(StockDetailsActivity.this, R.string.graph_unavailable, Toast.LENGTH_LONG).show();
                    isLoading = false;
                }
                wheel.startAnimation(AnimationUtils.loadAnimation(StockDetailsActivity.this, R.anim.zoom_out));
            }
        }, UI_THREAD_SYNC_DURATION);
    }

    private class GetGraphData extends AsyncTask<Integer, Void, LineSet> {
        private String BASE_URL = "https://query.yahooapis.com/v1/public/yql?q=";
        private String END_URL = "&format=json&diagnostics=false&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys";

        private OkHttpClient client;

        public void onPreExecute() {
            super.onPreExecute();
            wheel.postDelayed(new Runnable() {
                @Override
                public void run() {
                    wheel.startAnimation(AnimationUtils.loadAnimation(StockDetailsActivity.this, R.anim.zoom_in));
                }
            }, GRAPH_ANIM_DURATION / 3);

            try {
                BASE_URL += URLEncoder.encode("select * from yahoo.finance.historicaldata where symbol", "UTF-8") + "=\""+getIntent().getStringExtra("STOCK_NAME")+"\"";
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            client = new OkHttpClient();
            if(chartView.getData().size() != 0) {
                chartView.dismiss(new Animation().setDuration((int)(GRAPH_ANIM_DURATION/1.3)));
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        chartView.reset();
                    }
                }, UI_THREAD_SYNC_DURATION);
            }
        }

        @SuppressLint("SimpleDateFormat")
        @Override
        protected LineSet doInBackground(Integer... params) {
            isLoading = true;
            String endDate = getDate(cal);
            cal.add(Calendar.DATE, params[0]);
            String startDate = getDate(cal);
            cal.add(Calendar.DATE, -params[0]);

            String MID_URL = " and startDate=\"" + startDate +"\"";
            Log.d("MID_1", MID_URL);

            try {
                MID_URL += " and endDate=\"" + endDate + "\"" + "%7Creverse()";
                URLEncoder.encode(MID_URL, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            Log.d("MID_2", MID_URL);

            LineSet dataSet = new LineSet();

            try {
                Log.d("FIN_URL", BASE_URL + MID_URL + END_URL);
                Response response = client.newCall(new Request.Builder().url((BASE_URL + MID_URL + END_URL).replace(" ", "%20")).build()).execute();
                int factor = 0;
                String RESP_GRAPH = response.body().string();
                Log.d("RESP_GRAPH", RESP_GRAPH);
                JSONArray quotes = new JSONObject(RESP_GRAPH).getJSONObject("query").getJSONObject("results").getJSONArray("quote");

                switch (params[1]) {
                    case 1:
                        factor = 1;
                        break;
                    case 2:
                        factor = 5;
                        break;
                    case 3:
                        factor = 18;
                        break;
                    case 4:
                        factor = 36;
                        break;
                    case 5:
                        factor = 72;
                        break;
                }
                for(int i = 0; i < quotes.length(); i++) {
                    JSONObject object = (JSONObject)quotes.get(i);
                    avg_high += Float.parseFloat(object.getString("Close"));
                    if(i % factor != 0)
                        dataSet.addPoint("", Float.parseFloat(object.getString("Close")));
                    else
                        dataSet.addPoint(new SimpleDateFormat("dd-MMM-yy").format(new SimpleDateFormat("yyyy-MM-dd").parse(object.getString("Date"))), Float.parseFloat(object.getString("Close")));
                }

                avg_high /= quotes.length();

                dataSet.setColor(Color.parseColor("#77B0B0B0"))//#FFC840"))
                        .setFill(Color.parseColor("#44B0B0B0")) //3d6c73
                        .setThickness(8)
                        //.setGradientFill(new int[]{Color.parseColor("#364d5a"), Color.parseColor("#3f7178")}, null)
                        .beginAt(0);

                StockDetailsActivity.this.dataSet = dataSet;
                return dataSet;

            } catch (IOException | JSONException | ParseException e) {
                Log.d("DATA_RET_EX", "An Exception occurred: " + e.getMessage());
            }
            return null;
        }

        public void onPostExecute(final LineSet dataSet) {
            setGraphData(dataSet);
        }

        private String getDate(Calendar cal){
            return "" + cal.get(Calendar.YEAR) +"-" + (cal.get(Calendar.MONTH)+1 < 10 ? "0" + (cal.get(Calendar.MONTH)+1) : cal.get(Calendar.MONTH)+1) + "-" + cal.get(Calendar.DATE);
        }
    }
}