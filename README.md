# Borbély-foglaló

Időpontfoglaló rendszer borbélyüzleteknek, szakdolgozati projekt.

- Backend: Spring Boot (Java 21)
- Frontend: Angular
- Adatbázis: PostgreSQL Dockerben

## Futtatás

Szükséges: JDK 21, Docker Desktop, Node.js.

1. Másold le a `.env.example` fájlt `.env` néven, és írd át benne a jelszót.
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