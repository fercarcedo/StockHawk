package com.udacity.stockhawk.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.udacity.stockhawk.DetailActivity;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.sync.QuoteSyncJob;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements StockAdapter.StockAdapterOnClickHandler,
        MainFragment.StocksLoaded {

    @Override
    public void onClick(String symbol) {
        Timber.d("Symbol clicked: %s", symbol);

        if (findViewById(R.id.containerDetail) == null) {
            Intent detailIntent = new Intent(this, DetailActivity.class);
            detailIntent.putExtra(DetailActivity.EXTRA_SYMBOL, symbol);
            startActivity(detailIntent);
        } else {
            //Launch aside
            Bundle args = new Bundle();
            args.putString(DetailActivity.EXTRA_SYMBOL, symbol);

            DetailFragment detailFragment = new DetailFragment();
            detailFragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.containerDetail, detailFragment)
                                        .commit();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new MainFragment())
                    .commit();
        }
    }

    @Override
    public void onStocksLoaded(Cursor data) {
        //Display it in detail fragment
        if (findViewById(R.id.containerDetail) != null) {
            data.moveToFirst();
            onClick(data.getString(Contract.Quote.POSITION_SYMBOL));
        }
    }
}
