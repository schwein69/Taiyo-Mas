inverter_state(active).
current_weather(unknown).
current_routing(balanced).

// ==========================================
// RICEZIONE INFORMAZIONI METEO
// ==========================================

+weather(sunny)[source(weather)]
    <-  -+current_weather(sunny);
        .print("[PANELS] Ricevuto meteo: sunny.").

+weather(night)[source(weather)]
    <-  -+current_weather(night);
        .print("[PANELS] Ricevuto meteo: night.").

+weather(rainy)[source(weather)]
    <-  -+current_weather(rainy);
        .print("[PANELS] Ricevuto meteo: rainy.").

+weather(foggy)[source(weather)]
    <-  -+current_weather(foggy);
        .print("[PANELS] Ricevuto meteo: foggy.").


// ==========================================
// GESTIONE DEI FLUSSI ENERGETICI
// ==========================================

// Gestione Standard (Alimenta casa, ricarica batteria e immette eventuale surplus)
+pv_flow(combined_distribution)
    <-  -+current_routing(hybrid);
        .print("[PANELS] Distribuzione ibrida.").

// Alta richiesta. Pannelli, batteria e rete alimentano la casa contemporaneamente
+pv_flow(high_load_all_sources_to_house)
    <-  -+current_routing(house_only);
        .print("[PANELS] Pannelli, batteria e rete alimentano la casa.").

// Batteria Piena. L'energia copre la casa e la differenza va tutta in rete
+pv_flow(battery_full_surplus_to_grid)
    <-  -+current_routing(house_and_grid);
        .print("[PANELS] BATTERIA PIENA. Alimento la casa e immetto il surplus in rete.").

// Immissione totale in rete
+pv_flow(full_grid_injection)
    <-  -+current_routing(grid_only);
        .print("[PANELS] Immissione totale in rete.").


// ==========================================
// REAZIONE ALLE INFORMAZIONI DEGLI AGENTI
// ==========================================

// Reazione autonoma al blackout notificato da un altro agente
+grid_status(blackout)[source(A)]
    <-  .print("[PANELS] L'agente ", A, " mi informa di non produrre.");
        .print("[PANELS] STANDBY.");
        -+inverter_state(standby);
        !do_action(panel_standby).

// Reazione autonoma al ripristino della rete
+grid_status(connected)[source(A)]
    <-  .print("[PANELS] L'agente ", A, " mi informa che la rete è tornata.");
        .print("[PANELS] Riprendo la produzione.");
        -+inverter_state(active);
        !do_action(panel_resume).

// Reazione autonoma alle STRATEGIE notificate da house_grid
+operation_mode(selling)[source(house_grid)]
    <-  .print("[PANELS] House Grid avvisa: modalità SELLING. Attivo immissione totale in rete!");
        !do_action(set_selling_mode).

+operation_mode(balanced)[source(house_grid)]
    <-  .print("[PANELS] House Grid avvisa: modalità BALANCED. Ripristino distribuzione ibrida.");
        !do_action(set_balanced_mode).

+operation_mode(direct)[source(house_grid)]
    <-  .print("[PANELS] House Grid avvisa: modalità DIRECT. Continuo produzione (bypass batteria).");
        !do_action(set_direct_mode).



+!do_action(panel_standby)
    <- .print("      -> Standby inverter fotovoltaico");
       panel_standby.

+!do_action(panel_resume)
    <- .print("      -> Ripristino inverter fotovoltaico");
       panel_resume.

+!do_action(set_selling_mode)
    <- .print("      -> Imposto inverter su VENDITA TOTALE");
       set_selling_mode.

+!do_action(set_balanced_mode)
    <- .print("      -> Imposto inverter su MODALITÀ BILANCIATA");
       set_balanced_mode.

+!do_action(set_balanced_mode)
    <- .print("      -> Imposto inverter su MODALITÀ DIRECT");
       set_direct_mode.