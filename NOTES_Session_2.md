# Notatki z sesji 2 – migracja projektu do VS Code

## Kontekst

System: **Bazzite** (Fedora Atomic – immutable Linux, oparte na rpm-ostree)
Sesja 1: projekt budowany w **IntelliJ IDEA** (zainstalowanym jako Flatpak przez JetBrains Toolbox)
Sesja 2: migracja do **VS Code** + uruchomienie testów bezpośrednio na hoście

---

## Dlaczego migrowaliśmy z IntelliJ do VS Code

Główna przyczyna: **IntelliJ zainstalowany przez JetBrains Toolbox na Bazzite działa jako Flatpak** (`com.jetbrains.IntelliJ-IDEA-Community`). Flatpak izoluje aplikację od systemu hosta – co w przypadku narzędzia developerskiego jest poważnym problemem.

Skutki izolacji Flatpak w przypadku IntelliJ:

- **Terminal IntelliJ nie widzi narzędzi hosta** – `podman`, `docker` i inne systemowe narzędzia są niedostępne z terminala wbudowanego w IDE
- **Zmienne środowiskowe z `.bash_profile` nie są widoczne** – Flatpak nie sourcuje profilu użytkownika
- **Chrome jako Flatpak + IntelliJ jako Flatpak = podwójna warstwa izolacji** – szczególnie trudna do debugowania (patrz Sesja 1: problemy z DevToolsActivePort)
- Każda próba dostępu do zasobów hosta wymagała osobnych `flatpak override --user ...` i obejść

Decyzja: przejść na IDE, które **nie jest izolowane od systemu**.

---

## Jak był zainstalowany IntelliJ – i dlaczego to był problem

### Metoda instalacji: JetBrains Toolbox → Flatpak

JetBrains Toolbox to menedżer IDE od JetBrains. Na **Bazzite/Fedora Atomic** (system immutable) naturalnym sposobem instalacji aplikacji graficznych są Flatpaki – i Toolbox instaluje IntelliJ właśnie w ten sposób.

Rezultat: IntelliJ działa jako `com.jetbrains.IntelliJ-IDEA-Community` w pełnym sandboxie Flatpak.

### Konkretne problemy z tej instalacji (Sesja 1)

| Problem | Przyczyna (izolacja Flatpak) |
|---|---|
| Chrome niedostępny dla ChromeDriver | Chrome jako Flatpak – brak w `/usr/bin`, tylko w sandboxie |
| DevToolsActivePort nie istnieje | Chrome ma prywatną `/tmp` niewidoczną dla ChromeDriver na hoście |
| `podman: nie znaleziono polecenia` | Terminal IntelliJ (Flatpak) nie widzi Podmana z hosta |
| `/run/host/usr/bin/podman` nie działa | Niekompatybilne biblioteki Flatpak vs host (`libsubid.so.5`) |

### Jak byłoby lepiej zainstalować IntelliJ – rekomendacje

**Opcja 1 (najlepsza na Bazzite): Distrobox / Toolbox container**
```bash
distrobox create --name dev --image fedora:41
distrobox enter dev
# wewnątrz kontenera:
sudo dnf install jetbrains-idea-community
# eksportuj aplikację na pulpit hosta:
distrobox-export --app idea
```
Efekt: IntelliJ działa w kontenerze Fedory, ale **widzi cały system hosta** przez montowanie `/home`. Brak izolacji Flatpak przy pełnej integracji z IDE. Terminal w IntelliJ = terminal Fedory z dostępem do wszystkich narzędzi.

**Opcja 2: rpm-ostree (natywna instalacja systemowa)**
```bash
rpm-ostree install jetbrains-idea-community
# wymagany restart systemu
```
IntelliJ zainstalowany jako pakiet systemowy – brak izolacji, pełny dostęp do hosta. Minusy: wolniejsze aktualizacje, wymaga restartu po każdej zmianie.

**Opcja 3: Archiwum tar.gz (ręczna instalacja)**
```bash
tar -xzf ideaIC-*.tar.gz -C ~/.local/share/JetBrains/
~/.local/share/JetBrains/idea-*/bin/idea
```
Stary, sprawdzony sposób – brak izolacji, działa na każdym Linuxie.

**Opcja 4: Dev Container (najczystsza)**
IntelliJ (nawet jako Flatpak) łączy się z kontenerem deweloperskim przez plugin Dev Containers. Terminal IDE = sesja w kontenerze z natywnym Chrome, Gradlem i wszystkimi narzędziami. Opisane szczegółowo w Sesji 1.

> **Wniosek:** Na Bazzite/Fedora Atomic **nigdy nie instaluj IDE przez Flatpak jeśli IDE ma być narzędziem do uruchamiania testów, terminalów lub kontenerów**. Flatpak jest dobry dla aplikacji użytkowych (przeglądarka, edytor tekstu), nie dla środowisk developerskich.

---

## Jak zainstalowany jest VS Code – i dlaczego to rozwiązało problemy

### Metoda instalacji: rpm-ostree (systemowy pakiet)

```bash
# VS Code pochodzi z oficjalnego repozytorium Microsoft,
# dodanego do systemu i zainstalowanego przez rpm-ostree:
rpm-ostree install code
```

Na Bazzite VS Code jest dostępny pod `/usr/bin/code` – jako **natywny pakiet systemowy**, nie Flatpak. Oznacza to:

- Terminal VS Code = normalny bash użytkownika – ma dostęp do wszystkich narzędzi hosta
- Zmienne środowiskowe i PATH z `.bashrc` / `.bash_profile` działają normalnie
- `podman`, narzędzia systemowe, skrypty wrapperowe – wszystko dostępne

### Co to zmieniło w praktyce

| Co nie działało w IntelliJ (Flatpak) | Status w VS Code (natywny) |
|---|---|
| Terminal widzi Podmana | ✓ Działa |
| Terminal widzi Chrome wrapper (`~/bin/google-chrome`) | ✓ Działa |
| Zmienne z `.bash_profile` są widoczne | ✓ Działa |
| Brak podwójnej izolacji Flatpak | ✓ Brak problemu |

---

## Problemy napotkane podczas migracji (Sesja 2)

### Problem 1: Java nie jest w PATH

**Objaw:** `./gradlew` nie uruchamia się – `java: nie znaleziono polecenia`

**Przyczyna:**
Java (zainstalowana przez Linuxbrew) jest dodana do PATH tylko w `.bash_profile`:
```bash
# ~/.bash_profile
export PATH="/home/linuxbrew/.linuxbrew/opt/openjdk@21/bin:$PATH"
```
VS Code (podobnie jak większość terminali graficznych) uruchamia **non-login shell** – sourcuje tylko `.bashrc`, nie `.bash_profile`. Przez to Java była niewidoczna.

**Rozwiązanie – dwa poziomy:**

*Poziom 1: `.bashrc`* – dla każdego terminala na hoście:
```bash
# ~/.bashrc (dodano na końcu)
export PATH="/home/linuxbrew/.linuxbrew/opt/openjdk@21/bin:$PATH"
export JAVA_HOME="/home/linuxbrew/.linuxbrew/opt/openjdk@21/libexec"
```

*Poziom 2: `.vscode/tasks.json`* – dla zadań VS Code (niezależnie od konfiguracji shellu):
```json
"options": {
  "env": {
    "JAVA_HOME": "/home/linuxbrew/.linuxbrew/opt/openjdk@21/libexec",
    "PATH": "/home/linuxbrew/.linuxbrew/opt/openjdk@21/bin:${env:PATH}"
  }
}
```

> **Wniosek:** Konfiguracja PATH w `.bash_profile` jest niewystarczająca dla aplikacji graficznych. Zawsze dodawaj krytyczne wpisy PATH **również do `.bashrc`**.

---

### Problem 2: Zahardcodowana wersja Java w `settings.json`

**Objaw:**
`.vscode/settings.json` wskazywał na ścieżkę z numerem wersji:
```
/home/linuxbrew/.linuxbrew/Cellar/openjdk@21/21.0.10/libexec
```

**Problem:** Po aktualizacji Javy (np. `21.0.10` → `21.0.11`) ścieżka przestaje istnieć – VS Code przestaje rozpoznawać JDK.

**Rozwiązanie:** Użycie stabilnego symlinku Linuxbrew (`opt/`):
```
/home/linuxbrew/.linuxbrew/opt/openjdk@21/libexec
```
Symlink `opt/openjdk@21` zawsze wskazuje na aktualnie zainstalowaną wersję i nie zmienia się po aktualizacjach.

---

### Problem 3: Deprecated `$buildDir` w Gradle 8

**Objaw:** Warning podczas każdego builda:
```
Deprecated Gradle features were used in this build,
making it incompatible with Gradle 9.0.
```

**Przyczyna:**
W `build.gradle.kts` (workaround na bug allure-gradle) używany był przestarzały `$buildDir`:
```kotlin
file("$buildDir/copy-categories").deleteRecursively()  // deprecated
```

**Rozwiązanie:**
```kotlin
layout.buildDirectory.dir("copy-categories").get().asFile.deleteRecursively()
```

> **Wniosek:** Gradle 8 deprecuje `$buildDir` na rzecz `layout.buildDirectory`. Zawsze używaj Provider API (`layout.buildDirectory`) zamiast bezpośredniej właściwości.

---

### Problem 4: allureServe nie otwiera przeglądarki automatycznie

**Objaw:**
```
Browse operation is not supported on your platform.
java.lang.UnsupportedOperationException:
  The BROWSE action is not supported on the current platform!
```

**Przyczyna:**
Allure CLI próbuje otworzyć przeglądarkę przez `java.awt.Desktop.browse()` – API, które na Linuxie bez aktywnej sesji graficznej dostępnej dla JVM (Wayland + brak DISPLAY) nie działa.

**Rozwiązanie:**
Serwer **uruchamia się poprawnie** mimo błędu – link do raportu pojawia się w terminalu:
```
Server started at <http://192.168.0.18:PORT/>. Press <Ctrl+C> to exit
```
Należy skopiować link ręcznie do przeglądarki.

---

## Wyniki testów po migracji

```
HomePageTest > homePageHeadingIsVisible()  PASSED
LoginTest > successfulLogin()              PASSED
LoginTest > wrongPasswordShowsError()      PASSED
LoginTest > emptyFieldsShowValidationError() PASSED

BUILD SUCCESSFUL in 10s
```

Wszystkie 4 testy przechodzą na hoście, bez kontenera, z Chrome przez Flatpak wrapper.

---

## Podsumowanie: IntelliJ vs VS Code na Bazzite

| | IntelliJ (Flatpak/Toolbox) | VS Code (rpm-ostree) |
|---|---|---|
| Instalacja | JetBrains Toolbox → Flatpak | `rpm-ostree install code` |
| Izolacja od hosta | Pełna (sandbox Flatpak) | Brak – natywny pakiet |
| Terminal widzi narzędzia hosta | ✗ (podman, brew, etc. niedostępne) | ✓ |
| Chrome Flatpak z testami | Wymaga wielu obejść | Wymaga tych samych obejść |
| Java w PATH | Wymaga dodatkowych override | Wymaga `.bashrc` (nie `.bash_profile`) |
| Integracja z Gradle | Dobra (plugin Gradle) | Dobra (rozszerzenie Gradle) |
| Integracja z JUnit 5 | Natywna, bardzo dobra | Dobra (rozszerzenie vscjava) |
| Allure raport | Identyczne komendy Gradle | Identyczne komendy Gradle |
| Dev Container support | ✓ (plugin Dev Containers) | ✓ (natywne wsparcie) |

---

## Wnioski na przyszłość

1. **Na Bazzite/Fedora Atomic nie instaluj IDE jako Flatpak** jeśli ma ono uruchamiać testy, kontenery lub inne narzędzia systemowe. Flatpak jest dobre dla aplikacji użytkowych, nie deweloperskich.

2. **Zawsze konfiguruj PATH w `.bashrc`, nie tylko `.bash_profile`** – aplikacje graficzne (VS Code, terminale) uruchamiają non-login shell i nie sourcują `.bash_profile`.

3. **Używaj stabilnych symlinków Linuxbrew** (`opt/package-name`) zamiast ścieżek wersjonowanych (`Cellar/package-name/x.y.z`) w plikach konfiguracyjnych projektu.

4. **VS Code na Bazzite = rpm-ostree** – daje pełny dostęp do hosta, brak problemów z izolacją.

5. **Jeśli chcesz IntelliJ** na Bazzite bez problemów: użyj Distrobox lub Dev Container zamiast Flatpak/Toolbox.

6. **Gradle `$buildDir` jest deprecated od wersji 8** – używaj `layout.buildDirectory`.
