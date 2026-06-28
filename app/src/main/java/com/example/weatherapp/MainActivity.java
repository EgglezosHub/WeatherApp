package com.example.weatherapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private AutoCompleteTextView etCityName;
    private Button btnSearch, btnCurrentLocation;
    private TextView tvCityName, tvTemperature, tvCondition;
    private ProgressBar progressBar;

    private FusedLocationProviderClient fusedLocationClient;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); // Force Light Mode
        setContentView(R.layout.activity_main);

        etCityName = findViewById(R.id.etCityName);
        btnSearch = findViewById(R.id.btnSearch);
        btnCurrentLocation = findViewById(R.id.btnCurrentLocation);
        tvCityName = findViewById(R.id.tvCityName);
        tvTemperature = findViewById(R.id.tvTemperature);
        tvCondition = findViewById(R.id.tvCondition);
        progressBar = findViewById(R.id.progressBar);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        String[] popularCities = {"Athens", "Thessaloniki", "Patras", "London", "New York", "Tokyo", "Paris", "Berlin", "Rome", "Madrid"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, popularCities);
        etCityName.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> {
            String city = etCityName.getText().toString().trim();
            if (!city.isEmpty()) {
                searchCity(city);
            } else {
                Toast.makeText(this, "Please enter a city name", Toast.LENGTH_SHORT).show();
            }
        });

        btnCurrentLocation.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            } else {
                getCurrentLocation();
            }
        });

        //background extrem weather checks
        scheduleBackgroundWeatherCheck();

        //notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        //initial load
        fetchWeatherData(37.9838, 23.7275, "Athens");
    }

    private void scheduleBackgroundWeatherCheck() {
        PeriodicWorkRequest weatherWorkRequest = new PeriodicWorkRequest.Builder(WeatherWorker.class, 12, TimeUnit.HOURS).build();
        WorkManager.getInstance(this).enqueue(weatherWorkRequest);
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            progressBar.setVisibility(View.VISIBLE);
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    etCityName.setText("");
                    fetchWeatherData(location.getLatitude(), location.getLongitude(), "Current Location");
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Ensure GPS is on.", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
    }

    private void searchCity(String cityName) {
        progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            try {
                String formattedCity = cityName.replace(" ", "%20");
                URL url = new URL("https://geocoding-api.open-meteo.com/v1/search?name=" + formattedCity + "&count=1");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) result.append(line);
                reader.close();

                JSONObject jsonResponse = new JSONObject(result.toString());

                if (jsonResponse.has("results")) {
                    JSONObject cityData = jsonResponse.getJSONArray("results").getJSONObject(0);
                    fetchWeatherData(cityData.getDouble("latitude"), cityData.getDouble("longitude"), cityData.getString("name"));
                } else {
                    handler.post(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "City not found!", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Error finding city", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void fetchWeatherData(double latitude, double longitude, String cityName) {
        executor.execute(() -> {
            try {
                String urlString = "https://api.open-meteo.com/v1/forecast?latitude="
                        + latitude + "&longitude=" + longitude + "&current=temperature_2m,weather_code";
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) result.append(line);
                reader.close();

                JSONObject currentObject = new JSONObject(result.toString()).getJSONObject("current");
                double temperature = currentObject.getDouble("temperature_2m");
                int weatherCode = currentObject.getInt("weather_code");

                handler.post(() -> {
                    tvCityName.setText(cityName);
                    tvTemperature.setText("Temp: " + temperature + "°C");
                    tvCondition.setText("Condition: " + getWeatherDescription(weatherCode));
                    progressBar.setVisibility(View.GONE);
                });
            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Error fetching weather", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private String getWeatherDescription(int code) {
        if (code == 0) return "Clear sky";
        if (code >= 1 && code <= 3) return "Cloudy";
        if (code >= 45 && code <= 48) return "Fog";
        if (code >= 51 && code <= 57) return "Drizzle";
        if (code >= 61 && code <= 67) return "Rain";
        if (code >= 71 && code <= 77) return "Snow";
        if (code >= 80 && code <= 82) return "Rain Showers";
        if (code >= 85 && code <= 86) return "Snow Showers";
        if (code >= 95 && code <= 99) return "Thunderstorm";
        return "Unknown";
    }
}