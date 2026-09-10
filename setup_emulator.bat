@echo off
set SDKROOT=C:\Users\vdako\AppData\Local\Android\Sdk
echo y | "%SDKROOT%\cmdline-tools\latest\cmdline-tools\bin\sdkmanager.bat" --sdk_root="%SDKROOT%" "emulator" "system-images;android-34;google_apis;x86_64"
echo SDK_SETUP_DONE
