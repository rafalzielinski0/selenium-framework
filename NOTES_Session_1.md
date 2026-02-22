# Notatki z budowy frameworka – problemy i rozwiązania

## Kontekst

System: **Bazzite** (Fedora Atomic – immutable Linux)
Środowisko pracy: **IntelliJ IDEA** zainstalowany jako **Flatpak**
To połączenie jest źródłem większości problemów opisanych poniżej.

---

## Problem 1: ChromeDriver nie może uruchomić Chrome

### Objaw
```
SessionNotCreatedException: Chrome instance exited
```

### Przyczyna
Chrome zainstalowany jest jako **Flatpak** (`com.google.Chrome`).
ChromeDriver szuka przeglądarki w standardowych ścieżkach systemowych (`/usr/bin/chrome` itp.).
Flatpakowy Chrome tam nie istnieje – jest izolowany w swoim sandboxie.

### Rozwiązanie – wrapper script
```bash
# ~/bin/google-chrome
#!/bin/bash
exec flatpak run com.google.Chrome "$@"
```
Plik umieszczony w `~/bin/` (który jest w PATH) sprawia, że ChromeDriver
"widzi" Chrome pod normalną ścieżką.

---

## Problem 2: ChromeDriver nie może się połączyć z Chrome (DevToolsActivePort)

### Objaw
```
SessionNotCreatedException: DevToolsActivePort file doesn't exist
```

### Przyczyna
Wrapper zadziałał – Chrome startuje – ale ChromeDriver nadal nie może się z nim
połączyć. Mechanizm komunikacji wygląda tak:

```
ChromeDriver (host)                Chrome (Flatpak sandbox)
       |                                    |
       | czeka na plik:                     |
       | /tmp/scoped_dir.../                |
       |   DevToolsActivePort               |
       |                         tworzy plik w swojej prywatnej /tmp
       |                         (niewidocznej dla hosta)
       X  nie znalazł → błąd
```

Flatpak daje każdej aplikacji **prywatną `/tmp`**. Chrome tworzy plik
`DevToolsActivePort` wewnątrz swojej `/tmp`, a ChromeDriver szuka go
w `/tmp` hosta – nigdy go nie znajdzie.

### Rozwiązanie – dwa kroki

**Krok 1:** Daj Chrome dostęp do katalogu domowego (jednorazowo):
```bash
flatpak override --user --filesystem=home com.google.Chrome
```

**Krok 2:** Wymuś `--user-data-dir` w katalogu domowym w `TestConfiguration.java`:
```java
//options.addArguments("--no-sandbox");
//options.addArguments("--user-data-dir=" + home + "/.chrome-wd-profile");
```

Teraz Chrome zapisuje `DevToolsActivePort` w `~/.chrome-wd-profile/` –
miejscu widocznym zarówno dla Chrome (Flatpak, ma dostęp do home)
jak i dla ChromeDriver (host).

```
ChromeDriver (host)                Chrome (Flatpak sandbox)
       |                                    |
       | czeka na plik:                     |
       | ~/.chrome-wd-profile/              |
       |   DevToolsActivePort               |
       |                         tworzy plik w ~/.chrome-wd-profile/
       |                         (home jest teraz dostępny)
       ✓  znalazł → połączenie OK
```

Flaga `--no-sandbox` jest wymagana ponieważ Chrome uruchamiany
wewnątrz sandboxa Flatpak nie może dodatkowo uruchomić własnego
sandboxa (podwójny sandbox = crash).

---

## Problem 3: Podman niedostępny z terminala IntelliJ

### Objaw
```
sh: podman: nie znaleziono polecenia
```
...podczas gdy w zwykłym terminalu systemowym `podman --version` działa.

### Przyczyna
**IntelliJ zainstalowany jako Flatpak** izoluje swój terminal od systemu hosta.
Flatpak tworzy własne środowisko z ograniczonym dostępem do narzędzi systemowych.
Podman jest zainstalowany na hoście, ale terminal IntelliJ go nie widzi.

Próba użycia wrappera (`/run/host/usr/bin/podman`) kończy się błędem:
```
error while loading shared libraries: libsubid.so.5: cannot open shared object file
```
Biblioteki hosta są niekompatybilne z bibliotekami środowiska Flatpak.

### Rozwiązanie zastosowane
Używamy Podmana z **zewnętrznego terminala systemowego** (poza IntelliJ).
Do codziennej pracy (testy, Allure) IntelliJ i tak nie potrzebuje Podmana –
kontener służy głównie do CI/CD.

### Rozwiązanie docelowe → Dev Container (patrz niżej)

---

## Problem 4: Bug w allure-gradle plugin 2.12.0

### Objaw
```
property 'markerFile' is not writable because
'build/copy-categories/copyCategories' is not a file
```

### Przyczyna
Plugin przy pierwszym uruchomieniu tworzy **katalog** `build/copy-categories/copyCategories/`
w miejscu gdzie Gradle spodziewa się **pliku**-znacznika. Przy kolejnych
uruchomieniach walidacja Gradle wykrywa konflikt i przerywa build.

Gradle waliduje outputy tasków **przed** wykonaniem `doFirst` – dlatego
próba usunięcia katalogu w `doFirst` nie działa.

### Rozwiązanie
Usunięcie katalogu w **fazie konfiguracji** (przed walidacją):
```kotlin
// build.gradle.kts – uruchamia się podczas konfiguracji, przed walidacją tasków
file("$buildDir/copy-categories").deleteRecursively()
```

---

## Dlaczego warto rozważyć Dev Container

### Co to jest
Dev Container to kontener z gotowym środowiskiem developerskim.
IntelliJ łączy się z tym kontenerem przez plugin –
piszesz kod normalnie w IDE, ale **terminal i wszystkie narzędzia działają
wewnątrz kontenera**.

### Jakie problemy z tej sesji by nie wystąpiły

| Problem | Z Dev Containerem |
|---|---|
| Chrome niedostępny (Flatpak) | Chrome zainstalowany natywnie w kontenerze |
| DevToolsActivePort (prywatna /tmp) | Brak Flatpak sandbox = brak problemu |
| Podman niedostępny w terminalu IntelliJ | Podman dostępny wewnątrz kontenera |
| Konfiguracja specyficzna dla maszyny | Każdy w zespole ma identyczne środowisko |

### Jak by to wyglądało w praktyce
```
IntelliJ IDEA (Flatpak)
    └── łączy się z → kontener dev
                          ├── Java 21
                          ├── Chrome (natywny)
                          ├── Gradle
                          ├── Claude Code CLI
                          └── Podman / Docker
```
Terminal w IntelliJ = sesja w kontenerze.
Kod edytujesz w IntelliJ, wszystkie komendy (`./gradlew test`, `claude`, `podman`)
działają bez żadnych ograniczeń Flatpak.

### Jak to skonfigurować (gdy zajdzie potrzeba)
Dodać do projektu plik `.devcontainer/devcontainer.json` oparty
na tym samym obrazie co `Containerfile` (już mamy w projekcie).
Plugin **Dev Containers** w IntelliJ obsługuje to natywnie.

### Czy warto teraz?
Wszystkie problemy zostały rozwiązane obejściami i środowisko działa.
Dev Container ma sens gdy:
- przenosisz projekt na nowy komputer
- pracujesz w zespole (identyczne środowisko dla wszystkich)
- napotkasz kolejne problemy wynikające z izolacji Flatpak
