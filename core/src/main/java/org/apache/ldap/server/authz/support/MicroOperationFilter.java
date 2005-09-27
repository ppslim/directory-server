/*
 *   @(#) $Id$
 *   
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.ldap.server.authz.support;

import java.util.Collection;
import java.util.Iterator;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.ldap.common.aci.ACITuple;
import org.apache.ldap.common.aci.AuthenticationLevel;
import org.apache.ldap.common.aci.MicroOperation;
import org.apache.ldap.server.interceptor.NextInterceptor;

public class MicroOperationFilter implements ACITupleFilter
{
    public Collection filter( Collection tuples, OperationScope scope, NextInterceptor next, Name userGroupName, Name userName, Attributes userEntry, AuthenticationLevel authenticationLevel, Name entryName, String attrId, Object attrValue, Attributes entry, Collection microOperations ) throws NamingException
    {
        if( tuples.size() == 0 )
        {
            return tuples;
        }

        for( Iterator i = tuples.iterator(); i.hasNext(); )
        {
            ACITuple tuple = ( ACITuple ) i.next();
            boolean retain = false;
            for( Iterator j = microOperations.iterator(); j.hasNext(); )
            {
                MicroOperation microOp = ( MicroOperation ) j.next();
                if( tuple.getMicroOperations().contains( microOp ) )
                {
                    retain = true;
                    break;
                }
            }
            
            if( !retain )
            {
                i.remove();
            }
        }
        
        return tuples;
    }

}
