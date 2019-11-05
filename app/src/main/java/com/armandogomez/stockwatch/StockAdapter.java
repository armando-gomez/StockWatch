package com.armandogomez.stockwatch;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StockAdapter extends RecyclerView.Adapter<ViewHolder> {
	private List<Stock> stockList;
	private MainActivity mainActivity;

	StockAdapter(List<Stock> stockList, MainActivity mainActivity) {
		this.stockList  = stockList;
		this.mainActivity = mainActivity;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
		View itemView = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.stock_ticker_row, parent, false);
		itemView.setOnClickListener(mainActivity);
		itemView.setOnLongClickListener(mainActivity);
		return new ViewHolder(itemView);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int pos) {
		Stock stock = stockList.get(pos);
		double stockChange = stock.getChange();
		String arrow = "▲";
		int stockColor = ContextCompat.getColor(mainActivity, R.color.positiveChange);
		if(stockChange < 0) {
			arrow = "▼";
			stockColor = ContextCompat.getColor(mainActivity, R.color.negativeChange);
		}

		holder.stockSymbol.setText(stock.getStockSymbol());
		holder.stockSymbol.setTextColor(stockColor);
		holder.stockPrice.setText(Double.toString(stock.getPrice()));
		holder.stockPrice.setTextColor(stockColor);
		holder.stockCompanyName.setText(stock.getCompanyName());
		holder.stockCompanyName.setTextColor(stockColor);
		String changeText = arrow + Double.toString(stockChange) + "(" + Double.toString(stock.getChangePercent()) + "%)";
		holder.stockChangeText.setText(changeText);
		holder.stockChangeText.setTextColor(stockColor);
	}

	@Override
	public int getItemCount() {return stockList.size();}
}
