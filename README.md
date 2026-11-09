# Chungaro

App Android para ahorrar en combustible en Portugal: precios oficiales cerca de
ti, registro de cargas, consumo, gráficos y mantenimiento.

## Pestañas

- **Inicio**: con tu ubicación, tabla con todos los combustibles (Gasolina 95,
  95+, 98, Gasóleo, Gasóleo+ y GPL) que compara la gasolinera más cercana con
  la más barata en 5, 10 o 20 km. Tu combustible aparece primero, con una
  tarjeta que dice si **merece la pena** ir a la más barata (ahorro en el
  depósito menos lo que cuesta el desvío). Los precios se guardan en el
  teléfono y se muestran al instante al abrir.
- **Mapa**: gasolineras de la zona en OpenStreetMap con su precio, en verde las
  baratas y en rojo las caras. «Cómo llegar» abre Maps.
- **Cargas**: por vehículo. Historial de abastecimientos, **gráficos** (gasto
  mensual, precio pagado y consumo) y **mantenimiento** (recordatorios por
  fecha o kilometraje con aviso).
- **Favoritas**: busca gasolineras por distrito y guarda tus favoritas.

## Ajustes

- Litros o galones, consumo en **L/100 km** o km/L, símbolo de moneda.
- **Avisos de precio**: notificación cuando tu combustible baje de un precio
  cerca de tu última ubicación (se revisa cada 6 h).
- **Copia de seguridad**: exportar e importar vehículos, cargas y recordatorios
  en un archivo JSON (por ejemplo, a Google Drive).

## Descargar el APK

Cada push compila la app con GitHub Actions. En la pestaña **Actions**, abre
la última ejecución de **Android** y descarga el artefacto `chungaro-apk`.
Los APK se firman con una clave de depuración fija, así que las versiones
nuevas se instalan encima de la anterior.

Fuentes de datos: DGEG (precoscombustiveis.dgeg.gov.pt) y © OpenStreetMap.
Tecnología: Kotlin, Jetpack Compose (Material 3), Room, WorkManager y osmdroid.
