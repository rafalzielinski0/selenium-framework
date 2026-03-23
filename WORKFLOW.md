# Codzienny workflow – selenium-framework

## Środowisko

System: **Bazzite** (Fedora Atomic), IDE: **VS Code** (natywny, `/usr/bin/code`)
Java 21 pochodzi z Linuxbrew – jest w PATH po skonfigurowaniu `.bashrc` (Sesja 2).

---

## Rano – otwórz środowisko

```bash
cd ~/projekty/selenium-framework
code .
```

Alternatywnie otwórz projekt przez VS Code: **File → Open Folder**.

---

## Praca z asystentem Claude Code

```bash
claude
```

Uruchom z katalogu projektu. Claude widzi cały kod, historię gita i może uruchamiać testy.

---

## Uruchamianie testów podczas developmentu

```bash
# Jeden test class
./gradlew test --tests "tests.LoginTest"

# Jeden konkretny test
./gradlew test --tests "tests.LoginTest.successfulLogin"

# Wszystkie testy
./gradlew test

# Czysty run przed pushem (usuwa cache, wyniki i kompilację)
./gradlew clean test
```

W VS Code możesz też użyć **Ctrl+Shift+B** → task `test`.

---

## Podgląd raportu Allure

```bash
./gradlew allureServe
```

Serwer startuje i wypisuje link w terminalu – skopiuj do przeglądarki:
```
Server started at <http://192.168.0.18:PORT/>. Press <Ctrl+C> to exit
```

> **Uwaga:** przeglądarka nie otwiera się automatycznie (brak wsparcia `java.awt.Desktop`
> dla Wayland bez DISPLAY). Link kopiuj ręcznie.

Statyczny raport HTML (bez serwera):
```bash
./gradlew allureReport
# raport: build/reports/allure-report/allureReport/index.html
```

---

## Git workflow – branch i PR

Każda zmiana idzie przez branch, nie bezpośrednio na `main`.

```bash
# 1. Utwórz branch z opisową nazwą
git checkout -b feat/nazwa-testu

# 2. Pisz testy, commituj przyrostowo
git add src/test/java/tests/NazwaTest.java
git commit -m "feat: opis co dodajesz"

# 3. Push brancha na GitHub
git push origin feat/nazwa-testu

# 4. Otwórz PR przez gh CLI
gh pr create --title "feat: opis" --body "Co i dlaczego"

# 5. CI odpali testy automatycznie – po zielonym merge:
gh pr merge --merge --delete-branch
```

Po merge do `main` GitHub Actions automatycznie:
- odpala wszystkie testy
- generuje raport Allure
- publikuje raport na https://rafalzielinski0.github.io/selenium-framework

> **Nie rób** `git add .` bez sprawdzenia `git status` – możesz przypadkowo dodać
> pliki tymczasowe lub lokalne konfiguracje.

---

## Rozszerzenia – kolejne kroki dla frameworka

### 1. Retry logic dla niestabilnych testów

Prawdziwy retry (ponów przy failurze, nie powtórz zawsze) wymaga zewnętrznej biblioteki:

```
Dodaj do projektu bibliotekę junit-pioneer i użyj @RetryingTest(3)
do oznaczania niestabilnych testów. Skonfiguruj w taki sposób,
żeby Allure raportował każdą próbę osobno.
```

> **Uwaga:** `@RepeatedTest` z JUnit5 to NIE jest retry – powtarza test zawsze,
> niezależnie od wyniku. Do wznawiania po failurze potrzebny jest `@RetryingTest`
> z junit-pioneer lub własny `TestExecutionExceptionHandler`.

### 2. Rozszerzenie BaseTest

Mamy już `BaseTest` z `@BeforeAll` (konfiguracja Selenide) i `@AfterEach` (zamknięcie
przeglądarki). Brakuje:

```
Rozszerz BaseTest o:
- @BeforeEach który czyści cookies i localStorage między testami
- TestWatcher który robi screenshot przy każdym failurze i dołącza
  go do raportu Allure z nazwą testu w nazwie pliku
- Logowanie nazwy testu na starcie (@BeforeEach) dla czytelniejszego output
```

### 3. Testy równoległe

```
Skonfiguruj równoległe wykonanie testów w JUnit5:
- dodaj src/test/resources/junit-platform.properties
  z junit.jupiter.execution.parallel.enabled=true
  i junit.jupiter.execution.parallel.config.fixed.parallelism=4
- upewnij się że Page Objecty są thread-safe (brak stanu statycznego)
- sprawdź czy Selenide.closeWebDriver() w @AfterEach działa poprawnie
  w trybie równoległym
```

### 4. Matrix strategy – Chrome i Firefox w CI

```
Dodaj do GitHub Actions matrix strategy:
- matrix: browser: [chrome, firefox]
- każda przeglądarka jako osobny job
- wyniki obu jobów zebrane w jednym raporcie Allure
  (upload-artifact z różnymi nazwami, merge przed allureReport)
- firefox zainstalowany na ubuntu-latest jest dostępny natywnie
```
