package com.mdp.eventos.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SalonInfoApiService {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
    private static final Pattern LATITUDE = Pattern.compile("\"lat\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern LONGITUDE = Pattern.compile("\"lon\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern TIMES = Pattern.compile("\"time\"\\s*:\\s*\\[([^\\]]*)\\]");
    private static final Pattern TEMPERATURES =
        Pattern.compile("\"temperature_2m_max\"\\s*:\\s*\\[([^\\]]*)\\]");
    private static final Pattern PRECIPITATION =
        Pattern.compile("\"precipitation_probability_max\"\\s*:\\s*\\[([^\\]]*)\\]");

    public DatosSalon consultar(String direccion, LocalDate fecha) throws IOException, InterruptedException {
        URI mapaBusqueda = URI.create("https://www.openstreetmap.org/search?query="
            + URLEncoder.encode(direccion, StandardCharsets.UTF_8));
        Coordenadas coordinates;
        try {
            coordinates = geocodificar(direccion);
        } catch (IOException exception) {
            return new DatosSalon(mapaBusqueda, null);
        }
        if (coordinates == null) return new DatosSalon(mapaBusqueda, null);
        String forecast;
        try {
            forecast = pronostico(coordinates, fecha);
        } catch (IOException exception) {
            forecast = null;
        }
        return new DatosSalon(
            URI.create("https://www.openstreetmap.org/?mlat=" + coordinates.latitud()
                + "&mlon=" + coordinates.longitud() + "#map=15/"
                + coordinates.latitud() + "/" + coordinates.longitud()),
            forecast
        );
    }

    private Coordenadas geocodificar(String direccion) throws IOException, InterruptedException {
        URI uri = URI.create("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q="
            + URLEncoder.encode(direccion + ", Mar del Plata, Argentina", StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(8))
            .header("User-Agent", "EventosMDP/1.0 (desktop event booking application)")
            .header("Accept", "application/json")
            .GET()
            .build();
        String body = enviar(request);
        Matcher lat = LATITUDE.matcher(body);
        Matcher lon = LONGITUDE.matcher(body);
        if (!lat.find() || !lon.find()) return null;
        try {
            double latitude = Double.parseDouble(lat.group(1));
            double longitude = Double.parseDouble(lon.group(1));
            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) return null;
            return new Coordenadas(latitude, longitude);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String pronostico(Coordenadas coordinates, LocalDate fecha) throws IOException, InterruptedException {
        LocalDate hoy = LocalDate.now();
        if (fecha.isBefore(hoy) || fecha.isAfter(hoy.plusDays(15))) return null;
        URI uri = URI.create("https://api.open-meteo.com/v1/forecast?latitude=" + coordinates.latitud()
            + "&longitude=" + coordinates.longitud()
            + "&daily=temperature_2m_max,precipitation_probability_max"
            + "&timezone=America%2FArgentina%2FBuenos_Aires"
            + "&forecast_days=16");
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(8))
            .header("Accept", "application/json")
            .GET()
            .build();
        String body = enviar(request);
        List<String> dates = arreglo(body, TIMES);
        List<String> temperatures = arreglo(body, TEMPERATURES);
        List<String> rain = arreglo(body, PRECIPITATION);
        int index = dates.indexOf(fecha.toString());
        if (index < 0 || index >= temperatures.size()) return null;
        String temperature = temperatures.get(index);
        if ("null".equals(temperature)) return null;
        String result = "Máxima prevista: " + temperature + " °C";
        if (index < rain.size() && !"null".equals(rain.get(index))) {
            result += " · Probabilidad de precipitación: " + rain.get(index) + "%";
        }
        return result;
    }

    private String enviar(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("El servicio externo respondió HTTP " + response.statusCode() + ".");
        }
        return response.body();
    }

    private List<String> arreglo(String json, Pattern pattern) {
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) return List.of();
        return java.util.Arrays.stream(matcher.group(1).split(","))
            .map(String::trim)
            .map(value -> value.replace("\"", ""))
            .toList();
    }

    private record Coordenadas(double latitud, double longitud) {
    }

    public record DatosSalon(URI mapa, String pronostico) {
    }
}
