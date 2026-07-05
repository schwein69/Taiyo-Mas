package model

/**
 * Rappresenta le strategie operative a livello di sistema per TAIYO-MAS.
 * Questi stati influenzano il comportamento decisionale degli agenti BDI.
 */
enum class Mode {
    /**
     * Bilanciamento dinamico.
     * Priorità all'autoconsumo, alla stabilità del sistema e alla longevità della batteria.
     */
    BALANCED,

    /**
     * Alimentazione diretta.
     * Bypass dell'accumulo. L'energia solare
     * va direttamente ai carichi; l'eccesso viene immesso in rete.
     */
    DIRECT,

    /**
     * Massimizzazione del profitto.
     * Il sistema cerca di scaricare la batteria e immettere tutta la produzione
     * solare nella rete durante i picchi di prezzo dell'energia.
     */
    SELLING
}