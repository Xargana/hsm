:: wasted so much time on this
@echo off
if exist "src\" (
:: Go to source folder
cd src
:: Check if server exists
where java >nul 2>nul
if %errorlevel%==0 (
    echo Java installation confirmed, running...
    if exist "Server.class" (
        title hsm Server
        echo Server script found, running...
        ::wait for 1 second
        :sleep
        ping 127.0.0.1 -n 2 -w 1000 > NUL
        ping 127.0.0.1 -n %1 -w 1000 > NUL
        :: Run client
        cls
        java Server
    ) else (
        echo Server.class does not exist, building...
        :sleep
        ping 127.0.0.1 -n 2 -w 1000 > NUL
        ping 127.0.0.1 -n %1 -w 1000 > NUL
        echo checking JDK 
        where javac >nul 2>nul
    if %errorlevel%==0 (
        echo JDK installation confirmed, Building..
        title Building...
        javac Client.java
        javac Server.java
    if exist "Client.class" (
        title hsm Server
        echo Server script found, running...
        :sleep
        ping 127.0.0.1 -n 2 -w 1000 > NUL
        ping 127.0.0.1 -n %1 -w 1000 > NUL
        :: Run client
        cls
        java Server
        else (
        :: Rage quit
        echo JDK is not installed. Please install JDK to continue.
        echo if you have JDK installed, please make sure it is in your PATH.
        pause
        exit 
    )
    )
    )
    )
)  else (
    echo source folder is missing, please reinstall the repository.
)
)
 
