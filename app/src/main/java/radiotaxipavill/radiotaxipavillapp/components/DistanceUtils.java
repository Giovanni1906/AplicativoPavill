package radiotaxipavill.radiotaxipavillapp.components;

public class DistanceUtils {

    /**
     * Calcula el tiempo estimado en minutos basado en distancia y velocidad promedio.
     *
     * @param distanceInMeters Distancia en metros.
     * @param averageSpeedKmH Velocidad promedio en km/h.
     * @return Tiempo estimado en minutos.
     */
    public static int calculateEstimatedTimeInMinutes(float distanceInMeters, float averageSpeedKmH) {
        float distanceInKm = distanceInMeters / 1000; // Convertir metros a kilómetros
        float timeInHours = distanceInKm / averageSpeedKmH; // Tiempo en horas
        return Math.round(timeInHours * 60); // Convertir a minutos y redondear
    }
}
