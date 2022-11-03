#!/bin/sh

mvn install:install-file -Dfile=hy.common.file.jar                              -DpomFile=./src/main/resources/META-INF/maven/cn.openapis/hy.common.file/pom.xml
mvn install:install-file -Dfile=hy.common.file-sources.jar -Dclassifier=sources -DpomFile=./src/main/resources/META-INF/maven/cn.openapis/hy.common.file/pom.xml

mvn deploy:deploy-file   -Dfile=hy.common.file.jar                              -DpomFile=./src/main/resources/META-INF/maven/cn.openapis/hy.common.file/pom.xml -DrepositoryId=thirdparty -Durl=http://HY-ZhengWei:1481/repository/thirdparty
mvn deploy:deploy-file   -Dfile=hy.common.file-sources.jar -Dclassifier=sources -DpomFile=./src/main/resources/META-INF/maven/cn.openapis/hy.common.file/pom.xml -DrepositoryId=thirdparty -Durl=http://HY-ZhengWei:1481/repository/thirdparty
