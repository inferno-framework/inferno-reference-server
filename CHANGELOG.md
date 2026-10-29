# Changelog

## 5.0.0

### Upgrade steps

This version upgrades HAPI FHIR from 7.6.0 to 8.12.1.
Existing servers must be rebuilt: reset the database as described in
[Resetting the server](README.md#resetting-the-server), then start the server so the data is reloaded.

### Changes

- **Read-only mode blocks POST requests with write operations.** Batches and transactions are only allowed if every
  entry is a `GET`, and only a fixed list of read-only `$operations` may be called (for example
  `$export`, `$everything`, `$expand`). Everything else returns `405 Method Not Allowed`.
  Bulk Data `$export`, and cancelling an export job, are still allowed.
- **The CapabilityStatement is no longer cached.** It is now built fresh for every request.
  Previously the first response was reused, which could serve `http` URLs to `https` clients.
- **Dependency upgrades** to address known vulnerabilities: HAPI FHIR 8.12.1, Jetty 12.0.40
  (including the Docker base image), PostgreSQL driver, Logback, Spring Boot, Jackson, Guava
  and others. Advisories that cannot be fixed yet are listed in a comment in `pom.xml`.
