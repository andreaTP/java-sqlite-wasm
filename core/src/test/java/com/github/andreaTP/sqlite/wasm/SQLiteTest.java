package com.github.andreaTP.sqlite.wasm;

import org.junit.jupiter.api.Test;

public class SQLiteTest {

    @Test
    public void basicTest() {
        var sqlite = new SQLite();

//        var dbPtr = sqlite.allocDbPtr();
//        var dbName = sqlite.allocateString(":memory:");
//        var vfsName = sqlite.allocateString("");
//
//        sqlite.openDb(dbPtr, dbName, vfsName);
        sqlite.demoOpenDB();
    }
}
