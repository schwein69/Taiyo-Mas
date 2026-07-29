inverter_state(active).
current_weather(unknown).
current_routing(hybrid).

// RICEZIONE PERCEZIONI METEO DALL'ENVIRONMENT

// Quando l'Environment Kotlin invia un nuovo status meteo che è DIVERSO dall'ultimo noto
+weather_status(W) : not current_weather(W)
    <-  -+current_weather(W);
        .print("[PANELS] Rilevato cambio meteo dall'ambiente: ", W);
        !evaluate_weather_impact.

// Se l'Environment ripete lo stesso meteo, ignoriamo per evitare spam in console
+weather_status(W) : current_weather(W)
    <-  true.



// È notte
+!evaluate_weather_impact : current_weather(night) & inverter_state(active)
    <-  .print("[PANELS] È notte: produzione nulla. Metto l'inverter in STANDBY.");
        -+inverter_state(standby);
        !do_action(panel_standby).

// Torna il sole
+!evaluate_weather_impact : current_weather(sunny) & inverter_state(standby) & not grid_status(blackout)
    <-  .print("[PANELS] È tornato il sole");
        -+inverter_state(active);
        !do_action(panel_resume).

// Maltempo (pioggia o nebbia): informiamo la casa che produrremo poco
+!evaluate_weather_impact : (current_weather(rainy) | current_weather(foggy))
    <-  .print("[PANELS] Attenzione: produzione ridotta a causa del maltempo.");

// Piano di fallback: per tutti gli altri casi, non è necessaria alcuna azione
+!evaluate_weather_impact
    <-  true.


// GESTIONE DEI FLUSSI ENERGETICI

+pv_flow(combined_distribution) : not current_routing(hybrid)
    <-  -+current_routing(hybrid);
        .print("[PANELS] Distribuzione ibrida.").

+pv_flow(high_load_all_sources_to_house) : not current_routing(house_only)
    <-  -+current_routing(house_only);
        .print("[PANELS] Pannelli, batteria e rete alimentano la casa.").

+pv_flow(battery_full_surplus_to_grid) : not current_routing(house_and_grid)
    <-  -+current_routing(house_and_grid);
        .print("[PANELS] BATTERIA PIENA. Alimento la casa e immetto il surplus in rete.").

+pv_flow(full_grid_injection) : not current_routing(grid_only)
    <-  -+current_routing(grid_only);
        .print("[PANELS] Immissione totale in rete.").


// REAZIONE ALLE INFORMAZIONI DEGLI AGENTI

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
    <-  .print("[PANELS] House Grid avvisa: modalità DIRECT. Continuo immissione.");
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

+!do_action(set_direct_mode)
    <- .print("      -> Imposto inverter su MODALITÀ DIRECT");
       set_direct_mode.