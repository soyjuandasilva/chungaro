# Atesta: Preço do Combustível

> Nombre en el móvil: **Atesta** (`app_name` en `app/src/main/res/values/strings.xml`).
> Título en Google Play: **Atesta: Preço do Combustível**. La interfaz está en
> **português de Portugal**, y le hablamos al usuario **de tú** («o teu veículo»,
> «toca», «escolhe»), nunca de «você»/«si».

App Android para ahorrar en combustible en Portugal: precios oficiales cerca de
ti, registro de cargas, consumo, gráficos y mantenimiento.

## Pestañas

- **Início**: lo general, sin depender de tu vehículo.
  - **Preço médio dos combustíveis hoje**, en grande: media de todas las
    gasolineras de Portugal con precio en la DGEG (se avisa de que varía de
    una a otra). No necesita la ubicación.
  - **Evolução dos preços** de tu zona frente a la semana pasada.
  - **Preço dos combustíveis na próxima semana**: las 5 últimas noticias de
    Google Notícias Portugal para esa búsqueda (título, medio y enlace; el
    extracto solo si el feed lo trae).
- **Mapa**: eliges el combustible entre todos a la vista (sin deslizar);
  gasolineras de la zona en OpenStreetMap con su precio, en verde las
  baratas y en rojo las caras. «Cómo llegar» abre Maps.
- **Meu veículo** (antes «Registos»), con cuatro secciones:
  - **Preços**: consejos del momento, radio (5, 10 o 20 km), la más barata y
    la más cercana para tu combustible con si **merece la pena** el desvío, y
    la tabla de **outros combustíveis**.
  - **Histórico**, **Gráficos** y **Manutenção** de tus cargas.
- **Vehículos**: varios, cada uno con tipo (coche, moto, furgoneta, camión,
  autocaravana), marca y modelo (con sugerencias), año, combustible, litros al
  llenar y consumo.
- **Favoritas**: busca gasolineras por distrito y guarda tus favoritas.

## Ajustes

- Litros o galones, consumo en **L/100 km** o km/L, símbolo de moneda.
- **Avisos de precio**: notificación cuando tu combustible baje de un precio
  cerca de tu última ubicación (se revisa cada 6 h).
- **Copia de seguridad**: exportar e importar vehículos, cargas y recordatorios
  en un archivo JSON (por ejemplo, a Google Drive).

## Descargar el APK

Cada push compila la app con GitHub Actions. En la pestaña **Actions**, abre
la última ejecución de **Android** y descarga el artefacto `atesta-apk`.
Los APK se firman con una clave de depuración fija, así que las versiones
nuevas se instalan encima de la anterior.

Fuentes de datos: DGEG (precoscombustiveis.dgeg.gov.pt) y © OpenStreetMap.
Tecnología: Kotlin, Jetpack Compose (Material 3), Room, WorkManager y osmdroid.
