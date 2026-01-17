# DArbiter
Data trust, eligibility & AI readiness

## Structure
- `backend`: Spring Boot API for scanning, detection, and reporting
- `frontend`: React UI for intake, scan configuration, and results

## Run Backend
```bash
cd backend
mvn spring-boot:run
```

## Run Frontend
```bash
cd frontend
npm install
npm run dev
```

Backend listens on `http://localhost:8080`. Frontend defaults to `http://localhost:5173`.
