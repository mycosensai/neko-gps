@echo off
set ANDROID_SDK_ROOT=C:\Users\vdako\AppData\Local\Android\Sdk
set ANDROID_HOME=C:\Users\vdako\AppData\Local\Android\Sdk
"%ANDROID_SDK_ROOT%\emulator\emulator.exe" -avd neko -no-snapshot -no-audio -no-boot-anim -gpu swiftshader_indirect -accel on -memory 2048 > C:\Users\vdako\neko-gps\native-android\emulator.log 2>&1
