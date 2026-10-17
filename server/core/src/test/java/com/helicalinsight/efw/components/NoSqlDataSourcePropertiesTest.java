package com.helicalinsight.efw.components;

import static org.junit.Assert.assertEquals;

import com.google.gson.JsonObject;
import org.junit.Test;

public class NoSqlDataSourcePropertiesTest {

    @Test
    public void mapsMongoJdbcDriverToMongoNoSqlLoader() {
        JsonObject formData = new JsonObject();
        formData.addProperty("driverName", "com.helical.mongodb.MongoJdbcDriver");

        assertEquals("com.helicalinsight.nosql.mongo", NoSqlDataSourceProperties.getSubType(formData));
    }

    @Test
    public void preservesExistingJdbcDriverNames() {
        JsonObject formData = new JsonObject();
        formData.addProperty("driverName", "com.mysql.cj.jdbc.Driver");

        assertEquals("com.mysql.cj.jdbc.Driver", NoSqlDataSourceProperties.getSubType(formData));
    }
}
