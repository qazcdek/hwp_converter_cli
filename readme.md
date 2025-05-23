# 폴더 구조
```
root/
├── convertorCLI/
│   ├── .gitignore
│   ├── HwpConverterCLI.java
│   └── readme.md
├── hwp_server/
│   ├── hwp2hwpx/
│   ├── hwplib/
│   └── hwpxlib/
├── src/
├── target/
├── .gitignore
├── ConvertMain.class
├── ConvertMain.java
├── pom.xml
└── readme.md
```

# compile
[windows]
javac -cp "..\hwp_server\hwp2hwpx\target\hwp2hwpx-1.0.0.jar;..\hwp_server\hwplib\target\hwplib-1.1.10.jar;..\hwp_server\hwpxlib\target\hwpxlib-1.0.5.jar" HwpxConverterCLI.java

[linux]
javac -cp "..\hwp_server\hwp2hwpx\target\hwp2hwpx-1.0.0.jar:..\hwp_server\hwplib\target\hwplib-1.1.10.jar:..\hwp_server\hwpxlib\target\hwpxlib-1.0.5.jar" HwpxConverterCLI.java
