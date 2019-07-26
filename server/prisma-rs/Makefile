default:
	cargo build

# Build the crates with deny-warnings on to emulate CI
pedantic:
	RUSTFLAGS="-D warnings" cargo build

release:
	cargo build --release


dev-sqlite:
	cp dev-configs/sqlite.yml prisma.yml
	echo 'sqlite' > current_connector

dev-postgres:
	docker-compose -f ../docker-compose/postgres/dev-postgres.yml up -d --remove-orphans
	cp dev-configs/postgres.yml prisma.yml
	echo 'postgres' > current_connector

use-local-migration-engine:
	cargo build --release
	cp target/release/migration-engine $(PRISMA2_BINARY_PATH)/

use-local-query-engine:
	cargo build --release
	cp target/release/prisma $(PRISMA2_BINARY_PATH)/runtime/

