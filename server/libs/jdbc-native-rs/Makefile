build:
	cargo build
	cp target/debug/*.dylib ../jdbc-native/src/main/resources/ 2>/dev/null || :
	cp target/debug/*.so ../jdbc-native/src/main/resources/ 2>/dev/null || :

build-release:
	cargo build --release
	cp target/debug/*.dylib ../jdbc-native/src/main/resources/ 2>/dev/null || :
	cp target/debug/*.so ../jdbc-native/src/main/resources/ 2>/dev/null || :

clean:
	cargo clean

update:
	cargo update