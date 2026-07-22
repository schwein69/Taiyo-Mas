grid_status(connected).
active_mode(balanced).


+blackout_active
    <-  -+grid_status(blackout);
        .print("[HOUSE_GRID] ALLARME: Rilevato Blackout della rete elettrica esterna");
        !do_action(disconnect_loads);
        .broadcast(tell, grid_status(blackout)).

+grid_connected
    <-  ?grid_status(S);
        if (S == blackout) {
            -+grid_status(connected);
            .print("[HOUSE_GRID] INFO: Rete elettrica ripristinata. ");
            !do_action(reconnect_loads);
            .broadcast(tell, grid_status(connected));
        }.

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

// ==========================================
// RISPOSTA ALLE RICHIESTE DELLA BATTERIA
// ==========================================

+!disconnect_loads[source(battery)]
    <-  .print("[HOUSE_GRID] Ricevuta richiesta STRIPS da battery: stacco i carichi.");
        !do_action(disconnect_loads).



+!do_action(disconnect_loads)
    <- .print("      -> [Hardware] Stacco i carichi non essenziali di casa.");
       disconnect_loads.

+!do_action(reconnect_loads)
    <- .print("      -> [Hardware] Riattacco i carichi domestici.");
       reconnect_loads.