; Script generated with the Venis Install Wizard

; Define your application name
!define APPNAME "OBS Remote"
!define APPVERSION "1.2"
!define APPNAMEANDVERSION "OBS Remote ${APPVERSION}"

; Additional script dependencies
!include WinVer.nsh
!include x64.nsh

; Main Install settings
Name "${APPNAMEANDVERSION}"
InstallDir "$PROGRAMFILES\OBS Remote"
InstallDirRegKey HKLM "Software\${APPNAME}" ""
OutFile "OBS_Remote_${APPVERSION}_Installer.exe"

RequestExecutionLevel admin

; Include Firewall Plugin
!addplugindir "."

; Modern interface settings
!include "MUI.nsh"

!define MUI_ICON "..\WebClient\favicon.ico"

!define MUI_ABORTWARNING
!define MUI_FINISHPAGE_RUN "$INSTDIR\OBS.exe"

!define MUI_PAGE_CUSTOMFUNCTION_LEAVE PreReqCheck

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE "gplv2.txt"
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_INSTFILES

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

  ; Check for prior installation
  ReadRegStr $0 HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "UninstallString"
  
  ${If} $0 != "";
    ExecWait '"$0" /S _?=$INSTDIR'
    DetailPrint "Uninstalled Previous Version"
  ${EndIf}
  
  ; Set Section properties
  SetOverwrite on

  ; Set Section Files and Shortcuts
  SetOutPath "$INSTDIR\plugins\"
  File ".\OBSRemoteManualInstall\32-bit\WebSocketAPIPlugin.dll"
  
  ; Enable firewall port opening. Needs http://nsis.sourceforge.net/NSIS_Simple_Firewall_Plugin
  #SimpleFC::AddPort 4444 "OBS Remote" 6 3 2 "" 1
  SimpleFC::AdvAddRule "OBS Remote" "Allow OBS Remote Connections" "6" "1" "1" "2147483647" "1" "$INSTDIR\OBS.exe" "" "$INSTDIR\OBS.exe,-10000" "4444" "" "" ""
  
  ${if} ${RunningX64}
    SetOutPath "$PROGRAMFILES64\OBS\plugins\"
    File ".\OBSRemoteManualInstall\64-bit\WebSocketAPIPlugin.dll"

    SimpleFC::AdvAddRule "OBS Remote x64" "Allow OBS Remote Connections" "6" "1" "1" "2147483647" "1" "$PROGRAMFILES64\OBS\OBS.exe" "" "$PROGRAMFILES64\OBS\OBS.exe,-10000" "4444" "" "" ""

  ${endif}
  
  WriteUninstaller "$INSTDIR\UninstallOBSRemote.exe"
  CreateShortCut "$SMPROGRAMS\Open Broadcaster Software\UninstallOBSRemote.lnk" "$INSTDIR\UninstallOBSRemote.exe"
  
SectionEnd

Section -FinishSection

  WriteRegStr HKLM "SOFTWARE\${APPNAME}" "" "$INSTDIR"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayName" "${APPNAME}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "UninstallString" "$INSTDIR\UninstallOBSRemote.exe"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayVersion" "${APPVERSION}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayIcon" "$INSTDIR\OBS.exe,0"

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
  Delete "$SMPROGRAMS\Open Broadcaster Software\UninstallOBSRemote.lnk"

  ; Clean up OBS Remote
  Delete "$INSTDIR\plugins\WebSocketAPIPlugin.dll"
  
  ${if} ${RunningX64}
    Delete "$PROGRAMFILES64\OBS\plugins\WebSocketAPIPlugin.dll"
  ${endif}
  
  #SimpleFC::RemovePort 4444 6
  SimpleFC::AdvRemoveRule "OBS Remote"
  SimpleFC::AdvRemoveRule "OBS Remote x64"
  
SectionEnd

; eof