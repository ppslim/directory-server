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
package org.apache.directory.server.kerberos.shared.store.operations;


import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InvalidAttributeValueException;
import javax.naming.directory.SearchResult;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.SamType;
import org.apache.directory.server.kerberos.shared.store.KerberosAttribute;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntry;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntryModifier;
import org.apache.directory.server.protocol.shared.store.ContextOperation;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;


/**
 * Encapsulates the action of looking up a principal in an embedded ApacheDS DIT.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class GetPrincipal implements ContextOperation
{
    private static final long serialVersionUID = 4598007518413451945L;

    /** The name of the principal to get. */
    private final KerberosPrincipal principal;


    /**
     * Creates the action to be used against the embedded ApacheDS DIT.
     * 
     * @param principal The principal to search for in the directory.
     */
    public GetPrincipal( KerberosPrincipal principal )
    {
        this.principal = principal;
    }


    /**
     * Note that the base is a relative path from the existing context.
     * It is not a DN.
     */
    public Object execute( DirContext ctx, Name base )
    {
        if ( principal == null )
        {
            return null;
        }

        String[] attrIDs =
            {   KerberosAttribute.KRB5_PRINCIPAL_NAME_AT, 
                KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT, 
                KerberosAttribute.KRB5_KEY_AT,
                KerberosAttribute.APACHE_SAM_TYPE_AT, 
                KerberosAttribute.KRB5_ACCOUNT_DISABLED_AT,
                KerberosAttribute.KRB5_ACCOUNT_EXPIRATION_TIME_AT, 
                KerberosAttribute.KRB5_ACCOUNT_LOCKEDOUT_AT };

        Attributes matchAttrs = new AttributesImpl( true );
        matchAttrs.put( new AttributeImpl( KerberosAttribute.KRB5_PRINCIPAL_NAME_AT, principal.getName() ) );

        PrincipalStoreEntry entry = null;

        try
        {
            NamingEnumeration<SearchResult> answer = ctx.search( "", matchAttrs, attrIDs );

            if ( answer.hasMore() )
            {
                SearchResult result = answer.next();

                Attributes attrs = result.getAttributes();

                if ( attrs == null )
                {
                    return null;
                }

                String distinguishedName = result.getName();
                entry = getEntry( distinguishedName, attrs );
            }
        }
        catch ( NamingException e )
        {
            return null;
        }

        return entry;
    }


    /**
     * Marshals an a PrincipalStoreEntry from an Attributes object.
     *
     * @param dn the distinguished name of the Kerberos principal
     * @param attrs the attributes of the Kerberos principal
     * @return the entry for the principal
     * @throws NamingException if there are any access problems
     */
    private PrincipalStoreEntry getEntry( String distinguishedName, Attributes attrs ) throws NamingException
    {
        PrincipalStoreEntryModifier modifier = new PrincipalStoreEntryModifier();

        modifier.setDistinguishedName( distinguishedName );

        String principal = ( String ) attrs.get( KerberosAttribute.KRB5_PRINCIPAL_NAME_AT ).get();
        modifier.setPrincipal( new KerberosPrincipal( principal ) );

        String keyVersionNumber = ( String ) attrs.get( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT ).get();
        modifier.setKeyVersionNumber( Integer.parseInt( keyVersionNumber ) );

        if ( attrs.get( KerberosAttribute.KRB5_ACCOUNT_DISABLED_AT ) != null )
        {
            String val = ( String ) attrs.get( KerberosAttribute.KRB5_ACCOUNT_DISABLED_AT ).get();
            modifier.setDisabled( "true".equalsIgnoreCase( val ) );
        }

        if ( attrs.get( KerberosAttribute.KRB5_ACCOUNT_LOCKEDOUT_AT ) != null )
        {
            String val = ( String ) attrs.get( KerberosAttribute.KRB5_ACCOUNT_LOCKEDOUT_AT ).get();
            modifier.setLockedOut( "true".equalsIgnoreCase( val ) );
        }

        if ( attrs.get( KerberosAttribute.KRB5_ACCOUNT_EXPIRATION_TIME_AT ) != null )
        {
            String val = ( String ) attrs.get( KerberosAttribute.KRB5_ACCOUNT_EXPIRATION_TIME_AT ).get();
            try
            {
                modifier.setExpiration( KerberosTime.getTime( val ) );
            }
            catch ( ParseException e )
            {
                throw new InvalidAttributeValueException( "Account expiration attribute "
                    + KerberosAttribute.KRB5_ACCOUNT_EXPIRATION_TIME_AT + " contained an invalid value for generalizedTime: "
                    + val );
            }
        }

        if ( attrs.get( KerberosAttribute.APACHE_SAM_TYPE_AT ) != null )
        {
            String samType = ( String ) attrs.get( KerberosAttribute.APACHE_SAM_TYPE_AT ).get();
            modifier.setSamType( SamType.getTypeByOrdinal( Integer.parseInt( samType ) ) );
        }

        if ( attrs.get( KerberosAttribute.KRB5_KEY_AT ) != null )
        {
            Attribute krb5key = attrs.get( KerberosAttribute.KRB5_KEY_AT );
            try
            {
                Map<EncryptionType, EncryptionKey> keyMap = modifier.reconstituteKeyMap( krb5key );
                modifier.setKeyMap( keyMap );
            }
            catch ( IOException ioe )
            {
                throw new InvalidAttributeValueException( "Account Kerberos key attribute '" + KerberosAttribute.KRB5_KEY_AT
                    + "' contained an invalid value for krb5key." );
            }
        }

        return modifier.getEntry();
    }
}
