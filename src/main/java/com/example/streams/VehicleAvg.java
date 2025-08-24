package com.example.streams;

public class VehicleAvg {
    private double sum;
    private long count;

    public VehicleAvg() {this.sum = 0.0; this.count = 0L;}

    public double getSum() { return sum; }
    public void setSum(double sum) { this.sum = sum; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
}
