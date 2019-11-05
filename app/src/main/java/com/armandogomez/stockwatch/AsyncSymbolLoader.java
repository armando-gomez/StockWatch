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
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AsyncSymbolLoader extends AsyncTask<String, Integer, String> {
	@SuppressLint("StaticFieldLeak")
	private MainActivity mainActivity;
	private static final String TAG = "AsyncSymbolLoader";
	private static Map<String, String> symbolMap;
	private static final String SYMBOL_URL = "https://api.iextrading.com/1.0/ref-data/symbols";

	AsyncSymbolLoader(MainActivity mainActivity) {
		this.mainActivity = mainActivity;
		this.symbolMap = new HashMap<>();
	}

	@Override
	protected void onPostExecute(String s) {
		parseJSON(s);
	}

	@Override
	protected String doInBackground(String... params) {
		Uri symbolUri = Uri.parse(SYMBOL_URL);
		String urlToUse = symbolUri.toString();
		StringBuilder sb = new StringBuilder();
		try {
			URL url = new URL(urlToUse);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			Log.d(TAG, "doInBackground: ResponseCode: " + conn.getResponseCode());
			conn.setRequestMethod("GET");
			InputStream is = conn.getInputStream();
			BufferedReader reader = new BufferedReader((new InputStreamReader(is)));

			String line;
			while((line = reader.readLine()) != null) {
				sb.append(line).append('\n');
			}

			Log.d(TAG, "doInBackground: " + sb.toString());
		} catch (Exception e) {
			Log.e(TAG, "doInBackground: ", e);
			return null;
		}

		return sb.toString();
	}

	private void parseJSON(String s) {
		Map<String, String> symbolMap = new HashMap<>();
		try {
			JSONArray jObjMain = new JSONArray(s);
			for(int i=0; i < jObjMain.length(); i++) {
				JSONObject jStock = (JSONObject) jObjMain.get(i);
				String symbol = jStock.getString("symbol");
				String name = jStock.getString("name");
				if(!name.isEmpty()) {
					symbolMap.put(symbol, name);
				}
			}
		} catch (Exception e) {
			Log.d(TAG, "parseOuterJSON: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static Map<String, String> findStocks(String s) {
		Map<String, String> similarMap = new HashMap<>();
		for(String key: symbolMap.keySet()) {
			if(key.contains(s)) {
				similarMap.put(key, symbolMap.get(key));
			}
		}

		return similarMap;
	}
}
