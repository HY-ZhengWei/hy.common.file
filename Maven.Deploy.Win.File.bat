start mvn install:install-file -Dfile=hy.common.file.jar                              -DpomFile=./src/main/resources/META-INF/maven/cn.openapis/hy.common.file/pom.xml
start mvn install:install-file -Dfile=hy.common.file-sources.jar -Dclassifier=sources -DpomFile=./src/main/resources/META-INF/maven/cn.openapis/hy.common.file/pom.xml

start mvn deploy:deploy-file   -Dfile=hy.common.file.jar                              -DpomFile=./src/main/resources/META-INF/maven/cn.openapis/hy.common.file/pom.xml -DrepositoryId=thirdparty -Durl=http://HY-ZhengWei:8081/repository/thirdparty
start mvn deploy:deploy-file   -Dfile=hy.common.file-sources.jar -Dclassifier=sources -DpomFile=./src/main/resources/META-INF/maven/cn.openapis/hy.common.file/pom.xml -DrepositoryId=thirdparty -Durl=http://HY-ZhengWei:8081/repository/thirdparty
