build:
	cargo build
	cp target/debug/libjdbc_native.dylib ../jdbc-native/src/main/resources/ 2>/dev/null || :
	cp target/debug/libjdbc_native.a ../jdbc-native/src/main/resources/libjdbc_native_static.a 2>/dev/null || :
	cp target/debug/libjdbc_native.so ../jdbc-native/src/main/resources/ 2>/dev/null || :

build-release:
	cargo build --release
	cp target/debug/libjdbc_native.dylib ../jdbc-native/src/main/resources/ 2>/dev/null || :
	cp target/debug/libjdbc_native.a ../jdbc-native/src/main/resources/libjdbc_native_static.a 2>/dev/null || :
	cp target/debug/libjdbc_native.so ../jdbc-native/src/main/resources/ 2>/dev/null || :

clean:
	cargo clean

update:
	cargo update