dev-mysql:
	docker-compose -f docker-compose/dev-mysql.yml up -d --remove-orphans

dev-postgres:
	docker-compose -f docker-compose/dev-postgres.yml up -d --remove-orphans

dev-down:
	docker-compose -f docker-compose/dev-mysql.yml down -v --remove-orphans
	docker-compose -f docker-compose/dev-postgres.yml down -v --remove-orphans
