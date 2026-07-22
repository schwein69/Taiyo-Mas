current_weather(sunny).


+weather_status(sunny)
    <-  -+current_weather(sunny);
        .print("[WEATHER] Sunny");
        .send(panels, tell, weather(sunny)).

+weather_status(night)
    <-  -+current_weather(night);
        .print("[WEATHER] Night");
        .send(panels, tell, weather(night)).

+weather_status(foggy)
    <-  -+current_weather(foggy);
        .print("[WEATHER] Foggy");
        .send(panels, tell, weather(foggy)).

+weather_status(rainy)
    <-  -+current_weather(rainy);
        .print("[WEATHER] Rainy");
        .send(panels, tell, weather(rainy)).

