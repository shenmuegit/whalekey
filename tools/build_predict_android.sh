#!/bin/sh
set -eu
P="${1:?Pass the in-repository source/build directory}"
N="$(dirname "$PWD")/.android-sdk/ndk/28.0.13004108/toolchains/llvm/prebuilt/linux-x86_64"
B="$PWD/lib/fcitx5/src/main/cpp/prebuilt"
mkdir -p "$P/include/rime"
printf '#define RIME_ENABLE_LOGGING\n' > "$P/include/rime/build_config.h"
"$N/bin/x86_64-linux-android35-clang++" -DGLOG_USE_GLOG_EXPORT -std=c++17 -O2 -static-libstdc++ \
-I"$P/include" -I"$P/librime-de4700e9f6b75b109910613df907965e3cbe0567/src" -I"$P/librime-de4700e9f6b75b109910613df907965e3cbe0567/include" \
-I"$P/darts-clone-87b71afd6cf784953e3c08f24c64203397f3b724/include" -I"$P/librime-predict-920bd41ebf6f9bf6855d14fbe80212e54e749791/src" \
-I"$B/boost/include" -I"$B/marisa/x86_64/include" -I"$B/glog/x86_64/include" \
"$P/librime-predict-920bd41ebf6f9bf6855d14fbe80212e54e749791/tools/build_predict.cc" \
"$P/librime-predict-920bd41ebf6f9bf6855d14fbe80212e54e749791/src/predict_db.cc" \
"$B/librime/x86_64/lib/librime.a" "$B/marisa/x86_64/lib/libmarisa.a" "$B/glog/x86_64/lib/libglog.a" \
-llog -o "$P/build_predict-android"
