[ Frontend (Mapa) ] 
       │
       ▼
[ Controller ]
       │
       ▼
[ WeatherService (Strategy Context) ]
       │
       ├────► [ OpenWeatherAdapter ] ───► (API OpenWeather)
       │
       └────► [ MeteoRedAdapter ]    ───► (API MeteoRed - Fallback si falla OpenWeather)