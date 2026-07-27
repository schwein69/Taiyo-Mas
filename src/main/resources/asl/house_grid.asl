grid_status(connected).
active_mode(balanced).

loads(connected).
car(charging).
battery(ok).

// GESTIONE BLACKOUT CON PLANNER STRIPS

+blackout_active
    <-  -+grid_status(blackout);
        .print("[HOUSE_GRID] ALLARME: Rilevato Blackout della rete elettrica esterna");
        .print("[HOUSE_GRID] Chiedo al Planner STRIPS come mettere il sistema in sicurezza...");

        // planning.CalculatePlan(Domain, InitialState, GoalState, PlanResultVar)
        planning.CalculatePlan("house_grid",
            ["grid_offline", "loads_connected", "car_charging", "battery_ok"],
            ["system_safe"],
            Plan);

        .print("[HOUSE_GRID] Piano calcolato dal Planner: ", Plan);

        !execute_plan(Plan);

        -+loads(disconnected);
        -+car(paused);
        .broadcast(tell, grid_status(blackout)).

+grid_connected
    <-  ?grid_status(S);
        if (S == blackout) {
            -+grid_status(connected);
            .print("[HOUSE_GRID] INFO: Rete elettrica ripristinata.");
            .print("[HOUSE_GRID] Chiedo al Planner STRIPS come ripristinare i servizi...");

            planning.CalculatePlan("house_grid",
                ["grid_connected", "loads_disconnected", "car_paused", "battery_ok"],
                ["loads_connected", "car_charging"],
                Plan);

            .print("[HOUSE_GRID] Piano di ripristino calcolato: ", Plan);
            !execute_plan(Plan);

            -+loads(connected);
            -+car(charging);
            .broadcast(tell, grid_status(connected));
        }.

// GESTIONE RISCHIO SOVRACCARICO O MALTEMPO
+overload_risk : car(charging)
    <-  .print("[HOUSE_GRID] ATTENZIONE: Rilevato Sovraccarico");
        .print("[HOUSE_GRID] Chiedo al Planner STRIPS come bilanciare il carico...");

        planning.CalculatePlan("house_grid",
            ["overload_risk", "car_charging"],
            ["load_balanced"],
            Plan);

        .print("[HOUSE_GRID] Piano calcolato: ", Plan);
        !execute_plan(Plan);

        -+car(throttled);
        .broadcast(tell, system_status(balanced)).

// Quando il rischio rientra, ripristiniamo la carica alla massima potenza
-overload_risk : car(throttled) & grid_status(connected) & battery(ok)
    <-  .print("[HOUSE_GRID] INFO: Rischio sovraccarico rientrato. Ripristino i carichi.");

        planning.CalculatePlan("house_grid",
            ["grid_connected", "ev_throttled", "battery_ok"],
            ["car_charging"],
            Plan);

        .print("[HOUSE_GRID] Piano di ripristino calcolato: ", Plan);
        !execute_plan(Plan);

        -+car(charging).


// PROTEZIONE BATTERIA SCARICA DI NOTTE

+weather_status(night) : battery_soc(B) & B < 20 & car(charging)
    <-  .print("[HOUSE_GRID] ATTENZIONE: È notte e la batteria è scesa sotto il 20%!");
        .print("[HOUSE_GRID] Chiedo al Planner STRIPS come proteggere l'accumulo...");

        // Obiettivo: "battery_protected"
        planning.CalculatePlan("house_grid",
            ["night_time", "battery_low", "car_charging"],
            ["battery_protected"],
            Plan);

        .print("[HOUSE_GRID] Piano calcolato: ", Plan);
        !execute_plan(Plan);

        -+car(paused);
        -+battery(low).



+!execute_plan([]).

+!execute_plan([Action | Tail])
    <-  !do_action(Action);
        !execute_plan(Tail).



+!do_action(disconnect_loads)
    <- .print("      -> [Hardware] Stacco i carichi non essenziali di casa.");
       disconnect_loads.

+!do_action(reconnect_loads)
    <- .print("      -> [Hardware] Riattacco i carichi domestici.");
       reconnect_loads.

+!do_action(pause_ev_charging)
    <- .print("      -> [Hardware] Sospendo la ricarica dell'auto elettrica per risparmiare energia.");
        pause_ev_charging.

+!do_action(resume_ev_charging)
    <- .print("      -> [Hardware] Riprendo la ricarica dell'auto elettrica.");
        resume_ev_charging.

+!do_action(activate_island_mode)
    <- .print("      -> [Hardware] Attivo Modalità a Isola. La casa è isolata e sicura.");
       activate_island_mode.

+!do_action(throttle_ev_charging)
    <- .print("      -> [Hardware] Limito la ricarica dell'auto per evitare il sovraccarico");
       throttle_ev_charging.

+!do_action(suspend_ev_for_night)
    <- .print("      -> [Hardware] Blocco totalmente l'auto per non prosciugare la batteria di notte.");
       pause_ev_charging.

+!do_action(force_grid_charging)
    <- .print("      -> [Hardware] Abilito il prelievo forzato dalla rete verso la batteria.");
       force_grid_charging.

// ==========================================
// GESTIONE DELLE STRATEGIE OPERATIVE
// ==========================================

// Passaggio a modalità BALANCED
+system_mode(balanced) : active_mode(M) & M \== balanced
    <-  -+active_mode(balanced);
        .print("[HOUSE_GRID] STRATEGIA: Passaggio a modalità BALANCED.");
        .broadcast(tell, operation_mode(balanced)).

// Passaggio a modalità DIRECT
+system_mode(direct) : active_mode(M) & M \== direct
    <-  -+active_mode(direct);
        .print("[HOUSE_GRID] STRATEGIA: Passaggio a modalità DIRECT.");
        .broadcast(tell, operation_mode(direct)).

// Passaggio a modalità SELLING
+system_mode(selling) : active_mode(M) & M \== selling
    <-  -+active_mode(selling);
        .print("[HOUSE_GRID] STRATEGIA: Passaggio a modalità SELLING.");
        .broadcast(tell, operation_mode(selling)).

+!disconnect_loads[source(battery)]
    <-  .print("[HOUSE_GRID] Ricevuta richiesta STRIPS da battery: stacco i carichi.");
        !do_action(disconnect_loads).

// La batteria chiede di essere ricaricata forzatamente dalla rete
+!charge_from_grid[source(battery)]
    <-  .print("[HOUSE_GRID] Ricevuta richiesta da battery: prelevo energia dalla rete per ricaricarla.");
        !do_action(force_grid_charging).
