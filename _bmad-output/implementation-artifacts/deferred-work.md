# Deferred Work

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-iniciar-sesion-con-anilist-oauth.md`
  summary: Configurar el pipeline de build de Tailwind CSS 4.x (CLI standalone) y migrar los estilos de Login de CSS inline/embebido a clases Tailwind con tokens compartidos.
  evidence: Se carveó de Story 1.1 para bajar el tamaño de la spec por debajo del rango objetivo; el login puede satisfacer los tokens de diseño oscuro-primero con CSS embebido mínimo (copiado 1:1 del mockup) sin bloquear las ACs de OAuth ni requerir el toolchain de Tailwind todavía.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-iniciar-sesion-con-anilist-oauth.md`
  summary: Documentación de desarrollo (README) con instrucciones para registrar una app OAuth real en AniList y comandos de build/run del proyecto.
  evidence: No es necesaria para que las ACs de OAuth de Story 1.1 sean verificables; se carveó para reducir el tamaño de la spec.

- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-iniciar-sesion-con-anilist-oauth.md`
  summary: Agregar un endpoint de logout (`/logout`) para que un usuario autenticado pueda cerrar sesión.
  evidence: Surgió en la revisión de Step 4 (review layers). Story 1.1 solo cubre login; no hay AC ni FR que pida logout todavía, y agregarlo ahora sería scope creep sobre la spec congelada. Real pero no bloqueante — revisar cuándo se implemente la Story con el chrome autenticado (Epic 3) o antes si se detecta necesidad de operar la whitelist manualmente y hay que forzar logout de un usuario.
