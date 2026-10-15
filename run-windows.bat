@echo off
setlocal
where java >nul 2>nul
if errorlevel 1 (
  echo Java was not found. Install JDK 21 and add it to PATH.
  pause
  exit /b 1
)
java -version
where mvn >nul 2>nul
if errorlevel 1 (
  echo Maven was not found. Import this project into Eclipse as an Existing Maven Project,
  echo or install Maven 3.9+ and add it to PATH.
  pause
  exit /b 1
)
mvn spring-boot:run
