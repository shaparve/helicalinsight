# MongoDB connectivity

Helical Insight exposes MongoDB through its existing NoSQL data-source workflow. The server uses the Mongo Java driver (`org.mongodb:mongo-java-driver:3.12.10`) to test the connection and registers the connection with the configured Apache Drill storage plugin for report use.

## Prerequisites

- MongoDB must be reachable from both Helical Insight and Apache Drill.
- Apache Drill must be configured and available for the NoSQL data-source provider.
- Use a MongoDB account scoped to the required database and collections.

## Configure a connection

1. Open **Data Sources** and select **Helical Mongodb** (`com.helical.mongodb.MongoJdbcDriver`).
2. Select **NoSql Datasource** as the data-source provider. This routes connection testing and save through Helical Insight's MongoDB/Drill integration.
3. Enter a data-source name, MongoDB host, port (normally `27017`), and database.
4. For an authenticated deployment, enter the MongoDB username and password in their respective fields. Reserved characters in these credentials are URL-encoded when the MongoDB URI is built.
5. In **Other Options**, optionally enter MongoDB URI options, beginning with `?`. For example:

   ```text
   ?authSource=admin&authMechanism=SCRAM-SHA-256&ssl=true
   ```

   `authSource` selects the authentication database; `authMechanism` is optional when the driver/server default is suitable; `ssl=true` enables TLS.
6. Select **Test Connection**. Helical Insight sends a MongoDB `ping` command to validate network access, database selection, and authentication.
7. Save the data source, then select its database and collection when creating metadata or reports.

A typical unauthenticated configuration uses host `localhost`, port `27017`, database `analytics`, and collection `events`. Where a full MongoDB URI is supplied through an integration/API, it has this shape:

```text
mongodb://<username>:<percent-encoded-password>@<host>:27017/<database>?authSource=admin&ssl=true
```

Use placeholders only in documentation and tickets; never commit real credentials. Credentials entered in the separate username/password fields are added to the URI when the connection is tested and registered with Drill. Restrict access to Drill storage-plugin configuration and the saved data source, and use least-privilege database credentials.

## Build and tests

From the `server` directory, inspect the resolved driver dependency, run the focused tests, and package the application:

```bash
mvn -pl adhoc dependency:tree -Dincludes=org.mongodb:mongo-java-driver
mvn -pl adhoc -am -Dtest=MongoConnectionSupportTest,NoSqlDataSourcePropertiesTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn clean package -DskipTests
```

For runtime prerequisites, WAR deployment, and Tomcat startup steps, follow the [backend run instructions](../server/README.md#running-the-backend).

## Verification status

Previously recorded verification with Eclipse Temurin `25.0.4.1` and Apache Maven `3.9.11`:

- Dependency tree: `BUILD SUCCESS`; `org.mongodb:mongo-java-driver:3.12.10:compile` resolves for `hi-adhoc`.
- Focused Mongo connection tests: `BUILD SUCCESS`; 8 tests, 0 failures, 0 errors, 0 skipped. Tests cover URI generation, credential escaping, URI options, malformed configuration, and a bounded unreachable-local-server failure.
- Package command: `BUILD SUCCESS` for `parent`, `hi-core`, `hi-cache`, and `hi-adhoc`.

The MongoDB-to-NoSQL-loader routing tests were added after that recorded run and still need to be executed. In the current workspace, Maven and Java are not installed or available on `PATH`, so the updated Java tests and build could not be rerun. The Docker engine is also unavailable. A successful authenticated connection, invalid live credentials, report use through Drill, and a live existing-database regression therefore still need deployment-level verification. The unavailable-server unit test uses loopback port `1`; it does not replace a live environment test.
