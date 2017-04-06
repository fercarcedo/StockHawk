package com.udacity.stockhawk.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.StockHawkApp;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import au.com.bytecode.opencsv.CSVReader;
import butterknife.BindView;
import butterknife.ButterKnife;

public class DetailFragment extends Fragment {

    public static final String EXTRA_SYMBOL = "extra_symbol";

    @BindView(R.id.symbol)
    TextView tvSymbol;

    @BindView(R.id.price)
    TextView tvPrice;

    @BindView(R.id.change)
    TextView tvChange;

    @BindView(R.id.stockHistoryChart)
    LineChart stockHistoryChart;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detail, container, false);

        ButterKnife.bind(this, view);

        Bundle arguments = getArguments();

        if (arguments != null) {
            if (arguments.containsKey(EXTRA_SYMBOL)) {
                String symbol = arguments.getString(EXTRA_SYMBOL);
                setSymbol(symbol);
            }
        }

        return view;
    }

    public void setSymbol(String symbol) {
        tvSymbol.setText(symbol);
        new LoadDataTask().execute(symbol);
    }

    private class LoadDataTask extends AsyncTask<String, Void, Object[]> {
        @Override
        protected Object[] doInBackground(String... params) {
            String symbol = params[0];

            Cursor cursor = StockHawkApp.getInstance().getContentResolver().query(
                    Contract.Quote.URI,
                    Contract.Quote.QUOTE_COLUMNS.toArray(new String[] {}),
                    Contract.Quote.COLUMN_SYMBOL + "=?",
                    new String[] { symbol },
                    null
            );

            cursor.moveToFirst();

            Object[] result = new Object[5];
            DecimalFormat dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);

            //Price as string
            result[0] = dollarFormat.format(cursor.getFloat(Contract.Quote.POSITION_PRICE));

            float rawAbsoluteChange = cursor.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
            float percentageChange = cursor.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);

            //Change textview backgroundresource
            if (rawAbsoluteChange > 0) {
                result[1] = R.drawable.percent_change_pill_green;
            } else {
                result[1] = R.drawable.percent_change_pill_red;
            }

            DecimalFormat dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
            dollarFormatWithPlus.setPositivePrefix("+$");
            DecimalFormat percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
            percentageFormat.setMaximumFractionDigits(2);
            percentageFormat.setMinimumFractionDigits(2);
            percentageFormat.setPositivePrefix("+");

            String change = dollarFormatWithPlus.format(rawAbsoluteChange);
            String percentage = percentageFormat.format(percentageChange / 100);

            //Change textview text
            if (getActivity() != null && isAdded()) {
                if (PrefUtils.getDisplayMode(StockHawkApp.getInstance())
                        .equals(getString(R.string.pref_display_mode_absolute_key))) {
                    result[2] = change;
                } else {
                    result[2] = percentage;
                }
            }

            //Parse history CSV
            String history = cursor.getString(Contract.Quote.POSITION_HISTORY);
            Log.d("TAG", history);
            CSVReader reader = new CSVReader(new StringReader(history));
            try {
                List<String[]> csvEntries = reader.readAll();
                List<Entry> chartEntries = new ArrayList<>(csvEntries.size());

                int counter = 0;
                final List<String> xLabels = new ArrayList<>();
                DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

                for (String[] csvEntry : csvEntries) {
                    chartEntries.add(new Entry(counter++, Float.parseFloat(csvEntry[1])));
                    xLabels.add(dateFormat.format(new Date(Long.parseLong(csvEntry[0]))));
                }

                if (getActivity() != null && isAdded()) {

                    LineDataSet dataSet = new LineDataSet(chartEntries, getString(R.string.history));

                    //Chart data
                    result[3] = new LineData(dataSet);

                }

                //Chart x axis labels
                result[4] = xLabels;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(Object[] objects) {
            super.onPostExecute(objects);

            if (getActivity() == null || !isAdded())
                return;

            String priceStr = (String) objects[0];
            int changeTvBgRes = (int) objects[1];
            String changeStr = (String) objects[2];
            LineData lineData = (LineData) objects[3];
            tvPrice.setText(priceStr);
            tvChange.setBackgroundResource(changeTvBgRes);
            tvChange.setText(changeStr);
            stockHistoryChart.setData(lineData);

            @SuppressWarnings("unchecked")
            final List<String> xLabel = (List<String>) objects[4];

            XAxis xAxis = stockHistoryChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setDrawGridLines(false);
            xAxis.setValueFormatter(new IAxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    if (value == 0 || (axis.getAxisMaximum() - value) < 10)
                        return xLabel.get((int) value);
                    return "";
                }
            });
            stockHistoryChart.getDescription().setEnabled(false);
            stockHistoryChart.invalidate();
        }
    }
}
