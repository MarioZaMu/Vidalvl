# Vidalvl

App Android de hábitos con estilo RPG. Cada misión que completas da experiencia a uno de cuatro atributos
(**Cuerpo, Mente, Disciplina y Social**) y hace crecer a tu avatar en pixel art. Está pensada para medir
**constancia**, no esfuerzo: nunca se pierde experiencia ni nivel por faltar un día.

Proyecto del curso *Diseño de Interfaces de Software*. Es un proyecto de front end: toda la app corre en el
teléfono y no tiene servidor propio.

## Qué hace

- **Misiones diarias y semanales** (las semanas van de lunes a domingo), con tarjetas por atributo, secciones de
  activas y completadas, racha y mensajes de ánimo que nunca culpan.
- **Crear, editar y borrar misiones propias.** La experiencia la fija el tipo de misión (8 XP la diaria, 15 la
  semanal), no el usuario.
- **Avatar en pixel art de 32×32** dibujado por capas en un `Canvas` (género, tono de piel, ojos, peinado, color
  de pelo, ropa, sombreros, lentes y capas) y con una respiración suave.
- **Recompensas e inventario:** 62 ítems repartidos por nivel, por misiones semanales y por logros.
- **Logros y pestaña "Reflejo":** tu semana de un vistazo, línea de tiempo de todo lo que has hecho y lista de logros.
- **Tareas sugeridas cada día y cada semana** a partir de lo que quieres lograr, con Gemini o con ideas guardadas
  en la app si no hay internet o prefieres no usar IA.
- **Verificación de misiones con foto** (CameraX + Gemini). La foto no se guarda y nunca bloquea al usuario:
  tras dos intentos fallidos se puede completar sin verificar.
- **Español e inglés**, modo oscuro "fantasía oscura" y modo claro "pergamino", contraste de texto comprobado con
  pruebas y diseños revisados con la letra del sistema al 200 %.

## Tecnología

Kotlin 2.2, Jetpack Compose (Material 3), Room 2.8, CameraX 1.6, Retrofit 3, navegación con `navigation-compose`.
Arquitectura MVVM con `AndroidViewModel` y conexión manual de dependencias (sin Hilt). `minSdk` 26, `compileSdk` 37.

El código está en tres capas dentro de `app/src/main/java/com/vidalvl/app/`:

| Carpeta | Contenido |
|---|---|
| `domain/` | Reglas del juego puras (niveles, racha, recompensas, logros, sugerencias). Sin dependencias de Android, por eso se prueban fácil. |
| `data/` | Room (base de datos y migraciones 1 a 8), almacenamiento local y cliente de Gemini. |
| `ui/` | Pantallas, componentes, tema (colores, tipografía) y el dibujo del avatar. |

## Cómo ejecutarla

1. Abre la carpeta con **Android Studio** y deja que Gradle sincronice.
2. Conecta un teléfono con depuración USB o abre un emulador, y pulsa **Run**.

### Clave de Gemini (opcional)

La app funciona sin clave: las tareas sugeridas usan ideas guardadas y la verificación con foto avisa de que no
está configurada. Para activar la IA, crea una clave en Google AI Studio y agrégala a `local.properties`, en la
raíz del proyecto:

```properties
GEMINI_API_KEY=tu_clave
# Opcional: cambia el modelo si el predeterminado ya no existe
# GEMINI_MODEL=gemini-3.5-flash-lite
```

`local.properties` está en `.gitignore`: **no subas tu clave**. Ten presente que una clave dentro de un APK se
puede extraer, así que no distribuyas un APK con tu clave real.

## Pruebas

```bash
./gradlew testDebugUnitTest
```

Son pruebas unitarias de las reglas del juego, del protocolo con Gemini, del límite diario de verificaciones y del
contraste de los colores. Las migraciones de la base de datos se probaron instalando sobre una base real.

## Documentos

- [`docs/pruebas-usabilidad-vidalvl.docx`](docs/pruebas-usabilidad-vidalvl.docx): guion, tareas, hojas de
  observación, cuestionario SUS y plantilla de resultados para las pruebas de usabilidad.
- [`CLAUDE.md`](CLAUDE.md): reglas del proyecto, decisiones de diseño y avance por fases.

## Créditos

- Tipografía **Cinzel**, con licencia SIL Open Font License (`third_party/fonts/Cinzel-OFL.txt`).
- Los sprites del avatar están en `app/src/main/res/drawable-nodpi/`. Revisa las condiciones de uso de su autor
  antes de reutilizarlos en otro proyecto.

Autor: Mario Alberto Zambrano Muñoz.
