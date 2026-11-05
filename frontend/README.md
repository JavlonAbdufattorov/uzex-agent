# UZEX Chat: фронтенд (React + Vite)

## Куда положить
Распакуйте архив так, чтобы папка `frontend/` лежала рядом с `pom.xml`:

    ~/uzex-agent/
    ├── pom.xml
    ├── data/
    ├── uzex.duckdb
    ├── src/main/...
    └── frontend/        <-- сюда

## Установка (Arch Linux)
    sudo pacman -S nodejs npm
    cd ~/uzex-agent/frontend
    npm install

## Разработка (два терминала)
    # терминал 1: Spring Boot
    cd ~/uzex-agent && ./mvnw spring-boot:run
    # терминал 2: фронт с горячей перезагрузкой
    cd ~/uzex-agent/frontend && npm run dev
Откройте http://localhost:5173. Запросы /api/* проксируются на :8080, CORS настраивать не нужно.

## Продакшн (один сервис, одна ссылка)
    cd ~/uzex-agent/frontend && npm run build
Сборка попадает в `src/main/resources/static`, и Spring Boot отдаёт её на `/`.
Затем `./mvnw clean package` и деплой одного jar.

## Контракт API (меняется в начале src/App.jsx)
    POST /api/ask
    { "question": "..." }
    ->
    { "answer": "...", "sql": "...", "rows": [ {...} ] }   // sql и rows необязательны

## Логотип
Файл `public/uzex-logo.svg` временная заглушка. Положите настоящий логотип UZEX
с тем же именем (svg или png: тогда поменяйте путь в компоненте Logo) и пересоберите.
Фирменные цвета меняются в переменных `:root` в `src/styles.css`.

## Cloudflare Pages + локальный Spring Boot

Фронтенд можно разместить на Cloudflare Pages, оставив Spring Boot и DuckDB на своём компьютере.
Компьютер должен быть включён, а бэкенд запущен. Для доступа к нему извне нужен Cloudflare Tunnel.

1. Создайте Pages-проект из Git-репозитория: **Root directory** — `frontend`, **Build command** — `CLOUDFLARE_PAGES=1 npm run build`, **Build output directory** — `dist`.
2. В переменных сборки Pages задайте `VITE_API_BASE_URL` как публичный HTTPS-адрес бэкенда, например `https://api.example.com`.
3. Запустите Spring Boot локально. Затем установите `cloudflared` и создайте Tunnel к `http://localhost:8080`. Для быстрой проверки можно выполнить `cloudflared tunnel --url http://localhost:8080`; Quick Tunnel выдаст временный адрес `https://…trycloudflare.com`.
4. Для Quick Tunnel укажите выданный адрес в `VITE_API_BASE_URL` и пересоберите Pages. Для постоянной работы настройте именованный Tunnel и собственный поддомен.
5. При запуске бэкенда задайте `FRONTEND_ORIGIN` равным точному адресу Pages, например `https://my-app.pages.dev`. `GEMINI_API_KEY` остаётся только на компьютере с бэкендом и не должен добавляться в переменные фронтенда.

Без `VITE_API_BASE_URL` фронтенд продолжает использовать `/api` и локальный Vite-прокси. Обычная сборка `npm run build` без `CLOUDFLARE_PAGES=1` по-прежнему кладёт файлы в Spring Boot `static`.

**Безопасность:** публичный Tunnel делает API доступным из интернета; CORS сам по себе не является авторизацией. Не запускайте его с конфиденциальными данными без защиты доступа и ограничения запросов.
