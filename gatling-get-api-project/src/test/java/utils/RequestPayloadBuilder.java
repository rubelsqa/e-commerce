package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class RequestPayloadBuilder {

    private RequestPayloadBuilder() {
    }

    public static Iterator<Map<String, Object>> buildFeederFromCsv(String filePath) {
        Set<String> users = new LinkedHashSet<>();
        Set<String> entitlements = new LinkedHashSet<>();
        List<Map<String, Object>> combinations = new ArrayList<>();

        try (BufferedReader br = Files.newBufferedReader(Paths.get(filePath))) {
            String line = br.readLine(); // skip header

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split(",", -1);
                if (parts.length < 2) {
                    continue;
                }

                String user = parts[0].trim();
                String entitlement = parts[1].trim();

                if (!user.isEmpty()) {
                    users.add(user);
                }
                if (!entitlement.isEmpty()) {
                    entitlements.add(entitlement);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CSV file: " + filePath, e);
        }

        for (String user : users) {
            for (String entitlement : entitlements) {
                Map<String, Object> row = new HashMap<>();
                row.put("User", user);
                row.put("Entitlement", entitlement);
                combinations.add(row);
            }
        }

        return combinations.iterator();
    }

    public static String buildJson(String user, String entitlement) {
        return "{"
               + "\"manager\":\"A123456\","
               + "\"identityName\":\"" + user + "\","
               + "\"account\":\"" + user + "\","
               + "\"entitlement\":\"" + entitlement + "\","
               + "\"status\":\"ACTIVE\""
               + "}";
    }
}