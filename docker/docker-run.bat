@echo off
chcp 65001 >nul
setlocal EnableExtensions
cd /d "%~dp0.."

rem Start duid-server via docker/docker-compose.yml (single replica, profile=prod)
set "COMPOSE_FILE=docker\docker-compose.yml"
set "IMAGE_NAME=duid-server"
set "IMAGE_TAG=1.0.0"
if defined DUID_IMAGE_TAG set "IMAGE_TAG=%DUID_IMAGE_TAG%"
if exist .env (
  for /f "usebackq tokens=1,* delims==" %%A in (`findstr /b /c:"DUID_IMAGE_TAG=" ".env" 2^>nul`) do (
    if not "%%B"=="" set "IMAGE_TAG=%%B"
  )
)
set "IMAGE=%IMAGE_NAME%:%IMAGE_TAG%"

if /i "%~1"=="--multi" set "COMPOSE_FILE=docker\docker-compose.multi.yml"
if /i "%~1"=="multi" set "COMPOSE_FILE=docker\docker-compose.multi.yml"

echo ========================================
echo  Run Docker: %COMPOSE_FILE%
echo  Profile: prod
echo  Image: %IMAGE%
echo ========================================
echo.

where docker >nul 2>&1
if errorlevel 1 (
  echo [ERROR] docker not found in PATH
  exit /b 1
)

if not exist .env (
  echo [ERROR] .env not found. Copy and edit first:
  echo   copy .env.example .env
  exit /b 1
)

docker image inspect %IMAGE% >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Image not found: %IMAGE%
  echo         Run docker\docker-build.bat first.
  exit /b 1
)

if /i "%COMPOSE_FILE%"=="docker\docker-compose.multi.yml" (
  set "REPLICAS=3"
  for /f "usebackq tokens=1,* delims==" %%A in (`findstr /b /c:"DUID_REPLICAS=" ".env" 2^>nul`) do (
    if not "%%B"=="" set "REPLICAS=%%B"
  )
  echo [1/2] Starting multi replica scale=%REPLICAS%
  docker compose -f %COMPOSE_FILE% --env-file .env up -d --scale duid-server=%REPLICAS% --remove-orphans
) else (
  echo [1/2] Starting single replica...
  docker compose -f %COMPOSE_FILE% --env-file .env up -d --remove-orphans
)
if errorlevel 1 (
  echo [ERROR] docker compose up failed
  exit /b 1
)

echo [2/2] Status:
docker compose -f %COMPOSE_FILE% --env-file .env ps
echo.
echo Ready check:
echo   curl http://localhost:18086/ready
echo Logs:
echo   docker compose -f %COMPOSE_FILE% --env-file .env logs -f
echo.
echo Done.
exit /b 0
