package model

/**
 * Rappresenta i possibili stati climatici supportati dalla simulazione.
 */
enum class WeatherStatus {
    SUNNY,
    NIGHT,
    FOGGY,
    RAINY
}

/**
 * Interfaccia che definisce il contratto per la gestione dello stato meteo
 * e dei suoi effetti sui componenti del sistema TAIYO-MAS.
 */
interface Weather {
    /** Lo stato climatico corrente del sistema. */
    var status: WeatherStatus

    /** Fattore di irraggiamento solare normalizzato (compreso tra 0.0 e 1.0). */
    val solarIrradiance: Double

    /** Flag che indica se la condizione meteo corrente comporta criticità operative. */
    val negativeImpact: Boolean

    /** Aggiorna casualmente lo stato del meteo tra quelli disponibili. */
    fun updateRandomly()
}