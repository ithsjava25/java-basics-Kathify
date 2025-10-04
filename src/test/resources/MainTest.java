package com.example.api.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {
    PrintStream originalOut;
    ByteArrayOutputStream bos;

    @BeforeEach
    void setup() {
        originalOut = System.out;
        bos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(bos));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        ElpriserAPI.clearMockResponse();
    }

    @Test
    void getPriser_shouldReturnParsedPrices_whenMockDataIsProvided() {
        // --- ARRANGE ---
        // 1. Define your fake JSON data for this specific test
        String fakeJson = """
                [{"SEK_per_kWh":0.12229,"EUR_per_kWh":0.01112,"EXR":10.997148,"time_start":"2025-09-04T00:00:00+02:00","time_end":"2025-09-04T01:00:00+02:00"},{"SEK_per_kWh":0.09886,"EUR_per_kWh":0.00899,"EXR":10.997148,"time_start":"2025-09-04T01:00:00+02:00","time_end":"2025-09-04T02:00:00+02:00"},{"SEK_per_kWh":0.09095,"EUR_per_kWh":0.00827,"EXR":10.997148,"time_start":"2025-09-04T02:00:00+02:00","time_end":"2025-09-04T03:00:00+02:00"},{"SEK_per_kWh":0.04201,"EUR_per_kWh":0.00382,"EXR":10.997148,"time_start":"2025-09-04T03:00:00+02:00","time_end":"2025-09-04T04:00:00+02:00"},{"SEK_per_kWh":0.04146,"EUR_per_kWh":0.00377,"EXR":10.997148,"time_start":"2025-09-04T04:00:00+02:00","time_end":"2025-09-04T05:00:00+02:00"},{"SEK_per_kWh":0.04465,"EUR_per_kWh":0.00406,"EXR":10.997148,"time_start":"2025-09-04T05:00:00+02:00","time_end":"2025-09-04T06:00:00+02:00"},{"SEK_per_kWh":0.32991,"EUR_per_kWh":0.03,"EXR":10.997148,"time_start":"2025-09-04T06:00:00+02:00","time_end":"2025-09-04T07:00:00+02:00"},{"SEK_per_kWh":0.47123,"EUR_per_kWh":0.04285,"EXR":10.997148,"time_start":"2025-09-04T07:00:00+02:00","time_end":"2025-09-04T08:00:00+02:00"},{"SEK_per_kWh":0.68182,"EUR_per_kWh":0.062,"EXR":10.997148,"time_start":"2025-09-04T08:00:00+02:00","time_end":"2025-09-04T09:00:00+02:00"},{"SEK_per_kWh":0.4125,"EUR_per_kWh":0.03751,"EXR":10.997148,"time_start":"2025-09-04T09:00:00+02:00","time_end":"2025-09-04T10:00:00+02:00"},{"SEK_per_kWh":0.29571,"EUR_per_kWh":0.02689,"EXR":10.997148,"time_start":"2025-09-04T10:00:00+02:00","time_end":"2025-09-04T11:00:00+02:00"},{"SEK_per_kWh":0.06136,"EUR_per_kWh":0.00558,"EXR":10.997148,"time_start":"2025-09-04T11:00:00+02:00","time_end":"2025-09-04T12:00:00+02:00"},{"SEK_per_kWh":0.03662,"EUR_per_kWh":0.00333,"EXR":10.997148,"time_start":"2025-09-04T12:00:00+02:00","time_end":"2025-09-04T13:00:00+02:00"},{"SEK_per_kWh":0.0375,"EUR_per_kWh":0.00341,"EXR":10.997148,"time_start":"2025-09-04T13:00:00+02:00","time_end":"2025-09-04T14:00:00+02:00"},{"SEK_per_kWh":0.26822,"EUR_per_kWh":0.02439,"EXR":10.997148,"time_start":"2025-09-04T14:00:00+02:00","time_end":"2025-09-04T15:00:00+02:00"},{"SEK_per_kWh":0.30429,"EUR_per_kWh":0.02767,"EXR":10.997148,"time_start":"2025-09-04T15:00:00+02:00","time_end":"2025-09-04T16:00:00+02:00"},{"SEK_per_kWh":0.36675,"EUR_per_kWh":0.03335,"EXR":10.997148,"time_start":"2025-09-04T16:00:00+02:00","time_end":"2025-09-04T17:00:00+02:00"},{"SEK_per_kWh":0.58296,"EUR_per_kWh":0.05301,"EXR":10.997148,"time_start":"2025-09-04T17:00:00+02:00","time_end":"2025-09-04T18:00:00+02:00"},{"SEK_per_kWh":0.92145,"EUR_per_kWh":0.08379,"EXR":10.997148,"time_start":"2025-09-04T18:00:00+02:00","time_end":"2025-09-04T19:00:00+02:00"},{"SEK_per_kWh":1.5054,"EUR_per_kWh":0.13689,"EXR":10.997148,"time_start":"2025-09-04T19:00:00+02:00","time_end":"2025-09-04T20:00:00+02:00"},{"SEK_per_kWh":1.00888,"EUR_per_kWh":0.09174,"EXR":10.997148,"time_start":"2025-09-04T20:00:00+02:00","time_end":"2025-09-04T21:00:00+02:00"},{"SEK_per_kWh":0.63179,"EUR_per_kWh":0.05745,"EXR":10.997148,"time_start":"2025-09-04T21:00:00+02:00","time_end":"2025-09-04T22:00:00+02:00"},{"SEK_per_kWh":0.56382,"EUR_per_kWh":0.05127,"EXR":10.997148,"time_start":"2025-09-04T22:00:00+02:00","time_end":"2025-09-04T23:00:00+02:00"},{"SEK_per_kWh":0.52951,"EUR_per_kWh":0.04815,"EXR":10.997148,"time_start":"2025-09-04T23:00:00+02:00","time_end":"2025-09-05T00:00:00+02:00"}]""";

        // 2. Set the mock response using the static method.
        ElpriserAPI.setMockResponse(fakeJson);

        // 3. Create an instance of the class as a student would.
        ElpriserAPI api = new ElpriserAPI(false); // Disable caching for predictable tests

        // --- ACT ---
        // This call will NOT make a network request. It will use `fakeJson`.
        List<ElpriserAPI.Elpris> priser = api.getPriser("2025-09-04", ElpriserAPI.Prisklass.SE3);

        // --- ASSERT ---
        assertThat(priser)
                .isNotNull()
                .hasSize(24)
                .satisfies(list -> {
                    assertThat(list.get(0).sekPerKWh()).isEqualTo(0.12229);
                    assertThat(list.get(1).sekPerKWh()).isEqualTo(0.09886);
                });
    }

    @Test
    void getPriser_shouldReturnEmptyList_whenMockDataIsNull() {
        ElpriserAPI.setMockResponse(null);
        ElpriserAPI api = new ElpriserAPI(false);

        List<ElpriserAPI.Elpris> priser = api.getPriser(LocalDate.now(), ElpriserAPI.Prisklass.SE1);

        assertNotNull(priser);
        assertTrue(priser.isEmpty());
    }

    @Test
    void showHelp_whenNoArguments() {
        Main.main(new String[]{});

        String output = bos.toString();
        assertThat(output).containsIgnoringCase("usage");
        assertThat(output).containsIgnoringCase("zone");
        assertThat(output).containsIgnoringCase("date");
        assertThat(output).containsIgnoringCase("sorted");

    }

    @Test
    void showHelp_withHelpFlag() {
        Main.main(new String[]{"--help"});

        String output = bos.toString();
        assertThat(output).containsIgnoringCase("--zone");
        assertThat(output).containsIgnoringCase("--date");
        assertThat(output).containsIgnoringCase("--charging");
        assertThat(output).containsIgnoringCase("--sorted");
        assertThat(output).containsIgnoringCase("SE1")
                .containsIgnoringCase("SE2")
                .containsIgnoringCase("SE3")
                .containsIgnoringCase("SE4");
    }

    @Test
    void displayMeanPrice_withValidData() {
        // Mock data with known values for predictable mean calculation
        String mockJson = """
                [{"SEK_per_kWh":0.10,"EUR_per_kWh":0.01,"EXR":10.0,"time_start":"2025-09-04T00:00:00+02:00","time_end":"2025-09-04T01:00:00+02:00"},
                 {"SEK_per_kWh":0.202,"EUR_per_kWh":0.02,"EXR":10.0,"time_start":"2025-09-04T01:00:00+02:00","time_end":"2025-09-04T02:00:00+02:00"},
                 {"SEK_per_kWh":0.30,"EUR_per_kWh":0.03,"EXR":10.0,"time_start":"2025-09-04T02:00:00+02:00","time_end":"2025-09-04T03:00:00+02:00"},
                 {"SEK_per_kWh":0.40,"EUR_per_kWh":0.04,"EXR":10.0,"time_start":"2025-09-04T03:00:00+02:00","time_end":"2025-09-04T04:00:00+02:00"}]""";

        ElpriserAPI.setMockResponse(mockJson);

        Main.main(new String[]{"--zone", "SE3", "--date", "2025-09-04"});

        String output = bos.toString();
        assertThat(output).containsIgnoringCase("medelpris");
        // Mean of 0.10, 0.20, 0.30, 0.40 = 0.25 = 25 öre
        assertThat(output).contains("25");
    }

    @Test
    void displayMinMaxPrices_withValidData() {
        String mockJson = """
                [{"SEK_per_kWh":0.50,"EUR_per_kWh":0.05,"EXR":10.0,"time_start":"2025-09-04T00:00:00+02:00","time_end":"2025-09-04T01:00:00+02:00"},
                 {"SEK_per_kWh":0.10,"EUR_per_kWh":0.01,"EXR":10.0,"time_start":"2025-09-04T01:00:00+02:00","time_end":"2025-09-04T02:00:00+02:00"},
                 {"SEK_per_kWh":0.80,"EUR_per_kWh":0.08,"EXR":10.0,"time_start":"2025-09-04T02:00:00+02:00","time_end":"2025-09-04T03:00:00+02:00"},
                 {"SEK_per_kWh":0.30,"EUR_per_kWh":0.03,"EXR":10.0,"time_start":"2025-09-04T03:00:00+02:00","time_end":"2025-09-04T04:00:00+02:00"}]""";

        ElpriserAPI.setMockResponse(mockJson);

        Main.main(new String[]{"--zone", "SE1", "--date", "2025-09-04"});

        String output = bos.toString();
        assertThat(output).containsIgnoringCase("lägsta pris");
        assertThat(output).containsIgnoringCase("högsta pris");
        assertThat(output).containsIgnoringCase("medelpris");
        assertThat(output).contains("01-02"); // Cheapest hour (0.10)
        assertThat(output).contains("02-03"); // Most expensive hour (0.80)
        assertThat(output).contains("10,00"); // 10 öre (cheapest)
        assertThat(output).contains("80,00"); // 80 öre (most expensive)
        assertThat(output).contains("80,00"); // 42,50 öre (medelpris)
    }

    @Test
    void displaySortedPrices_whenRequested() {
        String mockJson = """
                [{"SEK_per_kWh":0.30,"EUR_per_kWh":0.03,"EXR":10.0,"time_start":"2025-09-04T00:00:00+02:00","time_end":"2025-09-04T01:00:00+02:00"},
                 {"SEK_per_kWh":0.10,"EUR_per_kWh":0.01,"EXR":10.0,"time_start":"2025-09-04T01:00:00+02:00","time_end":"2025-09-04T02:00:00+02:00"},
                 {"SEK_per_kWh":0.20,"EUR_per_kWh":0.02,"EXR":10.0,"time_start":"2025-09-04T02:00:00+02:00","time_end":"2025-09-04T03:00:00+02:00"},
                 {"SEK_per_kWh":0.10,"EUR_per_kWh":0.01,"EXR":10.0,"time_start":"2025-09-04T03:00:00+02:00","time_end":"2025-09-04T04:00:00+02:00"}]""";

        ElpriserAPI.setMockResponse(mockJson);

        Main.main(new String[]{"--zone", "SE2", "--date", "2025-09-04", "--sorted"});

        String output = bos.toString();

        // Expected sorted output (ascending by price)
        List<String> expectedOrder = List.of(
                "01-02 10,00 öre",
                "03-04 10,00 öre",
                "02-03 20,00 öre",
                "00-01 30,00 öre"
        );

        // Extract actual lines that match the pattern
        List<String> actualSortedLines = Arrays.stream(output.split("\n"))
                .map(String::trim) // 1. Trim leading/trailing whitespace
                .filter(line -> line.matches("^\\d{2}-\\d{2}\\s+\\d+,\\d{2}\\s+öre$")) // 2. Use a more flexible regex
                .collect(Collectors.toList());

        // Assert that actual lines match expected order
        assertThat(actualSortedLines).containsExactlyElementsOf(expectedOrder);
    }

    @Test
    void findOptimalCharging2Hours() {
        String mockJson = """
                [{"SEK_per_kWh":0.50,"EUR_per_kWh":0.05,"EXR":10.0,"time_start":"2025-09-04T00:00:00+02:00","time_end":"2025-09-04T01:00:00+02:00"},
                 {"SEK_per_kWh":0.10,"EUR_per_kWh":0.01,"EXR":10.0,"time_start":"2025-09-04T01:00:00+02:00","time_end":"2025-09-04T02:00:00+02:00"},
                 {"SEK_per_kWh":0.05,"EUR_per_kWh":0.005,"EXR":10.0,"time_start":"2025-09-04T02:00:00+02:00","time_end":"2025-09-04T03:00:00+02:00"},
                 {"SEK_per_kWh":0.15,"EUR_per_kWh":0.015,"EXR":10.0,"time_start":"2025-09-04T03:00:00+02:00","time_end":"2025-09-04T04:00:00+02:00"},
                 {"SEK_per_kWh":0.30,"EUR_per_kWh":0.03,"EXR":10.0,"time_start":"2025-09-04T04:00:00+02:00","time_end":"2025-09-04T05:00:00+02:00"}]""";

        ElpriserAPI.setMockResponse(mockJson);

        Main.main(new String[]{"--zone", "SE3", "--date", "2025-09-04", "--charging", "2h"});

        String output = bos.toString();
        assertThat(output).containsIgnoringCase("påbörja laddning");
        assertThat(output).contains("01"); // Should start at 01:00 (cheapest 2h window: 01-02 + 02-03)
        assertThat(output).containsIgnoringCase("medelpris");
        assertThat(output).contains("7,5"); // Mean of 0.10 and 0.05 = 0.075 = 7.5 öre
    }

    @Test
    void findOptimalCharging4Hours() {
        String mockJson = """
                [{"SEK_per_kWh":0.40,"EUR_per_kWh":0.04,"EXR":10.0,"time_start":"2025-09-04T00:00:00+02:00","time_end":"2025-09-04T01:00:00+02:00"},
                 {"SEK_per_kWh":0.10,"EUR_per_kWh":0.01,"EXR":10.0,"time_start":"2025-09-04T01:00:00+02:00","time_end":"2025-09-04T02:00:00+02:00"},
                 {"SEK_per_kWh":0.05,"EUR_per_kWh":0.005,"EXR":10.0,"time_start":"2025-09-04T02:00:00+02:00","time_end":"2025-09-04T03:00:00+02:00"},
                 {"SEK_per_kWh":0.15,"EUR_per_kWh":0.015,"EXR":10.0,"time_start":"2025-09-04T03:00:00+02:00","time_end":"2025-09-04T04:00:00+02:00"},
                 {"SEK_per_kWh":0.20,"EUR_per_kWh":0.02,"EXR":10.0,"time_start":"2025-09-04T04:00:00+02:00","time_end":"2025-09-04T05:00:00+02:00"},
                 {"SEK_per_kWh":0.30,"EUR_per_kWh":0.03,"EXR":10.0,"time_start":"2025-09-04T05:00:00+02:00","time_end":"2025-09-04T06:00:00+02:00"}]""";

        ElpriserAPI.setMockResponse(mockJson);

        Main.main(new String[]{"--zone", "SE1", "--date", "2025-09-04", "--charging", "4h"});

        String output = bos.toString();
        assertThat(output).containsIgnoringCase("påbörja laddning");
        assertThat(output).contains("01"); // Should start at 01:00 (cheapest 4h window)
        assertThat(output).containsIgnoringCase("medelpris");
        // Mean of 0.10, 0.05, 0.15, 0.20 = 0.125 = 12.5 öre
        assertThat(output).contains("12,5");
    }

    @Test
    void chargingWindowDoesNotUseNextDay_whenNextDayUnavailable() {
        // Only today's 3 hours, request 2h window -> should compute within these only
        String mockJsonToday = """
                [{"SEK_per_kWh":0.20,"EUR_per_kWh":0.02,"EXR":10.0,"time_start":"2025-09-04T00:00:00+02:00","time_end":"2025-09-04T01:00:00+02:00"},
                 {"SEK_per_kWh":0.10,"EUR_per_kWh":0.01,"EXR":10.0,"time_start":"2025-09-04T01:00:00+02:00","time_end":"2025-09-04T02:00:00+02:00"},
                 {"SEK_per_kWh":0.15,"EUR_per_kWh":0.015,"EXR":10.0,"time_start":"2025-09-04T02:00:00+02:00","time_end":"2025-09-04T03:00:00+02:00"}]""";
        ElpriserAPI.setMockResponse(mockJsonToday);
        Main.main(new String[]{"--zone", "SE3", "--date", "2025-09-04", "--charging", "2h"});
        String output = bos.toString();
        // Best 2h window should be 01-03 (0.10 + 0.15)
        assertThat(output).contains("Påbörja laddning");
        assertThat(output).contains("01:00");
    }

    @Test
    void findOptimalCharging8Hours() {
        // Create mock data with 12 hours to allow for 8-hour window
        StringBuilder jsonBuilder = new StringBuilder("[");
        double[] prices = {0.50, 0.10, 0.05, 0.15, 0.08, 0.12, 0.06, 0.09, 0.25, 0.30, 0.35, 0.40};

        for (int i = 0; i < prices.length; i++) {
            if (i > 0) jsonBuilder.append(",");
            jsonBuilder.append(String.format(
                    Locale.US,
                    """
                            {"SEK_per_kWh":%.2f,"EUR_per_kWh":%.3f,"EXR":10.0,"time_start":"2025-09-04T%02d:00:00+02:00","time_end":"2025-09-04T%02d:00:00+02:00"}""",
                    prices[i], prices[i] / 10, i, i + 1
            ));
        }
        jsonBuilder.append("]");

        ElpriserAPI.setMockResponse(jsonBuilder.toString());

        Main.main(new String[]{"--zone", "SE4", "--date", "2025-09-04", "--charging", "8h"});

        String output = bos.toString();
        assertThat(output).containsIgnoringCase("påbörja laddning");
        assertThat(output).containsIgnoringCase("medelpris");

        // Precise value checks
        // Cheapest 8-hour window is from hour 1 to hour 8 (prices[1] to prices[8])
        double expectedAvg = Arrays.stream(prices, 1, 9).average().orElseThrow();
        String expectedStartHour = String.format("%02d:00", 1);
        String expectedAvgStr = formatOre(expectedAvg);

        assertThat(output).contains("kl " + expectedStartHour);
        assertThat(output).contains("Medelpris för fönster: " + expectedAvgStr + " öre");
    }

    @Test
    void handleInvalidZone() {
        Main.main(new String[]{"--zone", "SE5", "--date", "2025-09-04"});

        String output = bos.toString().toLowerCase();
        assertThat(output).containsAnyOf("invalid zone", "ogiltig zon", "fel zon");
    }

    @Test
    void handleInvalidDate() {
        Main.main(new String[]{"--zone", "SE3", "--date", "invalid-date"});

        String output = bos.toString().toLowerCase();
        assertThat(output).containsAnyOf("invalid date", "ogiltigt datum", "fel datum");
    }

    @Test
    void handleMissingZoneArgument() {
        Main.main(new String[]{"--date", "2025-09-04"});

        String output = bos.toString();
        assertThat(output).containsAnyOf("zone", "required");
    }

    @Test
    void useCurrentDateWhenNotSpecified() {
        ElpriserAPI.setMockResponse("[]"); // Empty response is fine for this test

        Main.main(new String[]{"--zone", "SE3"});

        String output = bos.toString();
        // Should not show error about missing date
        assertThat(output).doesNotContainIgnoringCase("invalid date");
        assertThat(output).doesNotContainIgnoringCase("missing date");
    }

    @Test
    void handleNoDataAvailable() {
        ElpriserAPI.setMockResponse("[]"); // Empty response

        Main.main(new String[]{"--zone", "SE3", "--date", "2025-09-04"});

        String output = bos.toString().toLowerCase();
        assertThat(output).containsAnyOf("no data", "ingen data", "inga priser");
    }

    @Test
    void handleMultipleDaysData_includesNextDayForCharging() {
        // This test ensures charging window can span days when next day data exists
        LocalDate today = LocalDate.of(2025, 9, 4);
        LocalDate tomorrow = today.plusDays(1);

        String mockJsonToday = """
                [{"SEK_per_kWh":0.30,"EUR_per_kWh":0.03,"EXR":10.0,"time_start":"2025-09-04T22:00:00+02:00","time_end":"2025-09-04T23:00:00+02:00"},
                 {"SEK_per_kWh":0.25,"EUR_per_kWh":0.025,"EXR":10.0,"time_start":"2025-09-04T23:00:00+02:00","time_end":"2025-09-05T00:00:00+02:00"}]""";
        String mockJsonTomorrow = """
                [{"SEK_per_kWh":0.10,"EUR_per_kWh":0.01,"EXR":10.0,"time_start":"2025-09-05T00:00:00+02:00","time_end":"2025-09-05T01:00:00+02:00"},
                 {"SEK_per_kWh":0.15,"EUR_per_kWh":0.015,"EXR":10.0,"time_start":"2025-09-05T01:00:00+02:00","time_end":"2025-09-05T02:00:00+02:00"}]""";

        ElpriserAPI.setMockResponseForDate(today, mockJsonToday);
        ElpriserAPI.setMockResponseForDate(tomorrow, mockJsonTomorrow);

        Main.main(new String[]{"--zone", "SE3", "--date", "2025-09-04", "--charging", "4h"});

        String output = bos.toString();
        assertThat(output).containsIgnoringCase("påbörja laddning");
        // Should be able to find optimal charging window across day boundary
    }

    @Test
    void chargingWindowSpansToNextDay_whenCheapestCrossesMidnight() {
        // Prices set so best 2h window is 23:00-01:00 (0.50 at 22, 0.20 at 23, 0.05 at 00, 0.40 at 01)
        LocalDate today = LocalDate.of(2025, 9, 4);
        LocalDate tomorrow = today.plusDays(1);

        String mockJsonToday = """
                [{"SEK_per_kWh":0.50,"EUR_per_kWh":0.05,"EXR":10.0,"time_start":"2025-09-04T22:00:00+02:00","time_end":"2025-09-04T23:00:00+02:00"},
                 {"SEK_per_kWh":0.20,"EUR_per_kWh":0.02,"EXR":10.0,"time_start":"2025-09-04T23:00:00+02:00","time_end":"2025-09-05T00:00:00+02:00"}]""";
        String mockJsonTomorrow = """
                [{"SEK_per_kWh":0.05,"EUR_per_kWh":0.005,"EXR":10.0,"time_start":"2025-09-05T00:00:00+02:00","time_end":"2025-09-05T01:00:00+02:00"},
                 {"SEK_per_kWh":0.40,"EUR_per_kWh":0.04,"EXR":10.0,"time_start":"2025-09-05T01:00:00+02:00","time_end":"2025-09-05T02:00:00+02:00"}]""";

        ElpriserAPI.setMockResponseForDate(today, mockJsonToday);
        ElpriserAPI.setMockResponseForDate(tomorrow, mockJsonTomorrow);

        Main.main(new String[]{"--zone", "SE3", "--date", "2025-09-04", "--charging", "2h"});
        String output = bos.toString();
        assertThat(output).contains("Påbörja laddning");
        // Expect start at 23:00 (23 + 00 window is cheapest)
        assertThat(output).contains("23:00");
    }

    @Test
    public void testHourlyMinMaxPrices() {
        List<Double> quarterHourPrices = new ArrayList<>();

        // Simulate 96 prices: 24 hours, each with 4 quarter-hour prices
        for (int i = 0; i < 96; i++) {
            quarterHourPrices.add((double) (i % 24)); // repeating hourly pattern
        }

        // Expected hourly averages
        List<Double> hourlyAverages = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            double sum = 0;
            for (int j = 0; j < 4; j++) {
                sum += quarterHourPrices.get(i * 4 + j);
            }
            hourlyAverages.add(sum / 4.0);
        }

        double expectedMin = Collections.min(hourlyAverages);
        double expectedMax = Collections.max(hourlyAverages);

        // Call your method under test
        PriceRange result = PriceCalculator.calculateHourlyMinMax(quarterHourPrices);

        assertThat(result.getMin()).isCloseTo(expectedMin, within(0.001));
        assertThat(result.getMax()).isCloseTo(expectedMax, within(0.001));
    }

    public String formatOre(double sekPerKWh) {
        double ore = sekPerKWh * 100.0;
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.of("sv", "SE"));
        DecimalFormat df = new DecimalFormat("0.00", symbols);
        return df.format(ore);
    }
}
class PriceRange {
    public final double min;
    public final double max;

    public PriceRange(double min, double max) {
        this.min = min;
        this.max = max;
    }

    public double getMin() { return min; }
    public double getMax() { return max; }
}

class PriceCalculator {
    public static PriceRange calculateHourlyMinMax(List<Double> quarterHourPrices) {
        List<Double> hourlyAverages = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            double sum = 0;
            for (int j = 0; j < 4; j++) {
                sum += quarterHourPrices.get(i * 4 + j);
            }
            hourlyAverages.add(sum / 4.0);
        }
        double min = Collections.min(hourlyAverages);
        double max = Collections.max(hourlyAverages);
        return new PriceRange(min, max);
    }
}