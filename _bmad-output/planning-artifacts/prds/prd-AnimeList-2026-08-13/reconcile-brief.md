# Input Reconciliation: Product Brief → PRD

**Input:** Product Brief (`briefs/brief-AnimeList-2026-08-13/brief.md`)
**Reconciled against:** PRD (`prds/prd-AnimeList-2026-08-13/prd.md`)
**No addendum.md exists yet for this PRD workspace.**

## Gaps Found (4)

### Gap 1 — "No moat" / experience-based differentiation rationale is flattened

**What's missing/altered:** The Brief has a dedicated section, "Qué Lo Hace Diferente" (lines 33–35), with a specific, self-aware piece of reasoning: there is no defensible technical moat — MAL-Sync and AniList are more mature than anything this project could build in their areas, and *that is exactly why* the project leans on them instead of competing. Differentiation is explicitly "de experiencia, no de datos ni de detección," and the Brief closes with "Es una app deliberadamente chica y enfocada, **no un tracker alternativo**."

The PRD folds this into §1 Visión (lines 15–19) but only carries forward "Es deliberadamente chico... Todo el valor está en presentar mejor un dato que ya existe." The explicit honesty about the *absence* of a moat, the "experience not data" framing, and the "not an alternative tracker" self-positioning are dropped. This isn't just wording — it's the qualitative rationale a future contributor or the PM would need to resist scope creep (e.g., a stray card asking "should we improve detection accuracy?" has no explicit brief-backed answer of "no, that's not our differentiation" in the PRD).

**Where in the Brief:** "Qué Lo Hace Diferente" section, lines 33–35.

**Where it should land:** Either expand PRD §1 Visión with one added sentence carrying the "no moat / experience over data / not an alternative tracker" framing, or — since this is rationale rather than a testable requirement — capture it in a new `addendum.md` under a "Positioning / Rejected Alternatives" heading, referenced from §1.

---

### Gap 2 — Brief's "Visión (más allá de V1)" has no equivalent section or framing in the PRD

**What's missing/altered:** The Brief's closing section (lines 85–87) frames three specific post-V1 growth paths as a conditional roadmap: enabling write/edit to AniList from the app, supporting other tracking platforms (MyAnimeList, Kitsu), and social features (comparing lists between invited users) — explicitly "si el enfoque... prueba tener valor," "ninguno comprometido para V1," done "deliberadamente... para mantener V1 chico."

The PRD has no equivalent section. These three items appear only inside §5 No-Objetivos (lines 162–170) and §6.2 Fuera de Alcance (lines 179–183) as flat, undifferentiated "not doing this" bullets, identical in weight to permanent non-goals (e.g., "no aplicación móvil nativa"). The one exception is the "racha"/streak-detection idea, which the PRD does correctly carry forward with future framing ("Candidato natural para v2 si Tendencias prueba tener valor," §6.2). The other three vision items get no such treatment — a reader of the PRD alone cannot tell that write-access, multi-platform support, and social comparison are *deliberately deferred growth paths* rather than *rejected ideas*. This is exactly the risk the task flagged: not that V1 accidentally promises these, but that the Brief's forward-looking framing is silently lost rather than translated.

**Where in the Brief:** "Visión (más allá de V1)" section, lines 85–87.

**Where it should land:** Add a short "§12 Visión Post-V1" (or similar) section to the PRD that lists these three paths with the Brief's conditional framing intact, distinct from §5 No-Objetivos. Alternatively, if the PM wants to keep the PRD narrowly V1-scoped, this belongs in `addendum.md` as "Future Scope (Not V1)" — but it should not be left only inside a flat out-of-scope list with no forward framing at all.

---

### Gap 3 — Thymeleaf/server-rendered frontend decision is stated as settled fact but has no source in the Brief

**What's missing/altered:** PRD §9 (line 210) states: "Stack de backend — Java Spring Boot, con **frontend server-rendered (Thymeleaf)** en el mismo monolito — decisión ya tomada, no abierta." PRD §8 (line 203) also references this choice ("consistente con el frontend server-rendered elegido, §Notas de Arquitectura") as an established constraint.

The Brief only ever commits to "Backend en Java Spring Boot con base de datos propia" (lines 14, 53) — it never mentions a frontend framework, Thymeleaf, or a server-rendered-monolith architecture at all. The PRD presents Thymeleaf as an already-locked decision ("no abierta") rather than flagging it with `[ASSUMPTION]` the way it does for other unstated specifics (whitelist management, sync interval, retention policy). If this came from a conversation not captured in the Brief, that's fine, but as written it reads as new, unsourced scope stated with more confidence than the Brief supports. Note also: the §8 reference to "§Notas de Arquitectura" points to a section that does not exist anywhere in this PRD — a dangling internal reference worth fixing regardless of the reconciliation question.

**Where in the Brief:** Not present — Brief §"La Solución" / "Integraciones y Restricciones Técnicas" (lines 26, 53) only fix the backend (Java Spring Boot) and note "base de datos propia," with no frontend stack decision.

**Where it should land:** Either (a) confirm with the user whether Thymeleaf was actually decided outside the Brief and, if so, add it to `addendum.md` as a technical decision with its rationale/source, or (b) downgrade the PRD's phrasing from "decisión ya tomada, no abierta" to an `[ASSUMPTION]` consistent with how §4.1/§4.5/§8 handle other unconfirmed specifics, and fix the dangling "§Notas de Arquitectura" cross-reference.

---

### Gap 4 — SM-2's illustrative frequency example is dropped

**What's missing/altered:** Brief's Criterios de Éxito (line 82) reads: "...vuelven al dashboard 'hoy / seguí viendo' con cierta regularidad (frecuencia a definir — **ej. varias veces por semana**)..." The PRD's SM-2 (§7, line 191) keeps "frecuencia exacta a definir" but drops the "ej. varias veces por semana" illustrative anchor entirely.

This is minor on its own, but it's the kind of concrete number that later becomes useful when someone has to actually pick a target — losing the Brief's suggested ballpark means that context has to be re-derived or re-asked for rather than just refined.

**Where in the Brief:** "Criterios de Éxito (borrador — a validar)," line 82.

**Where it should land:** A small edit to PRD §7 SM-2 to restore the "ej. varias veces por semana" example, or move it into the `[ASSUMPTION]`/Índice de Supuestos pattern already used elsewhere in §11 if the PM wants it to read as a placeholder rather than settled text.

---

## Summary

No contradictions of substance were found on scale numbers (100/1000 users), the read-only/no-write posture, the MAL-Sync/AniList dependency framing, or the core FR set — those transferred cleanly and in some cases (FR-9 degradation, whitelist as FR-2, racha-as-v2-candidate) the PRD improved on the Brief's looser framing. The four gaps above are about: (1) lost qualitative positioning rationale, (2) a missing home for the Brief's explicit post-V1 vision framing, (3) an unsourced technical decision stated with more confidence than the Brief supports, and (4) one dropped illustrative number.
