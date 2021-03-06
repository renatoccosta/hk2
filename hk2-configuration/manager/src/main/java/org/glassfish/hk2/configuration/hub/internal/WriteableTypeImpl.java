/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.hk2.configuration.hub.internal;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.glassfish.hk2.configuration.hub.api.Change;
import org.glassfish.hk2.configuration.hub.api.WriteableType;
import org.glassfish.hk2.utilities.reflection.ClassReflectionHelper;
import org.glassfish.hk2.utilities.reflection.internal.ClassReflectionHelperImpl;

/**
 * @author jwells
 *
 */
public class WriteableTypeImpl implements WriteableType {
    private final WriteableBeanDatabaseImpl parent;
    private final String name;
    private final HashMap<String, Object> beanMap = new HashMap<String, Object>();
    private final ClassReflectionHelper helper;
    
    /* package */ WriteableTypeImpl(WriteableBeanDatabaseImpl parent, TypeImpl mother) {
        this.parent = parent;
        this.name = mother.getName();
        beanMap.putAll(mother.getInstances());
        helper = mother.getHelper();
    }
    
    /* package */ WriteableTypeImpl(WriteableBeanDatabaseImpl parent, String name) {
        this.parent = parent;
        this.name = name;
        helper = new ClassReflectionHelperImpl();
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.configuration.hub.api.Type#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.configuration.hub.api.Type#getInstances()
     */
    @Override
    public synchronized Map<String, Object> getInstances() {
        return Collections.unmodifiableMap(beanMap);
    }
    
    /* (non-Javadoc)
     * @see org.glassfish.hk2.configuration.hub.api.Type#getInstance(java.lang.Object)
     */
    @Override
    public synchronized Object getInstance(String key) {
        return beanMap.get(key);
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.configuration.hub.api.WriteableType#addInstance(java.lang.Object, java.lang.Object)
     */
    @Override
    public synchronized void addInstance(String key, Object bean) {
        if (key == null || bean == null) throw new IllegalArgumentException();
        
        parent.addChange(new ChangeImpl(Change.ChangeCategory.ADD_INSTANCE,
                                   this,
                                   key,
                                   bean,
                                   null));
        
        beanMap.put(key, bean);
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.configuration.hub.api.WriteableType#removeInstance(java.lang.Object)
     */
    @Override
    public synchronized Object removeInstance(String key) {
        if (key == null) throw new IllegalArgumentException();
        
        Object removedValue = beanMap.remove(key);
        if (removedValue == null) return null;
        
        parent.addChange(new ChangeImpl(Change.ChangeCategory.REMOVE_INSTANCE,
                this,
                key,
                removedValue,
                null));
        
        return removedValue;
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.configuration.hub.api.WriteableType#modifyInstance(java.lang.Object, java.lang.Object, java.beans.PropertyChangeEvent[])
     */
    @Override
    public synchronized PropertyChangeEvent[] modifyInstance(String key, Object newBean,
            PropertyChangeEvent... propChanges) {
        if (key == null || newBean == null) throw new IllegalArgumentException();
        
        Object oldBean = beanMap.get(key);
        if (oldBean == null) {
            throw new IllegalStateException("Attempting to modify bean with key " + key + " but no such bean exists");
        }
        
        if (propChanges.length == 0) {
            propChanges = BeanReflectionHelper.getChangeEvents(helper, oldBean, newBean);
        }
        
        beanMap.put(key, newBean);

        ArrayList<PropertyChangeEvent> propChangesList = new ArrayList<PropertyChangeEvent>(propChanges.length);
        for (PropertyChangeEvent pce : propChanges) {
            propChangesList.add(pce);
        }
        
        parent.addChange(new ChangeImpl(Change.ChangeCategory.MODIFY_INSTANCE,
                this,
                key,
                newBean,
                propChangesList));
        
        return propChanges;
    }

    ClassReflectionHelper getHelper() {
        return helper;
    }

}
