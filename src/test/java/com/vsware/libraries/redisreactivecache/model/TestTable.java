package com.vsware.libraries.redisreactivecache.model;

import java.time.LocalDateTime;

public class TestTable {

    private Integer id;
    private String name;
    private LocalDateTime insertDate;

    public TestTable() {
    }

    public TestTable(Integer id, String name, LocalDateTime insertDate) {
        this.id = id;
        this.name = name;
        this.insertDate = insertDate;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getInsertDate() {
        return insertDate;
    }

    public void setInsertDate(LocalDateTime insertDate) {
        this.insertDate = insertDate;
    }
}
