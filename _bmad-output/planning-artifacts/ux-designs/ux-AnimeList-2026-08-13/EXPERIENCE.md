---
name: AnimeTracker — Experience Spine
status: final
created: 2026-08-13
updated: 2026-08-13
sources:
  - ../../prds/prd-AnimeList-2026-08-13/prd.md
  - ../../briefs/brief-AnimeList-2026-08-13/brief.md
---

# AnimeTracker — Experience Spine

## Foundation

Web responsive de una sola superficie por sesión (no multi-tenant, no multi-dispositivo simultáneo). Backend Spring Boot con frontend server-rendered (Thymeleaf) + Tailwind CSS — sin SPA ni framework JS `[ASSUMPTION: solo JS progresivo mínimo (toggle de tema, quizás gráfico de Tendencias) — coherente con el alcance "deliberadamente chico" del PRD §1; a confirmar en diseño técnico]`. `DESIGN.md` es la referencia de identidad visual; este documento es la experiencia.

Un solo rol de usuario autenticado (usuario de AniList invitado); no hay rol admin con UI propia — la Whitelist se gestiona manualmente fuera de la app (PRD §4.1, OQ-1). AnimeTracker es **estrictamente de solo lectura**: no existe ningún formulario de edición de datos de AniList en ninguna superficie — la única escritura de la app es de sesión (login/logout) y de preferencia de tema.

Modo oscuro por defecto con toggle a claro (decisión de esta sesión); la preferencia persiste por usuario `[ASSUMPTION: persistencia server-side junto al resto de la sesión, no solo localStorage, para que el toggle sobreviva entre dispositivos — a confirmar en diseño técnico]`.

## Information Architecture

| Surface | Reached from | Purpose |
|---|---|---|
| Login | Raíz de la app sin sesión | Inicia el flujo OAuth de AniList (FR-1) |
| Acceso denegado | Tras completar OAuth si no está en Whitelist | Mensaje explícito de acceso denegado, no error genérico (FR-2) |
| Hoy / Seguí Viendo | Post-login (default) / nav principal | Animes en *viendo* con próximo episodio pendiente (FR-3) |
| Por Estado | Nav principal | Listas filtradas por cada Estado de seguimiento de AniList (FR-4) |
| Tendencias | Nav principal | Episodios vistos por semana/mes desde Snapshots (FR-5) |
| Configurar MAL-Sync | Nav principal + banner de onboarding en Hoy/Por Estado/Tendencias si no hay Snapshots aún | Instrucciones de instalación/config de MAL-Sync (FR-10) |

Nav principal persistente (Hoy / Por Estado / Tendencias / Configurar MAL-Sync) — sin jerarquía anidada, las cuatro superficies están al mismo nivel porque no hay flujos multi-paso (PRD §2.3: "alcance liviano, sin flujos multi-stakeholder"). Sin modales de edición porque no hay nada que editar; los únicos overlays son el banner de staleness (no bloqueante) y el estado vacío de onboarding.

→ Referencia de composición: [`mockups/hoy.html`](mockups/hoy.html) (dashboard + variantes de staleness/vacío), [`mockups/por-estado.html`](mockups/por-estado.html) (grilla filtrada + los cuatro badges no-*viendo*), [`mockups/tendencias.html`](mockups/tendencias.html) (barras semanales + período sin Snapshot), [`mockups/login.html`](mockups/login.html) (Login + Acceso denegado), [`mockups/mal-sync-onboarding.html`](mockups/mal-sync-onboarding.html) (banner de onboarding + página de setup). El spine gana en caso de conflicto con cualquier mock.

## Voice and Tone

Voz de marca y postura estética en `DESIGN.md.Brand & Style`. Aquí solo el microcopy.

| Do | Don't |
|---|---|
| "Nada en *viendo* ahora mismo." | "¡Todavía no estás viendo nada! 🎬" |
| "Datos del 12/08, 14:30 — no pudimos sincronizar." | "Error de sincronización" |
| "No hay datos para esta semana." | "0 episodios" (cuando en realidad falta el Snapshot) |
| "Tu cuenta de AniList no está habilitada todavía." | "Acceso denegado" a secas |
| Frases cortas, sin signos de exclamación, sin emojis | Tono motivacional o de gamificación ("¡Racha de 5 días!") |

El tono es el de un panel de datos personal, no el de una app de streaming que busca retener — directo, sin urgencia artificial, coherente con que AnimeTracker no interpreta ni presiona (PRD §6.2: la detección de "racha" queda deliberadamente fuera de V1).

## Component Patterns

Comportamiento. Specs visuales en `DESIGN.md.Components`.

| Component | Use | Behavioral rules |
|---|---|---|
| Poster card | Hoy, Por Estado | Click en cualquier parte de la tarjeta no navega a ningún lado propio de AnimeTracker (no hay detalle de anime) — es informativa, no un link `[ASSUMPTION: sin página de detalle de anime en V1, dado que el PRD no define una — a confirmar]`. |
| Status badge | Poster card, filtros de Por Estado | Un badge por tarjeta, mapeado 1:1 al Estado de seguimiento de AniList (§4.3) — nunca un estado inventado por AnimeTracker. |
| Nav | Global, todas las superficies autenticadas | Persistente, no colapsa a menos scroll (sin auto-hide). Ítem activo resaltado (ver `DESIGN.md.Components.nav`); click navega sin confirmación, ningún ítem requiere estado de carga propio. |
| Button primary/secondary | Login (primary), reintentar sync tras degradación (primary), "Ver instrucciones de MAL-Sync" (secondary) | Primary sin estado disabled en V1 (no hay validación de formulario que lo justifique); en reintento de sync, se deshabilita mientras la petición está en curso y vuelve a habilitarse al resolver. Secondary siempre navega, nunca dispara una acción destructiva. |
| Skeleton | Cualquier superficie, carga inicial post-login | Reemplaza el layout final pieza por pieza (misma grilla de Poster cards o de Trend bars); se resuelve al llegar el dato, sin spinner de página completa. |
| Stale banner | Global (aparece en cualquier superficie tras login si FR-9 degrada) | No bloqueante, no dismissible manualmente — desaparece solo cuando una sincronización exitosa refresca el dato. |
| Trend bar | Tendencias | Por período (semana, con toggle a mes — ver § Interaction Primitives). Un período sin Snapshot se renderiza con contorno punteado, nunca como barra en cero (FR-5). |
| Banner de onboarding de MAL-Sync | Global mientras el usuario no tenga ningún Snapshot registrado (Hoy, Por Estado, Tendencias) | Reemplaza el estado vacío genérico de cada superficie mientras aplica — la causa real es "nunca sincronizó", no "vacío por elección" (ver § State Patterns). Persiste hasta la primera sincronización exitosa; enlaza a "Configurar MAL-Sync". |
| Empty state | Hoy, Por Estado (ver detalle de copy en § State Patterns) | Sin botón de acción cuando no hay nada que el usuario pueda hacer desde AnimeTracker (es solo-lectura) — el mensaje explica la ausencia de datos, no invita a una acción inexistente. |
| Theme toggle | Header, todas las superficies autenticadas | Un solo control, oscuro ⇄ claro, sin tercer estado "sistema" en V1 `[ASSUMPTION]`. |

## State Patterns

| State | Surface | Treatment |
|---|---|---|
| Hoy vacío (sin animes en *viendo*, pero con Snapshots históricos) | Hoy | Mensaje explícito ("Nada en *viendo* ahora mismo.") — nunca lista en blanco sin explicación (FR-3, consecuencia testeable). |
| Sin Snapshots todavía (usuario recién invitado, cero syncs desde siempre) | Global — Hoy, Por Estado, Tendencias | Ver § Component Patterns → Banner de onboarding de MAL-Sync. Precede a los estados "vacío" de abajo: mientras nunca hubo un Snapshot, éste gana (FR-10, UJ-3). |
| Estado filtrado vacío (con Snapshots históricos, pero ninguno en ese estado) | Por Estado | Mensaje por estado ("Nada en *planeado* por ahora.") — mismo patrón que Hoy vacío. |
| Falla de OAuth previa al login (AniList caída o el usuario cancela el consentimiento) | Login | Mensaje distinto de Acceso denegado ("No pudimos completar el login con AniList — inténtalo de nuevo.") con botón para reintentar; no es un rechazo de Whitelist, es una falla transitoria del flujo (FR-1). |
| Degradación de sync (AniList caída o token revocado, ya autenticado) | Global | Stale banner con fecha del último Snapshot conocido — nunca se presenta el dato viejo como si fuera actual (FR-9). |
| Acceso denegado (no en Whitelist) | Post-OAuth | Página dedicada, mensaje explícito, sin reintento automático — no es un error transitorio (FR-2). |
| Período sin Snapshot en Tendencias | Tendencias | Barra con contorno punteado + tooltip/label "sin datos", nunca cero (FR-5). |
| Carga inicial (primer render post-login) | Cualquiera | Skeleton de tarjetas/barras que anticipa el layout final `[ASSUMPTION: patrón estándar de skeleton, sin spinner de página completa]`. |

## Interaction Primitives

AnimeTracker es de solo lectura frente a AniList — no hay drag, no hay edición inline, no hay formularios más allá de login y toggle de tema. La superficie de interacción es deliberadamente chica.

- Click/tap para navegar entre las cuatro superficies de la nav principal.
- Click/tap en el toggle de tema — cambio inmediato, sin confirmación.
- Click/tap en el toggle semana/mes de Tendencias — recalcula las Trend bar en el lugar, sin navegar ni recargar la página.
- Click en "Configurar MAL-Sync" — navega a instrucciones, no abre modal (es contenido largo, no una acción rápida).
- Sin búsqueda ni filtros adicionales dentro de Por Estado más allá de la selección de estado — el volumen de datos por usuario es bajo (listas personales, no catálogo completo), no justifica un buscador `[ASSUMPTION]`.
- **Prohibido:** cualquier control de escritura hacia datos de AniList (cambiar estado, puntuar, marcar episodio) — eso vive en AniList/MAL-Sync por diseño (PRD §5, No-Objetivos). Prohibido también scroll infinito (listas cortas no lo ameritan) y cualquier notificación push/re-engagement (fuera de alcance y contrario al tono de la app).

## Accessibility Floor

Comportamiento. Contraste visual vive en `DESIGN.md`.

- WCAG 2.2 AA en ambos modos de color (oscuro por defecto y claro) para toda combinación fondo/texto cargada de significado: botones, y los cuatro pares fill/foreground de Status badge (`accent-dark`/`accent-foreground-dark`, `success-dark`/`success-foreground-dark`, `info-dark`/`info-foreground-dark`, `ink-disabled-dark`/`ink-primary-dark` — ver `DESIGN.md.components.status-badge`) deben verificarse contra AA cada uno, no solo el par de *viendo*.
- Cada Status badge lleva texto accesible además de color (el nombre del estado en `label-caps`, nunca solo un punto de color) — no depender del color como único portador de significado.
- El Stale banner se anuncia vía `aria-live="polite"` al aparecer, para que un lector de pantalla lo capture sin interrumpir la navegación en curso.
- Foco de teclado visible en nav principal, toggle de tema, y cualquier link — orden de tabulación sigue el orden de lectura en cada superficie.
- Gráfico de Tendencias: cada barra expone su valor (o "sin dato") como texto accesible, no solo como altura visual — un usuario de lector de pantalla obtiene la misma información que uno vidente.

## Responsive & Platform

| Breakpoint | Behavior |
|---|---|
| `≥ lg` (1024px+) | Nav principal como barra superior horizontal. Grilla de tarjetas 4+ columnas en Hoy/Por Estado. |
| `md` (768–1023px) | Grilla de tarjetas 2–3 columnas. Nav horizontal se mantiene. |
| `< md` (`sm`) | Grilla de 1 columna. Nav colapsa a barra inferior o menú hamburguesa `[ASSUMPTION: patrón exacto de colapso a definir en mocks — cualquiera de los dos cumple el requisito de nav accesible en mobile]`. |

AnimeTracker es web responsive, no una app nativa (PRD §5, No-Objetivos) — pero dado que el uso ilustrado en UJ-1 ("antes de cenar") sugiere chequeos rápidos, el layout mobile no es un caso secundario: se diseña con el mismo cuidado que desktop, no como una versión reducida.

## Inspiration & Anti-patterns

- **Rechazado — Gamificación / rachas ("streak"):** el PRD (§6.2) deja explícitamente fuera de V1 la detección o alerta proactiva de racha/caída de actividad — Tendencias expone el dato, no lo interpreta ni lo premia. Ningún badge de "5 semanas seguidas" ni notificación de "no viste nada esta semana".
- **Rechazado — Cualquier control de escritura hacia AniList:** por diseño (PRD §5), aunque técnicamente fuera trivial agregar un botón "marcar como visto" en la Poster card, eso rompe la frontera solo-lectura que es la base de la arquitectura de datos del producto.
- **Rechazado — Catálogo de descubrimiento (explorar animes nuevos):** AnimeTracker no es un catálogo, es una capa sobre la lista que el usuario ya tiene en AniList — no hay superficie de "buscar/descubrir series nuevas".

## Key Flows

### Flow 1 — Mica revisa qué le toca ver hoy (UJ-1)

1. Mica, que sigue cuatro animes en simultáneo, abre AnimeTracker antes de cenar `[ASSUMPTION: desde el navegador de su teléfono — chequeo rápido pre-cena, layout mobile de 1 columna]`.
2. La sesión ya está activa (login previo); entra directo a Hoy / Seguí Viendo.
3. Ve sus cuatro animes en *viendo* como Poster cards, cada una con el episodio visto más reciente y el próximo pendiente destacado en `{typography.numeric}` con el acento `{colors.accent-dark}`.
4. **Climax:** no tiene que abrir ninguna tarjeta ni navegar a Por Estado — las cuatro respuestas a "¿qué sigo viendo?" están en una sola pantalla, ordenadas por actividad reciente, sin que ella tuviera que recordar cuál vio último en cada serie.

Fallback: si el Sync más reciente falló, el Stale banner muestra la fecha del último dato conocido arriba de las mismas tarjetas — Mica igual ve su lista, con la fecha visible.

### Flow 2 — Diego nota que bajó el ritmo (UJ-2)

1. Diego entra a AnimeTracker y navega a Tendencias desde la nav principal `[ASSUMPTION: sesión de escritorio — revisión más analítica que el chequeo rápido de Mica]`.
2. Ve las barras semanales: la semana actual (`{colors.accent-dark}`) muestra la mitad de episodios que la semana anterior (`{colors.ink-disabled-dark}`).
3. No hay interpretación automática ("bajaste el ritmo") — solo el dato, coherente con que AnimeTracker expone, no interpreta (Inspiration & Anti-patterns).
4. **Climax:** Diego compara visualmente ambas barras en un vistazo, sin tener que calcular nada — algo que AniList no le muestra directamente — y decide él mismo si retoma o lo deja pasar.

Caso de borde: si alguna semana intermedia no tiene Snapshots (hueco de sincronización), esa barra aparece con contorno punteado — Diego distingue "no miré nada" de "no tenemos el dato", sin que el gráfico mienta por omisión.

### Flow 3 — Una nueva usuaria invitada entra por primera vez (UJ-3)

1. El admin agrega a Vale a la Whitelist de Invitación con su usuario de AniList (gestión manual, fuera de la app).
2. Vale entra a AnimeTracker, ve la pantalla de Login, y se autentica vía OAuth de AniList.
3. Post-login, llega a Hoy — pero como todavía no corrió ninguna sincronización con Snapshot registrado, ve el banner de onboarding de MAL-Sync en vez de sus animes.
4. Sigue el link a "Configurar MAL-Sync", lee las instrucciones (FR-10), instala y configura la extensión contra su cuenta de AniList.
5. **Climax:** en su próxima visita (tras el primer Sync exitoso), Hoy ya no muestra el banner de onboarding — muestra su lista real, reflejada automáticamente sin que Vale tuviera que hacer nada dentro de AnimeTracker más que autenticarse una vez.

Caso de borde: si Vale completa el OAuth pero el admin todavía no la agregó a la Whitelist, ve la pantalla de Acceso denegado con mensaje explícito, no un error genérico — y puede volver a intentar login apenas el admin la habilite.
