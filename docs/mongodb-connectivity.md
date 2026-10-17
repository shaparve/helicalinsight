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

Verified on 2026-10-18 with Eclipse Temurin `25.0.4.1` and Apache Maven `3.9.16`:

- Focused tests, run from `server` with `mvn -pl adhoc -am -Dtest=MongoConnectionSupportTest,NoSqlDataSourcePropertiesTest -Dsurefire.failIfNoSpecifiedTests=false test`: `BUILD SUCCESS`; 10 tests, 0 failures, 0 errors, 0 skipped. The 8 Mongo connection tests cover URI generation, credential escaping, URI options, malformed configuration, and a bounded unreachable-local-server failure. The 2 NoSQL loader tests verify MongoDB driver routing and preserve existing JDBC driver behavior.
- Full package build, run from `server` with `mvn clean package -DskipTests`: `BUILD SUCCESS` across all 11 Maven modules, including test-source compilation and WAR packaging at `presentation/target/hi-ee-7.0.0.war`. Tests were skipped during this package command; the focused tests above were run separately.
- Live connectivity: started a temporary MongoDB `7.0.45` container on `127.0.0.1:27018`. The server ping returned `1`, and Helical Insight's `MongoConnectionSupport.testConnection` returned `true`. The temporary container was removed after the check; Docker Desktop remains running.

Live authentication failure behavior, use of the connection through Apache Drill for an actual report, and regression against an existing deployed database were not exercised in this environment. The unit tests cover malformed connection settings and an unreachable MongoDB server, but do not replace those deployment-level checks.
