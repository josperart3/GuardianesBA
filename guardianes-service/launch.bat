@echo off

set mavenInput="%*"
set "JAVA_HOME=D:\Programas\jdk1.8.0_351"
rem set "JAVA_HOME=C:\Program Files\Java\jdk1.8.0_311"

if "%*" == "" (
	echo No Maven arguments skipping maven build
) else (
	echo Running with user input: %mavenInput%
	echo Running maven build on available project

	call mvn -v >con

	cd ..

	for %%s in ("-model" "-kjar" "guardianes-service") do (

			cd *%%s
			echo ===============================================================================
            for %%I in (.) do echo %%~nxI
            echo ===============================================================================

			if exist "%M3_HOME%\bin\mvn.bat" (
				call %M3_HOME%\bin\mvn.bat %* >con
			) else (
				call mvn %* >con
			)

			cd ..

	)
)

goto :startapp

:startapp
	if not x%mavenInput:docker=%==x%mavenInput% (
		echo Launching the application as docker container...
		call docker run -d -p 8090:8090 --name guardianes-service apps/guardianes-service:1.0-SNAPSHOT
	) else if not x%mavenInput:openshift=%==x%mavenInput% (
		echo Launching the application on OpenShift...
		call oc new-app guardianes-service:1.0-SNAPSHOT
		call oc expose svc/guardianes-service
	) else (
		echo "Launching the application locally..."
		setlocal EnableDelayedExpansion
		cd guardianes-service
		cd target
		for /f "delims=" %%x in ('dir /od /b *.jar') do set latestjar=%%x
		cd ..
		rem call java -Dorg.kie.server.bypass.auth.user=true -Dorg.kie.server.pwd=guardianes -Dorg.kie.server.user=guardianes -jar target\!latestjar!
		call java -jar target\!latestjar!
	)
:end
