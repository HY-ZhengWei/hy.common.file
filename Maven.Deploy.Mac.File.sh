#!/bin/sh

mvn deploy:deploy-file -Dfile=hy.common.file.jar                              -DpomFile=./src/META-INF/maven/org/hy/common/file/pom.xml -DrepositoryId=thirdparty -Durl=http://HY-ZhengWei:1481/repository/thirdparty
mvn deploy:deploy-file -Dfile=hy.common.file-sources.jar -Dclassifier=sources -DpomFile=./src/META-INF/maven/org/hy/common/file/pom.xml -DrepositoryId=thirdparty -Durl=http://HY-ZhengWei:1481/repository/thirdparty
