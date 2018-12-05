build:
	cargo build ${CARGO_FLAGS}
	mv target/debug/libjdbc_native.dylib ../jdbc-native/src/main/resources/ 2>/dev/null || :
	mv target/debug/libjdbc_native.a ../jdbc-native/src/main/resources/libjdbc_native_static.a 2>/dev/null || :
	mv target/debug/libjdbc_native.so ../jdbc-native/src/main/resources/ 2>/dev/null || :
	mv target/debug/libjdbc_native.dll ../jdbc-native/src/main/resources/ 2>/dev/null || :
	mv target/release/libjdbc_native.dylib ../jdbc-native/src/main/resources/ 2>/dev/null || :
	mv target/release/libjdbc_native.a ../jdbc-native/src/main/resources/libjdbc_native_static.a 2>/dev/null || :
	mv target/release/libjdbc_native.so ../jdbc-native/src/main/resources/ 2>/dev/null || :
	mv target/release/libjdbc_native.dll ../jdbc-native/src/main/resources/ 2>/dev/null || :

clean:
	cargo clean

update:
	cargo update