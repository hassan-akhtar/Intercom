package com.android.newintercom.Models;


public class Devices {

    private String name;
    private boolean isDnDon;
    private String ipAddress;



    public Devices(String name, String ipAddress, boolean isDnDon) {
        this.name = name;
        this.isDnDon = isDnDon;
        this.ipAddress = ipAddress;
    }

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

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}
