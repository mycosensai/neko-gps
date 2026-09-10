@echo off
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot
set ANDROID_SDK_ROOT=C:\Users\vdako\AppData\Local\Android\Sdk
set ANDROID_HOME=C:\Users\vdako\AppData\Local\Android\Sdk
call "%ANDROID_SDK_ROOT%\cmdline-tools\latest\bin\sdkmanager.bat" --list_installed
echo LIST_RC=%ERRORLEVEL%
