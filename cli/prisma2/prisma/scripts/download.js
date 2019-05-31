const { ensureMigrationBinary } = require('@prisma/fetch-engine')
eval(`ensureMigrationBinary(require('path').join(__dirname, '../'))`)
