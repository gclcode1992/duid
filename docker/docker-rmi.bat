@echo off
chcp 65001 >nul
setlocal EnableExtensions
cd /d "%~dp0.."

rem Remove old duid-server image: stop containers, rmi, prune dangling layers
set "IMAGE_NAME=duid-server"
set "IMAGE_TAG=1.0.0"
if defined DUID_IMAGE_TAG set "IMAGE_TAG=%DUID_IMAGE_TAG%"
if exist .env (
  for /f "usebackq tokens=1,* delims==" %%A in (`findstr /b /c:"DUID_IMAGE_TAG=" ".env" 2^>nul`) do (
    if not "%%B"=="" set "IMAGE_TAG=%%B"
  )
)

set "IMAGE=%IMAGE_NAME%:%IMAGE_TAG%"
set "COMPOSE_SINGLE=docker\docker-compose.yml"
set "COMPOSE_MULTI=docker\docker-compose.multi.yml"

echo ========================================
echo  Remove old Docker image: %IMAGE%
echo ========================================
echo.

where docker >nul 2>&1
if errorlevel 1 (
  echo [ERROR] docker not found in PATH
  exit /b 1
)

echo [1/3] Stopping compose containers...
if exist .env (
  docker compose -f %COMPOSE_SINGLE% --env-file .env down --remove-orphans 2>nul
  docker compose -f %COMPOSE_MULTI% --env-file .env down --remove-orphans 2>nul
) else (
  docker compose -f %COMPOSE_SINGLE% down --remove-orphans 2>nul
  docker compose -f %COMPOSE_MULTI% down --remove-orphans 2>nul
)

echo [2/3] Removing image %IMAGE% ...
docker image inspect %IMAGE% >nul 2>&1
if errorlevel 1 (
  echo       Image not found, skip: %IMAGE%
) else (
  docker rmi %IMAGE%
  if errorlevel 1 (
    echo       Normal remove failed, trying force...
    docker rmi -f %IMAGE%
    if errorlevel 1 (
      echo [ERROR] Cannot remove %IMAGE%, check if still in use
      exit /b 1
    )
  )
  echo       Removed: %IMAGE%
)

echo [3/3] Pruning dangling image layers...
docker image prune -f

echo.
echo Done.
exit /b 0
