cd bin

mkdir ..\binEncode\org\hy\common\file\
mkdir ..\binEncode\org\hy\common\file\event\
mkdir ..\binEncode\org\hy\common\file\plugins\
mkdir ..\binEncode\org\hy\common\file\junit\

copy .\org\hy\common\file\*           ..\binEncode\org\hy\common\file\
copy .\org\hy\common\file\event\*     ..\binEncode\org\hy\common\file\event\
copy .\org\hy\common\file\plugins\*   ..\binEncode\org\hy\common\file\plugins\
copy .\org\hy\common\file\junit\Code* ..\binEncode\org\hy\common\file\junit\
cd ..\binEncode


jar cvfm code.jar MANIFEST.MF LICENSE org javax net
copy code.jar ..
del /q code.jar
