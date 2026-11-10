# Security Policy

## Supported Versions

Security fixes are released for the latest minor version of the current major release.

| Version | Supported |
|---------|-----------|
| 4.0.x   | Yes       |
| < 4.0   | No        |

## Reporting a Vulnerability

**Do not open a public issue for a security problem.**

Report it privately through GitHub's
[private vulnerability reporting](https://github.com/davils-com/kreate/security/advisories/new),
or by email to development@davils.com.

Please include:

- the affected Kreate version and the Gradle version you are running,
- a description of the impact — what an attacker gains,
- the steps to reproduce it, ideally as a minimal build script,
- any mitigation you are already aware of.

## Scope

Kreate is a build plugin: it runs with the full privileges of the developer or CI agent
executing the build, and it deliberately invokes external toolchains (CMake, Cargo, Trivy).

The following are **in scope**:

- the plugin resolving or executing an unexpected binary — for example an executable path
  that can be influenced by untrusted repository content,
- credentials configured through the publishing DSL being written to logs, reports, or
  build outputs,
- generated code or build files that introduce a vulnerability into a consumer's artifact.

The following are **not** vulnerabilities in Kreate:

- a vulnerability in CMake, Cargo, Trivy, Gradle, or the Kotlin compiler itself — report
  those to the respective project,
- Kreate executing a malicious `CMakeLists.txt` that is already part of the repository
  being built; a build script is trusted code by definition,
- findings that require an attacker to already have write access to the build.

## Known Limitations

- A GitLab token declared through `gitlabPackageRegistry` is held as a plain value by the repository
  Gradle creates, so it reaches the configuration cache. Gradle encrypts that cache with a key kept in
  the Gradle user home; someone who can read both can recover the token. Gradle's lazy credentials
  read header values from Gradle properties only, which does not fit a token from the environment.
- A local mode build turns dependency locking off for the whole project, not only for the
  substituted modules, because a lock would otherwise pin the release the snapshot replaces.

## Verifying a Release

Releases are published to Maven Central and signed. Each release also carries a signed build
provenance attestation and an SBOM attestation, made by the GitHub Actions run that built it:

```bash
gh attestation verify kreate-4.1.0.jar --repo davils-com/kreate
```

CI builds the plugin JAR twice on one runner and compares the checksums, which guards against
accidental non-determinism in the build. It does not cover the other artefacts or a build on another
machine, so treat the attestation, not a rebuild, as the proof of origin.
