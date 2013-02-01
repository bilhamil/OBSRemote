/********************************************************************************
 Copyright (C) 2013 Hugh Bailey <obs.jim@gmail.com>
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

#include "OBSApi.h"
#include <stdio.h>
#include <fstream>
#include <vector> 

struct AccessListLine{
    AccessListLine();
    unsigned char addressbytes[4];
    unsigned char subnetmask[4];

    bool matches(unsigned char address[4]);

    void intitializeSubnetMask(UINT length);
};

class AccessList {
private:
    AccessList();
    std::vector<AccessListLine> lines;
public:

    static AccessList* createAccessList(char *hostfile);
    bool acceptHost(CTSTR host);
    
};
