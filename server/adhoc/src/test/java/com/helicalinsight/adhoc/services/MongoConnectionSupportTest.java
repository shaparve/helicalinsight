package com.helicalinsight.adhoc.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;
import com.mongodb.MongoClientURI;
import com.helicalinsight.efw.exceptions.EfwServiceException;
import org.junit.Test;

public class MongoConnectionSupportTest {

    @Test
    public void buildsUriFromHostPortAndDatabase() {
        JsonObject formData = new JsonObject();
        formData.addProperty("hostName", "mongo.example");
        formData.addProperty("port", "27018");
        formData.addProperty("database", "reports");

        assertEquals("mongodb://mongo.example:27018/reports",
                MongoConnectionSupport.buildConnectionUri(formData));
    }

    @Test
    public void encodesSeparateCredentialsAndAddsAuthenticationOptions() {
        JsonObject formData = new JsonObject();
        formData.addProperty("jdbcUrl", "mongodb://mongo.example:27017/reports?ssl=true");
        formData.addProperty("userName", "report@reader");
        formData.addProperty("password", "p:ass word");
        formData.addProperty("authSource", "admin");
        formData.addProperty("authMechanism", "SCRAM-SHA-256");

        String uri = MongoConnectionSupport.buildConnectionUri(formData);
        MongoClientURI parsedUri = new MongoClientURI(uri);

        assertEquals("report@reader", parsedUri.getCredentials().getUserName());
        assertEquals("p:ass word", new String(parsedUri.getCredentials().getPassword()));
        assertTrue(uri.contains("authSource=admin"));
        assertTrue(uri.contains("authMechanism=SCRAM-SHA-256"));
        assertTrue(uri.contains("ssl=true"));
    }

    @Test
    public void preservesCredentialsAndOptionsAlreadyInUri() {
        String uri = "mongodb://reader:p%3Aword@mongo.example/reports?authSource=admin&ssl=true";
        JsonObject formData = new JsonObject();
        formData.addProperty("jdbcUrl", uri);

        assertEquals(uri, MongoConnectionSupport.buildConnectionUri(formData));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsPartialSeparateCredentials() {
        JsonObject formData = new JsonObject();
        formData.addProperty("jdbcUrl", "mongodb://mongo.example/reports");
        formData.addProperty("userName", "reader");

        MongoConnectionSupport.buildConnectionUri(formData);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnsupportedUriScheme() {
        JsonObject formData = new JsonObject();
        formData.addProperty("jdbcUrl", "jdbc:mongodb://mongo.example/reports");

        MongoConnectionSupport.buildConnectionUri(formData);
    }

    @Test(expected = IllegalArgumentException.class)
    public void requiresHostAndDatabaseWhenUriIsNotProvided() {
        MongoConnectionSupport.buildConnectionUri(new JsonObject());
    }

    @Test
    public void invalidConnectionUriReturnsSafeErrorMessage() {
        JsonObject formData = new JsonObject();
        formData.addProperty("jdbcUrl", "mongodb://reader:secret@mongo.example:notaport/reports");

        try {
            MongoConnectionSupport.testConnection(formData);
        } catch (EfwServiceException e) {
            assertTrue(e.getMessage().contains("Invalid MongoDB connection settings"));
            assertFalse(e.getMessage().contains("secret"));
            return;
        }
        throw new AssertionError("Invalid MongoDB URI should fail validation.");
    }

    @Test
    public void unavailableServerReturnsSafeErrorMessage() {
        JsonObject formData = new JsonObject();
        formData.addProperty("jdbcUrl",
                "mongodb://reader:secret@127.0.0.1:1/reports?serverSelectionTimeoutMS=1000&connectTimeoutMS=1000");

        try {
            MongoConnectionSupport.testConnection(formData);
        } catch (EfwServiceException e) {
            assertTrue(e.getMessage().contains("MongoDB connection failed")
                    || e.getMessage().contains("MongoDB did not respond"));
            assertFalse(e.getMessage().contains("secret"));
            return;
        }
        throw new AssertionError("Unavailable MongoDB server should fail the connection test.");
    }
}
