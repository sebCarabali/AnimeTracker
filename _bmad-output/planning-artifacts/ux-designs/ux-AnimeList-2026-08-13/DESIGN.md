---
name: AnimeTracker
description: Dashboard personal "qué sigo viendo" sobre AniList + MAL-Sync. Editorial oscuro tipo streaming, Tailwind CSS sobre Thymeleaf server-rendered.
status: final
created: 2026-08-13
updated: 2026-08-13
colors:
  surface-base: '#FAFAF9'
  surface-raised: '#FFFFFF'
  surface-sunken: '#F0EFEC'
  ink-primary: '#15161A'
  ink-secondary: '#5B5E68'
  ink-disabled: '#A3A6AF'
  border-hairline: '#E4E3DF'
  accent: '#D8482F'
  accent-foreground: '#FFFFFF'
  info: '#3E6FB0'
  info-foreground: '#FFFFFF'
  success: '#3E7D4C'
  success-foreground: '#FFFFFF'
  warning: '#9A6B00'
  warning-foreground: '#FFFFFF'
  surface-base-dark: '#0B0C10'
  surface-raised-dark: '#16181D'
  surface-sunken-dark: '#07080A'
  ink-primary-dark: '#F2F1ED'
  ink-secondary-dark: '#9B9CA3'
  ink-disabled-dark: '#54565E'
  border-hairline-dark: '#26282F'
  accent-dark: '#FF6A47'
  accent-foreground-dark: '#160603'
  info-dark: '#7FA8DE'
  info-foreground-dark: '#0B1A2E'
  success-dark: '#6FBF82'
  success-foreground-dark: '#0F2415'
  warning-dark: '#E0AC3D'
  warning-foreground-dark: '#1F1400'
typography:
  display:
    fontFamily: 'Sora'
    fontSize: 32px
    fontWeight: '700'
    lineHeight: '1.15'
    letterSpacing: -0.01em
  display-sm:
    fontFamily: 'Sora'
    fontSize: 22px
    fontWeight: '600'
    lineHeight: '1.2'
  heading:
    fontFamily: 'Sora'
    fontSize: 18px
    fontWeight: '600'
    lineHeight: '1.3'
  body:
    fontFamily: 'Inter'
    fontSize: 15px
    fontWeight: '400'
    lineHeight: '1.6'
  body-sm:
    fontFamily: 'Inter'
    fontSize: 13px
    fontWeight: '400'
    lineHeight: '1.5'
  label-caps:
    fontFamily: 'Inter'
    fontSize: 11px
    fontWeight: '600'
    lineHeight: '1.4'
    letterSpacing: 0.08em
  numeric:
    fontFamily: 'Inter'
    fontSize: 15px
    fontWeight: '600'
    lineHeight: '1.4'
rounded:
  sm: 6px
  DEFAULT: 8px
  md: 10px
  lg: 14px
  full: 9999px
spacing:
  '1': 4px
  '2': 8px
  '3': 12px
  '4': 16px
  '5': 20px
  '6': 24px
  '8': 32px
  '10': 40px
  gutter: 20px
  card-gap: 16px
  section-gap: 40px
  margin-mobile: 16px
  margin-desktop: 48px
components:
  poster-card:
    background: '{colors.surface-raised-dark}'
    radius: '{rounded.md}'
    border: 'none'
    title-type: '{typography.heading}'
    meta-type: '{typography.body-sm}'
  status-badge:
    radius: '{rounded.full}'
    type: '{typography.label-caps}'
    viendo-background: '{colors.accent-dark}'
    viendo-foreground: '{colors.accent-foreground-dark}'
    completado-background: '{colors.success-dark}'
    completado-foreground: '{colors.success-foreground-dark}'
    planeado-background: '{colors.info-dark}'
    planeado-foreground: '{colors.info-foreground-dark}'
    repitiendo-background: '{colors.info-dark}'
    repitiendo-foreground: '{colors.info-foreground-dark}'
    abandonado-background: '{colors.ink-disabled-dark}'
    abandonado-foreground: '{colors.ink-primary-dark}'
  nav:
    background: '{colors.surface-base-dark}'
    border-bottom: '{colors.border-hairline-dark}'
    active-indicator: '{colors.accent-dark}'
    type: '{typography.body}'
  skeleton:
    background: '{colors.surface-sunken-dark}'
    shimmer: '{colors.surface-raised-dark}'
    radius: '{rounded.md}'
  button-primary:
    background: '{colors.accent-dark}'
    foreground: '{colors.accent-foreground-dark}'
    radius: '{rounded.DEFAULT}'
  button-secondary:
    background: 'transparent'
    foreground: '{colors.ink-primary-dark}'
    border: '{colors.border-hairline-dark}'
    radius: '{rounded.DEFAULT}'
  stale-banner:
    background: '{colors.warning-dark}'
    foreground: '{colors.warning-foreground-dark}'
    radius: '{rounded.sm}'
    opacity-background: 0.14
  trend-bar:
    fill-current-period: '{colors.accent-dark}'
    fill-past-period: '{colors.ink-disabled-dark}'
    fill-missing-period: 'transparent con borde punteado {colors.border-hairline-dark}'
  onboarding-banner-mal-sync:
    background: '{colors.surface-raised-dark}'
    border: '{colors.info-dark}'
    radius: '{rounded.sm}'
    accent-text: '{colors.info-dark}'
  theme-toggle:
    background: '{colors.surface-sunken-dark}'
    radius: '{rounded.full}'
    indicator: '{colors.accent-dark}'
---

## Brand & Style

AnimeTracker no compite en catálogo ni en detección — eso ya lo resuelven AniList y MAL-Sync. Compite en una sola cosa: mostrar de un vistazo "qué sigo viendo" con más autoridad visual que una lista genérica. El posicionamiento visual es **editorial oscuro tipo streaming**: pósters grandes como protagonistas, tipografía de peso fuerte para títulos, y una superficie oscura por defecto que no compite con las carátulas de los animes. No es una herramienta de productividad austera ni un diario cálido — es una pieza de "mi noche de series", con la confianza visual de un catálogo de streaming pero sin su fricción de navegación.

Un solo acento cromático (`{colors.accent-dark}`, un coral cálido) marca "esto es lo que sigue" (detalle de uso en § Colors). Todo lo demás vive en tonos neutros oscuros para que el acento no compita consigo mismo.

## Colors

- **`surface-base-dark` (`#0B0C10`)** es el lienzo por defecto — un negro con temperatura fría, no un gris plano, para que los pósters (que suelen tener colores saturados) se lean con contraste sin que el fondo "pelee" con ellos.
- **`surface-raised-dark` (`#16181D`)** separa tarjetas y contenedores del fondo mediante tono, no borde. Es la superficie de las tarjetas de anime, filas de estado, y paneles de Tendencias.
- **`accent-dark` (`#FF6A47`)** es el único color cromático protagonista. Se usa exclusivamente para: el episodio siguiente pendiente, el badge de estado *viendo*, la barra del período actual en Tendencias, y el botón primario de acción (login, reintentar sync). Nunca decorativo.
- **`info-dark` (`#7FA8DE`)** marca elementos informativos secundarios y no urgentes — enlaces, y como fondo de los badges de estado *planeado* / *repitiendo* (texto en `info-foreground-dark`, un azul-noche oscuro, para el contraste).
- **`success-dark` (`#6FBF82`)** exclusivo del badge de estado *completado* (texto en `success-foreground-dark`, un verde casi negro).
- **`warning-dark` (`#E0AC3D`)** exclusivo del banner de degradación (FR-9: "datos de [fecha]") — nunca se usa para otra cosa, para que su aparición sea inconfundible como señal de staleness.
- **`ink-disabled-dark` (`#54565E`)** representa explícitamente "sin dato" — se usa en la barra de un período sin Snapshots en Tendencias (FR-5: dato faltante, no cero falso) y en el estado *abandonado*.
- Modo claro (`surface-base`, `surface-raised`, etc.) existe como alternativa vía toggle, pero el producto se diseña oscuro-primero; el modo claro hereda la misma lógica de rol de color, no es una identidad visual separada.

Evitar: gradientes decorativos sobre pósters, más de un color de acento cromático, colorear texto de estado sin su badge correspondiente.

## Typography

**Sora** (peso fuerte) es la voz de "esto es lo que importa ahora" — títulos de anime en tarjetas, encabezados de sección (`Hoy`, `Tendencias`), y el número de próximo episodio. **Inter** es el contrapunto funcional — metadata, fechas, cuerpo de texto, badges de estado. `numeric` usa cifras tabulares para que los contadores de episodios y las cifras de Tendencias no salten de ancho al actualizarse.

`label-caps` (mayúsculas, tracking abierto) se reserva para etiquetas de estado y encabezados de columna — nunca para títulos de anime, que siempre van en su capitalización real.

## Layout & Spacing

Escala de 4px (`spacing.1`–`spacing.10`), alineada 1:1 con la escala por defecto de Tailwind para no introducir valores arbitrarios. `section-gap` (40px) separa bloques mayores (ej. el bloque "Hoy" del bloque de accesos rápidos); `card-gap` (16px) separa tarjetas dentro de una grilla.

Grilla de tarjetas responsive: 1 columna en mobile, 2–3 en tablet, 4+ en desktop — sin tabla ancha en ningún punto, porque el contenido primario (pósters) es visual, no tabular. Márgenes laterales `margin-mobile` (16px) / `margin-desktop` (48px).

## Elevation & Depth

Sin sombras como jerarquía visual — la separación de superficies es por tono (`surface-raised-dark` sobre `surface-base-dark`), consistente con la estética "streaming" donde las tarjetas de catálogo no flotan, se recortan por color. Única excepción: un `hover` sutil (leve aclarado de `surface-raised-dark`) en tarjetas interactivas para señalar que son clickeables.

## Shapes

`rounded.md` (10px) en tarjetas y pósters — suficiente para sentirse "app moderna" sin ablandar demasiado el borde recto de una carátula rectangular. `rounded.full` exclusivo de los badges de estado (forma de píldora, distingue "esto es una etiqueta" de "esto es contenido"). `rounded.DEFAULT` (8px) en botones e inputs.

## Components

- **Poster card** — Tarjeta de anime (usada en Hoy y en Vistas por Estado). Póster a la izquierda (o arriba en mobile), título en `heading`, próximo episodio pendiente en `numeric` con acento `{colors.accent-dark}` cuando corresponde a la vista Hoy, badge de estado en la esquina.
- **Status badge** — Píldora `label-caps`, fondo/texto siempre como par (nunca color de fondo sin su foreground definido): *viendo* = `accent-dark`/`accent-foreground-dark`, *completado* = `success-dark`/`success-foreground-dark`, *planeado* y *repitiendo* = `info-dark`/`info-foreground-dark`, *abandonado* = `ink-disabled-dark`/`ink-primary-dark`.
- **Nav** — Barra persistente (Hoy / Por Estado / Tendencias / Configurar MAL-Sync), fondo `surface-base-dark`, borde inferior `border-hairline-dark`. El ítem activo lleva un indicador de línea inferior en `accent-dark`; los inactivos en `body` sobre `ink-secondary-dark`.
- **Skeleton** — Placeholder de carga inicial: bloques en `surface-sunken-dark` con un tono de "shimmer" en `surface-raised-dark`, mismo `rounded.md` que el componente que reemplazan (tarjeta o barra).
- **Stale banner** — Franja superior de página (no modal, no bloquea), fondo `warning-dark` al 14% de opacidad sobre `surface-base-dark`, texto sólido. Aparece solo cuando FR-9 degrada; nunca decorativo.
- **Trend bar** — Barra por período en Tendencias: `accent-dark` para el período vigente, `ink-disabled-dark` para períodos pasados con dato, contorno punteado sin relleno para período sin Snapshot (dato faltante explícito).
- **Banner de onboarding de MAL-Sync** — Franja en `surface-raised-dark` con borde izquierdo `info-dark` (informativo, no urgente — a diferencia del Stale banner en `warning-dark`). Texto de acento en `info-dark`, enlace a "Configurar MAL-Sync".
- **Theme toggle** — Interruptor tipo píldora (`rounded.full`) en el header; indicador `accent-dark` marca el modo activo. Un solo control, sin tercer estado.
- **Button primary/secondary** — Primary: fill `accent-dark`. Secondary: transparente con borde `border-hairline-dark`, usado en acciones no urgentes (ej. "Ver instrucciones de MAL-Sync").
- **Empty state** — Ilustración tipográfica simple (sin icono decorativo), `display-sm` + una línea de `body`, sin botón si no hay acción posible (ver EXPERIENCE.md § State Patterns).

## Do's and Don'ts

| Do | Don't |
|---|---|
| Un solo acento cromático (`accent-dark`) para "esto es lo activo/próximo" | Usar el acento para chrome, navegación o decoración |
| Separar superficies por tono oscuro, no por sombra | Agregar sombras como jerarquía visual |
| Representar "sin dato" con `ink-disabled-dark` / contorno punteado | Mostrar cero falso donde falta un Snapshot (contradice FR-5) |
| `warning-dark` exclusivo del banner de staleness (FR-9) | Reusar el color de warning para otro estado o badge |
| Pósters como protagonistas visuales de cada tarjeta | Tablas anchas de datos tabulares como layout primario |
