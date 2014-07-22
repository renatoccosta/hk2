/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.jvnet.hk2.internal;

import java.net.URL;

import org.glassfish.hk2.utilities.reflection.Pretty;

/**
 * A classloader that delegates first to an optional parent and then to a delegate loader
 */
class DelegatingClassLoader<T> extends ClassLoader {
	
	private final ClassLoader delegates[];

	DelegatingClassLoader(ClassLoader parent, ClassLoader... classLoaderDelegates) {
		super(parent);
        delegates=classLoaderDelegates;
    }
	
	@Override
	public Class<?> loadClass(String clazz)
			throws ClassNotFoundException {
		
		if (getParent() != null) {
			try {
				return getParent().loadClass(clazz);
			} catch (ClassNotFoundException cnfe) {}
		}

		ClassNotFoundException firstFail = null;
		for (ClassLoader delegate : delegates) {
		    try {
		      return delegate.loadClass(clazz);
		    }
		    catch (ClassNotFoundException ncfe) {
		        if (firstFail == null) firstFail = ncfe;
		    }
		}
		
		if (firstFail != null) throw firstFail;
		throw new ClassNotFoundException("Could not find " + clazz);
	}

	@Override
	public URL getResource(String resource) {
		if (getParent() != null) {
			URL u = getParent().getResource(resource);
			
			if (u != null) {
				return u;
			}
		}
		
		for (ClassLoader delegate : delegates) {
		    URL u = delegate.getResource(resource);
		    
		    if (u != null) return u;
		}
		
		return null;
	}	
	
//	@Override
//	public int hashCode() {
//	    int code = (getParent() == null) ? 0 : getParent().hashCode();
//	    
//	    for (ClassLoader delegate : delegates) {
//	        code ^= delegate.hashCode();
//	    }
//	    
//	    return code;
//	}
	
	private final static boolean safeEquals(Object a, Object b) {
	    if (a == b) return true;
	    if (a == null) return false;
	    if (b == null) return false;
	    
	    return a.equals(b);
	}
	
	@Override
	public boolean equals(Object o) {
	    if (o == null) return false;
	    if (!(o instanceof DelegatingClassLoader)) return false;
	    
	    DelegatingClassLoader<?> other = (DelegatingClassLoader<?>) o;
	    
	    if (!safeEquals(getParent(), other.getParent())) return false;
	    
	    if (delegates.length != other.delegates.length) return false;
	    
	    for (int lcv = 0; lcv < delegates.length; lcv++) {
	        if (!safeEquals(delegates[lcv], other.delegates[lcv])) return false;
	    }
	    
	    return true;
	}
	
	@Override
	public String toString() {
	    return "DelegatingClassLoader(" + getParent() + "," + Pretty.array(delegates) + "," + System.identityHashCode(this) + ")";
	}
}