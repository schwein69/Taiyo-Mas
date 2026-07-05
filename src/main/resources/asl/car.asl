car_state(disconnected).
charge_level(ok).
grid_status(connected).
current_mode(balanced).

// ==========================================
// GESTIONE COLLEGAMENTO FISICO E BATTERIA (SoC)
// ==========================================

+car_plugged_in
    <-  -+car_state(connected);
        .print("[CAR] Auto collegata alla presa domestica.");
        !check_charging.

+car_unplugged
    <-  -+car_state(disconnected);
        .print("[CAR] Auto scollegata.");
        !do_action(stop_charging).

// Gestione Livello Batteria (es. soglia desiderata all'80%)
+car_soc(V) : V < 80 & car_state(connected)
    <-  -+charge_level(low);
        .print("[CAR] Livello batteria auto al ", V, "%. Necessaria ricarica.");
        !check_charging.

+car_soc(V) : V >= 80 & car_state(connected)
    <-  -+charge_level(ok);
        .print("[CAR] Livello batteria auto al ", V, "%. Ricarica completata.");
        !do_action(stop_charging).


// ==========================================
// REAZIONE ALLE INFORMAZIONI DEGLI AGENTI
// ==========================================

// --- Reazione Autonoma alla Rete  ---
+grid_status(blackout)[source(A)]
    <-  -+grid_status(blackout);
        .print("[CAR] Messaggio da ", A, ": BLACKOUT RILEVATO!");
        .print("[CAR] Sospendo immediatamente la ricarica.");
        !do_action(stop_charging).

+grid_status(connected)[source(A)]
    <-  -+grid_status(connected);
        .print("[CAR] Messaggio da ", A, ": Rete ripristinata.");
        !check_charging. // Valuta se riprendere a caricare

// --- Reazione Autonoma alle Strategie ---
+operation_mode(selling)[source(house_grid)]
    <-  -+current_mode(selling);
        .print("[CAR] House Grid avvisa: modalità SELLING.");
        .print("[CAR] Metto in pausa la ricarica per favorire la vendita del solare in rete.");
        !do_action(stop_charging).

+operation_mode(balanced)[source(house_grid)]
    <-  -+current_mode(balanced);
        .print("[CAR] House Grid avvisa: modalità BALANCED. Riprendo la normale gestione.");
        !check_charging.

+operation_mode(direct)[source(house_grid)]
    <-  -+current_mode(direct);
        .print("[CAR] House Grid avvisa: modalità DIRECT.");
        !do_action(stop_charging).


// ==========================================
// LOGICA DI DECISIONE INTERNA
// ==========================================
// Valuta se ci sono le condizioni per caricare

+!check_charging : charge_level(low) & grid_status(connected) & current_mode(M) & M \== selling & M \== direct & car_state(connected)
    <- .print("[CAR] Avvio la ricarica intelligente.");
       !do_action(start_charging).

// Piano di fallback: se le condizioni non sono soddisfatte, non fare nulla
+!check_charging
    <- .print("[CAR] Condizioni non adatte alla ricarica in questo momento.").



+!do_action(start_charging)
    <- .print("      -> [Hardware] Attivazione ricarica auto");
       car_start_charging.

+!do_action(stop_charging)
    <- .print("      -> [Hardware] Sospensione ricarica auto");
       car_stop_charging.