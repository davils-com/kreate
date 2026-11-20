# Standards

<link-summary>Declare the standards a project implements and learn when one of them changes.</link-summary>

<card-summary>RFCs, ISO, DIN, W3C, WHATWG and Unicode standards and vendored test suites, reviewed once and audited from then on.</card-summary>

<tldr>
<p><b>Enable</b>: <code>assurance { standards { enabled = true } }</code></p>
<p><b>Tasks</b>: <code>kreateStandardsInventory</code>, <code>kreateStandardsAudit</code>, <code>kreateStandardsReview</code></p>
<p><b>Record</b>: <code>standards.lock</code></p>
</tldr>

A project that implements protocols and formats follows standards: RFCs of the IETF, ISO and DIN
standards, specifications of the W3C and the WHATWG, Unicode, YAML, Semantic Versioning. Their bodies
keep working on them. An RFC is obsoleted by a newer one, an ISO standard gets an amendment or a new
edition, a draft becomes an RFC, errata correct what a standard says, a conformance test suite gets new
tests. Nothing in a build notices, and nothing notices when a vendored copy of a test suite drifts
from its original either.

The standards check makes these changes visible. A project declares what it implements, reviews each
standard once, and Kreate records what the issuing body published at that review in `standards.lock`.
From then on, `kreateStandardsAudit` compares that record with what the body publishes today and feeds
every difference into the [assurance gate](Assurance-Overview.md).

## Quick start

```kotlin
import java.time.Duration

kreate {
    assurance {
        enabled = true

        standards {
            enabled = true

            rfc(9562) { note = "UUID versions 4 and 7" }
            rfc(3339)
            iso("ISO 8601-1")
            w3c("webauthn-3")
            whatwg("url")
            unicode("UAX #31")
            github("yaml/yaml-spec")
            din("DIN 5008") { reviewEvery = Duration.ofDays(365) }
        }
    }
}
```

```bash
./gradlew kreateStandardsReview --all --note="Initial inventory of the standards."
```

Commit `standards.lock`. From now on `kreateAssurance` runs the inventory and the audit with every
other source of findings.

## Declaring standards

Every declaration has a canonical id. The id names the standard in `standards.lock`, in findings, in
`kreateStandardsReview --standard=<id>`, in `kreateExplain --rule=<id>` and in ignore rules.

An ISO standard published jointly with IEC or IEEE keeps one id however it is cited: `iso("ISO 10646")`,
`ISO-10646` in a source and `iso("ISO/IEC 10646")` all name `iso:iec-10646`. This holds for the joint
standards that are often cited without IEC, such as ISO/IEC 646, 8859, 9899, 10646, 14882 and 27001.

<table>
    <tr><td>Declaration</td><td>Id</td><td>Checked through</td></tr>
    <tr><td><code>rfc(9562)</code></td><td><code>rfc:9562</code></td><td>The RFC Editor: status, obsoleting and updating RFCs, errata</td></tr>
    <tr><td><code>draft("draft-ietf-httpbis-resumable-upload")</code></td><td><code>draft:draft-ietf-httpbis-resumable-upload</code></td><td>The IETF Datatracker: revision and state</td></tr>
    <tr><td><code>iso("ISO 8601-1")</code>, <code>iso("ISO/IEC 27001")</code></td><td><code>iso:8601-1</code>, <code>iso:iec-27001</code></td><td>The open data of ISO: edition, stage, amendments and corrigenda, withdrawal</td></tr>
    <tr><td><code>w3c("webauthn-3")</code></td><td><code>w3c:webauthn-3</code></td><td>The W3C API: latest version, maturity, newer levels of the series; the errata of the latest version</td></tr>
    <tr><td><code>whatwg("url")</code></td><td><code>whatwg:url</code></td><td>The review drafts of the living standard on GitHub</td></tr>
    <tr><td><code>unicode()</code>, <code>unicode("UAX #31")</code></td><td><code>unicode:ucd</code>, <code>unicode:uax-31</code></td><td>The version of the Unicode Character Database, the revision of an annex or technical standard</td></tr>
    <tr><td><code>github("yaml/yaml-spec")</code></td><td><code>github:yaml/yaml-spec</code></td><td>The latest final release, or the newest version tag; <code>tags</code> narrows them by a regular expression</td></tr>
    <tr><td><code>din("DIN 5008")</code></td><td><code>din:5008</code></td><td>Nothing: needs <code>reviewEvery</code>; an optional <code>url</code> is watched for changes</td></tr>
    <tr><td><code>document("ECMA-404")</code></td><td><code>document:ecma-404</code></td><td>Nothing: needs <code>reviewEvery</code>; an optional <code>url</code> is watched for changes</td></tr>
    <tr><td><code>suite("xmlconf") { }</code></td><td><code>suite:xmlconf</code></td><td>The vendored files against the pinned archive or commit, their digest against the review, a watched page or the newest commit upstream; see <a href="#test-suites">Test suites</a></td></tr>
</table>

Each declaration takes an optional block:

- `note` says why the project implements the standard.
- `reviewEvery` sets how long a review stands before it falls due. `standards { reviewEvery }` sets a
  default for every standard without one of its own, so even an RFC that never changes is read again
  from time to time.
- `tags` applies to `github()` only, `url` to `din()` and `document()` only, `errata` to `w3c()` only.

A declaration that no body names, two declarations of one standard, a DIN standard without an
interval, a `url` that is neither HTTPS nor a loopback address, an `errata` address outside `w3.org`
and `github.com`, and a W3C name without a version, such as `w3c("xpath")` or `w3c("xml-c14n")`,
fail the configuration of the build. The W3C answers for such a name with its latest level, so
`w3c("xpath")` would silently track XPath 3.1; declare `xpath-10` or `xpath-31` instead.

## W3C errata

The W3C API links the errata of the latest version of a specification. The audit reads them with
the specification:

- An errata **page on `w3.org`**, as the XML Recommendations have: every anchor of the page counts
  as a confirmed erratum, and the fingerprint of the page is recorded, so a page that changes without
  a new anchor still reports `STANDARD_CHANGED`.
- The **issues of a repository on `github.com`**: an open issue is a reported erratum, an issue
  closed as completed a confirmed one, an issue closed as not planned a rejected one.

New confirmed errata report `STANDARD_ERRATA`, new reported ones `STANDARD_ERRATA_REPORTED`, as for
RFCs. When the W3C API links no errata, or links them somewhere else, `errata` names them:

```kotlin
w3c("xmlschema11-1") {
    errata = "https://github.com/w3c/xsdtests/issues?q=label:erratum"
}
```

`errata` takes an HTTPS address on `w3.org`, or `https://github.com/<owner>/<name>/issues` with an
optional query (`is:open` and `repo:` are dropped, every state is read) or
`https://github.com/<owner>/<name>/labels/<label>`. The audit follows redirects only to `w3.org` and
`github.com`, reads at most 4 MB of a page and at most 1 000 issues.

A newer level of the series, such as XML 1.1 for XML 1.0, reports `STANDARD_UPDATED`, unless the
build declares that level too: `w3c("xml")` together with `w3c("xml11")` reports nothing.

## Test suites

A project that vendors a conformance test suite declares where the copy comes from:

```kotlin
standards {
    suite("xmlconf") {
        archive = "https://www.w3.org/XML/Test/xmlts20130923.tar.gz"
        sha256 = "9b61db9f5dbffa545f4b8d78422167083a8568c59bd1129f94138f936cf6fc1f"
        vendored = "src/jvmTest/resources/xmlconf"
        watch = "https://www.w3.org/XML/Test/"
    }
    suite("xsdtests") {
        github = "w3c/xsdtests"
        commit = "7bc3365c652a322f3d762021b3879eb92dae7e30"
        vendored = "src/jvmTest/resources/xsdtests"
    }
}
```

The audit checks three things:

1. **The copy is the original.** The archive still has the pinned `sha256`, and every file under
   `vendored` is byte for byte a file of the archive, or a blob of the pinned commit as the Git trees
   API of GitHub lists it. A copy that differs, such as one whose line endings a checkout converted,
   reports `STANDARD_SUITE_MISMATCH`. The archive is read with a budget of 512 MB and 65 535 entries,
   and an archive with a path that leaves its directory is refused. `root` names the directory of the
   archive or repository that `vendored` mirrors; it defaults to the one directory every entry of an
   archive lies in, such as `xmlconf/`, and to the root of a repository.
2. **The copy is the reviewed one.** The review records a digest of the vendored directory, over
   its sorted paths and raw bytes, as `contentDigest` in `standards.lock`. A file that is added,
   removed or edited afterwards reports `STANDARD_SUITE_MODIFIED`; a deliberate update of the suite is
   reviewed like any other change.
3. **Upstream moves on.** A changed `watch` page reports `STANDARD_CHANGED`, a new commit on the
   default branch of a `github` repository `STANDARD_REVISED`.

Both blocking findings come with the vendored files as inputs, so the audit runs again as soon as one
of them changes. The inventory does not search the vendored directories for references to standards.

A `.tar.gz`, `.tar` or `.zip` archive is read; `archive` and `watch` take HTTPS or loopback addresses.

## Reviewing

`kreateStandardsReview` records that someone read a standard, together with what its body publishes
today, as the audit read it:

```bash
./gradlew kreateStandardsReview --standard=rfc:9562 --note="Errata 7955 and 7958 do not touch our generator."
./gradlew kreateStandardsReview --all --note="Initial inventory of the standards."
./gradlew kreateStandardsReview --prune
```

- `--note` is required: a review says what it found.
- Without `--standard` or `--all` the task lists the standards that wait for a review.
- A misspelt id gets a suggestion, such as `Did you mean rfc:9562?`.
- A standard whose body cannot be reached right now cannot be reviewed.
- `--prune` removes the records of standards that are no longer declared.

The lock file is sorted and stable, so the diff of a review shows exactly what changed about the
standard, and the review is reviewed like any other change.

## Rules

`kreateStandardsAudit` feeds the source `standards`:

| Rule | Severity | When |
|---|---|---|
| `STANDARD_OBSOLETED` | High | The standard was replaced or withdrawn, such as an RFC obsoleted by a newer one, a superseded W3C Recommendation or a withdrawn ISO standard |
| `STANDARD_UNKNOWN` | High | The body does not know the declared standard, or GitHub does not know the pinned commit of a suite |
| `STANDARD_SUITE_MISMATCH` | High | A vendored suite is not byte for byte from its pinned archive or commit, or the archive lost its pinned `sha256`; cannot be reviewed away |
| `STANDARD_SUITE_MODIFIED` | High | The files of a vendored suite changed since its review |
| `STANDARD_UPDATED` | Medium | An RFC updates it, an amendment or corrigendum of the ISO edition appeared, or a newer level of a W3C specification exists |
| `STANDARD_REVISED` | Medium | A new edition, version or revision: an ISO edition, a W3C version, a Unicode version or revision, a draft revision, a WHATWG review draft, a GitHub release, a new commit of a suite on GitHub, a new pinned archive |
| `STANDARD_STATUS_CHANGED` | Medium | A new maturity, such as an ISO standard that is to be revised (stage 90.92) |
| `STANDARD_ERRATA` | Medium | New verified errata, or errata held for the next update; for W3C, new anchors of the errata page or issues closed as completed |
| `STANDARD_CHANGED` | Medium | A watched page changed: the `url` of a DIN standard or another document, the errata page of a W3C specification, the `watch` page of a suite |
| `STANDARD_ERRATA_REPORTED` | Low | New errata that are reported but not decided, such as open errata issues on GitHub |

`kreateStandardsInventory` feeds the source `standards-inventory`, without the network:

| Rule | Severity | When |
|---|---|---|
| `STANDARD_UNREVIEWED` | High | A declared standard has no review in `standards.lock` |
| `STANDARD_REVIEW_OVERDUE` | Medium | The `reviewEvery` of a standard has passed since its review |
| `STANDARD_UNDECLARED` | Medium | A file names a standard, such as `RFC 7396`, a link to `w3.org/TR/` or `UAX #31`, that is not declared; with the file and line of the first mention |
| `STANDARD_VERSIONLESS` | Medium | A file links a W3C specification without its version, such as `w3.org/TR/xpath`, which the W3C sends to the latest level |
| `STANDARD_LOCK_ORPHANED` | Low | `standards.lock` records a standard that is no longer declared |

With the default `failOn = HIGH`, a standard that was never reviewed and one that was obsoleted block
the gate; the other findings are reported. Every finding carries the id of its standard as its module,
so an ignore rule can accept the findings of one standard:

```kotlin
assurance {
    ignore("STANDARD_UNDECLARED") {
        module = "rfc:2119"
        reason = "We only use the key words of RFC 2119."
    }
}
```

The inventory reads old and dated addresses of the W3C by the short name they stand for today:
`w3.org/TR/REC-xml` is `w3c:xml`, `w3.org/TR/REC-xml-names` is `w3c:xml-names`,
`w3.org/TR/xml-c14n` is `w3c:xml-c14n10` and `w3.org/TR/2006/REC-xml11-20060816/` is `w3c:xml11`.

The inventory searches every file below `src` and `docs` and the Markdown files of the project
directory, except the vendored directories of suites. `inventory { includes; excludes }` changes that, and `inventory { enabled = false }` turns
the search off; the checks of the lock file stay.

## Freshness, caching and offline builds

- **Freshness.** A standard can change without anything in the build changing. The audit stands for
  `reuseVerdictFor`, 6 hours by default, and runs again after that, or as soon as a declaration or
  `standards.lock` changes.
- **Caching.** Every answer is kept for a day under `caches/kreate/standards` in the Gradle user home
  and then revalidated with `ETag` and `Last-Modified`. The errata of the RFC Editor (about 12 MB) and
  the open data of ISO (about 80 MB) are read only when an `rfc()` or an `iso()` is declared, once per
  build however many projects declare standards. The archive of a `suite()` and the Git tree of its
  commit are cached the same way. Cache this directory in CI.
- **Failures.** When a service fails, the audit uses the last answer it has, with its age;
  `maxAdvisoryAge`, 3 days by default, makes older data incomplete. Without any answer, or with
  `--offline`, the audit is incomplete and the gate says why. A project that only declares standards
  without a `url` needs no network at all.
- **Privacy.** Only the ids of the declared standards reach the bodies. A `GITHUB_TOKEN` in the
  environment raises the rate limit of the GitHub API and is only ever sent to it over HTTPS.

## Mirrors

Each service can be moved to a mirror or a proxy with a Gradle property; `kreateDoctor` probes them
once the root project checks standards:

| Property | Default |
|---|---|
| `kreate.network.rfcEditorUrl` | `https://www.rfc-editor.org` |
| `kreate.network.datatrackerUrl` | `https://datatracker.ietf.org` |
| `kreate.network.w3cApiUrl` | `https://api.w3.org` |
| `kreate.network.w3cUrl` | `https://www.w3.org`, where errata pages on `w3.org` are read |
| `kreate.network.githubApiUrl` | `https://api.github.com` |
| `kreate.network.unicodeUrl` | `https://www.unicode.org` |
| `kreate.network.isoOpenDataUrl` | `https://isopublicstorageprod.blob.core.windows.net/opendata/_latest` |

## Data sources

The audit reads public data of the RFC Editor and the IETF Datatracker, the W3C API and the errata
pages of the W3C, the GitHub API, the Unicode Consortium, the [open data of ISO](https://www.iso.org/open-data.html)
and the archives and pages a `suite()` names. Kreate does
not ship any of it; the terms of use of each source apply to the data a build reads.
