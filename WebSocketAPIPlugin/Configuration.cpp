/********************************************************************************
 Copyright (C) 2013 William Hamilton <bill@ecologylab.net>

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
********************************************************************************/

#include "Configuration.h"

extern "C" __declspec(dllexport) void ConfigPlugin(HWND);

HINSTANCE hinstMain = NULL;
Config *config = NULL;
LPSTR configPath = NULL;

#define DEFAULT_PASS_TEXT TEXT("asdfasdf")

LPSTR getPluginConfigPath()
{
    if(configPath == NULL)
    {
        String pluginPath = OBSGetPluginDataPath();
        pluginPath << TEXT("\\OBSRemote.xconfig");
        configPath = pluginPath.CreateUTF8String();
    }

    return configPath;
}

Config* getRemoteConfig()
{
    if(config == NULL)
    {
        config = new Config();
        havege_init(&(config->havegeState));

        config->load(getPluginConfigPath());
    }

    return config;
}

void Config::setAuth(bool _useAuth, const char *pass)
{
    size_t passLength = strlen(pass);

    this->useAuth = _useAuth;
    
    if(useAuth)
    {
        unsigned char salt[32];
        unsigned char salt64[64];
        size_t salt64Size = 64;
        havege_random((void*)&(this->havegeState), salt, 32);
        base64_encode(salt64, &salt64Size, salt, 32);
        salt64[salt64Size] = 0;

        int saltPlusPassSize = salt64Size + passLength;

        char* saltPlusPass = (char*)malloc(saltPlusPassSize);

        memcpy(saltPlusPass, pass, passLength);
        memcpy(saltPlusPass + passLength, salt64, salt64Size);

        unsigned char passHash[32];
        unsigned char passHash64[64];
        size_t passHash64Size = 64;

        sha2((unsigned char *)saltPlusPass, saltPlusPassSize, passHash, 0);

        zero(saltPlusPass, saltPlusPassSize);
        free(saltPlusPass);
        
        base64_encode(passHash64, &passHash64Size, passHash, 32);

        passHash64[passHash64Size] = 0;

        this->authHash = (char *)passHash64;
        this->authSalt = (char *)salt64;
    }
}

std::string Config::getChallenge()
{
    unsigned char challenge[32];
    unsigned char challenge64[64];
    size_t challenge64Size = 64;
    havege_random((void*)&(this->havegeState), challenge, 32);
    base64_encode(challenge64, &challenge64Size,challenge, 32);
    challenge64[challenge64Size] = 0;

    std::string ret = (char *) challenge64;

    return ret;
}

bool Config::checkChallengeAuth(const char *response, const char *challenge)
{
    size_t challengeLength = strlen(challenge);
    size_t authHashLength = this->authHash.length();
    size_t authPlusChallengeSize = authHashLength + challengeLength;
    char* authPlusChallenge = (char*)malloc(authPlusChallengeSize);

    /* concat authHash and challenge string */
    memcpy(authPlusChallenge, this->authHash.c_str(), authHashLength);
    memcpy(authPlusChallenge + authHashLength, challenge, challengeLength);

    unsigned char respHash[32];
    unsigned char respHash64[64];
    size_t respHash64Size = 64;

    /* hash concatenated authHash and string and base 64 encode */
    sha2((unsigned char *)authPlusChallenge, authPlusChallengeSize, respHash, 0);
    base64_encode(respHash64, &respHash64Size, respHash, 32);
    respHash64[respHash64Size] = 0;

    free(authPlusChallenge);

    /* compare against response */
    return strcmp((char*)respHash64, response) == 0;
}

void Config::save(const char *path)
{
    json_t* json = json_object();
    json_object_set_new(json, "useAuth", json_boolean(this->useAuth));

    if(this->useAuth)
    {
        json_object_set_new(json, "authHash", json_string(this->authHash.c_str()));
        json_object_set_new(json, "authSalt", json_string(this->authSalt.c_str()));
    }

    json_dump_file(json, path, JSON_INDENT(2));

    json_decref(json);
}

void Config::load(const char *path)
{
    json_error_t error;
    json_t* json = json_load_file(path, 0, &error);
        
    this->useAuth = false;
    this->authHash = "";
    this->authSalt = "";

    if(!json) {
		/* didn't find config file, setup default password */
		setAuth(true, OBS_REMOTE_DEFAULT_PASSWORD);
		save(path);
        return;        
    }

    json_t* jUseAuth = json_object_get(json, "useAuth");
    json_t* jAuthHash = json_object_get(json, "authHash");
    json_t* jAuthSalt = json_object_get(json, "authSalt");

    if(jUseAuth == NULL || !json_is_boolean(jUseAuth))
    {
        return;
    }

    if(json_typeof(jUseAuth) == JSON_FALSE)
    {
        return;
    }

    if(jAuthHash == NULL || !json_is_string(jAuthHash) ||
       jAuthSalt == NULL || !json_is_string(jAuthSalt))
    {
        /* couldn't get auth parameters */
        return;
    }

    this->useAuth = true;
    this->authHash = json_string_value(jAuthHash);
    this->authSalt = json_string_value(jAuthSalt);
}

INT_PTR CALLBACK ConfigDialogProc(HWND hwnd, UINT message, WPARAM wParam, LPARAM lParam)
{
	static bool edited = false;
	switch(message)
    {
        case WM_INITDIALOG:
        {
            Config* _config = getRemoteConfig();
            HWND checkBox = GetDlgItem(hwnd, ID_USEPASSWORDAUTH);
            SendMessage(checkBox, BM_SETCHECK, 
                (_config->useAuth)?BST_CHECKED:BST_UNCHECKED, 0);
            

            HWND editBox = GetDlgItem(hwnd, IDC_AUTH_EDIT);
            EnableWindow(editBox, _config->useAuth);
            SetWindowText(editBox, DEFAULT_PASS_TEXT);

			HWND okButton = GetDlgItem(hwnd, IDOK);
            EnableWindow(okButton, false);

			edited = false;
        }
        case WM_COMMAND:
			switch(LOWORD(wParam))
			{
				case IDOK:
                    {
                        TCHAR buff[1024];
                        HWND editBox = GetDlgItem(hwnd, IDC_AUTH_EDIT);
                        GetWindowText(editBox, buff, 1024);
                        
                        HWND checkBox = GetDlgItem(hwnd, ID_USEPASSWORDAUTH);
                        bool useAuth = SendMessage(checkBox, BM_GETCHECK, 0, 0) == BST_CHECKED;
                        
                        Config* _config = getRemoteConfig();

                        if(!useAuth || edited)
                        {
                            size_t wcharLen = slen(buff);
                            size_t curLength = (UINT)wchar_to_utf8_len(buff, wcharLen, 0);
                            char *utf8Pass = (char *) malloc((curLength+1));
                            wchar_to_utf8(buff,wcharLen, utf8Pass, curLength + 1, 0);
                            utf8Pass[curLength] = 0;

                            Config* _config = getRemoteConfig();
                            _config->setAuth(useAuth, utf8Pass);
                            _config->save(getPluginConfigPath());

                            free(utf8Pass);
                        }

						edited = false;
                        EndDialog(hwnd, LOWORD(wParam));
                    }
                    break;

				case IDCANCEL:
					EndDialog(hwnd, LOWORD(wParam));
					break;
                case ID_USEPASSWORDAUTH:
                    {
                        if(HIWORD(wParam) == BN_CLICKED)
                        {
                            bool useAuth = SendMessage((HWND)lParam, BM_GETCHECK, 0, 0) == BST_CHECKED;

                            HWND editBox = GetDlgItem(hwnd, IDC_AUTH_EDIT);
                            EnableWindow(editBox, useAuth);

							HWND okButton = GetDlgItem(hwnd, IDOK);
							EnableWindow(okButton, (edited && useAuth) || (!useAuth && useAuth != getRemoteConfig()->useAuth));
                        }
                    }
                    break;
				case IDC_AUTH_EDIT:
					{
						if(HIWORD(wParam) == EN_CHANGE)
                        {
							if(!edited)
							{
								edited = true;
								HWND okButton = GetDlgItem(hwnd, IDOK);
								EnableWindow(okButton, true);
							}
                        }
						break;
					}

			}
			break;
        case WM_CLOSE:
			EndDialog(hwnd, IDCANCEL);
            break;
    }

    return 0;
};

void ConfigPlugin(HWND hwnd)
{
    DialogBox(hinstMain, MAKEINTRESOURCE(IDD_CONFIGURE_OBS_REMOTE), hwnd, ConfigDialogProc);
}

BOOL CALLBACK DllMain(HINSTANCE hInst, DWORD dwReason, LPVOID lpBla)
{
	if(dwReason == DLL_PROCESS_ATTACH)
		hinstMain = hInst;

	return TRUE;
}