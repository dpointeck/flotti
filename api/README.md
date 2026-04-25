# flotti

This project was created using the [Ktor Project Generator](https://start.ktor.io).

Here are some useful links to get you started:
 * [Ktor Documentation](https://ktor.io/docs/home.html)
 * [Ktor GitHub page](https://github.com/ktorio/ktor)
 * [Ktor Slack chat](https://app.slack.com/client/T09229ZC6/C0A974TJ9). [Request an invite](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up).


## Features
Here's a list of features included in this project:

| Name | Description |
|------|-------------|
| [kotlinx.serialization](https://start.ktor.io/p/io.ktor/server-kotlinx-serialization) | Handles JSON serialization using kotlinx.serialization library |
| [Content Negotiation](https://start.ktor.io/p/io.ktor/server-content-negotiation) | Provides automatic content conversion according to Content-Type and Accept headers |
| [PostgreSQL](https://start.ktor.io/p/org.jetbrains/server-postgres) | Adds Postgres database support |
| [Exposed](https://start.ktor.io/p/org.jetbrains/server-exposed) | Adds Exposed database to your application |
| [Sessions](https://start.ktor.io/p/io.ktor/server-sessions) | Adds support for persistent sessions through cookies or headers |
| [CSRF](https://start.ktor.io/p/io.ktor/server-csrf) | Cross-site request forgery mitigation |


## Building & Running
To build or run the project, use one of the following tasks:


| Task | Description |
|------|-------------|

### Database configuration

Ktor resolves `postgres.url` in `src/main/resources/application.yaml` from a single `DATABASE_URL` environment variable, with this local default:

```text
jdbc:postgresql://localhost:5432/flotti?user=postgres
```

`DATABASE_URL` uses the standard JDBC URL format. For example, if your local Postgres is on port `5434` and uses `postgres` / `postgres`:

```shell
DATABASE_URL='jdbc:postgresql://localhost:5434/flotti?user=postgres&password=postgres' ./gradlew run
```

For local development, you can also put this in `.env`:

```dotenv
DATABASE_URL=jdbc:postgresql://localhost:5434/flotti?user=postgres&password=postgres
```

Real environment variables take precedence over values in `.env`.

Integration tests use temporary databases on the same Postgres server configured by `DATABASE_URL`. You can run them inline:

```shell
DATABASE_URL='jdbc:postgresql://localhost:5434/flotti?user=postgres&password=postgres' ./gradlew test
```

Or use the same `.env` file and run:

```shell
./gradlew test
```

The test database itself is still auto-generated as `flotti_test_{timestamp}` and dropped after the test run.

If the server starts successfully, you'll see the following output:
```
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8080
```
