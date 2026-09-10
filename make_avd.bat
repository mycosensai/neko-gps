@echo off
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot
set ANDROID_SDK_ROOT=C:\Users\vdako\AppData\Local\Android\Sdk
set ANDROID_HOME=C:\Users\vdako\AppData\Local\Android\Sdk
echo no | call "%ANDROID_SDK_ROOT%\cmdline-tools\latest\bin\avdmanager.bat" create avd -n neko -k "system-images;android-34;google_apis;x86_64" -d pixel --force
echo AVD_RC=%ERRORLEVEL%
dir "%USERPROFILE%\.android\avd" 2>nul
call "%ANDROID_SDK_ROOT%\extras\google\Android_Emulator_Hypervisor_Driver\silent_install.bat"
echo AEHD_RC=%ERRORLEVEL%
