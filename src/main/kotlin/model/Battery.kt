package model

/**
 * Rappresenta il contratto per la gestione del sistema di accumulo (batteria)
 * all'interno del sistema TAIYO-MAS.
 */
interface Battery {
    /** Capacità massima di accumulo della batteria. */
    val capacityKw: Double

    /** Carica energetica attuale accumulata nella batteria. */
    val currentChargeKw: Double

    /** Stato di carica (State of Charge) espresso in percentuale (0-100%). */
    val soc: Int

    /**
     * Scarica la batteria di una quantità specificata di energia.
     * @param amountKw Quantità di energia da prelevare.
     */
    fun discharge(amountKw: Double)

    /**
     * Carica la batteria di una quantità specificata di energia.
     * @param amountKw Quantità di energia da immettere.
     */
    fun charge(amountKw: Double)
}