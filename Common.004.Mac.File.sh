#!/bin/sh

cd ./bin

rm -R ./org/hy/common/file/junit

jar cvfm hy.common.file.jar MANIFEST.MF LICENSE org

cp hy.common.file.jar ..
rm hy.common.file.jar
cd ..

