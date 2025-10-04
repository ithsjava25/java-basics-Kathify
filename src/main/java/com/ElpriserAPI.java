package com.example.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Ett enkelt API för att hämta elpriser från elprisetjustnu.se.
 * Klassen använder endast standardbibliotek från Java 21+ (HttpClient, Records, etc.).
 */
public final class ElpriserAPI {

    // Baskonstanter för API-anrop
    private static final String API_BASE_URL = "https://www.elprisetjustnu.se/api/v1/prices";
    private static final DateTimeFormatter URL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM-dd");

    // En återanvändbar HttpClient-instans
    private final HttpClient httpClient;
    
    // Flagga för att styra cachlagring
    private final boolean cachingEnabled;
    
    // Ett enkelt minnes-cache. Nyckeln är en kombination av datum och prisklass, t.ex. "2025-08-30_SE3"
    private final Map<String, List<Elpris>> inMemoryCache;

    /**
     * En record som representerar ett enskilt elpris för en given tidsperiod.
     * Användningen av 'record' genererar automatiskt constructor, getters, equals, hashCode och toString.
     */
    public record Elpris(
        double sekPerKWh,
        double eurPerKWh,
        double exr,
        ZonedDateTime timeStart,
        ZonedDateTime timeEnd
    ) {}

    /**
     * Enum för de svenska elprisområdena för typsäkerhet.
     */
    public enum Prisklass {
        SE1, SE2, SE3, SE4
    }

    // --- Static fields for the test hook ---
    /**
     * This supplier is used ONLY for testing. If it's not null, the class will
     * use the String it provides instead of making a real HTTP call.
     */
    private static Supplier<String> mockResponseSupplier = null;
    
    // New: map mock responses per date, so tests can provide different JSON per day
    private static java.util.Map<LocalDate, String> datedMockResponses = new java.util.HashMap<>();

    /**
     * FOR TESTS ONLY: Sets a mock JSON response to be returned by the next API call.
     * This bypasses the actual network request.
     * @param jsonResponse The fake JSON string the API should parse.
     */
    public static void setMockResponse(String jsonResponse) {
        mockResponseSupplier = () -> jsonResponse;
    }
    
    /**
     * FOR TESTS ONLY: Sets a mock JSON response for a specific date. This allows
     * tests to simulate availability for one day but not another.
     */
    public static void setMockResponseForDate(LocalDate date, String jsonResponse) {
        if (jsonResponse == null) {
            datedMockResponses.remove(date);
        } else {
            datedMockResponses.put(date, jsonResponse);
        }
    }

    /**
     * FOR TESTS ONLY: Clears the mock response, causing the API to resume
     * making real network requests. This should be called after each test.
     */
    public static void clearMockResponse() {
        mockResponseSupplier = null;
        datedMockResponses.clear();
    }
    // --- End of test fields ---

    /**
     * Standardkonstruktor som aktiverar cachning.
     */
    public ElpriserAPI() {
        this(true);
    }

    /**
     * Konstruktor för att explicit styra om cachning ska användas.
     * @param enableCaching Sätt till true för att aktivera minnes-cachning, annars false.
     */
    public ElpriserAPI(boolean enableCaching) {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.cachingEnabled = enableCaching;
        // ConcurrentHashMap är trådsäker om klassen skulle användas i flera trådar
        this.inMemoryCache = new ConcurrentHashMap<>();
        System.out.println("ElpriserAPI initialiserat. Cachning: " + (enableCaching ? "På" : "Av"));
    }

    /**
     * Hämtar elpriser för ett specifikt datum och prisklass.
     * Detta är en överlagrad metod som accepterar datumet som en sträng i formatet "YYYY-MM-DD".
     *
     * @param datumStr En sträng som representerar datumet (t.ex. "2025-08-30").
     * @param prisklass Elprisområdet (SE1, SE2, SE3 eller SE4).
     * @return En lista av {@link Elpris}-objekt, eller en tom lista om data inte kunde hämtas.
     */
    public List<Elpris> getPriser(String datumStr, Prisklass prisklass) {
        try {
            LocalDate datum = LocalDate.parse(datumStr, DateTimeFormatter.ISO_LOCAL_DATE);
            return getPriser(datum, prisklass);
        } catch (Exception e) {
            System.err.println("Ogiltigt datumformat. Använd YYYY-MM-DD. Fel: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Hämtar elpriser för ett specifikt datum och prisklass.
     *
     * @param datum Ett {@link LocalDate}-objekt som representerar dagen att hämta priser för.
     * @param prisklass Elprisområdet (SE1, SE2, SE3 eller SE4).
     * @return En lista av {@link Elpris}-objekt, eller en tom lista om data inte kunde hämtas.
     */
    public List<Elpris> getPriser(LocalDate datum, Prisklass prisklass) {
        String cacheKey = getCacheKey(datum, prisklass);

        // Steg 1: Kolla minnes-cachen
        if (cachingEnabled && inMemoryCache.containsKey(cacheKey)) {
            System.out.println("Hämtar från minnes-cache för " + cacheKey);
            return inMemoryCache.get(cacheKey);
        }

        // Steg 2: Försök ladda från disk-cache (framtida implementation)
        var priserFrånDisk = loadFromDiskCache(cacheKey);
        if (cachingEnabled && priserFrånDisk != null && !priserFrånDisk.isEmpty()) {
             System.out.println("Hämtar från disk-cache för " + cacheKey);
             inMemoryCache.put(cacheKey, priserFrånDisk); // Lägg i minnes-cachen för snabbare åtkomst nästa gång
             return priserFrånDisk;
        }

        // Check for a mock response before making a network call ---
        if (mockResponseSupplier != null || !datedMockResponses.isEmpty()) {
            System.out.println("!!! ANVÄNDER MOCK-DATA FÖR TEST !!!");
            String mockJson = datedMockResponses.getOrDefault(datum, mockResponseSupplier == null ? null : mockResponseSupplier.get());
            if (mockJson == null || mockJson.isBlank()) {
                return Collections.emptyList();
            }
            List<Elpris> priser = parseSimpleJson(mockJson);
            if (cachingEnabled && !priser.isEmpty()) {
                inMemoryCache.put(cacheKey, priser);
            }
            return priser;
        }
        // --- End of mock check ---

        // Steg 3: Hämta från nätverket om det inte finns i cachen
        System.out.println("Hämtar från nätverket för " + cacheKey);
        String url = buildUrl(datum, prisklass);
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Om sidan inte finns (t.ex. priser för morgondagen) returneras 404
            if (response.statusCode() == 404) {
                System.out.println("Inga priser hittades för " + cacheKey + " (HTTP 404).");
                return Collections.emptyList();
            }
            if (response.statusCode() != 200) {
                 System.err.println("Misslyckades med att hämta priser. Statuskod: " + response.statusCode());
                 return Collections.emptyList();
            }

            List<Elpris> priser = parseSimpleJson(response.body());

            // Steg 4: Spara i cache om cachning är på
            if (cachingEnabled && !priser.isEmpty()) {
                inMemoryCache.put(cacheKey, priser);
                saveToDiskCache(cacheKey, response.body()); // Spara rådata till disk (framtida implementation)
            }
            return priser;

        } catch (IOException | InterruptedException e) {
            System.err.println("Ett fel inträffade vid hämtning av elpriser: " + e.getMessage());
            // I ett produktionssystem skulle man vilja logga detta fel mer utförligt
            Thread.currentThread().interrupt(); // Bra praxis vid InterruptedException
            return Collections.emptyList();
        }
    }

    // --- Privata hjälpmetoder ---

    private String buildUrl(LocalDate datum, Prisklass prisklass) {
        String formattedDate = datum.format(URL_DATE_FORMATTER);
        return String.format("%s/%s_%s.json", API_BASE_URL, formattedDate, prisklass.name());
    }
    
    private String getCacheKey(LocalDate datum, Prisklass prisklass) {
        return datum.format(DateTimeFormatter.ISO_LOCAL_DATE) + "_" + prisklass.name();
    }

    /**
     * En mycket enkel JSON-parser som är skräddarsydd för just detta API:s svarsformat.
     * Denna metod är inte en generell JSON-parser och är känslig för ändringar i formatet.
     */
    private List<Elpris> parseSimpleJson(String json) {
        List<Elpris> priser = new ArrayList<>();
        // Ta bort yttre [ och ], samt eventuella blanksteg
        String trimmedJson = json.trim();
        if (!trimmedJson.startsWith("[") || !trimmedJson.endsWith("]")) {
            return Collections.emptyList();
        }
        String content = trimmedJson.substring(1, trimmedJson.length() - 1).trim();
        if (content.isEmpty()) {
            return Collections.emptyList();
        }

        // Dela upp i enskilda JSON-objekt
        String[] objects = content.split("}\\s*,\\s*\\{");

        for (String objStr : objects) {
            // Rensa bort resterande { och }
            String cleanObjStr = objStr.replace("{", "").replace("}", "");
            
            try {
                // Skapa en temporär map för att hålla värdena för ett objekt
                Map<String, String> valueMap = new java.util.HashMap<>();
                String[] pairs = cleanObjStr.split(",");
                for (String pair : pairs) {
                    String[] keyValue = pair.split(":", 2);
                    String key = keyValue[0].trim().replace("\"", "");
                    String value = keyValue[1].trim().replace("\"", "");
                    valueMap.put(key, value);
                }

                // Skapa ett Elpris-objekt från värdena i mappen
                priser.add(new Elpris(
                    Double.parseDouble(valueMap.get("SEK_per_kWh")),
                    Double.parseDouble(valueMap.get("EUR_per_kWh")),
                    Double.parseDouble(valueMap.get("EXR")),
                    ZonedDateTime.parse(valueMap.get("time_start")),
                    ZonedDateTime.parse(valueMap.get("time_end"))
                ));
            } catch (Exception e) {
                // Hoppa över objekt som inte kan parsas, logga ett fel
                System.err.println("Kunde inte tolka ett elpris-objekt: " + cleanObjStr + " - Fel: " + e.getMessage());
            }
        }
        return priser;
    }
    
    // --- Stub-metoder för disk-cache ---
    
    /**
     * STUB: Spara data till en fil i en dold katalog i användarens hemkatalog.
     * Oimplementerad tills vidare.
     */
    private void saveToDiskCache(String cacheKey, String jsonData) {
        // Framtida implementation:
        // Path cacheDir = Paths.get(System.getProperty("user.home"), ".elpriser_cache");
        // Files.createDirectories(cacheDir);
        // Path cacheFile = cacheDir.resolve(cacheKey + ".json");
        // Files.writeString(cacheFile, jsonData);
        // System.out.println("Simulerar: Sparar " + cacheKey + " till disk.");
    }

    /**
     * STUB: Läs data från en fil i en dold katalog i användarens hemkatalog.
     * Oimplementerad tills vidare.
     * @return En lista av Elpris-objekt om filen finns och kan läsas, annars null.
     */
    private List<Elpris> loadFromDiskCache(String cacheKey) {
        // Framtida implementation:
        // Path cacheFile = Paths.get(System.getProperty("user.home"), ".elpriser_cache", cacheKey + ".json");
        // if (Files.exists(cacheFile)) {
        //     String jsonData = Files.readString(cacheFile);
        //     return parseSimpleJson(jsonData);
        // }
        // System.out.println("Simulerar: Försöker läsa " + cacheKey + " från disk. Fanns ej.");
        return null;
    }


    // --- Exempel på användning ---

    public static void main(String[] args) {
        System.out.println("--- Testar Elpriser API ---");
        ElpriserAPI api = new ElpriserAPI(); // Cachning är på som standard

        // Hämta dagens priser för SE3 med LocalDate
        LocalDate idag = LocalDate.now();
        List<Elpris> dagensPriser = api.getPriser(idag, Prisklass.SE3);

        if (dagensPriser.isEmpty()) {
            System.out.println("Kunde inte hämta några priser för idag i SE3.");
        } else {
            System.out.println("\nDagens elpriser för " + Prisklass.SE3 + " (" + dagensPriser.size() + " st värden):");
            // Skriv bara ut de 3 första för att hålla utskriften kort
            dagensPriser.stream().limit(3).forEach(pris -> 
                System.out.printf("Tid: %s, Pris: %.4f SEK/kWh\n", 
                    pris.timeStart().toLocalTime(), pris.sekPerKWh())
            );
            if(dagensPriser.size() > 3) System.out.println("...");
        }

        // Anropa igen för samma dag, bör nu komma från cachen
        System.out.println("\n--- Anropar igen för samma dag ---");
        api.getPriser(idag, Prisklass.SE3);

        // Hämta priser för en annan dag med sträng-metoden
        System.out.println("\n--- Hämtar priser för 2025-09-15 i SE4 ---");
        List<Elpris> framtidaPriser = api.getPriser("2025-09-15", Prisklass.SE4);
        if (framtidaPriser.isEmpty()) {
            System.out.println("Inga priser hittades (som förväntat).");
        }
    }
}
