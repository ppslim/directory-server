/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.mitosis.operation.support;


import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.directory.server.core.entry.ServerEntryUtils;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.NamespaceTools;
import org.apache.directory.shared.ldap.util.StringTools;
import org.apache.directory.mitosis.common.CSN;
import org.apache.directory.mitosis.common.Constants;
import org.apache.directory.mitosis.common.DefaultCSN;


public class EntryUtil
{
    @SuppressWarnings("unchecked")
    public static boolean isEntryUpdatable( PartitionNexus nexus, LdapDN name, CSN newCSN ) throws NamingException
    {
        Attributes entry = nexus.lookup( new LookupOperationContext( name ) );

        if ( entry == null )
        {
            return true;
        }

        Attribute entryCSNAttr = entry.get( Constants.ENTRY_CSN );

        if ( entryCSNAttr == null )
        {
            return true;
        }
        else
        {
            CSN oldCSN = null;

            try
            {
                Object val = entryCSNAttr.get();
                
                if ( val instanceof byte[] )
                {
                    oldCSN = new DefaultCSN( StringTools.utf8ToString( (byte[])val ) );
                }
                else
                {
                    oldCSN = new DefaultCSN( (String)val );
                }
            }
            catch ( IllegalArgumentException e )
            {
                return true;
            }

            return oldCSN.compareTo( newCSN ) < 0;
        }
    }


    public static void createGlueEntries( Registries registries, PartitionNexus nexus, LdapDN name, boolean includeLeaf )
        throws NamingException
    {
        assert name.size() > 0;

        for ( int i = name.size() - 1; i > 0; i-- )
        {
            createGlueEntry( registries, nexus, ( LdapDN ) name.getSuffix( i ) );
        }

        if ( includeLeaf )
        {
            createGlueEntry( registries, nexus, name );
        }
    }


    private static void createGlueEntry( Registries registries, PartitionNexus nexus, LdapDN name ) throws NamingException
    {
        try
        {
            if ( nexus.hasEntry( new EntryOperationContext( name ) ) )
            {
                return;
            }
        }
        catch ( NameNotFoundException e )
        {
            // Skip if there's no backend associated with the name.
            return;
        }

        // Create a glue entry.
        Attributes entry = new AttributesImpl( true );
        
        //// Add RDN attribute. 
        String rdn = name.get( name.size() - 1 );
        String rdnAttribute = NamespaceTools.getRdnAttribute( rdn );
        String rdnValue = NamespaceTools.getRdnValue( rdn );
        entry.put( rdnAttribute, rdnValue );
        
        //// Add objectClass attribute. 
        Attribute objectClassAttr = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT );
        objectClassAttr.add( SchemaConstants.TOP_OC );
        objectClassAttr.add( SchemaConstants.EXTENSIBLE_OBJECT_OC );
        entry.put( objectClassAttr );

        // And add it to the nexus.
        nexus.add( new AddOperationContext( registries, name, ServerEntryUtils.toServerEntry( entry, name, registries ) ) );
    }


    private EntryUtil()
    {
    }
}
