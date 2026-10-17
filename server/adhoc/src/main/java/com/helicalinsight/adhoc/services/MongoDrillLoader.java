package com.helicalinsight.adhoc.services;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.helicalinsight.datasource.nosql.NoSQLLoader;
import com.helicalinsight.efw.exceptions.EfwServiceException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Registers MongoDB connections with the Drill storage plugin.
 */
@Component("com.helicalinsight.nosql.mongo")
@Scope("prototype")
@Deprecated
public class MongoDrillLoader extends NoSQLLoader {

    @Override
    public boolean loadToMiddleWare(JsonObject formDataJson) {
        String connectionString = MongoConnectionSupport.buildConnectionUri(formDataJson);
        String storageName = formDataJson.get("name").getAsString();
        String theId = formDataJson.get("theId").getAsString();

        JsonObject mongo = new JsonObject();
        mongo.addProperty("type", "mongo");
        mongo.addProperty("connection", connectionString);
        mongo.addProperty("enabled", true);

        String drillStorageUrl = DrillCsvDataSourceCreator.getUrlOfDrill();
        String resourceUrl = drillStorageUrl + "/storage/" + storageName + "_" + theId + ".json";

        JsonObject storageJson = new JsonObject();
        storageJson.addProperty("name", storageName + "_" + theId);
        storageJson.add("config", mongo);

        String result = DrillCsvDataSourceCreator.drillRestApiCall(resourceUrl, "POST", storageJson.toString());
        if (result == null) {
            throw new EfwServiceException(
                    "MongoDB storage registration failed. Verify that Apache Drill is available and the connection settings are valid.");
        }

        try {
            JsonObject response = new Gson().fromJson(result, JsonObject.class);
            if (response == null) {
                throw new EfwServiceException("Apache Drill returned an empty response while registering MongoDB.");
            }
            if (hasError(response, "errorMessage") || hasError(response, "errorId")
                    || hasError(response, "errors") || hasError(response, "error")) {
                throw new EfwServiceException(
                        "Apache Drill rejected the MongoDB storage configuration. Verify the Drill service and connection settings.");
            }
        } catch (JsonSyntaxException | IllegalStateException e) {
            throw new EfwServiceException("Apache Drill returned an invalid response while registering MongoDB.");
        }
        return true;
    }

    private boolean hasError(JsonObject response, String key) {
        JsonElement error = response.get(key);
        if (error == null || error.isJsonNull()) {
            return false;
        }
        if (error.isJsonArray()) {
            return !error.getAsJsonArray().isEmpty();
        }
        if (error.isJsonObject()) {
            return !error.getAsJsonObject().entrySet().isEmpty();
        }
        return !"".equals(error.getAsString()) && !"false".equalsIgnoreCase(error.getAsString());
    }

    @Override
    public boolean testConnection(JsonObject formData) {
        return MongoConnectionSupport.testConnection(formData);
    }
}
