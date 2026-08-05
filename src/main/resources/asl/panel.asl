inverter_state(active).
current_weather(unknown).
current_routing(balanced).

// RICEZIONE METEO DALL'AGENTE WEATHER

+weather(W)[source(weather)] : not current_weather(W)
    <-  -+current_weather(W);
        .print("[PANEL] Rilevato cambio meteo: ", W);
        !evaluate_weather_impact.

+weather(W)[source(weather)] : current_weather(W)
    <-  true.

// LOGICA DI DECISIONE AUTONOMA SUL METEO

+!evaluate_weather_impact : current_weather(night) & inverter_state(active)
    <-  .print("[PANEL] È notte: produzione nulla. Metto l'inverter in STANDBY.");
        -+inverter_state(standby);
        !do_action(panel_standby).

+!evaluate_weather_impact : current_weather(sunny) & inverter_state(standby) & not grid_status(blackout)
    <-  .print("[PANEL] È tornato il sole e la rete è attiva: riaccendo l'inverter!");
        -+inverter_state(active);
        !do_action(panel_resume).

// Maltempo diviso in due regole per evitare crash del parser sull'operatore OR
+!evaluate_weather_impact : current_weather(rainy)
    <-  .print("[PANEL] Attenzione: produzione ridotta a causa della pioggia.").

+!evaluate_weather_impact : current_weather(foggy)
    <-  .print("[PANEL] Attenzione: produzione ridotta a causa della nebbia.").

+!evaluate_weather_impact
    <-  true.

// GESTIONE DEI FLUSSI ENERGETICI

+pv_flow(combined_distribution) : not current_routing(hybrid)
    <-  -+current_routing(hybrid);
        .print("[PANEL] Distribuzione ibrida.").

+pv_flow(high_load_all_sources_to_house) : not current_routing(house_only)
    <-  -+current_routing(house_only);
        .print("[PANEL] Pannelli, batteria e rete alimentano la casa.").

+pv_flow(battery_full_surplus_to_grid) : not current_routing(house_and_grid)
    <-  -+current_routing(house_and_grid);
        .print("[PANEL] BATTERIA PIENA. Alimento la casa e immetto surplus in rete.").

+pv_flow(full_grid_injection) : not current_routing(grid_only)
    <-  -+current_routing(grid_only);
        .print("[PANEL] Immissione totale in rete.").

// REAZIONE ALLE INFORMAZIONI DEGLI AGENTI

+grid_status(blackout)[source(A)]
    <-  .print("[PANEL] L'agente ", A, " mi informa di non produrre. STANDBY.");
        -+inverter_state(standby);
        !do_action(panel_standby).

+grid_status(connected)[source(A)]
    <-  .print("[PANEL] L'agente ", A, " mi informa che la rete è tornata. Riprendo.");
        -+inverter_state(active);
        !do_action(panel_resume).

+operation_mode(selling)[source(house_grid)]
    <-  .print("[PANEL] House Grid avvisa: SELLING. Attivo immissione in rete!");
        !do_action(set_selling_mode).

+operation_mode(balanced)[source(house_grid)]
    <-  .print("[PANEL] House Grid avvisa: BALANCED. Ripristino distribuzione.");
        !do_action(set_balanced_mode).

+operation_mode(direct)[source(house_grid)]
    <-  .print("[PANEL] House Grid avvisa: DIRECT. Continuo immissione.");
        !do_action(set_direct_mode).

// AZIONI HARDWARE

+!do_action(panel_standby)
    <-  .print("      -> Standby inverter fotovoltaico");
        panel_standby.

+!do_action(panel_resume)
    <-  .print("      -> Ripristino inverter fotovoltaico");
        panel_resume.

+!do_action(set_selling_mode)
    <-  .print("      -> Imposto inverter su VENDITA TOTALE");
        set_selling_mode.

+!do_action(set_balanced_mode)
    <-  .print("      -> Imposto inverter su MODALITÀ BILANCIATA");
        set_balanced_mode.

+!do_action(set_direct_mode)
    <-  .print("      -> Imposto inverter su MODALITÀ DIRECT");
        set_direct_mode.