# MongoDB connectivity

Helical Insight exposes MongoDB through its existing NoSQL data-source workflow. The server uses the Mongo Java driver (`org.mongodb:mongo-java-driver:3.12.10`) to test the connection and registers the connection with the configured Apache Drill storage plugin for report use.

## Prerequisites

- MongoDB must be reachable from both Helical Insight and Apache Drill.
- Apache Drill integration must be enabled. The current data-source list exposes MongoDB only when Drill is enabled.
- Use a MongoDB account scoped to the required database and collections.

## Configure a connection

1. Open **Data Sources** and select **Mongodb** under **No SQL & Big Data**.
2. Select the `com.helicalinsight.nosql.mongo` driver.
3. Enter a data-source name, MongoDB host, port (normally `27017`), database, and collection.
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

From the `server` directory, inspect the resolved driver dependency, run the Mongo connection tests, and package the affected modules:

```bash
mvn -pl adhoc dependency:tree -Dincludes=org.mongodb:mongo-java-driver
mvn -pl adhoc -am -Dtest=MongoConnectionSupportTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl adhoc -am -DskipTests package
```

## Verification results

Verified with Eclipse Temurin `25.0.4.1` and Apache Maven `3.9.11`:

- Dependency tree: `BUILD SUCCESS`; `org.mongodb:mongo-java-driver:3.12.10:compile` resolves for `hi-adhoc`.
- Focused test command: `BUILD SUCCESS`; 8 tests, 0 failures, 0 errors, 0 skipped. Tests cover URI generation, credential escaping, URI options, malformed configuration, and a bounded unreachable-local-server failure.
- Package command: `BUILD SUCCESS` for `parent`, `hi-core`, `hi-cache`, and `hi-adhoc`.

The unit tests do not require a live MongoDB server. A successful authenticated connection, invalid live credentials, and report use through Drill still need deployment-level verification against a MongoDB instance and enabled Drill service. The unavailable-server unit test uses loopback port `1`; it does not replace a live environment test. An existing relational connection was not exercised against a database server in this environment.
