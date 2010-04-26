/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core.partition.avl;


import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.directory.server.xdbm.AbstractXdbmPartition;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.search.impl.CursorBuilder;
import org.apache.directory.server.xdbm.search.impl.DefaultOptimizer;
import org.apache.directory.server.xdbm.search.impl.DefaultSearchEngine;
import org.apache.directory.server.xdbm.search.impl.EvaluatorBuilder;
import org.apache.directory.server.xdbm.search.impl.NoOpOptimizer;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ServerEntry;


/**
 * An XDBM Partition backed by in memory AVL Trees.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AvlPartition extends AbstractXdbmPartition<Long>
{
    private Set<AvlIndex<?, ServerEntry>> indexedAttributes;


    /**
     * Creates a store based on AVL Trees.
     */
    public AvlPartition()
    {
        super( new AvlStore<ServerEntry>() );
        indexedAttributes = new HashSet<AvlIndex<?, ServerEntry>>();
    }


    /**
     * {@inheritDoc}
     */
    protected void doInit() throws Exception
    {
        setSchemaManager( schemaManager );

        EvaluatorBuilder<Long> evaluatorBuilder = new EvaluatorBuilder<Long>( store, schemaManager );
        CursorBuilder<Long> cursorBuilder = new CursorBuilder<Long>( store, evaluatorBuilder );

        // setup optimizer and registries for parent
        if ( !optimizerEnabled )
        {
            optimizer = new NoOpOptimizer();
        }
        else
        {
            optimizer = new DefaultOptimizer<ServerEntry, Long>( store );
        }

        searchEngine = new DefaultSearchEngine<Long>( store, cursorBuilder, evaluatorBuilder, optimizer );

        if ( store.isInitialized() )
        {
            return;
        }

        // initialize the store
        store.setId( getId() );
        suffixDn.normalize( schemaManager.getNormalizerMapping() );
        store.setSuffixDn( suffixDn );

        Set<Index<?, ServerEntry, Long>> userIndices = new HashSet<Index<?, ServerEntry, Long>>();

        for ( AvlIndex<?, ServerEntry> index : indexedAttributes )
        {
            String oid = schemaManager.getAttributeTypeRegistry().getOidByName( index.getAttributeId() );
            if(!index.getAttributeId().equals( oid ))
            {
                index.setAttributeId( oid );
            }
            store.addIndex( index );
        }

        store.init( schemaManager );
    }


    /**
     * {@inheritDoc}
     */
    public final void modify( long entryId, List<Modification> modifications ) throws Exception
    {
        ( ( AvlStore<ServerEntry> ) store ).modify( entryId, modifications );
    }


    /*
     * TODO requires review 
     * 
     * This getter deviates from the norm. all the partitions
     * so far written never return a reference to store but I think that in this 
     * case the presence of this method gives significant ease and advantage to perform
     * add/delete etc. operations without creating a operation context.
     */
    public AvlStore<ServerEntry> getStore()
    {
        return ( AvlStore<ServerEntry> ) store;
    }

}
