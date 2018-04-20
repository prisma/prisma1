# todo prod (sim) env commands

dev-mysql:
	docker-compose -f docker-compose/mysql/dev-mysql.yml up -d --remove-orphans
	cp ./docker-compose/mysql/prisma.yml .

dev-postgres:
	docker-compose -f docker-compose/postgres/dev-postgres.yml up -d --remove-orphans
	cp ./docker-compose/postgres/prisma.yml .

dev-down:
	docker-compose -f docker-compose/dev-mysql.yml down -v --remove-orphans
	docker-compose -f docker-compose/dev-postgres.yml down -v --remove-orphans
