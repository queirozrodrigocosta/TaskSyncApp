@echo off
echo Gerando SHA-1 para o TaskSync...
echo.

REM Verificar se JAVA_HOME está configurado
if "%JAVA_HOME%"=="" (
    echo JAVA_HOME não encontrado. Tente encontrar Java automaticamente...
    for %%i in (java.exe) do set JAVA_PATH=%%~$PATH:i
    if "%JAVA_PATH%"=="" (
        echo Java não encontrado. Por favor, instale JDK ou configure JAVA_HOME.
        pause
        exit /b 1
    )
    set KEYTOOL_PATH=%JAVA_PATH:java.exe=keytool.exe%
) else (
    set KEYTOOL_PATH=%JAVA_HOME%\bin\keytool.exe
)

echo Usando: %KEYTOOL_PATH%
echo.

REM Verificar se o keystore existe
if not exist "%USERPROFILE%\.android\debug.keystore" (
    echo Keystore não encontrado. Criando novo...
    "%KEYTOOL_PATH%" -genkey -v -keystore "%USERPROFILE%\.android\debug.keystore" -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
)

echo Gerando SHA-1...
echo.
"%KEYTOOL_PATH%" -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -storepass android
echo.
pause
