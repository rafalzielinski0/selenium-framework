# selenium-framework

[![Tests](https://github.com/rafalzielinski0/selenium-framework/actions/workflows/tests.yml/badge.svg)](https://github.com/rafalzielinski0/selenium-framework/actions/workflows/tests.yml)

UI test framework oparty na Java 21, Selenide i JUnit 5 z raportami Allure.

## Stack

| Narzędzie | Wersja |
|---|---|
| Java | 21 |
| Selenide | 7.14.0 |
| JUnit 5 | 5.11.4 |
| Allure | 2.30.0 |
| Gradle | 8.12.1 |

## Uruchomienie testów

```bash
# Testy lokalnie
./gradlew test

# Raport Allure (generuje + otwiera serwer lokalny)
./gradlew allureServe

# Raport jako statyczny HTML
./gradlew allureReport
```

## Raport Allure

Po każdym pushu do `main` raport jest publikowany automatycznie na GitHub Pages:
`https://rafalzielinski0.github.io/selenium-framework`
