@echo off
setlocal enabledelayedexpansion

set "ROOT_DIR=%~dp0"
set "MODS_DIR=%ROOT_DIR%run\mods"

echo ===== Aegis Neo Launcher =====
echo.

REM Check if updater jar is in run/mods
set "HAS_UPDATER=0"
dir "%MODS_DIR%\AegisUpdater*.jar" >nul 2>nul
if %errorlevel% equ 0 set "HAS_UPDATER=1"

if %HAS_UPDATER% equ 0 (
    echo [INFO] Updater jar not found in run/mods
    if not exist "%MODS_DIR%" mkdir "%MODS_DIR%"
    echo.
    echo [INFO] Running build first...
    call "%ROOT_DIR%aegis-build.bat"
    if %errorlevel% neq 0 (
        echo [FAIL] Build failed!
        pause
        exit /b 1
    )
    echo.
    echo [INFO] Copying updater jar to run/mods...
    for %%f in ("%ROOT_DIR%DonePacks\AegisUpdater*.jar") do (
        copy /Y "%%f" "%MODS_DIR%" >nul
        echo      %%~nxf
    )
    echo [OK]
) else (
    echo [OK] Updater jar found in run/mods
)
echo.

cd /d "%ROOT_DIR%"

echo [1/2] clean compileJava...
call gradlew clean compileJava
if %errorlevel% neq 0 (
    echo [FAIL] clean compileJava failed!
    pause
    exit /b 1
)
echo [OK]
echo.

echo [2/2] runclient...
call gradlew runclient
echo.
pause
