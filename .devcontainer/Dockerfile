FROM eclipse-temurin:21-jdk-jammy

# ── System tools + Google Chrome + Node.js ───────────────────────────────────
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl gnupg ca-certificates git sudo \
    # Chrome stable
    && curl -fsSL https://dl.google.com/linux/linux_signing_key.pub \
        | gpg --dearmor -o /usr/share/keyrings/google-chrome.gpg \
    && echo "deb [arch=amd64 signed-by=/usr/share/keyrings/google-chrome.gpg] \
        https://dl.google.com/linux/chrome/deb/ stable main" \
        > /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update \
    && apt-get install -y google-chrome-stable \
    # Node.js 22 LTS (wymagany przez Claude Code)
    && curl -fsSL https://deb.nodesource.com/setup_22.x | bash - \
    && apt-get install -y nodejs \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# ── Użytkownik deweloperski ───────────────────────────────────────────────────
RUN groupadd -r developer \
    && useradd -r -g developer -m -d /home/developer -s /bin/bash developer \
    && echo "developer ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers

USER developer

# ── Claude Code CLI ───────────────────────────────────────────────────────────
RUN npm install -g @anthropic-ai/claude-code
