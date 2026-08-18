---
title: AnimeTracker
status: final
created: 2026-08-13
updated: 2026-08-13
---

# PRD: AnimeTracker
*Working title — confirm.*

## 0. Propósito del Documento

Este PRD define el alcance de AnimeTracker V1 para quien lo construye (uso personal/invitado) y sirve de insumo directo para el diseño técnico posterior (`bmad-architecture`) y el desglose en épicas/historias (`bmad-create-epics-and-stories`). Se apoya en el [Product Brief de AnimeTracker](../../briefs/brief-AnimeList-2026-08-13/brief.md), que ya fijó el problema, la solución y las restricciones técnicas — este documento no lo duplica, lo convierte en requisitos funcionales verificables. Vocabulario anclado en el Glosario (§3); features agrupadas con FRs anidados y numerados globalmente; supuestos marcados inline con `[ASSUMPTION]` e indexados en §11.

## 1. Visión

AnimeTracker responde una sola pregunta rápido: *"¿qué estoy mirando ahora y qué episodio sigue?"*. No es un tracker de anime desde cero — se apoya en dos piezas que ya existen y funcionan bien: **MAL-Sync**, que detecta la reproducción en JKAnime y AnimeFLV y escribe el progreso en AniList, y **AniList**, que actúa como catálogo y fuente de verdad del progreso vía su API GraphQL. AnimeTracker es la capa de visualización sobre esos datos: un dashboard "Hoy / Seguí Viendo" más directo que la lista genérica de AniList, vistas por estado, y tendencias históricas (episodios vistos por semana) — algo que AniList no ofrece de forma nativa, porque solo expone el estado actual, no una serie de tiempo.

Es deliberadamente chico: sin escritura hacia AniList, sin detección de reproducción propia, sin catálogo propio. Todo el valor está en presentar mejor un dato que ya existe. No hay una ventaja técnica defendible frente a AniList o MAL-Sync — ambos son más maduros en su terreno — y la diferenciación de AnimeTracker es deliberadamente de experiencia (un dashboard de "qué sigue" y tendencias en el tiempo), no de datos ni de detección (razón completa en `addendum.md`). Es un proyecto de uso invitado (no público), pensado para escalar de forma modesta — unos 100 usuarios en el primer año, con un techo de referencia de 1000.

## 2. Usuario Objetivo

### 2.1 Jobs To Be Done

- Como usuario que mira anime en JKAnime/AnimeFLV y trackea en AniList, quiero abrir una sola vista y saber inmediatamente qué sigue viendo — sin escanear una lista completa de catálogo.
- Quiero ver mi actividad reciente en el tiempo (episodios/semana) para notar rachas o caídas, algo que AniList no me muestra.
- Quiero que esto funcione sin que tenga que mantener nada — mi progreso ya se actualiza solo vía MAL-Sync, AnimeTracker solo lo tiene que reflejar.

### 2.2 No-Usuarios (V1)

- Personas sin cuenta de AniList, o que trackean en MyAnimeList/Kitsu/Simkl sin usar AniList — fuera de alcance V1.
- Personas que miran anime fuera de JKAnime/AnimeFLV (MAL-Sync no las cubre) — no van a tener detección automática de progreso.
- Público general / autoregistro abierto — V1 es estrictamente por invitación.

### 2.3 Key User Journeys

*Alcance liviano — proyecto personal/invitado de un solo rol de usuario, sin flujos multi-stakeholder ni multi-dispositivo complejos.*

- **UJ-1. Mica revisa qué le toca ver hoy.** Mica, que sigue cuatro animes en simultáneo, abre AnimeTracker antes de cenar, entra directo al Dashboard "Hoy / Seguí Viendo" y ve sus series en curso con el próximo episodio pendiente marcado — sin tener que revisar cuál vio último en cada una.
- **UJ-2. Diego nota que bajó el ritmo.** Diego entra a la vista de Tendencias, ve que esta semana miró la mitad de episodios que la anterior, y decide si retoma o lo deja pasar — algo que AniList no le muestra directamente.
- **UJ-3. Una nueva usuaria invitada entra por primera vez.** El admin la agrega a la Whitelist de Invitación con su usuario de AniList; ella entra a AnimeTracker, autentica vía OAuth de AniList, y ve su lista ya reflejada en las vistas por estado apenas se corre la primera sincronización.

## 3. Glosario

- **AniList** — Plataforma de tracking de anime con API GraphQL pública y OAuth. Fuente de verdad del catálogo y del estado de progreso del usuario. AnimeTracker la consume, nunca escribe en ella.
- **MAL-Sync** — Extensión de navegador de terceros, instalada por cada usuario, que detecta reproducción en JKAnime y AnimeFLV y actualiza el progreso directamente en AniList. Dependencia externa no controlada por AnimeTracker.
- **Estado de seguimiento** — Clasificación que AniList asigna a cada entrada de la lista de un usuario: *viendo*, *planeado*, *completado*, *abandonado*, *repitiendo* (heredados literalmente de AniList, no redefinidos por AnimeTracker).
- **Snapshot** — Registro puntual del estado de progreso de un usuario, persistido por AnimeTracker en su propia base de datos en cada corrida de sincronización. Es la unidad base de las Tendencias Históricas.
- **Sincronización (Sync)** — Proceso que consulta la API de AniList por usuario, actualiza el estado mostrado en AnimeTracker y registra un Snapshot.
- **Whitelist de Invitación** — Lista de usuarios de AniList habilitados por un admin para acceder a AnimeTracker. Sin autoregistro público.
- **Dashboard "Hoy / Seguí Viendo"** — Vista principal de AnimeTracker: animes con progreso activo y el próximo episodio pendiente por título.

## 4. Features

### 4.1 Autenticación y Acceso

**Descripción:** AnimeTracker no tiene sistema propio de usuarios/contraseñas — se autentica exclusivamente contra AniList vía OAuth. El acceso está cerrado por Whitelist de Invitación: un admin agrega manualmente al usuario de AniList antes de que pueda entrar. `[ASSUMPTION: la gestión de la whitelist en V1 es manual (edición directa por el admin, sin panel de administración dedicado) — a confirmar, ver Open Question OQ-1]`.

**Requisitos Funcionales:**

#### FR-1: Login vía OAuth de AniList

Un usuario puede autenticarse en AnimeTracker usando el flujo OAuth de AniList. Realiza UJ-3.

**Consecuencias (testeables):**
- El login redirige al flujo OAuth oficial de AniList y no pide ni almacena contraseña propia.
- Un usuario autenticado con éxito recibe una sesión válida en AnimeTracker.

#### FR-2: Acceso restringido por Whitelist

Solo un usuario de AniList presente en la Whitelist de Invitación puede completar el login y acceder a las vistas de AnimeTracker. Realiza UJ-3.

**Consecuencias (testeables):**
- Un usuario que completa el OAuth de AniList pero no está en la Whitelist recibe un mensaje claro de acceso denegado, no un error genérico.
- Agregar un usuario a la Whitelist habilita su acceso sin requerir que se re-registre.

**Notas:** `[NOTE FOR PM]` Confirmar si V1 necesita una UI de administración para la Whitelist o si alcanza con edición manual (config/DB) dado el volumen esperado (~100 usuarios año 1). Ver OQ-1.

### 4.2 Dashboard "Hoy / Seguí Viendo"

**Descripción:** Vista principal post-login. Muestra los animes con progreso activo (estado *viendo*) y, para cada uno, el próximo episodio pendiente — la respuesta directa a "¿qué sigue?".

**Requisitos Funcionales:**

#### FR-3: Vista "Hoy" con próximo episodio pendiente

El usuario autenticado ve, al entrar, una lista de sus animes en estado *viendo* ordenada por actividad reciente, cada uno con el número de próximo episodio a ver. Realiza UJ-1.

**Consecuencias (testeables):**
- Cada entrada de la vista "Hoy" muestra: título, episodio visto más reciente, próximo episodio pendiente.
- Un anime sin progreso registrado en la sincronización más reciente no aparece en "Hoy" (solo estado *viendo*).
- Si el usuario no tiene ningún anime en *viendo*, la vista comunica ese estado vacío explícitamente (no una lista en blanco sin explicación).

### 4.3 Vistas por Estado

**Descripción:** Listas filtradas según el Estado de seguimiento que reporta AniList — *viendo*, *planeado*, *completado*, *abandonado*, *repitiendo*.

**Requisitos Funcionales:**

#### FR-4: Listas filtradas por estado

El usuario puede ver su lista de anime filtrada por cualquiera de los Estados de seguimiento estándar de AniList.

**Consecuencias (testeables):**
- Cada estado disponible en AniList tiene una vista filtrada correspondiente en AnimeTracker.
- El conteo de animes por estado en AnimeTracker coincide con el reportado por AniList al momento del último Sync.

### 4.4 Tendencias Históricas

**Descripción:** Vista de actividad en el tiempo — episodios vistos por semana/mes — construida a partir de los Snapshots que AnimeTracker guarda periódicamente. Es la vista que AniList no ofrece de forma nativa.

**Requisitos Funcionales:**

#### FR-5: Vista de episodios vistos por período

El usuario puede ver un gráfico o listado de episodios vistos agrupados por semana (y/o mes) a partir de los Snapshots históricos. Realiza UJ-2.

**Consecuencias (testeables):**
- El valor de "episodios vistos" de un período se calcula por diferencia entre Snapshots consecutivos dentro de ese período, no por conteo total acumulado.
- Un período sin Snapshots registrados se muestra como dato faltante, no como cero falso.

**Feature-specific NFRs:**
- La precisión de esta vista depende directamente de que la Sincronización (§4.5) corra sin huecos — ver SM-3.

### 4.5 Sincronización de Datos

**Descripción:** Proceso que mantiene AnimeTracker al día con AniList sin consultar en vivo en cada carga de página. Corre como job periódico y también al login del usuario `[ASSUMPTION: intervalo del job periódico entre 30 y 60 minutos — a confirmar en diseño técnico, ver OQ-2]`.

**Requisitos Funcionales:**

#### FR-6: Job periódico de sincronización

Un proceso de background consulta la API de AniList por cada usuario habilitado a intervalos regulares y actualiza su estado en AnimeTracker.

**Consecuencias (testeables):**
- Cada corrida del job actualiza el estado mostrado de todos los usuarios activos sin requerir que el usuario tenga sesión abierta.
- El job respeta los rate limits de la API de AniList: procesa usuarios con concurrencia limitada en vez de disparar una consulta por usuario en paralelo sin control de tasa `[ASSUMPTION: límite inicial de referencia, no más de 5 requests concurrentes a la API de AniList por corrida de job — valor exacto a validar contra los límites publicados por AniList en diseño técnico, ver OQ-2]`.

#### FR-7: Sincronización al login

Al iniciar sesión, AnimeTracker fuerza una sincronización del usuario que está entrando, además del job periódico.

**Consecuencias (testeables):**
- Los datos mostrados inmediatamente después de un login reflejan una consulta a AniList hecha en ese login (no un Snapshot con más de un ciclo de antigüedad, salvo degradación por FR-9).

#### FR-8: Snapshot histórico en cada sincronización

Cada corrida de sincronización (periódica o por login) persiste un Snapshot del estado del usuario en la base de datos propia de AnimeTracker.

**Consecuencias (testeables):**
- Cada Sync exitosa deja un registro de Snapshot asociado a usuario y timestamp.
- Los Snapshots persistidos son la única fuente de datos para las Tendencias Históricas (§4.4) — AnimeTracker no re-consulta AniList para calcular tendencias pasadas.

#### FR-9: Degradación clara ante falla de AniList

Si la API de AniList no responde o el usuario revocó el acceso OAuth, AnimeTracker muestra el último Snapshot conocido con su fecha, en vez de romper o mostrar un dato engañoso como actual.

**Consecuencias (testeables):**
- Ante una falla de sincronización, la UI indica explícitamente "datos de [fecha]" en vez de presentar el último Snapshot como si fuera en vivo.
- Una falla de sincronización para un usuario no bloquea el job para el resto de los usuarios.

### 4.6 Onboarding de MAL-Sync

**Descripción:** MAL-Sync es un requisito de setup del usuario, no algo que AnimeTracker resuelva (§9) — pero un usuario recién invitado necesita saber que existe y cómo configurarlo contra su cuenta de AniList antes de que el dashboard tenga algo que mostrar.

**Requisitos Funcionales:**

#### FR-10: Documentación de setup de MAL-Sync

AnimeTracker publica una página con instrucciones para instalar y configurar MAL-Sync contra AniList, accesible para un usuario recién invitado.

**Consecuencias (testeables):**
- Existe una página (dentro de la app o enlazada desde ella) con los pasos de instalación de MAL-Sync y su configuración contra AniList.
- La página es accesible sin depender de que la sincronización ya haya corrido (un usuario nuevo sin Snapshots todavía puede llegar a ella).

## 5. No-Objetivos (Explícito)

- No se construye una extensión de navegador propia — se usa MAL-Sync tal cual, sin modificarlo.
- No hay escritura/edición hacia AniList desde AnimeTracker (cambiar estado, calificar, notas) — eso se sigue haciendo en AniList o vía MAL-Sync.
- No hay soporte para otras plataformas de tracking (MyAnimeList, Kitsu, Simkl) — solo AniList.
- No hay soporte para sitios de streaming más allá de JKAnime/AnimeFLV — lo resuelve MAL-Sync, fuera del control de este proyecto.
- No hay comparación ni cruce de listas entre distintos usuarios de AnimeTracker.
- No hay aplicación móvil nativa — V1 es web.
- No hay registro público / autoregistro abierto.

### 5.1 Visión Post-V1 (condicionada, no comprometida)

Ninguno de estos ítems está comprometido para V1 — se listan porque el Brief los nombra explícitamente como caminos de crecimiento natural *si* el enfoque "solo lectura sobre AniList + MAL-Sync" prueba tener valor. A diferencia de §5, no son límites permanentes del producto, son candidatos a reconsiderar más adelante:

- Habilitar edición/escritura hacia AniList desde la misma app (hoy explícitamente fuera de alcance, §5).
- Sumar soporte a otras plataformas de tracking (MyAnimeList, Kitsu).
- Funciones sociales — comparar listas entre usuarios invitados.

## 6. Alcance V1 (MVP)

### 6.1 Dentro de Alcance

- FR-1 a FR-10 (autenticación por invitación, dashboard "Hoy / Seguí Viendo", vistas por estado, tendencias históricas, sincronización con snapshots y degradación, onboarding de MAL-Sync).

### 6.2 Fuera de Alcance para MVP

- Todo lo listado en §5 No-Objetivos.
- `[NOTE FOR PM]` UI de administración de Whitelist — deferred si la gestión manual (OQ-1) resulta suficiente para el volumen esperado; revisar si el ritmo de invitaciones lo justifica.
- Detección de "racha" o alerta proactiva de caída de actividad — mencionada como dolor en el Brief pero no comprometida como FR en V1; la vista de Tendencias (FR-5) expone el dato, no lo interpreta activamente. Candidato natural para v2 si Tendencias prueba tener valor.

## 7. Métricas de Éxito

*Mantenidas del Brief tal cual, como borrador inicial — no se definieron targets numéricos más duros en esta conversación.*

**Primarias**
- **SM-1**: Adopción — alcanzar el rango de ~100 usuarios invitados activos durante el primer año. Valida FR-1, FR-2.
- **SM-2**: Uso real del feature ancla — los usuarios activos vuelven al Dashboard "Hoy / Seguí Viendo" con cierta regularidad — ilustrativamente, varias veces por semana — en vez de usar AniList directamente para esa consulta. Target exacto sin confirmar, ver OQ-5. Valida FR-3.

**Secundarias**
- **SM-3**: Confiabilidad de sincronización — los Snapshots históricos se generan de forma consistente, sin huecos, para que la vista de Tendencias sea confiable. Valida FR-6, FR-7, FR-8.

**Contra-métricas (no optimizar)**
- **SM-C1**: Frecuencia del job de sincronización — no se debe maximizar la frecuencia de Sync para inflar SM-3 a costa de acercarse a los rate limits de la API de AniList. El objetivo es frescura suficiente para el uso diario, no tiempo real. Contrapesa SM-3.

## 8. NFRs Transversales

- **Escala:** diseño para ~100 usuarios activos el primer año, con margen hasta 1000 — no requiere arquitectura de escala masiva, pero el Dashboard nunca debe disparar una consulta en vivo a AniList por carga de página (ver FR-6, SM-C1).
- **Staleness aceptable:** al ser solo-lectura con Snapshots periódicos, el dashboard no es tiempo real estricto. `[ASSUMPTION: staleness máxima aceptable ~30-60 min fuera de un login activo — a confirmar en diseño técnico, OQ-2]`.
- **Seguridad de sesión/token:** el token OAuth de AniList se maneja server-side (consistente con el frontend server-rendered elegido, §9). `[ASSUMPTION: sin requisitos de seguridad adicionales más allá de manejo estándar de sesión server-side — a revisar en diseño técnico si se detectan datos sensibles adicionales]`.
- **Retención de datos:** `[ASSUMPTION: los Snapshots se retienen indefinidamente en V1 dado el volumen bajo de datos esperado (~100-1000 usuarios) — sin política de purga definida, ver OQ-3]`.

## 9. Integraciones y Dependencias

- **AniList API (GraphQL, OAuth)** — fuente de verdad de catálogo y progreso. AnimeTracker consume, nunca escribe. Sujeta a rate limits; con la escala proyectada es necesario cachear y limitar frecuencia de consulta (FR-6, SM-C1) en vez de pedir en vivo en cada carga.
- **MAL-Sync** — dependencia externa no controlada por este proyecto. Si un usuario no la instala o no la configura contra AniList, AnimeTracker no tiene forma de detectar reproducción; es un requisito de setup del usuario (cubierto por la documentación en §6.1), no algo que la app resuelva o valide.
- **Stack de backend** — Java Spring Boot, restricción de stack fijada en el Brief. El frontend server-rendered (Thymeleaf) en el mismo monolito se decidió durante este PRD (el Brief no especificaba frontend) — no es una restricción heredada, pero se trata como decisión cerrada para V1. Base de datos propia: no es fuente de verdad de catálogo/progreso (eso es AniList), su rol es cache de lectura y almacenamiento de Snapshots históricos.
- **Riesgo de continuidad:** el producto entero depende de que AniList y MAL-Sync sigan existiendo y manteniéndose. Limitación aceptada conscientemente, no un descuido (ver Brief, "Qué lo hace diferente").

## 10. Preguntas Abiertas

1. **OQ-1 (Whitelist):** ¿Alcanza con gestión manual de la Whitelist de Invitación (config/DB directa) para V1, o hace falta una UI mínima de administración desde el arranque? Afecta el alcance de §4.1.
2. **OQ-2 (Frecuencia de sync):** ¿Cuál es el intervalo exacto del job periódico (30 min, 60 min, otro) que balancea frescura vs. rate limits de AniList? A resolver en diseño técnico.
3. **OQ-3 (Retención de Snapshots):** ¿Se retienen los Snapshots indefinidamente o se define una política de purga/agregación a futuro (por ejemplo, agregar a resumen semanal pasado cierto tiempo)?
4. **OQ-4 (Hosting):** ¿Dónde se despliega el backend Spring Boot (VPS propio, cloud gestionado, etc.)? Pendiente para diseño técnico/arquitectura.
5. **OQ-5 (Target de SM-2):** ¿Cuál es la frecuencia de retorno al Dashboard "Hoy / Seguí Viendo" que cuenta como éxito (el Brief sugiere "varias veces por semana" a título ilustrativo, sin confirmar como target)?

## 11. Índice de Supuestos

- §4.1 — Gestión de Whitelist manual (sin panel admin) en V1. Ver OQ-1.
- §4.5 — Intervalo del job periódico de sincronización entre 30 y 60 minutos. Ver OQ-2.
- §4.5 (FR-6) — Límite de referencia de no más de 5 requests concurrentes a la API de AniList por corrida de job. Ver OQ-2.
- §8 — Staleness máxima aceptable ~30-60 min fuera de login activo.
- §8 — Sin requisitos de seguridad adicionales más allá de sesión server-side estándar para el token OAuth.
- §8 — Retención indefinida de Snapshots en V1, sin política de purga. Ver OQ-3.
