package com.armandogomez.stockwatch;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Context;
import android.content.DialogInterface;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {
	private RecyclerView recyclerView;
	private SwipeRefreshLayout swiper;
	private List<Stock> stockList = new ArrayList<>();
	private Map<String, Stock> stockMap = new HashMap<>();
	private StockAdapter stockAdapter;
	private Menu menu;

	private String addInput = "";

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

		loadJSON();
	}

	private void doRefresh() {
		stockAdapter.notifyDataSetChanged();
		swiper.setRefreshing(false);
		Toast.makeText(this, "Stocks Updated", Toast.LENGTH_SHORT).show();
	}

	private void openAddDialogBox() {
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
				if(!addInput.isEmpty()) {
					Map<String, String> similarKeys = AsyncSymbolLoader.findStocks(addInput);
					if(similarKeys.size() == 1) {
						getFinancialData(addInput);
					} else if(similarKeys.size() > 1) {
						openMultiStockDialog(similarKeys);
					}
				}
				getFinancialData(addInput);
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

	private void openMultiStockDialog(Map<String, String> stocks) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		List<String> items = new ArrayList<>();
		for(String key: stocks.keySet()) {
			items.add(key + " - " + stocks.get(key));
		}
		builder.setTitle("Make a selection")
				.setItems(stocks, new DialogInterface.OnClickListener() {
				})

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
		Toast.makeText(v.getContext(), s.getCompanyName(), Toast.LENGTH_SHORT).show();
	}

	@Override
	public boolean onLongClick(View v) {
		final int pos = recyclerView.getChildLayoutPosition(v);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Delete Note");
		builder.setMessage("Do you want to delete this note?");
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				deleteStock(pos);
			}
		});
		builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});

		AlertDialog dialog = builder.create();
		dialog.show();

		return false;
	}

	private void getFinancialData(String symbol) {
		new AsyncStockLoader(this, symbol).execute();
	}

	private void deleteStock(int pos) {
		stockList.remove(pos);
		updateList();
	}

	private void updateList() {
		stockAdapter.notifyDataSetChanged();
		try {
			saveStocks();
		} catch(IOException | JSONException e) {
			Toast.makeText(this, "Not saved", Toast.LENGTH_SHORT).show();
		}
	}

	public void updateStockList(Stock s) {
		if(!stockMap.containsKey(s.getStockSymbol())) {
			stockList.add(s);
			stockMap.put(s.getStockSymbol(), s);
			Toast.makeText(this, "Loaded " + s.getStockSymbol(), Toast.LENGTH_SHORT).show();
		}
		updateList();
	}

	private void loadJSON() {
		try {
			InputStream is = getApplicationContext().openFileInput("Stocks.json");
			BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

			StringBuilder sb = new StringBuilder();
			String line;
			while((line = reader.readLine()) != null) {
				sb.append(line);
			}
			reader.close();
			List<String> tempSymbols = new ArrayList<>();
			JSONArray jsonArray = new JSONArray(sb.toString());
			for(int i=0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = (JSONObject) jsonArray.get(i);
				String symbol = jsonObject.getString("symbol");
				tempSymbols.add(symbol);
			}

			new AsyncSymbolLoader(this).execute();

			for(String symbol: tempSymbols) {
				getFinancialData(symbol);
			}
		} catch(FileNotFoundException e) {
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private void saveStocks() throws IOException, JSONException {
		FileOutputStream fos = getApplicationContext()
				.openFileOutput("Stocks.json", Context.MODE_PRIVATE);
		JSONArray jsonArray = new JSONArray();
		for(Stock s: stockList) {
			JSONObject stockJSON = new JSONObject();
			stockJSON.put("symbol", s.getStockSymbol());
			jsonArray.put(stockJSON);
		}

		String jsonText = jsonArray.toString();
		fos.write(jsonText.getBytes());
		fos.close();
	}
}
