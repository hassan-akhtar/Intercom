package com.android.newintercom.Models;


public class Devices {

    private String name;
    private boolean isDnDon;


    public Devices(String name, boolean isDnDon) {
        this.name = name;
        this.isDnDon = isDnDon;
    }

    public boolean isDnDon() {
        return isDnDon;
    }

    public void setDnDon(boolean dnDon) {
        isDnDon = dnDon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
