build:
	whoami
	cargo build
	cp target/debug/*.dylib ../jdbc-native/src/main/resources/ 2>/dev/null
	cp target/debug/*.so ../jdbc-native/src/main/resources/ 2>/dev/null

update:
	cargo update