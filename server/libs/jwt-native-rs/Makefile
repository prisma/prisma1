build:
	cargo build
	cp target/debug/*.dylib ../jwt-native/src/main/resources/ 2>/dev/null || :
	cp target/debug/*.so ../jwt-native/src/main/resources/ 2>/dev/null || :

clean:
	cargo clean

update:
	cargo update