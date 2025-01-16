package com.github.andreaTP.sqlite.wasm;

import com.dylibso.chicory.experimental.hostmodule.annotations.WasmModuleInterface;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.ChicoryException;
import com.dylibso.chicory.wasm.Parser;

@WasmModuleInterface("wasm/libsqlite3-opt.wasm")
public class SQLite implements SQLite_ModuleImports, SQLite_Env {
    private final Instance instance;
    private final SQLite_ModuleExports exports;

    public SQLite() {
        this.instance =
        // interpreter mode:
        //                Instance.builder(
        //                                Parser.parse(
        //                                        SQLite.class.getResourceAsStream(
        //                                                "/wasm/libsqlite3-opt.wasm")))
        //                        .withImportValues(toImportValues())
        //                        .build();
        // pre compiled mode:
                        Instance.builder(SQLiteModule.load())
                                .withMachineFactory(SQLiteModule::create)
                                .withImportValues(toImportValues())
                                .build();
        this.exports = () -> instance;
    }

    @Override
    public int sqlite3OsInit() {
        return 0;
    }

    @Override
    public int sqlite3OsEnd() {
        return 0;
    }

    @Override
    public SQLite_Env env() {
        return this;
    }

    // Demo implementation of this simple interface:
    // https://github.com/mathetake/wazero-sqlite/blob/main/main.go
    static class StringPtr {
        private final int ptr;
        private final int size;

        StringPtr(int ptr, int size) {
            this.ptr = ptr;
            this.size = size;
        }

        public int ptr() {
            return ptr;
        }

        public int size() {
            return size;
        }
    }

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

    public void demoOpenDB() {
        exports.Initialize();
        var dbPtr = exports.LibcMalloc(4);
        System.out.println(errmsg(dbPtr));
        try {
            var dbName = ":memory:";
            var dbNamePtr = exports.LibcMalloc(dbName.length());
            instance.memory().writeCString(dbNamePtr, dbName);

            var vfsName = "";
            var vfsNamePtr = exports.LibcMalloc(vfsName.length());
            instance.memory().writeCString(vfsNamePtr, vfsName);

            exports.sqlite3OpenV2(dbNamePtr, dbPtr, 0b110, vfsNamePtr);
            // exports.sqlite3Open(dbNamePtr, dbPtr);
        } catch (Exception e) {
            throw new RuntimeException(errmsg(dbPtr), e);
        }
    }

    public void openDb(int dbPtr, int dbNamePtr, int vfsName) {
        try {
            // exports.sqlite3Open -> bad parameter or other API misuse
            var res = exports.sqlite3OpenV2(dbNamePtr, dbPtr, 0x0b110, vfsName);
            if (res != 0) {
                throw new RuntimeException("Opening the database returned error code: " + res);
            }
        } catch (ChicoryException e) {
            throw new RuntimeException("failed to open the database: " + errmsg(dbPtr), e);
        }
    }
}
