build:
	cargo build
	cp target/debug/libjwt_native.dylib ../jwt-native/src/main/resources/ 2>/dev/null || :
	cp target/debug/libjwt_native.a ../jwt-native/src/main/resources/libjwt_native_static.a 2>/dev/null || :
	cp target/debug/libjwt_native.so ../jwt-native/src/main/resources/ 2>/dev/null || :

build-release:
	cargo build --release
	cp target/debug/libjwt_native.dylib ../jdbc-native/src/main/resources/ 2>/dev/null || :
	cp target/debug/libjwt_native.a ../jwt-native/src/main/resources/libjwt_native_static.a 2>/dev/null || :
	cp target/debug/libjwt_native.so ../jdbc-native/src/main/resources/ 2>/dev/null || :

clean:
	cargo clean

update:
	cargo update