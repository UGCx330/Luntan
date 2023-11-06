package com.zzh.luntan.entity;

public class PageHelper {
    private int current = 1;
    private int rows;
    private int limit = 10;
    private String urlPath;

    public int getLimit() {
        return limit;
    }


    public int getOffSet() {
        return (current - 1) * limit;
    }

    public int getPreTwoPage() {
        return Math.max(current - 2, 1);
    }

    public int getLastTwoPage() {
        return Math.min(current + 2, getTotalPages());
    }

    public int getTotalPages() {
        return rows % limit == 0 ? rows / limit : rows / limit + 1;
    }

    public void setLimit(int limit) {
        if (limit >= 1) {
            this.limit = limit;
        }
    }

    public String getUrlPath() {
        return urlPath;
    }

    public void setUrlPath(String urlPath) {
        this.urlPath = urlPath;
    }


    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        if (current >= 1) {
            this.current = current;
        }
    }


    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

}
