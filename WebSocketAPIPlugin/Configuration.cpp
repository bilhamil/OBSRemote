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

#include "WebSocketMain.h"
#include "Configuration.h"
#include "jansson.h"
#include "polarssl/sha2.h"
#include "polarssl/base64.h"

extern "C" __declspec(dllexport) void ConfigPlugin(HWND);

HINSTANCE hinstMain = NULL;
Config *config;

void Config::setAuth(bool _useAuth, const char *path)
{

}

void Config::save(const char *path)
{

}

void Config::load(const char *path)
{
    json_error_t error;
    json_t* json = json_load_file(path, 0, &error);

    if(!json) {
        /*unable to load config file*/
        this->useAuth = false;
        this->authHash = 0;
        this->authSalt = 0;
    }

}

INT_PTR CALLBACK ConfigDialogProc(HWND hwnd, UINT message, WPARAM wParam, LPARAM lParam)
{
	switch(message)
    {
        case WM_INITDIALOG:
        {
            HWND editBox = GetDlgItem(hwnd, IDC_AUTH_EDIT);
            SetWindowText(editBox, TEXT("asdfasdf"));
        }
        case WM_COMMAND:
			switch(LOWORD(wParam))
			{
				case IDOK:
                    {
                        TCHAR buff[1024];
                        HWND editBox = GetDlgItem(hwnd, IDC_AUTH_EDIT);
                        GetWindowText(editBox, buff, 1024);
                        
                        size_t wcharLen = slen(buff);
                        size_t curLength = (UINT)wchar_to_utf8_len(buff, wcharLen, 0);
                        char *utf8Pass = (char *) malloc((curLength+1));
                        wchar_to_utf8(buff,wcharLen, utf8Pass, curLength + 1, 0);
                        utf8Pass[curLength] = 0;

                        HWND checkBox = GetDlgItem(hwnd, ID_USEPASSWORDAUTH);
                        bool useAuth = SendMessage(checkBox, BM_GETCHECK, 0, 0) == BST_CHECKED;
                        
                        config->setAuth(useAuth, utf8Pass);
                        config->save("");

                        free(utf8Pass);
                    }
                    break;

				case IDCANCEL:
					EndDialog(hwnd, LOWORD(wParam));
                case ID_USEPASSWORDAUTH:
                    {
                        if(HIWORD(wParam) == BN_CLICKED)
                        {
                            bool useAuth = SendMessage((HWND)lParam, BM_GETCHECK, 0, 0) == BST_CHECKED;

                            HWND editBox = GetDlgItem(hwnd, IDC_AUTH_EDIT);
                            EnableWindow(editBox, useAuth);
                        }
                    }
                    break;

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