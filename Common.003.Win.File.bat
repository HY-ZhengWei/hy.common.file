

del /Q hy.common.file.jar
del /Q hy.common.file-sources.jar


call mvn clean package
cd .\target\classes


rd /s/q .\org\hy\common\file\junit


jar cvfm hy.common.file.jar META-INF/MANIFEST.MF META-INF org

copy hy.common.file.jar ..\..
del /q hy.common.file.jar
cd ..\..





cd .\src\main\java
xcopy /S ..\resources\* .
jar cvfm hy.common.file-sources.jar META-INF\MANIFEST.MF META-INF org 
copy hy.common.file-sources.jar ..\..\..
del /Q hy.common.file-sources.jar
rd /s/q META-INF
cd ..\..\..

pause
