package com.armandogomez.stockwatch;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

public class ViewHolder extends RecyclerView.ViewHolder {
	TextView stockSymbol;
	TextView stockPrice;
	TextView stockChangeText;
	TextView stockCompanyName;

	ViewHolder(View view) {
		super(view);
		stockSymbol = view.findViewById(R.id.stockSymbol);
		stockPrice = view.findViewById(R.id.stockPrice);
		stockChangeText = view.findViewById(R.id.stockChangeText);
		stockCompanyName = view.findViewById(R.id.stockCompanyName);
	}
}
