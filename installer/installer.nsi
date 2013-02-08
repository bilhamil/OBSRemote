; Script generated with the Venis Install Wizard

; Define your application name
!define APPNAME "OBS Remote"
!define APPNAMEANDVERSION "OBS Remote 1.0"

; Additional script dependencies
!include WinVer.nsh
!include x64.nsh

; Main Install settings
Name "${APPNAMEANDVERSION}"
InstallDir "$PROGRAMFILES\OBS Remote"
InstallDirRegKey HKLM "Software\${APPNAME}" ""
OutFile "InstallOBSRemote.exe"

; Modern interface settings
!include "MUI.nsh"

!define MUI_ABORTWARNING
!define MUI_FINISHPAGE_RUN "$INSTDIR\OBS.exe"

!define MUI_PAGE_CUSTOMFUNCTION_LEAVE PreReqCheck

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE "gplv2.txt"
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_UNPAGE_CONFIRM

; Set languages (first is default language)
!insertmacro MUI_LANGUAGE "English"
!insertmacro MUI_RESERVEFILE_LANGDLL

Function PreReqCheck
	; Check to see if OBS is Installed
	ReadRegStr $0 HKLM "Software\Open Broadcaster Software" ""
	
	${If} $0 == "";
		MessageBox MB_OK|MB_ICONSTOP "${APPNAME} requires that Open Broadcaster Software be already installed."
		Quit
	${Else}
		strCpy $INSTDIR "$0"
	${EndIf}
FunctionEnd

Section "OBS Remote" Section1

	; Set Section properties
	SetOverwrite on

	; Set Section Files and Shortcuts
	SetOutPath "$INSTDIR\plugins\"
	File "..\WebSocketAPIPlugin\Output\Win32\Release\WebSocketAPIPlugin.dll"
	
	${if} ${RunningX64}
		SetOutPath "$INSTDIR\64bit\plugins\"
		File "..\WebSocketAPIPlugin\Output\x64\Release\WebSocketAPIPlugin.dll"
		SetOutPath "$INSTDIR\64bit\plugins\WebSocketAPIPlugin\"
		File /r "..\WebClient\"
	${endif}
	
	SetOutPath "$INSTDIR\plugins\WebSocketAPIPlugin\"
	File /r "..\WebClient\"
	
SectionEnd

Section -FinishSection

	WriteRegStr HKLM "Software\${APPNAME}" "" "$INSTDIR"
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayName" "${APPNAME}"
	WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "UninstallString" "$INSTDIR\UninstallOBSRemote.exe"
	WriteUninstaller "$INSTDIR\UninstallOBSRemote.exe"

SectionEnd

; Modern install component descriptions
!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
	!insertmacro MUI_DESCRIPTION_TEXT ${Section1} ""
!insertmacro MUI_FUNCTION_DESCRIPTION_END

;Uninstall section
Section Uninstall

	;Remove from registry...
	DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}"
	DeleteRegKey HKLM "SOFTWARE\${APPNAME}"

	; Delete self
	Delete "$INSTDIR\UninstallOBSRemote.exe"

	; Delete Shortcuts

	; Clean up OBS Remote
	Delete "$INSTDIR\plugins\WebSocketAPIPlugin.dll"
	RMDir /r "$INSTDIR\plugins\WebSocketAPIPlugin"
	
	${if} ${RunningX64}
		Delete "$INSTDIR\64bit\plugins\WebSocketAPIPlugin.dll"
		RMDir /r "$INSTDIR\64bit\plugins\WebSocketAPIPlugin"
	${endif}
	
SectionEnd

; eof