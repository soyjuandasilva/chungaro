# Atesta: Preço do Combustível

> Nombre en el móvil: **Atesta** (`app_name` en `app/src/main/res/values/strings.xml`).
> Título en Google Play: **Atesta: Preço do Combustível**. La interfaz está en
> **português de Portugal**, y le hablamos al usuario **de tú** («o teu veículo»,
> «toca», «escolhe»), nunca de «você»/«si».

App Android para ahorrar en combustible en Portugal: precios oficiales cerca de
ti, registro de cargas, consumo, gráficos y mantenimiento.

## Pantallas

- **Tour de bienvenida**: la primera vez que se abre la app, 4 pantallas
  animadas (bienvenida, mapa, «¿abastecer hoy o esperar al lunes?» y Meu
  veículo) que terminan pidiendo la ubicación. Se puede volver a ver desde el
  menú: «Como funciona a Atesta».

- **Inicio (mapa)**: mapa a pantalla completa con el precio de cada gasolinera
  (verde las baratas, rojo las caras) y una hoja que se arrastra desde abajo:
  combustible (desplegable), radio (5, 10 o 20 km), orden (más barata, más
  próxima o favoritas), ahorro por depósito y la lista de bombas con precio y
  hace cuánto se actualizó. Botón de menú arriba a la izquierda.
  Arriba de la lista, **«Abastece hoje ou espera por segunda?»**: en Portugal
  los precios cambian los lunes y los medios publican la previsión a final de
  semana. La app lee los titulares de esta semana (Google Notícias), detecta si
  anuncian subida o bajada para tu tipo de combustible (gasolina, gasóleo o
  GPL) y aconseja llenar antes del lunes o esperar, enlazando la noticia.
- **Ficha de gasolinera**: logotipo, favorita (♥) e «Ir lá»; precios de todos
  sus combustibles, tendencia y morada.
- **Menú lateral**: tu vehículo y las secciones:
  - **Meu veículo**: Preços (consejos, mejores opciones y otros combustibles),
    Histórico, Gráficos y Manutenção.
  - **Preços e notícias**: precio medio de hoy en Portugal, evolución de tu
    zona y las 5 últimas noticias de Google Notícias sobre el precio de los
    combustibles la próxima semana.
  - **Bombas favoritas**, **Alertas de preço** y **Definições**.
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
