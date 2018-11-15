#include <stdint.h>
#include <stdlib.h>

typedef struct ProtocolBuffer {
  char *error;
  uint8_t *data;
  uintptr_t data_len;
} ProtocolBuffer;

typedef struct ExtGrant {
  const char *target;
  const char *action;
} ExtGrant;

ProtocolBuffer *create_token(const char *algorithm,
                             const char *secret,
                             int64_t expiration_in_seconds,
                             const ExtGrant *grant);

void destroy_buffer(ProtocolBuffer *buffer);

ProtocolBuffer *verify_token(const char *token,
                             const char *secrets,
                             int64_t num_secrets,
                             const ExtGrant *grant);
