package com.github.andreaTP.sqlite.wasm;

import com.dylibso.chicory.experimental.hostmodule.annotations.WasmModuleInterface;
import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.ChicoryException;

@WasmModuleInterface("wasm/libsqlite3-opt.wasm")
public class SQLite implements AutoCloseable {
    private final Instance instance;
    private final WasiPreview1 wasiPreview1;
    private final SQLite_ModuleExports exports;

    public SQLite() {
        var wasiOpts = WasiOptions.builder().inheritSystem().build();
        this.wasiPreview1 = WasiPreview1.builder().withOptions(wasiOpts).build();
        this.instance =
                // interpreter
                //                Instance.builder(
                //                                Parser.parse(
                //                                        SQLite.class.getResourceAsStream(
                //                                                "/wasm/libsqlite3-opt.wasm")))
                //                        .withImportValues(
                //                                ImportValues.builder()
                //
                // .addFunction(wasiPreview1.toHostFunctions())
                //                                        .build())
                //                        .withStart(false) // why?
                //                        .build();
                // pre compiled mode:
                Instance.builder(SQLiteModule.load())
                        .withMachineFactory(SQLiteModule::create)
                        .withImportValues(
                                ImportValues.builder()
                                        .addFunction(wasiPreview1.toHostFunctions())
                                        .build())
                        .build();
        this.exports = () -> instance;
    }

    @Override
    public void close() {
        if (wasiPreview1 != null) {
            wasiPreview1.close();
        }
    }

    // Demo implementation of this simple interface:
    // https://github.com/mathetake/wazero-sqlite/blob/main/main.go
    //    static class StringPtr {
    //        private final int ptr;
    //        private final int size;
    //
    //        StringPtr(int ptr, int size) {
    //            this.ptr = ptr;
    //            this.size = size;
    //        }
    //
    //        public int ptr() {
    //            return ptr;
    //        }
    //
    //        public int size() {
    //            return size;
    //        }
    //    }

    public int malloc(int size) {
        return exports.realloc(0, size);
    }

    public int free(int ptr) {
        return exports.realloc(ptr, 0);
    }

    public int allocDbPtr() {
        return malloc(4);
    }

    public int allocateString(String str) {
        var ptr = malloc(str.length());
        instance.memory().writeCString(ptr, str);
        return ptr;
    }

    public String errmsg(int dbPtr) {
        var errPtr = exports.sqlite3Errmsg(dbPtr);
        return instance.memory().readCString(errPtr);
    }

    private static final int SQLITE_MISUSE = 21;
    private static final int SQLITE_ROW = 100;

    public void demo() {
        var dbPtr = malloc(4);
        try {
            var dbName = ":memory:";
            var dbNamePtr = malloc(dbName.length());
            instance.memory().writeCString(dbNamePtr, dbName);

            exports.sqlite3Open(dbNamePtr, dbPtr);

            var dbPtrPtr = instance.memory().readInt(dbPtr);

            // Create table
            var query = "CREATE TABLE users (id int, name varchar(10))";
            var queryPtr = malloc(query.length());
            instance.memory().writeCString(queryPtr, query);
            var res0 = exports.sqlite3Exec(dbPtrPtr, queryPtr, 0, 0, 0);
            // var res0 = exports.sqlite3Exec(dbPtr, queryPtr, query.length() /* callback pointer
            // */, 0 /* callback_first_arg */, 0 /* errormsg */);
            assert (res0 != SQLITE_MISUSE);
            assert (res0 == 0);

            var query2 = "INSERT INTO users(id, name) VALUES(0, 'go'), (1, 'zig'), (2, 'whatever')";
            var query2Ptr = malloc(query2.length());
            instance.memory().writeCString(query2Ptr, query2);
            var res1 = exports.sqlite3Exec(dbPtrPtr, query2Ptr, query2.length(), 0, 0);
            assert (res1 != SQLITE_MISUSE);
            assert (res1 == 0);

            var query3 = "SELECT id, name FROM users";
            var query3Ptr = malloc(query3.length());
            var stmtPtr = malloc(4);
            instance.memory().writeCString(query3Ptr, query3);
            var res3 = exports.sqlite3Prepare(dbPtrPtr, query3Ptr, query3.length(), stmtPtr, 0);
            assert (res3 != SQLITE_MISUSE);
            assert (res3 == 0);

            var stmtPtrPtr = instance.memory().readInt(stmtPtr);
            var stmt = exports.sqlite3Step(stmtPtrPtr);
            assert (stmt == SQLITE_ROW);

            var id = exports.sqlite3ColumnInt64(stmtPtrPtr, 0);
            var txtPtr = exports.sqlite3ColumnText(stmtPtrPtr, 1);
            var txt = instance.memory().readCString(txtPtr);
            System.out.println("row: id " + id + " - txt " + txt);

            stmt = exports.sqlite3Step(stmtPtrPtr);
            assert (stmt == SQLITE_ROW);

            id = exports.sqlite3ColumnInt64(stmtPtrPtr, 0);
            txtPtr = exports.sqlite3ColumnText(stmtPtrPtr, 1);
            txt = instance.memory().readCString(txtPtr);
            System.out.println("row: id " + id + " - txt " + txt);

            stmt = exports.sqlite3Step(stmtPtrPtr);
            assert (stmt == SQLITE_ROW);

            id = exports.sqlite3ColumnInt64(stmtPtrPtr, 0);
            txtPtr = exports.sqlite3ColumnText(stmtPtrPtr, 1);
            txt = instance.memory().readCString(txtPtr);
            System.out.println("row: id " + id + " - txt " + txt);
        } catch (Exception e) {
            throw new RuntimeException(errmsg(dbPtr), e);
        }
    }

    public void openDb(int dbPtr, int dbNamePtr, int vfsName) {
        try {
            // TODO: go on from here...
            // exports.sqlite3Open -> bad parameter or other API misuse
            var res = 1; // exports.sqlite3OpenV2(dbNamePtr, dbPtr, 0x0b110, vfsName);
            if (res != 0) {
                throw new RuntimeException("Opening the database returned error code: " + res);
            }
        } catch (ChicoryException e) {
            throw new RuntimeException("failed to open the database: " + errmsg(dbPtr), e);
        }
    }
}
