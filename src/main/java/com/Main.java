package com.example.api;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class Main {
    public static void main(String[] args) {
        // se hjälp
        if (args.length > 0 && args[0].equals("--help")) {
            skrivUtHjalp();
            return;
        }

        // standardinställningar och elmområde
        String zone = "SE3";
        LocalDate date = LocalDate.now();
        LocalDate startDate = date.plusDays(1);
        boolean sorted = false;
        int chargingHours = 0;

        // hantera argument
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--date":
                    if (i + 1 < args.length) {
                        String dateArg = args[i + 1].toLowerCase();
                        if (dateArg.equals("imorgon")) {
                            startDate = LocalDate.now();
                        } else if (dateArg.equals("idag")) {
                            startDate = LocalDate.now();
                        } else {
                            try {
                                startDate = LocalDate.parse(dateArg);
                            } catch (DateTimeParseException e) {
                                System.out.println("Fel: Ogiltigt datumformat. Använd YYYY-MM-DD.");
                                return;
                            }
                        }
                        i++;
                    }
                    break;
                case "--sorted":
                    sorted = true;
                    break;
                case "--charging":
                    if (i + 1 < args.length) {
                        String val = args[i + 1].replace("h", "");
                        try {
                            chargingHours = Integer.parseInt(val);
                        } catch (NumberFormatException e) {
                            System.out.println("error: use 2h, 4h or 8h.");
                            return;
                        }
                        i++;
                    }
                    break;
            }
        }

        // anropa formatOre
        Main mainObj = new Main();

        // API o prisklass
        ElpriserAPI elpriserAPI = new ElpriserAPI();
        ElpriserAPI.Prisklass prisklass;
        try {
            prisklass = ElpriserAPI.Prisklass.valueOf(zone);
        } catch (IllegalArgumentException e) {
            System.out.println("Felaktig zon: " + zone);
            prisklass = ElpriserAPI.Prisklass.SE3;
        }

        // priser
        List<ElpriserAPI.Elpris> priser = new ArrayList<>(elpriserAPI.getPriser(date, prisklass));
        if (priser.isEmpty()) {
            System.out.println("no prices available for " + date + " in zon " + zone + ".");
            return;
        }

        List<ElpriserAPI.Elpris> imorgonPriser = elpriserAPI.getPriser(startDate, prisklass);
        if (imorgonPriser != null && !imorgonPriser.isEmpty()) {
            priser.addAll(imorgonPriser);
        }

        if (sorted) {
            priser.sort(Comparator.comparingDouble(ElpriserAPI.Elpris::sekPerKWh).reversed());
        }

        // stats
        DateTimeFormatter tidFormat = DateTimeFormatter.ofPattern("HH:mm");
        ElpriserAPI.Elpris billigast = priser.getFirst();
        ElpriserAPI.Elpris dyrast = priser.getFirst();
        double summa = 0;

        // skriv ut timpriser
        for (ElpriserAPI.Elpris pris : priser) {
            System.out.printf("%s: %s öre/kWh%n",
                    pris.timeStart().format(tidFormat), mainObj.formatOre(pris.sekPerKWh()));
        }

        for (ElpriserAPI.Elpris pris : priser) {
            summa += pris.sekPerKWh();
            if (pris.sekPerKWh() < billigast.sekPerKWh()) billigast = pris;
            if (pris.sekPerKWh() > dyrast.sekPerKWh()) dyrast = pris;
        }

        double medelpris = summa / priser.size();

        System.out.println("Statistik");
        System.out.println("Datum: " + date);
        System.out.println("Zon: " + zone);
        System.out.printf("Medelpris: %s öre/kWh%n", mainObj.formatOre(medelpris));
        System.out.printf("Billigast pris: %s - %s öre/kWh%n",
                billigast.timeStart().format(tidFormat), mainObj.formatOre(billigast.sekPerKWh()));
        System.out.printf("Dyrast pris: %s - %s öre/kWh%n",
                dyrast.timeStart().format(tidFormat), mainObj.formatOre(dyrast.sekPerKWh()));

        // opt laddning
        if (chargingHours > 0) {
            beraknaLaddningsfonster(priser, chargingHours, tidFormat, mainObj);
        } else {
            // 2h, 4h, 8h
            for (int period : new int[]{2, 4, 8}) {
                beraknaLaddningsfonster(priser, period, tidFormat, mainObj);
            }
        }
    }

    // metod för laddningsfönster
    public static void beraknaLaddningsfonster(List<ElpriserAPI.Elpris> priser, int timmar, DateTimeFormatter format, Main mainObj) {
        if (priser.size() < timmar) return;

        double minSum = Double.MAX_VALUE;
        int minIndex = 0;

        for (int i = 0; i <= priser.size() - timmar; i++) {
            double windowSum = 0;
            for (int j = i; j < i + timmar; j++) {
                windowSum += priser.get(j).sekPerKWh();
            }
            if (windowSum < minSum) {
                minSum = windowSum;
                minIndex = i;
            }
        }

        String startTid = priser.get(minIndex).timeStart().format(format);
        String slutTid = priser.get(minIndex + timmar - 1).timeStart().plusHours(1).format(format);

        System.out.printf("Billigast pris %dh-period: %s - %s (%s öre totalt)%n",
                timmar, startTid, slutTid, mainObj.formatOre(minSum));
    }

    // hjälptext
    public static void skrivUtHjalp() {
        System.out.println("Usage: java -cp target/classes com.example.Main --zone <zon> [options]");
        System.out.println("--zone SE1|SE2|SE3|SE4");
        System.out.println("--date YYYY-MM-DD|imorgon");
        System.out.println("--sorted");
        System.out.println("--charging 2h|4h|8h");
        System.out.println("--help");
    }

    // formatteringsmetod
    public String formatOre(double sekPerKWh) {
        double ore = sekPerKWh * 100.0;
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(java.util.Locale.of("sv", "SE"));
        DecimalFormat df = new DecimalFormat("0.00", symbols);
        return df.format(ore);
    }
}
