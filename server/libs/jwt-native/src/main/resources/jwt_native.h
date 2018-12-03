#include <stdint.h>
#include <stdlib.h>

typedef struct ProtocolBuffer {
  char *error;
  uint8_t *data;
  uintptr_t data_len;
} ProtocolBuffer;

ProtocolBuffer *create_token(const char *algorithm,
                             const char *secret,
                             int64_t expiration_in_seconds,
                             const char *allowed_target,
                             const char *allowed_action);

ProtocolBuffer *verify_token(const char *token,
                             const char **secrets,
                             int64_t num_secrets,
                             const char *expect_target,
                             const char *expect_action);

void destroy_buffer(ProtocolBuffer *buffer);
