

cd .\bin

rd /s/q .\org\hy\common\file\junit

jar cvfm hy.common.file.jar MANIFEST.MF META-INF org

copy hy.common.file.jar ..
del /q hy.common.file.jar
cd ..

