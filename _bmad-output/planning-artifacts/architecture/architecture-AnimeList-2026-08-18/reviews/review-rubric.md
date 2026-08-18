# Rubric Review — ARCHITECTURE-SPINE.md (AnimeTracker)

Reviewer: independent rubric walker (fresh read, cross-checked against PRD, UX docs, and live web verification of named tech).

Overall: a strong, unusually well-grounded spine. Every AD traces to a concrete PRD FR/NFR, the tech stack is verifiably current, and the diagrams are real rather than decorative. Two structural gaps and one internal inconsistency keep it from a clean pass.

---

## 1. Divergence points fixed for the level below — mostly complete, one real gap

AD-1 through AD-10 cover the divergence points that actually matter for this PRD: layering + ACL isolation from AniList's GraphQL shape, single-writer on derived data, source-of-truth direction, session/token handling, whitelist-before-session, sync concurrency/rate-limiting, login-sync/job-sync code reuse, on-read trend computation, package dependency direction, and deployment topology. These map cleanly to FR-1/2 (auth), FR-5 (trends), FR-6/7/8/9 (sync), and the NFR §8 scale note. Good coverage.

**Gap found:** persistence/schema-evolution technology is never named. AD-1 formally names "Repository" as one of the three architectural layers, and the ER diagram implies a real relational schema (`APP_USER`, `TRACKING_ENTRY`, `SNAPSHOT` with FKs) that six feature-packages will read/write against over time — yet the Stack table has no row for the persistence framework (Spring Data JPA vs Spring Data JDBC vs plain `JdbcTemplate`/jOOQ) or for schema migration tooling (Flyway/Liquibase vs `ddl-auto`). This is exactly the kind of thing that lets two independently-built stories diverge: one story's author assumes JPA entities with Hibernate DDL, another assumes hand-written SQL migrations, and the two collide the first time a shared table changes shape. This should be decided or explicitly deferred; right now it is simply silent.

## 2. AD Rule enforceability — one Rule is undercut by the doc's own package layout

Most Rules are enforceable by direct code review (AD-3's "no mutation queries in the codebase" is greppable; AD-6's concurrency cap and AD-10's single-container form are structurally visible). Two are weaker:

- **AD-2 (single-writer)** states "solo `SyncService` escribe en `TrackingEntry` y `Snapshot`," but the Structural Seed package tree places the JPA repositories for those entities in `domain/` — a package explicitly shared and importable by every feature-package (`hoy`, `porestado`, `tendencias`, etc., per AD-9). Nothing in the spine gives this rule a compile-time backstop (e.g., scoping the repositories inside `sync/` so only `sync`'s own classes can even reference the type). As written, the rule is a convention enforced only by code-review discipline, which is exactly the kind of thing that erodes across many independently-built stories.
- **AD-9 (dependency direction)** bans cycles and cross-feature-package calls but names no enforcement mechanism (no ArchUnit test, no Spring Modulith verification, no Java module boundary). Java packages provide no compiler-level barrier between `hoy` and `porestado`; this is pure convention today.

Neither is fatal — both are common, fixable gaps — but as stated, "enforceable" is aspirational rather than actual for these two.

## 3. Deferred section — safe

Every Deferred item was checked against "could this let two independently-built units diverge in a way that breaks the system": Whitelist admin UI, hosting provider, multi-instance scaling/distributed locking, trend pre-aggregation, and Snapshot purge policy are all genuinely optimizations or operational choices layered *on top of* an already-fixed AD (AD-10 fixes deployment form before deferring the provider; AD-8 fixes on-read computation before deferring pre-aggregation; AD-6 already forecloses distributed locking by keeping the job in-process). None of these leave a load-bearing decision unmade. No issue here.

One omission worth naming alongside this section: the schema/persistence-tech gap from §1 above is not even listed under Deferred — it isn't deferred, it's just absent from the document entirely.

## 4. Named tech — verified current, no invented or stale claims

Spot-checked live against current sources (today: 2026-08-18):

- **Java 25 (LTS)** — correct; 25 is the current LTS release per the standard 2-year LTS cadence (17, 21, 25).
- **Spring Boot 4.1.x / Spring Framework 7 / Jakarta EE 11** — confirmed: Spring Boot 4.1.0 shipped June 2026 on Spring Framework 7.0.x; Spring Framework 7 targets Jakarta EE 11 (Servlet 6.1, JPA 3.2, Bean Validation 3.1) and recommends JDK 25 LTS. Matches exactly.
- **PostgreSQL 18.x** — correct; PG18 released Sept 2025, current minor as of this month is 18.6.
- **Tailwind CSS 4.3.x** — correct; v4.3 is the current minor line (v4.3.3 published July 2026).
- **AniList API: 90 req/min rate limit, OAuth token ~1 year with no refresh token** — both confirmed against AniList's own API docs (rate-limiting guide; auth guide explicitly states tokens are long-lived ~1 year and refresh tokens are not supported).

This is a genuine strength of the document — nothing here reads as guessed or stale, which is unusual and worth calling out explicitly.

## 5. Structural dimensions — deployment/infra addressed; operations dimension is silent

- **Deployment:** decided (AD-10 — single Docker container, web+scheduler in-process, separate Postgres instance).
- **Infra/provider:** explicitly deferred (AD-10 names the deferral directly, doesn't leave it silently unaddressed).
- **Operations (logging/monitoring/backup):** **silent, not deferred.** AD-6 mentions per-user try/catch and logging failures, but there is no statement anywhere — decided or deferred — about backup/restore strategy for the Postgres instance that holds the *entire historical Snapshot record* (the one thing AniList itself doesn't provide, and the core differentiator per the PRD's Trends feature). No mention of structured logging, health checks, or basic monitoring. Given the checklist explicitly flags "operations" as a dimension this altitude should own or explicitly punt on, this is a finding: not a dealbreaker for a 100–1000-user personal project, but it should appear at least as a one-line Deferred entry ("backup/restore strategy: deferred, revisit if Snapshot loss becomes a real risk") rather than being absent.
- Environments (dev/local/prod config split) are implicitly covered by "Config/secrets vía `application.yml` + variables de entorno" in the Consistency table — thin, but present, not silent.

## 6. Paradigm naming and layer mapping — clear and concrete

Strong pass. The paradigm ("Layered Controller→Service→Repository por feature-package, con ACL para AniList") is named once and stated consistently everywhere else in the doc. The Structural Seed's package tree makes it concrete, not just conceptual:

```
domain/, integration/anilist/, auth/, sync/, hoy/, porestado/, tendencias/, onboarding/, config/
```

This directly satisfies the "packages/directories, not vague" bar. The one place this concreteness breaks down is the Repository-technology gap noted in §1/§2 — the layer is named and located, but what implements it is unstated.

## 7. Diagrams — valid mermaid, real structure, one prose/diagram inconsistency

All three mermaid blocks parse as valid syntax and each conveys real, non-trivial structure (not a placeholder graph):

- AD-9's `graph TD` package-dependency diagram.
- The Structural Seed `graph LR` system diagram (Browser/App/DB/AniList/MAL-Sync with labeled edges).
- The `erDiagram` with real cardinalities and attribute blocks for the three core entities.

**Inconsistency found:** AD-9's Rule text says "cada feature-package depende de `domain` e `integration.anilist`" (every feature-package depends on both domain *and* integration.anilist) — but the diagram directly under it only draws `anilist` edges from `auth` and `sync`; `hoy`, `porestado`, `tendencias`, and `onboarding` have no edge to `anilist` at all. The diagram's version is almost certainly the intended one (those four feature-packages only ever need to read already-synced local data via `domain`, never touch AniList directly), but the Rule's prose overstates it as a universal dependency. Left as-is, this is precisely the ambiguity the spine exists to prevent: a story author for `hoy` or `tendencias` could read the Rule text and conclude a direct call into `integration.anilist` is architecturally sanctioned for their feature, when the diagram (and the system's actual data flow) says it isn't. Worth a one-line fix: change "depende de" to "puede depender de, cuando lo necesite" or explicitly enumerate which packages use `anilist`.

## 8. Filler / placeholders — clean

No unresolved template placeholders, no Lorem-ipsum-style filler, no dangling TODOs. `status: draft` and `companions: []` are legitimate metadata states, not filler. Every table row and prose paragraph carries real content tied to a PRD requirement or a stated rationale.

---

## Summary of Findings (ranked)

1. **(Medium-High) AD-2 enforceability undercut by package layout** — single-writer rule has no structural backstop; repositories are implied to live in the shared `domain` package rather than scoped to `sync`.
2. **(Medium) Persistence/schema-migration technology entirely unaddressed** — no ORM/data-access choice, no migration tool, not even listed under Deferred. Real divergence risk across a shared schema touched by six feature-packages.
3. **(Medium) AD-9 prose/diagram inconsistency** — Rule text says every feature-package depends on `integration.anilist`; the diagram (correctly) shows only `auth` and `sync` doing so. Creates exactly the kind of ambiguity the spine should foreclose.
4. **(Low-Medium) AD-9 dependency-direction rule has no enforcement mechanism named** — no ArchUnit/Modulith/module-boundary check; convention-only.
5. **(Low) Operations dimension (backup/restore, logging/monitoring) is silent rather than decided/deferred** — notable because Postgres holds the product's core differentiating data (Snapshot history).

**Positive finding worth preserving:** the Stack table's tech-version claims (Java 25 LTS, Spring Boot 4.1.x/Spring Framework 7/Jakarta EE 11, PostgreSQL 18.x, Tailwind 4.3.x, AniList's 90 req/min limit and ~1-year no-refresh OAuth token) were independently verified live and are all accurate as of the document's date — no staleness or invention detected.
