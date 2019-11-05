package com.armandogomez.stockwatch;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {
	private RecyclerView recyclerView;
	private SwipeRefreshLayout swiper;
	private List<Stock> stockList = new ArrayList<>();
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

		createStocks();
	}

	private void createStocks() {
		Stock apple = new Stock("APPL", "Apple Inc.", 132.72, 0.38, 0.28);
		Stock google = new Stock("GOOG", "Alphabet Inc", 132.72, -0.85, -0.14);
		Stock amazon = new Stock("AMZN", "Amazon.com inc.", 132.72, 0.38, 0.28);

		stockList.add(apple);
		stockList.add(google);
		stockList.add(amazon);
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
				Toast.makeText(input.getContext(), addInput, Toast.LENGTH_SHORT).show();
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

	private void deleteStock(int pos) {
		stockList.remove(pos);
		updateList();
	}

	private void updateList() {
		stockAdapter.notifyDataSetChanged();
	}
}
