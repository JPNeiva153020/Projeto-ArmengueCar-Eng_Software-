@echo off
setlocal
chcp 65001 >nul
set "PROJECT=%USERPROFILE%\Downloads\Unit-2025\Projeto Ficina_Back_E_Front"
set "MAVEN=%USERPROFILE%\Downloads\IntelliJ IDEA 2026.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd"
if not exist "%PROJECT%\Backend\pom.xml" (echo Projeto nao encontrado em "%PROJECT%" & pause & exit /b 1)
if not exist "%MAVEN%" (echo Maven nao encontrado em "%MAVEN%" & pause & exit /b 1)
where python >nul 2>&1 || (echo Python nao encontrado no PATH. & pause & exit /b 1)
where java >nul 2>&1 || (echo Java nao encontrado no PATH. & pause & exit /b 1)
set /p "DB_PASSWORD=Senha do PostgreSQL para o usuario postgres: "
if not defined DB_PASSWORD (echo Senha nao informada. & pause & exit /b 1)
cd /d "%PROJECT%\Backend"
echo Compilando o backend...
call "%MAVEN%" package -DskipTests -q
if errorlevel 1 (echo Falha na compilacao. & pause & exit /b 1)
echo Iniciando backend e interface em janelas separadas...
start "ArmengueCar Backend" cmd /k "cd /d ""%PROJECT%\Backend"" && call ""%MAVEN%"" spring-boot:run"
start "ArmengueCar Interface" cmd /k "cd /d ""%PROJECT%\Frontend"" && python -m http.server 5500 --bind 127.0.0.1"
echo Aguardando a API iniciar por ate 90 segundos...
powershell -NoProfile -Command "$end=(Get-Date).AddSeconds(90); do { try { $c=New-Object Net.Sockets.TcpClient; $c.Connect('127.0.0.1',8080); $c.Close(); exit 0 } catch { Start-Sleep -Seconds 2 } } while ((Get-Date) -lt $end); exit 1"
if errorlevel 1 (echo API nao iniciou. Confira a janela ArmengueCar Backend para ver o erro. & pause & exit /b 1)
start "" "http://127.0.0.1:5500"
echo Para encerrar, feche as duas janelas do projeto.
pause
endlocal
