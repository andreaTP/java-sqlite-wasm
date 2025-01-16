#! /bin/bash
set -euxo pipefail

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# TODO: improve this script and automate it fully

# PREPARATION: into this script folder

# Download WASI-SDK
# rm -f wasi-sdk-24.0-x86_64-linux.tar.gz
# rm -rf wasi-sdk
# wget https://github.com/WebAssembly/wasi-sdk/releases/download/wasi-sdk-25/wasi-sdk-25.0-x86_64-linux.tar.gz
# tar -xvf wasi-sdk-25.0-x86_64-linux.tar.gz
# mv wasi-sdk-25.0-x86_64-linux wasi-sdk
# rm wasi-sdk-25.0-x86_64-linux.tar.gz

# Download the latest SQLite amalgamation 3.48.0 as of today
# rm -rf sqlite-amalgamation
# wget https://www.sqlite.org/2025/sqlite-amalgamation-3480000.zip
# unzip sqlite-amalgamation-3480000.zip
# mv sqlite-amalgamation-3480000 sqlite-amalgamation
# rm sqlite-amalgamation-3480000.zip

# Download and install Binaryen for optimizations
# rm -rf binaryen
# wget https://github.com/WebAssembly/binaryen/releases/download/version_121/binaryen-version_121-x86_64-linux.tar.gz
# tar -xvf binaryen-version_121-x86_64-linux.tar.gz
# mv binaryen-version_121 binaryen
# rm binaryen-version_121-x86_64-linux.tar.gz

export WASI_SDK_PATH=${SCRIPT_DIR}/wasi-sdk

rm -f ${SCRIPT_DIR}libsqlite3.wasm

# rm -f ${SCRIPT_DIR}/sqlite-amalgamation/sqlite_wrapper.c
# cp ${SCRIPT_DIR}/sqlite_wrapper.c ${SCRIPT_DIR}/sqlite-amalgamation/

rm -f ${SCRIPT_DIR}/sqlite-amalgamation/sqlite_opt.h
rm -f ${SCRIPT_DIR}/sqlite-amalgamation/sqlite_cfg.h
cp ${SCRIPT_DIR}/sqlite_opt.h ${SCRIPT_DIR}/sqlite-amalgamation/
cp ${SCRIPT_DIR}/sqlite_cfg.h ${SCRIPT_DIR}/sqlite-amalgamation/

(
    cd ${SCRIPT_DIR}/sqlite-amalgamation
    # --sysroot=${WASI_SDK_PATH}/share/wasi-sysroot \
    # ${WASI_SDK_PATH}/bin/clang \
    #     -Wall -Wextra -Wno-unused-parameter -Wno-unused-function \
    #     --target=wasm32-wasi \
    #     -o ../libsqlite3.wasm \
    #     sqlite3.c sqlite_wrapper.c \
    #     -D_HAVE_SQLITE_CONFIG_H \
	#     -DSQLITE_CUSTOM_INCLUDE=sqlite_opt.h \
    #     -Wl,--export=sqlite_open \
    #     -Wl,--export=sqlite_exec \
    #     -Wl,--export=sqlite_errmsg \
    #     -Wl,--export=realloc \
    #     -mexec-model=reactor \
    #     -g0 \
    #     -Oz

    ${WASI_SDK_PATH}/bin/clang --sysroot=${WASI_SDK_PATH}/share/wasi-sysroot \
        --target=wasm32-wasi \
        -o ../libsqlite3.wasm \
        sqlite3.c \
        -D_HAVE_SQLITE_CONFIG_H \
	    -DSQLITE_CUSTOM_INCLUDE=sqlite_opt.h \
        -Wl,--export=sqlite3_open \
        -Wl,--export=sqlite3_exec \
        -Wl,--export=sqlite3_errmsg \
        -Wl,--export=realloc \
        -Wl,--export=get_result_ptr \
        -Wl,--export=get_result_size \
        -Wl,--export=sqlite3_step \
        -Wl,--export=sqlite3_column_int64 \
        -Wl,--export=sqlite3_column_text \
        -Wl,--export=sqlite3_prepare \
        -Wl,--allow-undefined \
        -Wl,--no-entry \
        -mexec-model=reactor \
        -g0 -Oz
)

${SCRIPT_DIR}/binaryen/bin/wasm-opt -g --strip --strip-producers -c -O3 \
    ${SCRIPT_DIR}/libsqlite3.wasm -o ${SCRIPT_DIR}/libsqlite3-opt.wasm

rm ${SCRIPT_DIR}/../core/src/main/resources/wasm/libsqlite3-opt.wasm
cp ${SCRIPT_DIR}/libsqlite3-opt.wasm ${SCRIPT_DIR}/../core/src/main/resources/wasm/libsqlite3-opt.wasm
