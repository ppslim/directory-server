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
package org.apache.directory.shared.kerberos.codec.transitedEncoding.actions;


import org.apache.directory.shared.asn1.ber.Asn1Container;
import org.apache.directory.shared.asn1.codec.actions.AbstractReadOctetString;
import org.apache.directory.shared.kerberos.codec.transitedEncoding.TransitedEncodingContainer;


/**
 * The action used to read the TransitedEncoding contents
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreContents extends AbstractReadOctetString
{
    /**
     * Instantiates a new StoreContents action.
     */
    public StoreContents()
    {
        super( "TransitedEncoding contents", true );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void setOctetString( byte[] data, Asn1Container container )
    {
        TransitedEncodingContainer transitedEncodingContainer = ( TransitedEncodingContainer ) container;
        transitedEncodingContainer.getTransitedEncoding().setContents( data );
        container.setGrammarEndAllowed( true );
    }
}