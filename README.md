# Atesta: Preço do Combustível

> Nombre en el móvil: **Atesta** (`app_name` en `app/src/main/res/values/strings.xml`).
> Título en Google Play: **Atesta: Preço do Combustível**. La interfaz está en
> **português de Portugal**, y le hablamos al usuario **de tú** («o teu veículo»,
> «toca», «escolhe»), nunca de «você»/«si».

App Android para ahorrar en combustible en Portugal: precios oficiales cerca de
ti, registro de cargas, consumo, gráficos y mantenimiento.

## Pantallas

Tema oscuro (verde casi negro con acentos verde brillante) en toda la app.

- **Tour de bienvenida**: la primera vez, 4 pantallas animadas que terminan
  pidiendo la ubicación (se repite desde Definições).
- **Inicio**: logotipo, avisos (🔔 → Alertas) y Definições (⚙️); buscador de
  postos por nombre, marca, localidad o morada; y 5 pestañas:
  - **Mapa**: filtros (combustível, raio, ordem), marcadores con el logotipo y color de cada marca,
    capas (normal/relieve), centrar y «Mais baratos perto de ti».
  - **Preços**: zona y radio, casillas por combustible con la media de la
    zona y la de Portugal, postos más baratos (☆ favorito) y Calculadora.
  - **Notícias**: veredicto del lunes y noticias de Google Notícias por
    categoría (Preços, Mercado, Dicas, Mobilidade).
  - **Favoritos**: tus postos con su precio e interruptor de Alertas.
  - **Histórico**: evolución del precio medio de tu zona (7 días a 1 año) con
    gráfico, variación, mínimo y máximo; y «Comparar» todos los combustibles.
- **Detalhes do posto**: banner de la marca, morada, distancia y tiempo,
  «Como chegar», precios de todos sus combustibles, tendencia e información;
  compartir y favorito.
- **Calculadora de poupança**: litros, combustible y lo que pagas en cada
  posto; cuánto ahorras frente al más caro.
- **Alertas de preço**: combustible, precio objetivo y todas las bombas de la
  zona o solo las favoritas (se revisa cada 6 h).
- **Definições**: accesos (O meu veículo, Alertas, Calculadora, Unidades…),
  vehículos, unidades y copia de seguridad.
- **O meu veículo**: Preços, Histórico de cargas, Gráficos y Manutenção.
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
