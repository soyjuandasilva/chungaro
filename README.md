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
- Ajustes: litros o galones y símbolo de moneda.
- Todo se guarda en el teléfono, sin internet ni cuenta.

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
