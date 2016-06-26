package com.sam_chordas.android.stockhawk.widget;

/**
 * Created by Arjun on 19-Jun-2016 for StockHawk.
 */
public class StockQuote {
    private String id;
    private String symbol;
    private String percent_change;
    private String change;
    private String bidPrice;
    private int isUp;
    private int isCurrent;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getPercent_change() {
        return percent_change;
    }

    public void setPercent_change(String percent_change) {
        this.percent_change = percent_change;
    }

    public String getChange() {
        return change;
    }

    public void setChange(String change) {
        this.change = change;
    }

    public String getBidPrice() {
        return bidPrice;
    }

    public void setBidPrice(String bidPrice) {
        this.bidPrice = bidPrice;
    }

    public int getIsUp() {
        return isUp;
    }

    public void setIsUp(int isUp) {
        this.isUp = isUp;
    }

    public int getIsCurrent() {
        return isCurrent;
    }

    public void setIsCurrent(int isCurrent) {
        this.isCurrent = isCurrent;
    }
}
