package org.firstinspires.ftc.teamcode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import android.content.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.util.Arrays;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class AutoDataStorageUtils {
    private AutoDataStorageUtils() {

    }

    public enum ArtifactColor {
        GREEN,
        PURPLE
    }

    public static class ArtifactSequence {
        ArtifactColor[] sequence;

        public ArtifactSequence() {

        }

        public ArtifactSequence(ArtifactColor[] sequence) {
            this.sequence = sequence;
        }

        public ArtifactColor[] getSequence() {
            return sequence;
        }

        public void setSequence(@NonNull ArtifactColor[] sequence) {
            if (sequence.length != 3) {
                throw new IllegalArgumentException("Sequence must have 3 elements");
            }
            this.sequence = sequence;
        }

        public String toString(int index) {
            if (index < 0 || index >= sequence.length) {
                throw new IllegalArgumentException("Index out of bounds");
            }

            ArtifactColor color = sequence[index];

            switch (color) {
                case GREEN:
                    return "GREEN";
                case PURPLE:
                    return "PURPLE";
            }
            throw new IllegalStateException("Unexpected ArtifactColor value: " + color);
        }
    }

    public static class Coords {
        double x;
        double y;
        double z;
        double pitch;
        double roll;
        double yaw;

        public void setCoords(double x, double y, double z, double pitch, double roll, double yaw) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.pitch = pitch;
            this.roll = roll;
            this.yaw = yaw;
        }
    }

    public static class DateMs {
        private long date_ms;

        public DateMs(long init_ms) {
            date_ms = init_ms;
        }

        public long getDateMs() {
            return date_ms;
        }

        public void setDateMs(long new_ms) {
            date_ms = new_ms;
        }

        public double getMinutesTimeSince() {
            long differenceMillis = System.currentTimeMillis() - date_ms;
            return (double) differenceMillis / 1000 / 60;
        }
    }

    public static class MotorPositions {
        private Map<String, Double> motorPositions = new LinkedHashMap<>();

        private MotorPositions(Builder builder) {
            this.motorPositions = builder.motorPositions;
        }

        public static class Builder {
            private Map<String, Double> motorPositions = new LinkedHashMap<>();

            public Builder() {

            }

            public Builder addMotor(String motorName, double position) {
                if (motorName == null) {
                    throw new IllegalArgumentException("Motor name cannot be null in Builder.");
                }

                motorPositions.put(motorName, position);

                return this;
            }

            public MotorPositions build() {
                return new MotorPositions(this);
            }
        }

        public void addMotorPosition(String motorName, double position) {
            if (motorName == null) {
                throw new IllegalArgumentException("Motor name cannot be null.");
            }

            motorPositions.put(motorName, position);
        }

        public double getMotorPosition(@NonNull String motorName) {
            if (motorPositions.containsKey(motorName)) {
                return motorPositions.get(motorName);
            } else {
                throw new IllegalArgumentException("Motor '" + motorName + "' not found");
            }
        }

        public List<Map.Entry<String, Double>> getEntries(int n) {
            if (motorPositions instanceof LinkedHashMap) {
                if (n < 0 || n >= motorPositions.size()) {
                    throw new IndexOutOfBoundsException("Index " + n + " is out of bounds for map size " + motorPositions.size());
                }

                return new ArrayList<>(motorPositions.entrySet());
            } else {
                throw new IllegalStateException("MotorPositions needs to be a LinkedHashMap");
            }
        }
    }

    public static class DataStorageContainer {
        private final Context appContext;

        private final File internalFilesDir;

        public DataStorageContainer(Context context) {
            if (context == null) {
                throw new IllegalArgumentException("Context cannot be null");
            }
            appContext = context.getApplicationContext();
            internalFilesDir = appContext.getFilesDir();
        }

        public String getInternalDir() {
            return internalFilesDir.getAbsolutePath();
        }

        public String getJsonFromMotorPositions(MotorPositions motorPositions) {
            StringBuilder motorPositionsJson = new StringBuilder();
            List<Map.Entry<String, Double>> motorPositionEntries = motorPositions.getEntries(0);
            for (int i = 0; i < motorPositionEntries.size(); i++) {
                motorPositionsJson
                        .append("[\"")
                        .append(motorPositionEntries.get(i).getKey())
                        .append(", ")
                        .append(motorPositionEntries.get(i).getValue())
                        .append((i != motorPositionEntries.size() - 1) ? "]," : "]")
                        .append("\n");
            }
            return motorPositionsJson.toString();
        }

        public String getJsonFromArguments(Coords coords, MotorPositions motorPositions, DateMs dateMs) {
            return
                "{\n" +
                "  \"artifactSequence\": [\"GREEN\", \"PURPLE\", \"PURPLE\"],\n" +
                "  \"endingPosition\": {\n" +
                "    \"x\": " + coords.x + ",\n" +
                "    \"y\": " + coords.y + ",\n" +
                "    \"z\": " + coords.z + ",\n" +
                "    \"pitch\": " + coords.pitch + ",\n" +
                "    \"roll\": " + coords.roll + ",\n" +
                "    \"yaw\": " + coords.yaw + "\n" +
                "  },\n" +
                "  \"motorPositions\": [\n" +
                getJsonFromMotorPositions(motorPositions) +
                "  ],\n" +
                "  \"saveDateMillis\": " + dateMs.getDateMs() + "\n" +
                "}";
        }

        public void writeToInternalStorage(@NonNull String filename, @NonNull Coords coords, @NonNull MotorPositions motorPositions, @NonNull DateMs dateMs) {
            String jsonStringContent = getJsonFromArguments(coords, motorPositions, dateMs);
            FileOutputStream fos = null;

            try {
                fos = appContext.openFileOutput(filename, Context.MODE_PRIVATE);
                // Context.MODE_PRIVATE: Default mode. If the file already exists, it will be overwritten.
                // Context.MODE_APPEND: If the file already exists, data will be appended to the end.

                fos.write(jsonStringContent.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
                // telemetry.addData("Internal Storage", "Error saving: " + e.getMessage());
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public String readFromJsonInternalStorage(String filename) {
            StringBuilder stringBuilder = new StringBuilder();
            FileInputStream fis = null;
            String fileContent;

            try {
                fis = appContext.openFileInput(filename);
                InputStreamReader inputStreamReader = new InputStreamReader(fis, StandardCharsets.UTF_8);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                fileContent = stringBuilder.toString();
                // telemetry.addData("Internal Storage", "Content: " + fileContent);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return "File not found";
            } catch (IOException e) {
                e.printStackTrace();
                return "Error reading file";
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return fileContent;
        }

        public String[] jsonArrayToStringArray(JSONArray array) {
            String[] stringArray = new String[array.length()];
            try {
                for (int i = 0; i < array.length(); i++) {
                    stringArray[i] = array.getString(i);
                }
                return stringArray;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            // Returning an empty array doesn't matter because it will catch error in other function anyways
            return stringArray;
        }

        public double[] jsonArrayToDoubleArray(JSONArray array) {
            double[] doubleArray = new double[array.length()];
            try {
                for (int i = 0; i < array.length(); i++) {
                    doubleArray[i] = array.getDouble(i);
                }
                return doubleArray;
            } catch (JSONException e) {
                e.printStackTrace();
            }
            // Returning an empty array doesn't matter because it will catch error in other function anyways
            return doubleArray;
        }

        public boolean readAndParseAutoData(String filename, Coords dstCoords, MotorPositions dstMotorPositions, DateMs dstDateMs) {
            try {
                String jsonStringContent = readFromJsonInternalStorage(filename);
                if (!jsonStringContent.isEmpty()) {
                    try {
                        JSONObject jsonObject = new JSONObject(jsonStringContent);

                        JSONObject coords = jsonObject.getJSONObject("endingPosition");

                        dstCoords.setCoords(
                                coords.getDouble("x"),
                                coords.getDouble("y"),
                                coords.getDouble("z"),
                                coords.getDouble("pitch"),
                                coords.getDouble("roll"),
                                coords.getDouble("yaw")
                        );

                        JSONArray motorPositionsArray = jsonObject.getJSONArray("motorPositions");

                        for (int i = 0; i < motorPositionsArray.length(); i++) {
                            dstMotorPositions.addMotorPosition(
                                    motorPositionsArray.getString(i),
                                    motorPositionsArray.getDouble(i)
                            );
                        }

                        dstDateMs.setDateMs(jsonObject.getLong("saveDateMillis"));

                        // Return Successful Fetch Calibration Data
                        return true;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // SOMETHING WENT WRONG, RELY ON ODOMETRY

            // Return Failure Fetch Calibration Data
            return false;
        }
    }
}