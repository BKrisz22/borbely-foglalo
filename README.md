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
   Címe: http://localhost:4200

A `backend/http` mappában vannak példakérések az API kipróbálásához.

## Tesztek

```
cd backend
.\mvnw.cmd test
```