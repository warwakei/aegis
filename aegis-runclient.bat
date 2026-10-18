@echo off
setlocal enabledelayedexpansion

set "ROOT_DIR=%~dp0"
set "SRC_DIR=%ROOT_DIR%source"
set "MODS_DIR=%ROOT_DIR%mods"

echo ===== Aegis Neo RunClient =====
echo.

REM Рантайм-папки прямо рядом со скриптом (ран-директория loom = корень репо)
if not exist "%ROOT_DIR%mods" mkdir "%ROOT_DIR%mods"
if not exist "%ROOT_DIR%resourcepacks" mkdir "%ROOT_DIR%resourcepacks"
if not exist "%ROOT_DIR%shaders" mkdir "%ROOT_DIR%shaders"

REM Check if updater jar is in mods
set "HAS_UPDATER=0"
dir "%MODS_DIR%\AegisUpdater*.jar" >nul 2>nul
if not errorlevel 1 set "HAS_UPDATER=1"

if not "%HAS_UPDATER%"=="1" (
    echo [INFO] Updater jar not found in mods
    echo.
    echo [INFO] Running build first...
    call "%ROOT_DIR%source\aegis-build.bat"
    if errorlevel 1 (
        echo [FAIL] Build failed!
        pause
        exit /b 1
    )
    echo.
    echo [INFO] Copying updater jar to mods...
    for %%f in ("%ROOT_DIR%DonePacks\AegisUpdater*.jar") do (
        copy /Y "%%f" "%MODS_DIR%" >nul
        echo      %%~nxf
    )
    echo [OK]
) else (
    echo [OK] Updater jar found in mods
)
echo.

cd /d "%SRC_DIR%"

echo [1/2] clean compileJava...
call gradlew.bat clean compileJava
if %errorlevel% neq 0 (
    echo [FAIL] clean compileJava failed!
    pause
    exit /b 1
)
echo [OK]
echo.

echo [2/2] runclient...
call gradlew.bat runClient
echo.
pause
