build:
	cargo build ${CARGO_FLAGS}
	mv target/debug/libjwt_native.dylib ../jwt-native/src/main/resources/ 2>/dev/null || :
	mv target/debug/libjwt_native.a ../jwt-native/src/main/resources/libjwt_native_static.a 2>/dev/null || :
	mv target/debug/libjwt_native.so ../jwt-native/src/main/resources/ 2>/dev/null || :
	mv target/debug/libjwt_native.dll ../jwt-native/src/main/resources/ 2>/dev/null || :
	mv target/release/libjwt_native.dylib ../jwt-native/src/main/resources/ 2>/dev/null || :
	mv target/release/libjwt_native.a ../jwt-native/src/main/resources/libjwt_native_static.a 2>/dev/null || :
	mv target/release/libjwt_native.so ../jwt-native/src/main/resources/ 2>/dev/null || :
	mv target/release/libjwt_native.dll ../jwt-native/src/main/resources/ 2>/dev/null || :

clean:
	cargo clean

update:
	cargo update