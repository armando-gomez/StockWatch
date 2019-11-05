package com.armandogomez.stockwatch;

public class Stock {
	private String stockSymbol;
	private String companyName;
	private double price;
	private double change;
	private double changePercent;

	Stock(String stockSymbol, String companyName, double price, double change, double changePercent) {
		this.stockSymbol = stockSymbol;
		this.companyName = companyName;
		this.price = price;
		this.change = change;
		this.changePercent = changePercent;
	}

	public String getStockSymbol() {
		return stockSymbol;
	}

	public String getCompanyName() {
		return companyName;
	}

	public double getPrice() {
		return price;
	}

	public double getChange() {
		return change;
	}

	public double getChangePercent() {
		return changePercent;
	}

	public void updateStock(double price, double change, double changePercent) {
		this.setPrice(price);
		this.setChange(change);
		this.setChangePercent(changePercent);
	}

	private void setPrice(double price) {
		this.price = price;
	}

	private void setChange(double change) {
		this.change = change;
	}

	private void setChangePercent(double changePercent) {
		this.changePercent = changePercent;
	}
}
