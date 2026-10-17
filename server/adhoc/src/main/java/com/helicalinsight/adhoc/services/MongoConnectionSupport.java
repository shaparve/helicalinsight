package com.helicalinsight.adhoc.services;

import com.google.gson.JsonObject;
import com.helicalinsight.efw.exceptions.EfwServiceException;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;
import com.mongodb.MongoSecurityException;
import com.mongodb.MongoTimeoutException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.bson.Document;

final class MongoConnectionSupport {

    private static final String DEFAULT_PORT = "27017";

    private MongoConnectionSupport() {
    }

    static String buildConnectionUri(JsonObject formData) {
        String uri = value(formData, "jdbcUrl");
        if (isBlank(uri)) {
            uri = buildUriFromFields(formData);
        }

        MongoClientURI parsedUri = parseUri(uri);
        String username = value(formData, "userName");
        String password = value(formData, "password");

        if (parsedUri.getCredentials() == null) {
            if (isBlank(username) != isBlank(password)) {
                throw new IllegalArgumentException("MongoDB username and password must both be provided.");
            }
            if (!isBlank(username)) {
                uri = addCredentials(uri, username, password);
            }
        }

        uri = addQueryOption(uri, "authSource", value(formData, "authSource"));
        uri = addQueryOption(uri, "authMechanism", value(formData, "authMechanism"));
        if ("true".equalsIgnoreCase(value(formData, "ssl"))) {
            uri = addQueryOption(uri, "ssl", "true");
        }

        parseUri(uri);
        return uri;
    }

    static boolean testConnection(JsonObject formData) {
        try {
            String connectionUri = buildConnectionUri(formData);
            MongoClientURI parsedUri = parseUri(connectionUri);
            String database = firstNonBlank(value(formData, "database"), value(formData, "databaseName"));
            if (isBlank(database)) {
                database = parsedUri.getDatabase();
            }
            if (isBlank(database)) {
                throw new IllegalArgumentException("MongoDB database is required.");
            }

            try (MongoClient client = new MongoClient(parsedUri)) {
                Document response = client.getDatabase(database).runCommand(new Document("ping", 1));
                Object result = response.get("ok");
                if (result instanceof Number && ((Number) result).doubleValue() == 1.0d) {
                    return true;
                }
                throw new EfwServiceException("MongoDB did not confirm the connection test.");
            }
        } catch (MongoSecurityException e) {
            throw new EfwServiceException(
                    "MongoDB authentication failed. Check the username, password, authentication database, and mechanism.");
        } catch (MongoTimeoutException e) {
            throw new EfwServiceException(
                    "MongoDB did not respond before the connection timed out. Check the host, port, and network access.");
        } catch (MongoException e) {
            throw new EfwServiceException(
                    "MongoDB connection failed. Verify the server, credentials, database permissions, and connection options.");
        } catch (IllegalArgumentException e) {
            throw new EfwServiceException(
                    "Invalid MongoDB connection settings. Verify the MongoDB URI, host, port, and database.");
        }
    }

    private static String buildUriFromFields(JsonObject formData) {
        String host = firstNonBlank(value(formData, "hostName"), value(formData, "host"));
        String database = firstNonBlank(value(formData, "database"), value(formData, "databaseName"));
        if (isBlank(host) || isBlank(database)) {
            throw new IllegalArgumentException("MongoDB host and database are required.");
        }

        String port = value(formData, "port");
        String authority = formatHost(host);
        if (!host.endsWith("]") && !hasPort(host)) {
            authority += ":" + (isBlank(port) ? DEFAULT_PORT : port);
        } else if (host.endsWith("]") && !isBlank(port)) {
            authority += ":" + port;
        }
        return "mongodb://" + authority + "/" + encode(database);
    }

    private static String formatHost(String host) {
        if (host.startsWith("[") && host.endsWith("]")) {
            return host;
        }
        if (count(host, ':') > 1) {
            return "[" + host + "]";
        }
        return host;
    }

    private static boolean hasPort(String host) {
        return count(host, ':') == 1 && host.matches(".+:\\d+");
    }

    private static int count(String value, char character) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == character) {
                count++;
            }
        }
        return count;
    }

    private static MongoClientURI parseUri(String uri) {
        if (isBlank(uri) || !(uri.startsWith("mongodb://") || uri.startsWith("mongodb+srv://"))) {
            throw new IllegalArgumentException("MongoDB URI must use mongodb:// or mongodb+srv://.");
        }
        try {
            return new MongoClientURI(uri);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid MongoDB connection URI.");
        }
    }

    private static String addCredentials(String uri, String username, String password) {
        int schemeEnd = uri.indexOf("://") + 3;
        int authorityEnd = uri.length();
        for (char delimiter : new char[] {'/', '?', '#'}) {
            int index = uri.indexOf(delimiter, schemeEnd);
            if (index >= 0 && index < authorityEnd) {
                authorityEnd = index;
            }
        }
        if (uri.substring(schemeEnd, authorityEnd).contains("@")) {
            return uri;
        }
        String credentials = encode(username) + ":" + encode(password) + "@";
        return uri.substring(0, schemeEnd) + credentials + uri.substring(schemeEnd);
    }

    private static String addQueryOption(String uri, String name, String value) {
        if (isBlank(value) || hasQueryOption(uri, name)) {
            return uri;
        }
        return uri + (uri.contains("?") ? "&" : "?") + name + "=" + encode(value);
    }

    private static boolean hasQueryOption(String uri, String name) {
        int queryStart = uri.indexOf('?');
        if (queryStart < 0) {
            return false;
        }
        String query = uri.substring(queryStart + 1);
        for (String option : query.split("&")) {
            int equals = option.indexOf('=');
            String optionName = equals < 0 ? option : option.substring(0, equals);
            if (name.equalsIgnoreCase(optionName)) {
                return true;
            }
        }
        return false;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String value(JsonObject formData, String key) {
        if (formData == null || !formData.has(key) || formData.get(key).isJsonNull()) {
            return "";
        }
        return formData.get(key).getAsString().trim();
    }

    private static String firstNonBlank(String first, String second) {
        return isBlank(first) ? second : first;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
