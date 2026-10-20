package com.myorg.WCSApp.model;

import java.util.Arrays;

public class WeatherData {
    private String coords;
    private String[] weather;
    private String base;
    private String main;
    private String wind;
    private String clouds;
    private String rain;
    private String snow;
    private String dt;
    private String sys;
    private String timezone;
    private String id;
    private String name;
    private String cod;

    public WeatherData() {
    }

    public WeatherData(String coords, String[] weather, String base, String main, String wind, String clouds,
                       String rain, String snow, String dt, String sys, String timezone, String id, String name, String cod) {
        this.coords = coords;
        this.weather = weather;
        this.base = base;
        this.main = main;
        this.wind = wind;
        this.clouds = clouds;
        this.rain = rain;
        this.snow = snow;
        this.dt = dt;
        this.sys = sys;
        this.timezone = timezone;
        this.id = id;
        this.name = name;
        this.cod = cod;
    }

    public String getCoords() {
        return coords;
    }

    public void setCoords(String coords) {
        this.coords = coords;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String getMain() {
        return main;
    }

    public void setMain(String main) {
        this.main = main;
    }

    public String getWind() {
        return wind;
    }

    public void setWind(String wind) {
        this.wind = wind;
    }

    public String getClouds() {
        return clouds;
    }

    public void setClouds(String clouds) {
        this.clouds = clouds;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRain() {
        return rain;
    }

    public void setRain(String rain) {
        this.rain = rain;
    }

    public String getSnow() {
        return snow;
    }

    public void setSnow(String snow) {
        this.snow = snow;
    }

    public String getDt() {
        return dt;
    }

    public void setDt(String dt) {
        this.dt = dt;
    }

    public String getSys() {
        return sys;
    }

    public void setSys(String sys) {
        this.sys = sys;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCod() {
        return cod;
    }

    public void setCod(String cod) {
        this.cod = cod;
    }

    public String[] getWeather() {
        return weather;
    }

    public void setWeather(String[] weather) {
        this.weather = weather;
    }

    @Override
    public String toString() {
        return "WeatherData [coords=" + coords + ", weather=" + Arrays.toString(weather) + ", base=" + base + ", main="
                + main + ", wind=" + wind + ", clouds=" + clouds + ", rain=" + rain + ", snow=" + snow + ", dt=" + dt
                + ", sys=" + sys + ", timezone=" + timezone + ", id=" + id + ", name=" + name + ", cod=" + cod + "]";
    }
}