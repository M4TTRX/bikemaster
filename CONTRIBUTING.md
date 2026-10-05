# Contributing to BikeMaster

Thank you for your interest in contributing to **BikeMaster**! Whether you are fixing a bug, adding new sensor profile support, improving battery life, or polishing UI, your help is appreciated.

---

## Code of Conduct

Please be respectful, constructive, and inclusive to all contributors and community members.

---

## How to Contribute

### 1. Reporting Bugs
- Search existing GitHub issues first to make sure the issue hasn't already been reported.
- When opening an issue, provide:
  - Device model, Android OS version, and Wear OS version (if applicable).
  - Hardware model of any connected BLE sensors (e.g., Garmin HRM-Dual, Wahoo Blue SC).
  - Clear steps to reproduce the issue.
  - Expected vs. actual behavior.
  - Relevant logcat snippets if available.

### 2. Suggesting Enhancements
- Open a feature request issue describing the feature and its intended use case for cyclists.
- If proposing UI modifications, include mockups or screenshots.

### 3. Submitting Pull Requests
- Fork the repository and create your branch from `main`:
  ```bash
  git checkout -b feature/my-cool-feature
  ```
- Write clear, concise commit messages.
- Ensure all existing unit tests pass before opening a PR:
  ```bash
  ./gradlew test
  ```
- Keep PRs focused on a single change or feature for easier review.

---

## Development & Code Style

- **Kotlin Best Practices**: Follow official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) and Android Compose guidelines.
- **Compose UI**:
  - Keep `@Composable` functions stateless where possible.
  - Optimize for zero unnecessary allocations in high-frequency telemetry loops (60Hz / 1Hz updates).
  - Preserve the `#000000` pitch black background and high contrast typography on OLED dashboards.
- **Sensors & Concurrency**:
  - Encapsulate BLE GATT callbacks cleanly inside Coroutine Flows.
  - Never block the Main / UI thread with sensor calculations or DataStore transactions.

---

## License

By contributing your code to the BikeMaster repository, you agree that your contributions will be licensed under the [Apache License, Version 2.0](LICENSE).
