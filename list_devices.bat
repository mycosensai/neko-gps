@echo off
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot
set SDKROOT=C:\Users\vdako\AppData\Local\Android\Sdk
"%SDKROOT%\cmdline-tools\latest\cmdline-tools\bin\avdmanager.bat" --sdk_root="%SDKROOT%" list device -c
