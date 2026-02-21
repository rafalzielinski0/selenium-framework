FROM eclipse-temurin:21-jdk-jammy

# ── Google Chrome stable via apt ─────────────────────────────────────────────
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl gnupg ca-certificates \
    && curl -fsSL https://dl.google.com/linux/linux_signing_key.pub \
        | gpg --dearmor -o /usr/share/keyrings/google-chrome.gpg \
    && echo "deb [arch=amd64 signed-by=/usr/share/keyrings/google-chrome.gpg] \
        https://dl.google.com/linux/chrome/deb/ stable main" \
        > /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update \
    && apt-get install -y google-chrome-stable \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# ── Użytkownik niebędący rootem ───────────────────────────────────────────────
# Chrome wymaga --no-sandbox gdy działa jako root – bezpieczniej użyć innego usera.
# --no-sandbox jest już skonfigurowane w TestConfiguration.java.
RUN groupadd -r testrunner \
    && useradd -r -g testrunner -m -d /home/testrunner -s /bin/bash testrunner

# ── Projekt ───────────────────────────────────────────────────────────────────
WORKDIR /app

COPY --chown=testrunner:testrunner . .

RUN chmod +x gradlew \
    && mkdir -p build/allure-results \
    && chown -R testrunner:testrunner /app/build

USER testrunner

# ── Cache: pobierz Gradle i zależności podczas budowania obrazu ───────────────
# Dzięki temu uruchomienie kontenera nie wymaga dostępu do internetu.
RUN ./gradlew dependencies --configuration testRuntimeClasspath --quiet

# ── Wyniki testów dostępne jako wolumin ───────────────────────────────────────
# Montuj: podman run -v ./results:/app/build/allure-results ...
VOLUME /app/build/allure-results

# ── Nadpisuje browserBinary z gradle.properties (tam jest ścieżka Flatpak) ───
ENTRYPOINT ["./gradlew", "test", "-PbrowserBinary=/usr/bin/google-chrome"]
