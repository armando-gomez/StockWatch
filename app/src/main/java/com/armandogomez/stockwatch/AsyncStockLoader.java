package com.armandogomez.stockwatch;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class AsyncStockLoader extends AsyncTask<String, Integer, String> {
	@SuppressLint("StaticFieldLeak")
	private MainActivity mainActivity;
	private String symbol;
	private static final String TAG = "AsyncStockLoader";

	private static final String DATA_URL = "https://cloud.iexapis.com/stable/stock/";
	private static final String DATA_QUERY = "/quote?token=";
	private static final String SECRET_TOKEN = "sk_c00a347722694ce9963eaf76b75b630c";

	AsyncStockLoader(MainActivity mainActivity, String symbol) {
		this.mainActivity = mainActivity;
		this.symbol = symbol;
	}

	@Override
	protected void onPostExecute(String s) {
		Stock stock = parseJSON(s);
		mainActivity.updateStockList(stock);
	}

	@Override
	protected String doInBackground(String... params) {
		Uri dataUri = Uri.parse(DATA_URL);
		StringBuilder sb = new StringBuilder();
		String urlToUse = dataUri.toString() + symbol + DATA_QUERY + SECRET_TOKEN;
		try {
			URL url = new URL(urlToUse);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			InputStream is = conn.getInputStream();
			BufferedReader reader = new BufferedReader((new InputStreamReader(is)));

			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line).append('\n');
			}

			Log.d(TAG, "doInBackground: " + sb.toString());
		} catch (Exception e) {
			Log.e(TAG, "doInBackground: ", e);
			return null;
		}
		return sb.toString();
	}

	private Stock parseJSON(String s) {
		try {
			JSONObject jStock = new JSONObject(s);
			String symbol = jStock.getString("symbol");
			String name = jStock.getString("companyName");
			double price = jStock.getDouble("latestPrice");
			double change = jStock.getDouble("change");
			double changePercent = jStock.getDouble("changePercent");
			Stock stock = new Stock(symbol, name, price, change, changePercent);
			return stock;
		} catch (Exception e) {
			Log.d(TAG, "parseOuterJSON: " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
}
