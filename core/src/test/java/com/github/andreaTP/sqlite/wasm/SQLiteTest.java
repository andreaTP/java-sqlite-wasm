package com.github.andreaTP.sqlite.wasm;

import org.junit.jupiter.api.Test;

public class SQLiteTest {

    @Test
    public void basicTest() {
        var sqlite = new SQLite();

        sqlite.demo();
    }
}
