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

#pragma once

#include <string>
#include "WebSocketMain.h"
#include "jansson.h"

#include "polarssl/havege.h"
#include "polarssl/sha2.h"
#include "polarssl/base64.h"

#define OBS_REMOTE_MAX_FAILED_AUTH_ATTEMPTS 10
#define OBS_REMOTE_DEFAULT_PASSWORD "admin"

class Config
{
public:
    bool useAuth;
    std::string authHash;
    std::string authSalt;
    havege_state havegeState;

    void setAuth(bool _useAuth, const char *auth);

    void save(const char *path);
    void load(const char *path);

    std::string getChallenge();
    bool checkChallengeAuth(const char* response, const char* challenge);    
};

Config* getRemoteConfig();
