#!/bin/sh

cd ./bin

rm -R ./org/hy/common/file/junit

jar cvfm hy.common.file.jar MANIFEST.MF META-INF org

cp hy.common.file.jar ..
rm hy.common.file.jar
cd ..

