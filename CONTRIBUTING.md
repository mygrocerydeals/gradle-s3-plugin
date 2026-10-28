# Contributing to the Gradle S3 Plugin

Thanks for helping improve the plugin. Bug reports, feature requests and pull requests are welcome.

## Reporting issues

Open an issue using one of the templates. For bugs, include the plugin, Gradle and JDK versions and a minimal `build.gradle` that shows the problem. Report security vulnerabilities privately as described in [SECURITY.md](SECURITY.md), not in a public issue.

## Prerequisites

- JDK 21 (CI uses Amazon Corretto 21). The Gradle wrapper downloads the right Gradle version.
- Docker, for the LocalStack specs (`LocalStackS3UploadTest`, `LocalStackS3DownloadTest`), which start a LocalStack container through Testcontainers.
- Optional: AWS credentials, for the real-AWS specs (`AwsS3UploadTest`, `AwsS3DownloadTest`). They create and delete a temporary `gradle-s3-plugin-test-<timestamp>` bucket in the account the credentials belong to.

## Building and testing

`./gradlew build` compiles the plugin, runs CodeNarc on the main and test sources, validates the plugin descriptor and runs every spec.

Without AWS credentials, run `./gradlew codenarcMain codenarcTest validatePlugins test --tests '*LocalStack*'` instead. In CI, the real-AWS specs are skipped automatically when no credentials are available, which is always the case for pull requests from forks.

The build rewrites the version in the README's setup snippet to the current project version. Do not commit that change; maintainers update the README as part of a release.

## Pull requests

- Branch from `master` and open the pull request against `master`.
- Keep each pull request to one change, and fill in the pull request template.
- Use a [Conventional Commits](https://www.conventionalcommits.org/) title, for example `fix: honour s3.profile for downloads`.
- Update the README when you change behaviour, add a task property or change a default.
- The "Build and Test PR" check must pass before review.

## Releases

Merging to `master` runs the "Build and Publish" workflow, which tags the release as `vX.Y.Z`, sets the next version and creates a GitHub Release with generated notes. Changes only to Markdown files or `.github/` do not trigger a release, and maintainers add `[skip ci]` to the merge commit for other changes that do not affect the published plugin. Publishing to the Gradle Plugin Portal is done by the maintainers.
