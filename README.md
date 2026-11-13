# Abastece Hoje: Preço do Combustível

> Nombre en el móvil: **Abastece Hoje** (`app_name` en `app/src/main/res/values/strings.xml`).
> Título en Google Play: **Abastece Hoje: Preço do Combustível**. La interfaz está en
> **português de Portugal**, y le hablamos al usuario **de tú** («o teu veículo»,
> «toca», «escolhe»), nunca de «você»/«si».

App Android para ahorrar en combustible en Portugal: precios oficiales cerca de
ti, registro de cargas, consumo, gráficos y mantenimiento.

## Pantallas

Tema **claro** y **oscuro** con la misma identidad (verde de combustible sobre
neutros verdosos). Por defecto sigue al móvil; se fija en Definições → Aparência.
El claro se lee mejor al sol, en la bomba; el oscuro, de noche en el coche.
El tour de bienvenida es siempre verde oscuro.

### Sistema de diseño

- **Colores**: todos salen de `ui/Theme.kt` (`MaterialTheme.colorScheme` y
  `AbasteceHoje.colors`: precio, sube/baja, aviso, estrella…). Nada de `Color(0x…)`
  sueltos en las pantallas. Texto ≥ 4,5:1 y iconos/bordes de controles ≥ 3:1
  en los dos temas.
- **Tarjetas**: `cardSurface()` / `DarkCard`, radio 16 dp. Sobre el fondo,
  borde fino sin sombra; flotando sobre el mapa, sombra sin borde.
- **Precio**: `PriceChip`, como los totens de los postos: panel negro en los dos
  temas, cifras de 7 segmentos (`ui/SegmentDisplay.kt`, con los segmentos apagados
  tenues) y una franja del color del combustible. La web usa la misma geometría.
- **Iconos**: solo Material Icons (nada de emojis como iconos).
- **Accesibilidad**: pestañas y opciones con `selectable` + `Role`, áreas
  táctiles ≥ 48 dp, títulos marcados como encabezado; con letra grande las
  pestañas de Início se desplazan en vez de cortarse.
- **Números**: siempre en pt-PT (`APP_LOCALE`), sea cual sea el idioma del móvil.

### Logotipo

Una gota verde con una manguera de surtidor y una flecha que baja (el precio), junto a
«Abastece» en negro y «Hoje» en verde. Los vectores salen del logo original:

- `res/drawable/ic_wordmark.xml` y `ic_wordmark_dark.xml`: el logo completo para tema claro y
  oscuro (cabecera de Início y tour).
- `res/drawable/ic_logo_color.xml`: la gota a todo color (tour).
- `res/drawable/ic_logo.xml`: la gota de un solo color, para teñir (notificaciones).
- Icono adaptativo: `ic_launcher_foreground.xml` sobre fondo blanco, y
  `ic_launcher_monochrome.xml` para los iconos temáticos de Android 13.

### Movimiento

Pocas animaciones y con un porqué (`ui/Motion.kt`): las pantallas entran desde
la derecha y salen al volver, la ficha del posto sube desde abajo, las pestañas
se funden, la lista del mapa se despliega desde su cabecera y el precio se
desliza hacia abajo si baja y hacia arriba si sube. Con «Remover animações» del
sistema todo cambia al instante.

### Textos

Português de Portugal, de tú. Se dice **posto** (no «bomba» ni «estação»), y el
combustible lleva su artículo (`FuelType.withArticle`/`ofThe`: «a Gasolina 95»,
«do Gasóleo»). Los errores dicen qué pasó y cómo arreglarlo, con «Tentar de novo».

### Lista de pantallas

- **Tour de bienvenida**: la primera vez, 5 páginas (Abastece Hoje, mapa, preços y
  poupança, segunda-feira, favoritos y alertas) que terminan pidiendo la
  ubicación. Se repite desde Definições.
- **Início**: logotipo, alertas y Definições; buscador de postos por nombre,
  marca, localidad o morada; y 5 pestañas:
  - **Mapa**: filtros (combustível, raio, ordem), marcadores con el logotipo y
    el precio de cada posto, capas (normal/relieve), centrar y «Mais baratos
    perto de ti».
  - **Preços**: zona y radio, casillas por combustible con la media de la
    zona y la de Portugal, postos más baratos (estrella = favorito) y Calculadora.
  - **Notícias**: veredicto del lunes y noticias de Google Notícias por
    categoría (Preços, Mercado, Dicas, Mobilidade).
  - **Favoritos**: tus postos con su precio e interruptor de Alertas.
  - **Histórico**: evolución del precio medio de tu zona (7 días a 1 año) con
    gráfico, variación, mínimo y máximo; y «Comparar» todos los combustibles.
- **Detalhes do posto**: logotipo, morada, distancia y tiempo, «Como chegar»,
  precios de todos sus combustibles, tendencia e información; compartir y favorito.
- **Calculadora de poupança**: litros, combustible y lo que pagas en cada
  posto; cuánto ahorras frente al más caro.
- **Alertas de preço**: combustible, precio objetivo y todos los postos de la
  zona o solo los favoritos (se revisa cada 6 h).
- **Postos por distrito**: busca postos por distrito y guárdalos en favoritos.
- **Definições**: o teu combustível (el que se ve por defecto en toda la app),
  aparência (automático, claro, escuro), alertas, calculadora y la
  presentación.

«O meu veículo» (abastecimientos, consumo, gráficos y mantenimiento) se quitó
en la versión 4.0. La base de datos pasa a la versión 5: borra esas tablas y el
combustible del vehículo que estaba elegido pasa a ser el combustible habitual.

## Descargar el APK

Cada push compila la app con GitHub Actions. En la pestaña **Actions**, abre
la última ejecución de **Android** y descarga el artefacto `atesta-apk`.
Los APK se firman con una clave de depuración fija, así que las versiones
nuevas se instalan encima de la anterior.

Fuentes de datos: DGEG (precoscombustiveis.dgeg.gov.pt) y © OpenStreetMap.
Tecnología: Kotlin, Jetpack Compose (Material 3), Room, WorkManager y osmdroid.
