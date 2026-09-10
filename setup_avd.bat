@echo off
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot
set SDKROOT=C:\Users\vdako\AppData\Local\Android\Sdk
echo no | "%SDKROOT%\cmdline-tools\latest\cmdline-tools\bin\avdmanager.bat" --sdk_root="%SDKROOT%" create avd -n neko -k "system-images;android-34;google_apis;x86_64" -d "pixel_7" --force
echo AVD_DONE
echo y | "%SDKROOT%\cmdline-tools\latest\cmdline-tools\bin\sdkmanager.bat" --sdk_root="%SDKROOT%" "extras;google;Android_Emulator_Hypervisor_Driver"
echo AEHD_DONE
