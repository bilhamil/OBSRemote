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


#include "AccessList.h"

AccessList::AccessList()
{
}

AccessList* AccessList::createAccessList(char *hostfile)
{
    std::ifstream fin;
    fin.open(hostfile);
    char line[256];
    if(!fin.good())
        return NULL;

    AccessList* list = new AccessList();

    while(fin.good())
    {
        fin.getline(line, 256);
        if(!fin.fail())
        {
            AccessListLine accessline;
            UINT subnetmaskLength = 0;
            
            /*comment line*/
            if(line[0] == '#')
            {
                continue;
            }
            UINT b[4] = {0, 0, 0, 0};
            int ret = sscanf(line,"%3u.%3u.%3u.%3u/%2u",&b[0], &b[1], &b[2], &b[3], &subnetmaskLength);
            if( ret == 4)
            {
                subnetmaskLength = 32;
            }
            else if( ret != 5)
            {
                continue;
            }
            accessline.addressbytes[0] = (unsigned char)b[0];
            accessline.addressbytes[1] = (unsigned char)b[1];
            accessline.addressbytes[2] = (unsigned char)b[2];
            accessline.addressbytes[3] = (unsigned char)b[3];

            accessline.intitializeSubnetMask(subnetmaskLength);

            list->lines.push_back(accessline);
        }
    }

    return list;
}

bool AccessList::acceptHost(CTSTR host)
{
    unsigned int address[4] = {0, 0, 0, 0};

    int ret = swscanf(host,TEXT("%3u.%3u.%3u.%3u"),
                &address[0], &address[1],
                &address[2], &address[3]);

    unsigned char caddress[4] = {0, 0, 0, 0};
    caddress[0] = (unsigned char)address[0];
    caddress[1] = (unsigned char)address[1];
    caddress[2] = (unsigned char)address[2];
    caddress[3] = (unsigned char)address[3];

    if(ret != 4)
    {
        return false;
    }

    bool foundMatch = false;

    for(UINT i = 0; i < lines.size(); i++)
    {
        if(lines[i].matches(caddress))
        {
            foundMatch = true;
        }
    }
    
    return foundMatch;
}

AccessListLine::AccessListLine()
{
}

void AccessListLine::intitializeSubnetMask(UINT length)
{
    UINT c = length;
    for(UINT i = 0; i < 4; i++)
    {
        if(c >= 8)
        {
            subnetmask[i] = 255;
            c -= 8;
        }
        else
        {
            subnetmask[i] = 0;
            while(c > 0)
            {
                subnetmask[i] >>= 1;
                subnetmask[i] |= 0x80;
                c--;
            }
        }
    }
}

bool AccessListLine::matches(unsigned char address[4])
{
    for(UINT i = 0; i < 4; i++)
    {
        if((this->addressbytes[i] & this->subnetmask[i]) != 
            (address[i] & this->subnetmask[i]))
        {
            return false;
        }
    }
    return true;
}