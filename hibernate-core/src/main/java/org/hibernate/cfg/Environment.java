/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.Version;
import org.hibernate.bytecode.internal.BytecodeProviderInitiator;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ConfigHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;

import org.jboss.logging.Logger;

import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;


/**
 * Provides access to configuration properties passed in {@link Properties} objects.
 * <p>
 * Hibernate has two property scopes:
 * <ul>
 * <li><em>Factory-level</em> properties are specified when a
 * {@link org.hibernate.SessionFactory} is configured and instantiated. Each instance
 * might have different property values.
 * <li><em>System-level</em> properties are shared by all factory instances and are
 * always determined by the {@code Environment} properties in {@link #getProperties()}.
 * </ul>
 * <p>
 * The only system-level property is {@value #USE_REFLECTION_OPTIMIZER},
 * and it's deprecated.
 * <p>
 * {@code Environment} properties are populated by calling {@link System#getProperties()}
 * and then from a resource named {@code /hibernate.properties}, if it exists. System
 * properties override properties specified in {@code hibernate.properties}.
 * <p>
 * The {@link org.hibernate.SessionFactory} obtains properties from:
 * <ul>
 * <li>{@link System#getProperties() system properties},
 * <li>properties defined in a resource named {@code /hibernate.properties}, and
 * <li>any instance of {@link Properties} passed to {@link Configuration#addProperties}.
 * </ul>
 * <table>
 * <caption>Configuration properties</caption>
 * <tr><td><b>Property</b></td><td><b>Interpretation</b></td></tr>
 * <tr>
 *   <td>{@value #DIALECT}</td>
 *   <td>name of {@link org.hibernate.dialect.Dialect} subclass</td>
 * </tr>
 * <tr>
 *   <td>{@value #CONNECTION_PROVIDER}</td>
 *   <td>name of a {@link org.hibernate.engine.jdbc.connections.spi.ConnectionProvider}
 *   subclass (if not specified heuristics are used)</td>
 * </tr>
 * <tr><td>{@value #USER}</td><td>database username</td></tr>
 * <tr><td>{@value #PASS}</td><td>database password</td></tr>
 * <tr>
 *   <td>{@value #URL}</td>
 *   <td>JDBC URL (when using {@link java.sql.DriverManager})</td>
 * </tr>
 * <tr>
 *   <td>{@value #DRIVER}</td>
 *   <td>classname of JDBC driver</td>
 * </tr>
 * <tr>
 *   <td>{@value #ISOLATION}</td>
 *   <td>JDBC transaction isolation level (only when using
 *     {@link java.sql.DriverManager})
 *   </td>
 * </tr>
 * <tr>
 *   <td>{@value #POOL_SIZE}</td>
 *   <td>the maximum size of the connection pool (only when using
 *     {@link java.sql.DriverManager})
 *   </td>
 * </tr>
 * <tr>
 *   <td>{@value #DATASOURCE}</td>
 *   <td>datasource JNDI name (when using {@link javax.sql.DataSource})</td>
 * </tr>
 * <tr>
 *   <td>{@value #JNDI_URL}</td><td>JNDI {@link javax.naming.InitialContext} URL</td>
 * </tr>
 * <tr>
 *   <td>{@value #JNDI_CLASS}</td><td>JNDI {@link javax.naming.InitialContext} class name</td>
 * </tr>
 * <tr>
 *   <td>{@value #MAX_FETCH_DEPTH}</td>
 *   <td>maximum depth of outer join fetching</td>
 * </tr>
 * <tr>
 *   <td>{@value #STATEMENT_BATCH_SIZE}</td>
 *   <td>enable use of JDBC2 batch API for drivers which support it</td>
 * </tr>
 * <tr>
 *   <td>{@value #STATEMENT_FETCH_SIZE}</td>
 *   <td>set the JDBC fetch size</td>
 * </tr>
 * <tr>
 *   <td>{@value #USE_GET_GENERATED_KEYS}</td>
 *   <td>enable use of JDBC3 {@link java.sql.PreparedStatement#getGeneratedKeys()}
 *   to retrieve natively generated keys after insert. Requires JDBC3+ driver and
 *   JRE1.4+</td>
 * </tr>
 * <tr>
 *   <td>{@value #HBM2DDL_AUTO}</td>
 *   <td>enable auto DDL export</td>
 * </tr>
 * <tr>
 *   <td>{@value #DEFAULT_SCHEMA}</td>
 *   <td>use given schema name for unqualified tables (always optional)</td>
 * </tr>
 * <tr>
 *   <td>{@value #DEFAULT_CATALOG}</td>
 *   <td>use given catalog name for unqualified tables (always optional)</td>
 * </tr>
 * <tr>
 *   <td>{@value #JTA_PLATFORM}</td>
 *   <td>name of {@link org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform}
 *   implementation</td>
 * </tr>
 * </table>
 *
 * @see org.hibernate.SessionFactory
 *
 * @apiNote This is really considered an internal contract, but leaving in place in this
 * package as many applications use it historically.  However, consider migrating to use
 * {@link AvailableSettings} instead.
 *
 * @author Gavin King
 */
@Internal
public final class Environment implements AvailableSettings {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, Environment.class.getName());

	private static final boolean ENABLE_REFLECTION_OPTIMIZER;

	private static final Properties GLOBAL_PROPERTIES;

	static {
		Version.logVersion();

		GLOBAL_PROPERTIES = new Properties();
		GLOBAL_PROPERTIES.setProperty( USE_REFLECTION_OPTIMIZER, Boolean.TRUE.toString() );

		try {
			InputStream stream = ConfigHelper.getResourceAsStream( "/hibernate.properties" );
			try {
				GLOBAL_PROPERTIES.load(stream);
				LOG.propertiesLoaded( ConfigurationHelper.maskOut( GLOBAL_PROPERTIES, PASS ) );
			}
			catch (Exception e) {
				LOG.unableToLoadProperties();
			}
			finally {
				try{
					stream.close();
				}
				catch (IOException ioe){
					LOG.unableToCloseStreamError( ioe );
				}
			}
		}
		catch (HibernateException he) {
			LOG.propertiesNotFound();
		}

		try {
			Properties systemProperties = System.getProperties();
		    // Must be thread-safe in case an application changes System properties during Hibernate initialization.
		    // See HHH-8383.
			synchronized (systemProperties) {
				GLOBAL_PROPERTIES.putAll(systemProperties);
			}
		}
		catch (SecurityException se) {
			LOG.unableToCopySystemProperties();
		}

		ENABLE_REFLECTION_OPTIMIZER = ConfigurationHelper.getBoolean(USE_REFLECTION_OPTIMIZER, GLOBAL_PROPERTIES);
		if ( ENABLE_REFLECTION_OPTIMIZER ) {
			LOG.usingReflectionOptimizer();
		}
		else {
			DEPRECATION_LOGGER.deprecatedSettingForRemoval( USE_REFLECTION_OPTIMIZER, "true" );
		}
	}

	/**
	 * Should we use reflection optimization?
	 *
	 * @return True if reflection optimization should be used; false otherwise.
	 *
	 * @see #USE_REFLECTION_OPTIMIZER
	 * @see BytecodeProvider#getReflectionOptimizer
	 *
	 * @deprecated Deprecated to indicate that the method will be moved to
	 * {@link org.hibernate.boot.spi.SessionFactoryOptions} /
	 * {@link org.hibernate.boot.SessionFactoryBuilder}.
	 * See <a href="https://hibernate.atlassian.net/browse/HHH-12194">HHH-12194</a> and
	 * <a href="https://hibernate.atlassian.net/browse/HHH-12193">HHH-12193</a> for details
	 */
	@Deprecated
	public static boolean useReflectionOptimizer() {
		return ENABLE_REFLECTION_OPTIMIZER;
	}

	/**
	 * Disallow instantiation
	 */
	private Environment() {
		//not to be constructed
	}

	/**
	 * The {@link System#getProperties() system properties}, extended with all
	 * additional properties specified in {@code hibernate.properties}.
	 */
	public static Properties getProperties() {
		Properties copy = new Properties();
		copy.putAll(GLOBAL_PROPERTIES);
		return copy;
	}

	/**
	 * @deprecated Replaced by {@code org.hibernate.bytecode.internal.BytecodeProviderInitiator#BYTECODE_PROVIDER_NAME_BYTEBUDDY},
	 * however note that that's an internal contract: a different BytecodeProvider Initiator might ignore these constants
	 * or interpret them differently.
	 */
	@Deprecated(forRemoval = true)
	public static final String BYTECODE_PROVIDER_NAME_BYTEBUDDY = "bytebuddy";

	/**
	 * @deprecated Replaced by {@code org.hibernate.bytecode.internal.BytecodeProviderInitiator#BYTECODE_PROVIDER_NAME_NONE},
	 * however note that that's an internal contract: a different BytecodeProvider Initiator might ignore these constants
	 * or interpret them differently.
	 */
	@Deprecated(forRemoval = true)
	public static final String BYTECODE_PROVIDER_NAME_NONE = "none";

	/**
	 * @deprecated Replaced by {@code org.hibernate.bytecode.internal.BytecodeProviderInitiator#BYTECODE_PROVIDER_NAME_DEFAULT}
	 * however note that that's an internal contract: a different BytecodeProvider Initiator might apply a different default.
	 */
	@Deprecated(forRemoval = true)
	public static final String BYTECODE_PROVIDER_NAME_DEFAULT = BytecodeProviderInitiator.BYTECODE_PROVIDER_NAME_BYTEBUDDY;

	/**
	 * @deprecated this will be removed; retrieval of the BytecodeProvider should be performed via the {@link org.hibernate.service.ServiceRegistry}.
	 */
	@Deprecated(forRemoval = true)
	public static BytecodeProvider buildBytecodeProvider(Properties properties) {
		String provider = ConfigurationHelper.getString( BYTECODE_PROVIDER, properties, BYTECODE_PROVIDER_NAME_DEFAULT );
		return BytecodeProviderInitiator.buildBytecodeProvider( provider );
	}

}
