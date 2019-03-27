# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## API
The API consists of all public Java types from `com.atlassian.performance.tools.virtualusers.api` and its subpackages:

  * [source compatibility]
  * [binary compatibility]
  * [behavioral compatibility] with behavioral contracts expressed via Javadoc

[source compatibility]: http://cr.openjdk.java.net/~darcy/OpenJdkDevGuide/OpenJdkDevelopersGuide.v0.777.html#source_compatibility
[binary compatibility]: http://cr.openjdk.java.net/~darcy/OpenJdkDevGuide/OpenJdkDevelopersGuide.v0.777.html#binary_compatibility
[behavioral compatibility]: http://cr.openjdk.java.net/~darcy/OpenJdkDevGuide/OpenJdkDevelopersGuide.v0.777.html#behavioral_compatibility

### POM
Changing the license is breaking a contract.
Adding a requirement of a major version of a dependency is breaking a contract.
Dropping a requirement of a major version of a dependency is a new contract.

## [Unreleased]
[Unreleased]: https://github.com/atlassian/virtual-users/compare/release-3.6.3...master

## [3.6.3] - 2019-03-27
[3.6.3]: https://github.com/atlassian/virtual-users/compare/release-3.6.2...release-3.6.3

### Fixed
- Remove login retry mechanism. Resolve [JPERF-432].

[JPERF-432]: https://ecosystem.atlassian.net/browse/JPERF-432

## [3.6.2] - 2019-03-19
[3.6.2]: https://github.com/atlassian/virtual-users/compare/release-3.6.1...release-3.6.2

### Fixed
- Have more patience for user generation. Fix [JPERF-411].
- Waste less VU time during user generation.

## [3.6.1] - 2019-03-11
[3.6.1]: https://github.com/atlassian/virtual-users/compare/release-3.6.0...release-3.6.1

### Fixed
- Close HTTP connections when creating Jira users. Fix [JPERF-411].

[JPERF-411]: https://ecosystem.atlassian.net/browse/JPERF-411

## [3.6.0] - 2019-03-06
[3.6.0]: https://github.com/atlassian/virtual-users/compare/release-3.5.0...release-3.6.0

### Added
- Support creating test users so each virtual user is logged in with a unique user. Reusing the same admin user resulted 
in a high number of http threads being blocked on user-data-specific locks, causing unrealistically high response times. 

## [3.5.0] - 2019-02-29
[3.5.0]: https://github.com/atlassian/virtual-users/compare/release-3.4.1...release-3.5.0

### Added
- Slow down each VU to a given `VirtualUserLoad.maxOverallLoad`. Fix [JPERF-403].
- Let `VirtualUserLoad` slice itself into multiple smaller loads.
- Let `VirtualUserLoad` check its equivalency with another load.
- Let `TemporalRate` multiply and divide itself.

### Deprecated
- Deprecate `VirtualUserLoad` constructor in favor of `VirtualUserLoad.Builder`.

[JPERF-403]: https://ecosystem.atlassian.net/browse/JPERF-403

## [3.4.1] - 2019-01-22
[3.4.1]: https://github.com/atlassian/virtual-users/compare/release-3.4.0...release-3.4.1

### Fixed
- Isolate `ChromeOptions` state between `GoogleChrome` starts. Fix [JPERF-353].

[JPERF-353]: https://ecosystem.atlassian.net/browse/JPERF-353

## [3.4.0] - 2019-01-11
[3.4.0]: https://github.com/atlassian/virtual-users/compare/release-3.3.6...release-3.4.0

### Added
- Let virtual users skip the setup. Unblock [JPERF-346].
- Add a builder for `VirtualUserBehavior`.

### Deprecate
- Deprecate all `VirtualUserBehavior` methods in favor of the builder.

### Fixed
- Install ChromeDriver only once instead of for every virtual user. Fix [JPERF-351].

[JPERF-351]: https://ecosystem.atlassian.net/browse/JPERF-351
[JPERF-346]: https://ecosystem.atlassian.net/browse/JPERF-346

## [3.3.6] - 2019-01-04
[3.3.6]: https://github.com/atlassian/virtual-users/compare/release-3.3.5...release-3.3.6

### Fixed
- Disable /dev/shm usage. Resolves [JPERF-333].

[JPERF-333]: https://ecosystem.atlassian.net/browse/JPERF-333

## [3.3.5] - 2019-01-03
[3.3.5]: https://github.com/atlassian/virtual-users/compare/release-3.3.4...release-3.3.5

### Fixed
- Stop overriding chromedriver if already installed. Resolves [JPERF-330].

[JPERF-330]: https://ecosystem.atlassian.net/browse/JPERF-330

## [3.3.4] - 2018-12-03
[3.3.4]: https://github.com/atlassian/virtual-users/compare/release-3.3.3...release-3.3.4

### Fixed 
- Shutting down virtual users which resolves [JPERF-281].

## [3.3.3] - 2018-11-30
[3.3.3]: https://github.com/atlassian/virtual-users/compare/release-3.3.2...release-3.3.3

### Fixed 
- Shutting down virtual users which resolves [JPERF-281].

[JPERF-281]: https://ecosystem.atlassian.net/browse/JPERF-281

## [3.3.2] - 2018-11-30
[3.3.2]: https://github.com/atlassian/virtual-users/compare/release-3.3.1...release-3.3.2

### Fixed 
- Race condition while stopping virtual user.

## [3.3.1] - 2018-11-30
[3.3.1]: https://github.com/atlassian/virtual-users/compare/release-3.3.0...release-3.3.1

### Fixed
- Add missing browser parameter when serialising VirtualUserOptions to command line parameters.

## [3.3.0] - 2018-11-27
[3.3.0]: https://github.com/atlassian/virtual-users/compare/release-3.2.0...release-3.3.0

### Added
- Compose `VirtualUserOptions` from `VirtualUserTarget` and `VirtualUserBehavior`.
- Add a `VirtualUserBehavior.withLoad` to avoid rewriting the rest of the parameters.

### Deprecate
- Deprecate the 9-arg `VirtualUserOptions` constructor in favor of the new 2-arg one.
- Deprecate the `VirtualUserOptions` getters. Prepare to expose only those getters, which are proven useful.

## [3.2.0] - 2018-11-23
[3.2.0]: https://github.com/atlassian/virtual-users/compare/release-3.1.1...release-3.2.0

### Added
- Add custom browser support which resolves [JPERF-169] and is required for [JPERF-243].
 It also adds SPI to resolve or workaround [JPERF-226],[JPERF-238],[JPERF-196],[JPERF-180].

[JPERF-169]: https://ecosystem.atlassian.net/browse/JPERF-169
[JPERF-243]: https://ecosystem.atlassian.net/browse/JPERF-243
[JPERF-226]: https://ecosystem.atlassian.net/browse/JPERF-226
[JPERF-238]: https://ecosystem.atlassian.net/browse/JPERF-238
[JPERF-196]: https://ecosystem.atlassian.net/browse/JPERF-196
[JPERF-180]: https://ecosystem.atlassian.net/browse/JPERF-180

## [3.1.1] - 2018-11-20
[3.1.1]: https://github.com/atlassian/virtual-users/compare/release-3.1.0...release-3.1.1

### Fixed
- Do not shutdown JVM in `EntryPoint`. Resolve [JPERF-259].

[JPERF-259]: https://ecosystem.atlassian.net/browse/JPERF-259

## [3.1.0] - 2018-11-14
[3.1.0]: https://github.com/atlassian/virtual-users/compare/release-3.0.0...release-3.1.0

### Added
- Allow custom logIn and setup actions. Resolves [JPERF-127] and [JPERF-150].

[JPERF-127]: https://ecosystem.atlassian.net/browse/JPERF-127
[JPERF-150]: https://ecosystem.atlassian.net/browse/JPERF-150

## [3.0.0] - 2018-11-06
[3.0.0]: https://github.com/atlassian/virtual-users/compare/release-2.2.0...release-3.0.0

### Removed
- Remove Kotlin data-class generated methods from API.
- Remove all deprecated API.

## [2.2.0] - 2018-10-31
[2.2.0]: https://github.com/atlassian/virtual-users/compare/release-2.1.5...release-2.2.0

### INCOMPATIBILITY BUG
Break binary compatibility for `com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions`. See [JPERF-253].
Roll back to `2.1.5` to restore this compatibility.

[JPERF-253]: https://ecosystem.atlassian.net/browse/JPERF-253

### Added
- Allow insecure connections. Resolve [JPERF-196].

### Fixed
- Print relative paths for dumps in WebDriverDiagnostics as a workaround for [JPERF-158].
- Fix serialization of the `help` CLI argument.
- Remove custom page load timeout. Decreases [JPERF-249] occurrence.

[JPERF-249]: https://ecosystem.atlassian.net/browse/JPERF-249

### Deprecated
- Deprecate the Kotlin-defaults-ridden `VirtualUserOptions` constructor.

[JPERF-196]: https://ecosystem.atlassian.net/browse/JPERF-196
[JPERF-158]: https://ecosystem.atlassian.net/browse/JPERF-158

## [2.1.5] - 2018-10-25
[2.1.5]: https://github.com/atlassian/virtual-users/compare/release-2.1.4...release-2.1.5

### Fixed
- Hold virtual users before running the setup. Fix [JPERF-230]

[JPERF-230]: https://ecosystem.atlassian.net/browse/JPERF-230

## [2.1.4] - 2018-10-23
[2.1.4]: https://github.com/atlassian/virtual-users/compare/release-2.1.3...release-2.1.4

### Fixed
- Validate Jira URI. Fix [JPERF-206].

[JPERF-206]: https://ecosystem.atlassian.net/browse/JPERF-206

## [2.1.3] - 2018-10-22
[2.1.3]: https://github.com/atlassian/virtual-users/compare/release-2.1.2...release-2.1.3

### Fixed
- Support for Chrome v69-71. Fix [JPERF-224].

[JPERF-224]: https://ecosystem.atlassian.net/browse/JPERF-224

### [2.1.2] - 2018-10-18
[2.1.2]: https://github.com/atlassian/virtual-users/compare/release-2.1.1...release-2.1.2

### Fixed
- Terminate the virtual user when it fails to log in or set up in the `setUp` phase. Fix [JPERF-217].

[JPERF-217]: https://ecosystem.atlassian.net/browse/JPERF-217

### [2.1.1] - 2018-10-16
[2.1.1]: https://github.com/atlassian/virtual-users/compare/release-2.1.0...release-2.1.1

### Fixed
- Take screenshots after failed login or setup. Fix [JPERF-179].

[JPERF-179]: https://ecosystem.atlassian.net/browse/JPERF-179

### [2.1.0] - 2018-09-12
[2.1.0]: https://github.com/atlassian/virtual-users/compare/release-2.0.0...release-2.1.0

### Added
- Expose virtual user error diagnostics.

## [2.0.0] - 2018-09-06
[2.0.0]: https://github.com/atlassian/virtual-users/compare/release-1.0.2...release-2.0.0

### Changed
- Change the type of `VirtualUserOptions.scenario`.

## [1.0.2] - 2018-09-06
[1.0.2]: https://github.com/atlassian/virtual-users/compare/release-1.0.1...release-1.0.2

### Fixed
- Restore `VirtualUserOptions` source and binary compatibility with `1.0.0`.

## [1.0.1] - 2018-09-05
[1.0.1]: https://github.com/atlassian/virtual-users/compare/release-1.0.0...release-1.0.1

### INCOMPATIBILITY BUG
Break source and binary compatibility for `com.atlassian.performance.tools.virtualusers.api.VirtualUserOptions`.
Switch to `1.0.2` or newer to restore this compatibility or roll forward with `2.0.0`.

## [1.0.0] - 2018-09-04
[1.0.0]: https://github.com/atlassian/virtual-users/compare/release-0.0.4...release-1.0.0

### Changed 
- Define public API for the module

### Added
- API for virtual users JAR command line arguments.

### Fixed
- Strict dependency resolution.

## [0.0.4] - 2018-08-28
[0.0.4]: https://github.com/atlassian/virtual-users/compare/release-0.0.3...release-0.0.4

### Removed
- Remove plain text report.

### Added
- Add diagnosticsLimit parameter.

## [0.0.3] - 2018-08-07
[0.0.3]: https://github.com/atlassian/virtual-users/compare/release-0.0.2...release-0.0.3

### Fixed
- Restore main log. See #2.

## [0.0.2] - 2018-08-03
[0.0.2]: https://github.com/atlassian/virtual-users/compare/release-0.0.1...release-0.0.2

### Fixed
- Gradle plugin to compile Kotlin source.

## [0.0.1] - 2018-08-03
[0.0.1]: https://github.com/atlassian/virtual-users/compare/initial-commit...release-0.0.1

### Added
- Generic Virtual Users mechanisms migrated from [JPT submodule].
- [README.md](README.md).
- Bitbucket Pipelines.

[JPT submodule]: https://stash.atlassian.com/projects/JIRASERVER/repos/jira-performance-tests/browse/virtual-users?at=ce7ab255e955891a927ac1c19b9b6178b56d1e4f
