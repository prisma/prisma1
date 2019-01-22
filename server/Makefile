# todo prod (sim) env commands

dev-mysql:
	docker-compose -f docker-compose/mysql/dev-mysql.yml up -d --remove-orphans
	cp ./docker-compose/mysql/prisma.yml .

dev-postgres-active:
	docker-compose -f docker-compose/postgres/dev-postgres.yml up -d --remove-orphans
	cp ./docker-compose/postgres/prisma_active.yml ./prisma.yml

dev-postgres-passive:
	docker-compose -f docker-compose/postgres/dev-postgres.yml up -d --remove-orphans
	cp ./docker-compose/postgres/prisma_passive.yml ./prisma.yml

dev-mongo:
	docker-compose -f docker-compose/mongo/dev-mongo.yml up -d --remove-orphans
	cp ./docker-compose/mongo/prisma.yml ./prisma.yml

dev-sqlite:
	docker-compose -f docker-compose/sqlite/dev-sqlite.yml up -d --remove-orphans
	cp ./docker-compose/sqlite/prisma.yml ./prisma.yml

dev-down:
	docker-compose -f docker-compose/mysql/dev-mysql.yml down -v --remove-orphans
	docker-compose -f docker-compose/postgres/dev-postgres.yml down -v --remove-orphans
	docker-compose -f docker-compose/mongo/dev-mongo.yml down -v --remove-orphans
	docker-compose -f docker-compose/sqlite/dev-sqlite.yml down -v --remove-orphans
