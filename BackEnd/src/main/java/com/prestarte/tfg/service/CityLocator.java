package com.prestarte.tfg.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Localiza ciudades para estimar distancias. Primero busca en una tabla
 * con las ciudades más habituales en préstamos de arte desde España (sin
 * llamadas externas) y, si no está, recurre a {@link RemoteGeocoder}
 * (OpenStreetMap).
 *
 * Si tampoco allí aparece (o el servicio no responde), la estimación de
 * costes lo indica y no calcula nada.
 */
@Component
public class CityLocator {

    /** Punto geográfico (grados decimales). */
    public record Point(double lat, double lon) {
        /** Aproximación: al oeste del Atlántico (las Américas). */
        boolean isAmericas() {
            return lon < -30;
        }
    }

    private final Map<String, Point> cities = new HashMap<>();
    private final RemoteGeocoder remote;

    public CityLocator(RemoteGeocoder remote) {
        this.remote = remote;
        // España: capitales de provincia y ciudades con museos relevantes.
        add(40.4168, -3.7038, "madrid");
        add(41.3874, 2.1686, "barcelona");
        add(39.4699, -0.3763, "valencia");
        add(37.3891, -5.9845, "sevilla", "seville");
        add(41.6488, -0.8891, "zaragoza");
        add(36.7213, -4.4214, "malaga");
        add(37.9922, -1.1307, "murcia");
        add(39.5696, 2.6502, "palma", "palma de mallorca");
        add(28.1235, -15.4363, "las palmas", "las palmas de gran canaria");
        add(43.2630, -2.9350, "bilbao");
        add(38.3452, -0.4810, "alicante", "alacant");
        add(37.8882, -4.7794, "cordoba");
        add(41.6523, -4.7245, "valladolid");
        add(42.2406, -8.7207, "vigo");
        add(43.5322, -5.6611, "gijon", "xixon");
        add(43.3623, -8.4115, "a coruna", "la coruna", "coruna");
        add(42.8467, -2.6716, "vitoria", "vitoria-gasteiz", "gasteiz");
        add(37.1773, -3.5986, "granada");
        add(43.3614, -5.8493, "oviedo");
        add(28.4636, -16.2518, "santa cruz de tenerife", "tenerife");
        add(42.8125, -1.6458, "pamplona", "iruna");
        add(36.8340, -2.4637, "almeria");
        add(43.3183, -1.9812, "san sebastian", "donostia", "donostia-san sebastian");
        add(43.4623, -3.8100, "santander");
        add(42.3439, -3.6969, "burgos");
        add(40.9701, -5.6635, "salamanca");
        add(42.4627, -2.4450, "logrono");
        add(38.8794, -6.9707, "badajoz");
        add(37.2614, -6.9447, "huelva");
        add(36.5271, -6.2886, "cadiz");
        add(42.5987, -5.5671, "leon");
        add(41.1189, 1.2445, "tarragona");
        add(41.6176, 0.6200, "lleida", "lerida");
        add(41.9794, 2.8214, "girona", "gerona");
        add(39.9864, -0.0513, "castellon", "castellon de la plana");
        add(37.7796, -3.7849, "jaen");
        add(38.9943, -1.8585, "albacete");
        add(39.4753, -6.3724, "caceres");
        add(38.9848, -3.9274, "ciudad real");
        add(39.8628, -4.0273, "toledo");
        add(40.0704, -2.1374, "cuenca");
        add(40.6330, -3.1672, "guadalajara");
        add(40.9429, -4.1088, "segovia");
        add(40.6565, -4.6818, "avila");
        add(41.7665, -2.4790, "soria");
        add(41.5035, -5.7446, "zamora");
        add(42.0095, -4.5288, "palencia");
        add(42.1401, -0.4089, "huesca");
        add(40.3456, -1.1065, "teruel");
        add(43.0097, -7.5568, "lugo");
        add(42.3358, -7.8639, "ourense", "orense");
        add(42.4310, -8.6444, "pontevedra");
        add(42.8782, -8.5448, "santiago de compostela", "santiago");
        add(38.9161, -6.3437, "merida");
        add(36.5101, -4.8825, "marbella");
        add(38.9067, 1.4206, "ibiza", "eivissa");
        add(35.8894, -5.3213, "ceuta");
        add(35.2923, -2.9381, "melilla");

        // Europa.
        add(38.7223, -9.1393, "lisboa", "lisbon");
        add(41.1579, -8.6291, "oporto", "porto");
        add(48.8566, 2.3522, "paris");
        add(51.5074, -0.1278, "londres", "london");
        add(41.9028, 12.4964, "roma", "rome");
        add(45.4642, 9.1900, "milan", "milano");
        add(43.7696, 11.2558, "florencia", "firenze", "florence");
        add(45.4408, 12.3155, "venecia", "venezia", "venice");
        add(45.0703, 7.6869, "turin", "torino");
        add(40.8518, 14.2681, "napoles", "napoli", "naples");
        add(52.5200, 13.4050, "berlin");
        add(48.1351, 11.5820, "munich", "munchen");
        add(50.1109, 8.6821, "frankfurt", "francfort");
        add(53.5511, 9.9937, "hamburgo", "hamburg");
        add(50.9375, 6.9603, "colonia", "koln", "cologne");
        add(52.3676, 4.9041, "amsterdam");
        add(52.0705, 4.3007, "la haya", "den haag", "the hague");
        add(51.9244, 4.4777, "rotterdam");
        add(50.8503, 4.3517, "bruselas", "brussels", "bruxelles", "brussel");
        add(51.2194, 4.4025, "amberes", "antwerp", "antwerpen");
        add(48.2082, 16.3738, "viena", "wien", "vienna");
        add(47.3769, 8.5417, "zurich");
        add(46.2044, 6.1432, "ginebra", "geneve", "geneva");
        add(47.5596, 7.5886, "basilea", "basel");
        add(45.7640, 4.8357, "lyon");
        add(43.2965, 5.3698, "marsella", "marseille");
        add(44.8378, -0.5792, "burdeos", "bordeaux");
        add(43.6047, 1.4442, "toulouse", "tolosa");
        add(43.7102, 7.2620, "niza", "nice");
        add(53.3498, -6.2603, "dublin");
        add(55.9533, -3.1883, "edimburgo", "edinburgh");
        add(55.6761, 12.5683, "copenhague", "copenhagen", "kobenhavn");
        add(59.3293, 18.0686, "estocolmo", "stockholm");
        add(59.9139, 10.7522, "oslo");
        add(50.0755, 14.4378, "praga", "prague", "praha");
        add(52.2297, 21.0122, "varsovia", "warsaw", "warszawa");
        add(47.4979, 19.0402, "budapest");
        add(37.9838, 23.7275, "atenas", "athens");
        add(49.6116, 6.1319, "luxemburgo", "luxembourg");
        add(43.7384, 7.4246, "monaco", "monte carlo");

        // América.
        add(40.7128, -74.0060, "nueva york", "new york");
        add(38.9072, -77.0369, "washington");
        add(34.0522, -118.2437, "los angeles");
        add(41.8781, -87.6298, "chicago");
        add(25.7617, -80.1918, "miami");
        add(19.4326, -99.1332, "ciudad de mexico", "mexico city", "cdmx");
        add(-34.6037, -58.3816, "buenos aires");
    }

    /**
     * Busca una ciudad por nombre. Tolera mayúsculas, tildes y textos como
     * "Madrid, España" o "Bilbao (Bizkaia)".
     */
    public Optional<Point> find(String place) {
        if (place == null || place.isBlank()) return Optional.empty();
        String name = place.split("[,(]")[0];
        Point known = cities.get(normalize(name));
        // Primero la tabla local (instantánea); si no está, OpenStreetMap.
        return known != null ? Optional.of(known) : remote.find(name);
    }

    /** Distancia por carretera estimada: línea recta × 1,25. */
    public static double roadKm(Point a, Point b) {
        return greatCircleKm(a, b) * 1.25;
    }

    /** Distancia en línea recta (fórmula del haversine). */
    public static double greatCircleKm(Point a, Point b) {
        double r = 6371;
        double dLat = Math.toRadians(b.lat() - a.lat());
        double dLon = Math.toRadians(b.lon() - a.lon());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(a.lat())) * Math.cos(Math.toRadians(b.lat()))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.asin(Math.sqrt(h));
    }

    /** Hace falta avión si una ciudad está en América y la otra no. */
    public static boolean needsAirFreight(Point a, Point b) {
        return a.isAmericas() != b.isAmericas();
    }

    private void add(double lat, double lon, String... names) {
        Point p = new Point(lat, lon);
        for (String n : names) cities.put(normalize(n), p);
    }

    private static String normalize(String s) {
        String noAccents = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccents.toLowerCase().replace('ñ', 'n').trim().replaceAll("\\s+", " ");
    }
}
