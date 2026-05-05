# Trivy Security & Compliance

Trivy is a comprehensive and versatile security scanner deeply integrated into the Kreate ecosystem. This integration enables developers to embed security checks directly into the build process, facilitating a "Shift Left" approach to identify vulnerabilities and compliance issues as early as possible.

In Kreate, the Trivy integration focuses on three core areas:

*   **License Compliance**: Automatically verify that dependencies comply with your legal requirements and organizational policies.
*   **Vulnerability Scanning (CVEs)**: Identify known security flaws in your third-party libraries.
*   **Secret Scanning**: Detect hardcoded secrets such as API keys, passwords, and tokens within your source code and configuration files.

Configuration is centralized within the `trivy { }` block inside `kreate { project { } }`.

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
brew install trivy
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

## Quick Start

All Trivy scans are **disabled by default**. To use the module, it must be globally enabled in the `trivy` block:

```kotlin
kreate {
    project {
        trivy {
            // Globally enable the Trivy module
            enabled.set(true)
        }
    }
}
```

Once enabled, Kreate automatically registers the following Gradle tasks for your project:

| Task Name                | Description                                                |
|:-------------------------|:-----------------------------------------------------------|
| `trivyVulnerabilityScan` | Scans lockfiles for known security vulnerabilities (CVEs). |
| `trivyLicenseScan`       | Verifies dependencies for license compliance.              |
| `trivySecretScan`        | Searches source code for hardcoded secrets.                |

## Running Scans

Kreate provides multiple ways to run Trivy scans, ranging from running all checks at once to executing specific, targeted scans.

### Run All Enabled Scans
The most common way to run scans is through the standard Gradle `check` lifecycle task. When the Trivy module is enabled, its tasks are automatically registered as dependencies of `check`.

<code-block lang="bash">
./gradlew check
</code-block>

<note>
This will run all enabled scans (e.g., License, Vulnerability, and Secret) along with other verification tasks like Detekt or unit tests.
</note>

### Run Individual Scans
If you want to perform only a specific type of scan, you can call the corresponding task directly. This is useful for faster feedback loops during development.

**License Compliance:**
<code-block lang="bash">
./gradlew trivyLicenseScan
</code-block>

**Vulnerability (CVE) Scanning:**
<code-block lang="bash">
./gradlew trivyVulnerabilityScan
</code-block>

**Secret Detection:**
<code-block lang="bash">
./gradlew trivySecretScan
</code-block>

<tip>
Individual tasks are particularly useful when you've just updated a lockfile or added a custom secret rule and want to verify your changes without running the full test suite.
</tip>

## Further Reading

*   [**License Scanning**](Trivy-License-Scan.md): Monitoring and enforcing license policies.
*   [**Vulnerability Scanning**](Trivy-Vulnerability-Scan.md): Details on searching for CVEs in dependencies.
*   [**Secret Scanning**](Trivy-Secret-Scan.md): Protecting your project from leaked credentials.
*   [**Configuration Reference**](Trivy-Configuration-Reference.md): A complete list of all available DSL parameters.
