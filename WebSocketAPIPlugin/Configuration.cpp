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

extern "C" __declspec(dllexport) void ConfigPlugin(HWND);

HINSTANCE hinstMain = NULL;

INT_PTR CALLBACK ConfigDialogProc(HWND hwnd, UINT message, WPARAM wParam, LPARAM lParam)
{
	switch(message)
    {
        case WM_COMMAND:
			switch(LOWORD(wParam))
			{
				case IDOK:
					//thePlugin->ApplyConfig(hwnd);
                    break;

				case IDCANCEL:
					EndDialog(hwnd, LOWORD(wParam));
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