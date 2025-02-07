/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.engine.spi;

import org.hibernate.Internal;
import org.hibernate.engine.internal.ManagedTypeHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyConfiguration;

/**
 * For a full explanation of the purpose of this interface see {@link ManagedTypeHelper}.
 *
 * @apiNote This is an internal, private marking interface; it's exposed in the SPI
 *          package as bytecode enhanced user code needs to be able to reference it.
 *
 * @author Sanne Grinovero
 */
@Internal
public interface PrimeAmongSecondarySupertypes {

	default ManagedEntity asManagedEntity() {
		return null;
	}

	default PersistentAttributeInterceptable asPersistentAttributeInterceptable() {
		return null;
	}

	default SelfDirtinessTracker asSelfDirtinessTracker() {
		return null;
	}

	//Included for consistency but doesn't seem to be used?
	default Managed asManaged() {
		return null;
	}

	//Included for consistency but doesn't seem to be used?
	default ManagedComposite asManagedComposite() {
		return null;
	}

	//Included for consistency but doesn't seem to be used?
	default ManagedMappedSuperclass asManagedMappedSuperclass() {
		return null;
	}

	default CompositeOwner asCompositeOwner() {
		return null;
	}

	default CompositeTracker asCompositeTracker() {
		return null;
	}

	default HibernateProxy asHibernateProxy() {
		return null;
	}

	default ProxyConfiguration asProxyConfiguration() {
		return null;
	}

}
