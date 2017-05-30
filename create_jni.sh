cmake .
make
rm -f xgboost4j/src/main/resources
mkdir -p src/main/resources
cp out/libperfmap.so src/main/resources/

