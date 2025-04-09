package org.onebusaway.util;

public class EarthDistanceUtil {

    private static final double EARTH_RADIUS_KM = 6371;

    public static double distanceInKilometers(double lat1, double lon1, double lat2, double lon2) {
        return computeDistanceKm(lat1, lon1, lat2, lon2, DistanceUnit.KILOMETERS);
    }

    public static double distanceInMeters(double lat1, double lon1, double lat2, double lon2) {
        return computeDistanceKm(lat1, lon1, lat2, lon2, DistanceUnit.METERS);
    }

    public static double distanceInMiles(double lat1, double lon1, double lat2, double lon2) {
        return computeDistanceKm(lat1, lon1, lat2, lon2, DistanceUnit.MILES);
    }

    public static double distanceInFeet(double lat1, double lon1, double lat2, double lon2) {
        return computeDistanceKm(lat1, lon1, lat2, lon2, DistanceUnit.FEET);
    }

    private static double computeDistanceKm(double lat1, double lon1, double lat2, double lon2, DistanceUnit unit) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(rLat1) * Math.cos(rLat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distanceKm = EARTH_RADIUS_KM * c;

        return unit.fromKilometers(distanceKm);
    }

    public enum DistanceUnit {
        KILOMETERS(1.0),
        METERS(1000.0),
        MILES(0.621371),
        FEET(3280.8399);

        private final double kmMultiplier;

        DistanceUnit(double kmMultiplier) {
            this.kmMultiplier = kmMultiplier;
        }

        public double fromKilometers(double km) {
            return km * kmMultiplier;
        }
    }

}
