# Trivy Security & Compliance

<link-summary>Installing Trivy, enabling the scans, and running them.</link-summary>

<card-summary>Three scans wired into your build: vulnerabilities, licences, and secrets.</card-summary>

<tldr>
<p><b>Enable</b>: <code>trivy { enabled = true }</code></p>
<p><b>Needs</b>: the Trivy CLI, and dependency lock files for two of the three scans</p>
<p><b>Run</b>: <code>./gradlew kreateTrivyScan</code></p>
</tldr>

Trivy is a comprehensive and versatile security scanner deeply integrated into the Kreate ecosystem. This integration enables developers to embed security checks directly into the build process, facilitating a "Shift Left" approach to identify vulnerabilities and compliance issues as early as possible.

In Kreate, the Trivy integration focuses on three core areas:

*   **License Compliance**: Automatically verify that dependencies comply with your legal requirements and organizational policies.
*   **Vulnerability Scanning (CVEs)**: Identify known security flaws in your third-party libraries.
*   **Secret Scanning**: Detect hardcoded secrets such as API keys, passwords, and tokens within your source code and configuration files.

Configuration is centralized within the `trivy { }` block inside `kreate { }`.

## Why Use Trivy in Kreate?

Manual verification of licenses and vulnerabilities is time-consuming and prone to human error. By automating these checks with Trivy, you benefit from:

*   **Early Detection**: Catch issues on the developer's machine before code is even committed to the repository.
*   **Automated Compliance**: Ensure that no software with incompatible licenses (e.g., AGPL in proprietary projects) is shipped.
*   **Infrastructure Protection**: Prevent credential theft by scanning for secrets in configuration files and source code.

## Prerequisites

To use the Trivy integration, the **Trivy CLI** must be installed on the executing system (developer machine or CI runner).

<tabs>
<tab title="macOS">
Install via Homebrew:
<code-block lang="bash">
<![CDATA[
brew install trivy
]]>
</code-block>
</tab>

<tab title="Debian/Ubuntu">
Add the repository and install:
<code-block lang="bash">
<![CDATA[
sudo apt-get install wget apt-transport-https gnupg lsb-release
wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | gpg --dearmor | sudo tee /usr/share/keyrings/trivy.gpg > /dev/null
echo "deb [signed-by=/usr/share/keyrings/trivy.gpg] https://aquasecurity.github.io/trivy-repo/deb $(lsb_release -sc) main" | sudo tee -a /etc/apt/sources.list.d/trivy.list
sudo apt-get update
sudo apt-get install trivy
]]>
</code-block>
</tab>

<tab title="RHEL/CentOS">
Add the repository and install:
<code-block lang="bash">
<![CDATA[
cat << 'EOF' | sudo tee /etc/yum.repos.d/trivy.repo
[trivy]
name=Trivy repository
baseurl=https://aquasecurity.github.io/trivy-repo/rpm/releases/$releasever/$basearch/
gpgcheck=1
enabled=1
gpgkey=https://aquasecurity.github.io/trivy-repo/rpm/public.key
EOF
sudo yum -y update
sudo yum -y install trivy
]]>
</code-block>
</tab>

<tab title="Windows">
<p><b>Manual Installation (Official):</b></p>
<list>
<li>Download the <code>trivy_x.xx.x_windows-64bit.zip</code> file from the <a href="https://github.com/aquasecurity/trivy/releases/">GitHub Releases</a> page.</li>
<li>Unzip the file and copy to any folder.</li>
</list>
</tab>

<tab title="Binary/Script">
Generic installation script (ideal for CI):
<code-block lang="bash">
<![CDATA[
curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b /usr/local/bin
]]>
</code-block>
</tab>
</tabs>

<note>
Kreate attempts to automatically resolve the path to the Trivy executable. On macOS, it specifically checks common Homebrew installation paths (e.g., <code>/opt/homebrew/bin/trivy</code>) to ensure compatibility with the Gradle environment.
</note>

## Dependency Locking

For **License Compliance** and **Vulnerability Scanning (CVEs)**, Kreate relies on Gradle lockfiles. These files provide a precise snapshot of all transitive dependencies, which is required for Trivy to perform an accurate analysis.

### 1. Enable Dependency Locking

You must enable dependency locking yourself, in your `build.gradle.kts`. **Lock only the
configurations you actually ship:**

```kotlin
val scannedConfigurations = setOf("compileClasspath", "runtimeClasspath")

configurations.matching { it.name in scannedConfigurations }.configureEach {
    resolutionStrategy.activateDependencyLocking()
}
```

<warning>
<p>
Avoid <code>dependencyLocking { lockAllConfigurations() }</code> if you intend to scan the
resulting lock file. It also locks every build tool's internal classpath — the Kotlin compiler,
Dokka's HTML generator, Detekt's rule set plugins — none of which ends up in your artifact.
</p>
<p>
In this repository's own example project the difference was <b>2 shipped dependencies out of 103
locked entries</b>. Scanning the broad lock file reported eight CVEs, every one of them in the XML
parser Dokka uses to render documentation. Nothing there is reachable by anything you publish,
none of it is fixable from your build, and reports like that are how teams learn to ignore their
security scans.
</p>
</warning>

<tip>
Add <code>testCompileClasspath</code> and <code>testRuntimeClasspath</code> if your policy covers
test dependencies too. They do not ship, but a compromised test dependency still executes on your
build machines.
</tip>

### 2. Generate Lockfiles
After enabling locking or when changing dependencies, you must generate or update the lockfiles using the following command:

<code-block lang="bash">
<![CDATA[
./gradlew dependencies --write-locks
]]>
</code-block>

<warning>
If no lockfiles are present, the <code>kreateTrivyLicenseScan</code> and <code>kreateTrivyVulnerabilityScan</code> tasks will not have any input to analyze and might not produce any results.
</warning>

## Quick Start

All Trivy scans are **disabled by default**. To use the module, it must be globally enabled in the `trivy` block:

```kotlin
kreate {
    trivy {
        // Globally enable the Trivy module
        enabled = true
    }
}
```

Once enabled, Kreate automatically registers the following Gradle tasks for your project:

| Task Name                | Description                                                |
|:-------------------------|:-----------------------------------------------------------|
| `kreateTrivyScan`              | Lifecycle aggregator that runs all enabled Trivy scans.    |
| `kreateTrivyVulnerabilityScan` | Scans lockfiles for known security vulnerabilities (CVEs). |
| `kreateTrivyLicenseScan`       | Verifies dependencies for license compliance.              |
| `kreateTrivySecretScan`        | Searches source code for hardcoded secrets.                |

## Running Scans

Kreate provides multiple ways to run Trivy scans, ranging from running all checks at once to executing specific, targeted scans.

### Run All Enabled Scans
Kreate registers a central `kreateTrivyScan` task that aggregates all individual scanners. It is **not** attached to `check`: the license and vulnerability scans need Trivy's database, which has to be downloaded. The secret scan alone is attached to `check` by default (`secrets { runOnCheck }`), because it runs offline in seconds and a secret has to be caught before the push.

To run all security checks:
<code-block lang="bash">
<![CDATA[
./gradlew kreateTrivyScan
]]>
</code-block>


### Run Individual Scans
If you want to perform only a specific type of scan, you can call the corresponding task directly. This is useful for faster feedback loops during development.

**License Compliance:**
<code-block lang="bash">
<![CDATA[
./gradlew kreateTrivyLicenseScan
]]>
</code-block>

**Vulnerability (CVE) Scanning:**
<code-block lang="bash">
<![CDATA[
./gradlew kreateTrivyVulnerabilityScan
]]>
</code-block>

**Secret Detection:**
<code-block lang="bash">
<![CDATA[
./gradlew kreateTrivySecretScan
]]>
</code-block>

<tip>
Individual tasks are particularly useful when you've just updated a lockfile or added a custom secret rule and want to verify your changes without running the full test suite.
</tip>


<seealso>
    <category ref="security">
        <a href="Trivy-Vulnerability-Scan.md">Vulnerability scanning</a>
        <a href="Trivy-License-Scan.md">Licence scanning</a>
        <a href="Trivy-Secret-Scan.md">Secret scanning</a>
        <a href="Trivy-Configuration-Reference.md">Configuration reference</a>
    </category>
    <category ref="reference">
        <a href="CI-Integration.md">CI integration</a>
        <a href="Task-Reference.md">Task reference</a>
    </category>
</seealso>
