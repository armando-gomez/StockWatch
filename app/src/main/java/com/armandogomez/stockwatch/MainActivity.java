package com.armandogomez.stockwatch;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {
	private static final String MARKET_WATCH = "https://www.marketwatch.com/investing/stock/";
	private RecyclerView recyclerView;
	private SwipeRefreshLayout swiper;
	private List<Stock> stockList = new ArrayList<>();
	private Set<String> stockSet = new HashSet<>();
	private StockAdapter stockAdapter;
	private Menu menu;
	private String addInput = "";
	private boolean networkStatusOnline = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		recyclerView = findViewById(R.id.recycler);
		stockAdapter = new StockAdapter(stockList, this);

		recyclerView.setAdapter(stockAdapter);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));

		swiper = findViewById(R.id.swiper);
		swiper.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				doRefresh();
			}
		});
		doNetworkCheck();
		loadJSON();
		new AsyncSymbolLoader(this).execute();
	}

	private void doRefresh() {
		doNetworkCheck();
		if (!networkStatusOnline) {
			showNoNetworkDialog();
			swiper.setRefreshing(false);
			return;
		}
		for (Stock s : stockList) {
			getFinancialData(s.getStockSymbol());
		}
		stockAdapter.notifyDataSetChanged();
		swiper.setRefreshing(false);
		Toast.makeText(this, "Stocks Updated", Toast.LENGTH_SHORT).show();
	}

	private void openAddDialogBox() {
		doNetworkCheck();
		if (!networkStatusOnline) {
			showNoNetworkDialog();
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Stock Selection");
		builder.setMessage("Please enter a Stock Symbol:");
		final EditText input = new EditText(this);
		input.setGravity(Gravity.CENTER_HORIZONTAL);
		input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
		builder.setView(input);
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				addInput = input.getText().toString().toUpperCase();
				if (!addInput.isEmpty()) {
					Map<String, String> similarKeys = AsyncSymbolLoader.findStocks(addInput);
					if (similarKeys.size() == 1) {
						if (!stockSet.contains(addInput)) {
							getFinancialData(addInput);
						} else {
							duplicateStock(addInput);
						}
					} else if (similarKeys.size() > 1) {
						openMultiStockDialog(similarKeys);
					} else {
						showNoStockError(addInput);
					}
				}
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});

		builder.show();
	}

	private void showNoStockError(String symbol) {
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setTitle("Symbol not found: " + symbol);
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	private void openMultiStockDialog(Map<String, String> stocksMap) {
		doNetworkCheck();
		if (!networkStatusOnline) {
			showNoNetworkDialog();
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		final String[] stocks = new String[stocksMap.size()];
		int i = 0;
		for (String key : stocksMap.keySet()) {
			stocks[i] = key + " - " + stocksMap.get(key);
			i++;
		}
		Arrays.sort(stocks, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				String s1 = o1.split("-")[0].trim();
				String s2 = o2.split("-")[0].trim();
				if (s1.length() > s2.length()) {
					return 1;
				} else if (s1.length() < s2.length()) {
					return -1;
				}
				return s1.compareTo(s2);
			}
		});
		builder.setTitle("Make a selection");
		builder.setItems(stocks, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				getStockChoice(which, stocks);
				dialog.dismiss();
			}
		});

		builder.setNegativeButton("Never Mind", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});

		builder.show();
	}

	private void getStockChoice(int i, String[] stocks) {
		String key = stocks[i].split("-")[0].trim();
		if (!stockSet.contains(key)) {
			getFinancialData(key);
		} else {
			duplicateStock(key);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		this.menu = menu;
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_add:
				openAddDialogBox();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onClick(View v) {
		int pos = recyclerView.getChildLayoutPosition(v);
		Stock s = stockList.get(pos);
		Intent i = new Intent(Intent.ACTION_VIEW);
		String stockUrl = MARKET_WATCH + s.getStockSymbol();
		i.setData(Uri.parse(stockUrl));
		startActivity(i);
	}

	@Override
	public boolean onLongClick(View v) {
		final int pos = recyclerView.getChildLayoutPosition(v);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Delete Stock");
		builder.setMessage("Do you want to delete this stock?");
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				deleteStock(pos);
			}
		});
		builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});

		AlertDialog dialog = builder.create();
		dialog.show();

		return false;
	}

	private void getFinancialData(String symbol) {
		new AsyncStockLoader(this).execute(symbol);
	}

	private void getEmptyFinancialData(String symbol, String name) {
		Stock s = new Stock(symbol, name);
		updateStockList(s);
	}

	private void deleteStock(int pos) {
		String symbol = stockList.get(pos).getStockSymbol();
		stockSet.remove(symbol);
		stockList.remove(pos);
		updateList();
	}

	private void updateList() {
		stockList.sort(new Comparator<Stock>() {
			@Override
			public int compare(Stock a, Stock b) {
				return a.getStockSymbol().compareTo(b.getStockSymbol());
			}
		});
		stockAdapter.notifyDataSetChanged();
		try {
			saveStocks();
		} catch (IOException | JSONException e) {
			Toast.makeText(this, "Not saved", Toast.LENGTH_SHORT).show();
		}
	}

	public void updateStockList(Stock s) {
		if (!stockSet.contains(s.getStockSymbol())) {
			stockList.add(s);
			stockSet.add(s.getStockSymbol());
		} else {
			int i = stockList.indexOf(s);
			stockList.set(i, s);
		}
		updateList();
	}

	private void duplicateStock(String key) {
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setTitle("Duplicate Stock");
		builder.setMessage("Stock symbol " + key + " is already displayed");
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	private void doNetworkCheck() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm == null) {
			Toast.makeText(this, "Cannot Access ConnectivityManager", Toast.LENGTH_SHORT).show();
			return;
		}

		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnected()) {
			this.networkStatusOnline = true;
		} else {
			this.networkStatusOnline = false;
		}
	}

	private void loadJSON() {
		try {
			InputStream is = getApplicationContext().openFileInput("Stocks.json");
			BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
			reader.close();
			List<String> tempSymbols = new ArrayList<>();
			List<String> tempNames = new ArrayList<>();
			JSONArray jsonArray = new JSONArray(sb.toString());
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = (JSONObject) jsonArray.get(i);
				String symbol = jsonObject.getString("symbol");
				tempSymbols.add(symbol);
				String name = jsonObject.getString("name");
				tempNames.add(symbol);
			}
			doNetworkCheck();
			if (this.networkStatusOnline) {
				for (String symbol : tempSymbols) {
					getFinancialData(symbol);
				}
			} else {
				showNoNetworkDialog();
				for (int i = 0; i < tempSymbols.size(); i++) {
					getEmptyFinancialData(tempSymbols.get(i), tempNames.get(i));
				}
			}


		} catch (FileNotFoundException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void showNoNetworkDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setTitle("No Network Connection");
		builder.setMessage("Stocks cannot be updated without a network connection");
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	private void saveStocks() throws IOException, JSONException {
		FileOutputStream fos = getApplicationContext()
				.openFileOutput("Stocks.json", Context.MODE_PRIVATE);
		JSONArray jsonArray = new JSONArray();
		for (Stock s : stockList) {
			JSONObject stockJSON = new JSONObject();
			stockJSON.put("symbol", s.getStockSymbol());
			stockJSON.put("name", s.getCompanyName());
			jsonArray.put(stockJSON);
		}

		String jsonText = jsonArray.toString();
		fos.write(jsonText.getBytes());
		fos.close();
	}

	public void networkCheck(View v) {
		doNetworkCheck();
	}
}
