// Extracts a user-facing message from an RFC 7807 problem detail returned by the API.
// Validation failures list every violation in `errors`; other problems explain themselves in `detail`.
export const problemMessage = (data, fallback) =>
  data?.errors?.join(" ") || data?.detail || fallback;
