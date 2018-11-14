#include <stdint.h>
#include <stdlib.h>

typedef struct PsqlConnection PsqlConnection;
typedef struct PsqlPreparedStatement PsqlPreparedStatement;

typedef struct PointerAndError {
  const char *error;
  const char *pointer;
} PointerAndError;

PsqlConnection *newConnection(const char *url);

PointerAndError *prepareStatement(const PsqlConnection *conn, const char *query);

const char *closeConnection(PsqlConnection *conn);

const char *closeStatement(PsqlPreparedStatement *stmt);

const char *commitTransaction(PsqlConnection *conn);

const char *executePreparedstatement(const PsqlPreparedStatement *stmt, const char *params);

const char *queryPreparedstatement(const PsqlPreparedStatement *stmt, const char *params);

const char *rollbackTransaction(PsqlConnection *conn);

const char *sqlExecute(const PsqlConnection *conn, const char *query, const char *params);

const char *sqlQuery(const PsqlConnection *conn, const char *query, const char *params);

const char *startTransaction(PsqlConnection *conn);

void destroy(PointerAndError *pointerAndError);

void destroy_string(const char *s);