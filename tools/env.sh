# Окружение сборки: source tools/env.sh
export JAVA_HOME="$(ls -d "$HOME"/.local/opt/jdk-21* 2>/dev/null | head -1)"
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
