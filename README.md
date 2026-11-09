# Chungaro

App Android para llevar el control del combustible: cuánto cargas, cuánto pagas
y cuánto rinde tu vehículo.

## Qué hace

- Registra cada abastecimiento: fecha, kilometraje, cantidad, precio y total
  (si escribes cantidad y precio, el total se calcula solo, y viceversa).
- Calcula el **rendimiento** (km por litro o galón) y el **costo por km**
  usando el método de tanque lleno: las cargas parciales se suman al siguiente
  llenado completo.
- Muestra lo **gastado este mes**, el gasto total, el combustible total y el
  precio promedio.
- **Precios del día en Portugal**: pestaña «Precios» con los precios oficiales
  de cada gasolinera según la DGEG (precoscombustiveis.dgeg.gov.pt). Eliges
  distrito, buscas la gasolinera y la marcas como favorita; sus precios se
  actualizan al abrir la app y aparecen como atajos al registrar una carga.
- **Cerca de ti** (pantalla principal): con tu ubicación muestra la gasolinera
  más barata dentro de 5, 10 o 20 km y la más cercana, cuánto más cara es por
  litro y en un depósito de 50 L, y el top 5 de la zona. Botón «Cómo llegar».
- Ajustes: litros o galones y símbolo de moneda (por defecto L y €).
- Tus cargas se guardan en el teléfono, sin cuenta. Solo la pestaña de precios
  usa internet.

## Descargar el APK

Cada push compila la app con GitHub Actions. En la pestaña **Actions** del
repositorio, abre la última ejecución de **Android** y descarga el artefacto
`chungaro-apk`. Descomprime el zip e instala `app-debug.apk` en el teléfono
(hay que permitir la instalación de apps de origen desconocido).

## Compilar localmente

Con Android Studio, abre la carpeta del proyecto, o desde la terminal:

```
./gradlew assembleDebug
```

Tecnología: Kotlin, Jetpack Compose (Material 3) y Room.
