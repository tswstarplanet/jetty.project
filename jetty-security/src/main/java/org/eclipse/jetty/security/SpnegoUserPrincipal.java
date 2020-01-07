//
//  ========================================================================
//  Copyright (c) 1995-2020 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.security;

import java.security.Principal;
import java.util.Base64;

public class SpnegoUserPrincipal implements Principal
{
    private final String _name;
    private byte[] _token;
    private String _encodedToken;

    public SpnegoUserPrincipal(String name, String encodedToken)
    {
        _name = name;
        _encodedToken = encodedToken;
    }

    public SpnegoUserPrincipal(String name, byte[] token)
    {
        _name = name;
        _token = token;
    }

    @Override
    public String getName()
    {
        return _name;
    }

    public byte[] getToken()
    {
        if (_token == null)
            _token = Base64.getDecoder().decode(_encodedToken);
        return _token;
    }

    public String getEncodedToken()
    {
        if (_encodedToken == null)
            _encodedToken = new String(Base64.getEncoder().encode(_token));
        return _encodedToken;
    }
}
