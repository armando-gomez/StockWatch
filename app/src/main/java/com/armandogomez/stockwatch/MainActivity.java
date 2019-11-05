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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {
	private RecyclerView recyclerView;
	private SwipeRefreshLayout swiper;
	private List<Stock> stockList = new ArrayList<>();
	private Set<String> stockSet = new HashSet<>();
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
		new AsyncSymbolLoader(this).execute();
	}

	private void doRefresh() {
	    for (Stock s: stockList) {
	        getFinancialData(s.getStockSymbol());
        }
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
					    if(!stockSet.contains(addInput)) {
                            getFinancialData(addInput);
                        } else {
					        duplicateStock(addInput);
                        }
					} else if(similarKeys.size() > 1) {
						openMultiStockDialog(similarKeys);
					} else {
						Toast.makeText(MainActivity.this, "Symbol not found", Toast.LENGTH_SHORT).show();
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

	private void openMultiStockDialog(Map<String, String> stocksMap) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		final String[] stocks = new String[stocksMap.size()];
		int i = 0;
		for(String key: stocksMap.keySet()) {
		    stocks[i] = key + "-" + stocksMap.get(key);
		    i++;
        }
        Arrays.sort(stocks);
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
	    String key = stocks[i].split("-")[0];
	    getFinancialData(key);
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
		} catch(IOException | JSONException e) {
			Toast.makeText(this, "Not saved", Toast.LENGTH_SHORT).show();
		}
	}

	public void updateStockList(Stock s) {
	    if(!stockSet.contains(s.getStockSymbol())) {
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
