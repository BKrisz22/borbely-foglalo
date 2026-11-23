# Borbély-foglaló

Időpontfoglaló rendszer borbélyüzleteknek, szakdolgozati projekt.

- Backend: Spring Boot (Java 21)
- Frontend: Angular
- Adatbázis: PostgreSQL Dockerben

## Futtatás

Szükséges: JDK 21, Docker Desktop, Node.js.

1. Másold le a `.env.example` fájlt `.env` néven, és töltsd ki:
   - `POSTGRES_PASSWORD`: az adatbázis jelszava
   - `JWT_SECRET`: legalább 32 bájtos, Base64 kódolású kulcs, például PowerShellben:
     `$b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)`
   - `OWNER_EMAIL`, `OWNER_PASSWORD`: a tulajdonosi fiók adatai (a jelszó 8–64 karakter, betűvel és számmal)
2. Indítsd el az adatbázist:
   ```
   docker compose up -d
   ```
3. Indítsd el a backendet:
   ```
   cd backend
   .\mvnw.cmd spring-boot:run
   ```
   Címe: http://localhost:8080
   Az első indításkor létrejön a tulajdonosi fiók a `.env` adataival.
4. Indítsd el a frontendet:
   ```
   cd frontend
   npm install
   npm start
   ```
   Címe: http://localhost:4200 (az `/api` kéréseket a fejlesztői szerver továbbítja a backendnek, lásd `frontend/proxy.conf.json`).

## Bemutató adatok

A backend `demo` profillal indítva üres adatbázisba bemutató adatokat tölt: szolgáltatásokat, borbélyokat, vendégeket, három hét munkaidőt és néhány foglalást.

```
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Minden bemutató fiók jelszava `Demo1234`, például `vilma@example.com` (vendég) és `kovacs.peter@example.com` (borbély). Ha az adatbázisban már van szolgáltatás vagy borbély, nem tölt be semmit; tiszta induláshoz `docker compose down -v`, majd `docker compose up -d`.

A `backend/http` mappában vannak példakérések az API kipróbálásához.

## Tesztek

```
cd backend
.\mvnw.cmd test
```

```
cd frontend
npm test
```