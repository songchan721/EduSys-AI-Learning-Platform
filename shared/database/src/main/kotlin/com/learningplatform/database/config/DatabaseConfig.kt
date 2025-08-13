package com.learningplatform.database.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource
import jakarta.persistence.EntityManagerFactory

/**
 * Database configuration with HikariCP connection pooling
 * Supports multiple database configurations per service
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = ["com.learningplatform"])
class DatabaseConfig {

    @Bean
    @Primary
    @ConfigurationProperties("app.datasource.primary")
    fun primaryDataSourceProperties(): DatabaseProperties {
        return DatabaseProperties()
    }

    @Bean
    @ConfigurationProperties("app.datasource.read-replica")
    fun readReplicaDataSourceProperties(): DatabaseProperties {
        return DatabaseProperties()
    }

    @Bean
    @Primary
    fun primaryDataSource(): DataSource {
        return createHikariDataSource(primaryDataSourceProperties(), "primary")
    }

    @Bean
    fun readReplicaDataSource(): DataSource {
        return createHikariDataSource(readReplicaDataSourceProperties(), "read-replica")
    }

    @Bean
    @Primary
    fun entityManagerFactory(primaryDataSource: DataSource): LocalContainerEntityManagerFactoryBean {
        val em = LocalContainerEntityManagerFactoryBean()
        em.dataSource = primaryDataSource
        em.setPackagesToScan("com.learningplatform.domain")
        
        val vendorAdapter = HibernateJpaVendorAdapter()
        em.jpaVendorAdapter = vendorAdapter
        
        val properties = mapOf(
            "hibernate.dialect" to "org.hibernate.dialect.PostgreSQLDialect",
            "hibernate.hbm2ddl.auto" to "validate", // Use Flyway for schema management
            "hibernate.show_sql" to "false",
            "hibernate.format_sql" to "true",
            "hibernate.use_sql_comments" to "true",
            "hibernate.jdbc.batch_size" to "25",
            "hibernate.order_inserts" to "true",
            "hibernate.order_updates" to "true",
            "hibernate.jdbc.batch_versioned_data" to "true",
            "hibernate.connection.provider_disables_autocommit" to "true",
            "hibernate.query.plan_cache_max_size" to "2048",
            "hibernate.query.plan_parameter_metadata_max_size" to "128"
        )
        
        em.setJpaPropertyMap(properties)
        return em
    }

    @Bean
    @Primary
    fun transactionManager(entityManagerFactory: EntityManagerFactory): PlatformTransactionManager {
        val transactionManager = JpaTransactionManager()
        transactionManager.entityManagerFactory = entityManagerFactory
        return transactionManager
    }

    private fun createHikariDataSource(properties: DatabaseProperties, poolName: String): HikariDataSource {
        val config = HikariConfig()
        
        // Basic connection properties
        config.jdbcUrl = properties.url
        config.username = properties.username
        config.password = properties.password
        config.driverClassName = properties.driverClassName
        
        // Pool configuration
        config.poolName = "LearningPlatform-$poolName"
        config.maximumPoolSize = properties.hikari.maximumPoolSize
        config.minimumIdle = properties.hikari.minimumIdle
        config.connectionTimeout = properties.hikari.connectionTimeout
        config.idleTimeout = properties.hikari.idleTimeout
        config.maxLifetime = properties.hikari.maxLifetime
        config.leakDetectionThreshold = properties.hikari.leakDetectionThreshold
        
        // Performance optimizations
        config.addDataSourceProperty("cachePrepStmts", "true")
        config.addDataSourceProperty("prepStmtCacheSize", "250")
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        config.addDataSourceProperty("useServerPrepStmts", "true")
        config.addDataSourceProperty("useLocalSessionState", "true")
        config.addDataSourceProperty("rewriteBatchedStatements", "true")
        config.addDataSourceProperty("cacheResultSetMetadata", "true")
        config.addDataSourceProperty("cacheServerConfiguration", "true")
        config.addDataSourceProperty("elideSetAutoCommits", "true")
        config.addDataSourceProperty("maintainTimeStats", "false")
        
        // PostgreSQL specific optimizations
        config.addDataSourceProperty("tcpKeepAlive", "true")
        config.addDataSourceProperty("socketTimeout", "30")
        config.addDataSourceProperty("loginTimeout", "10")
        config.addDataSourceProperty("connectTimeout", "10")
        config.addDataSourceProperty("cancelSignalTimeout", "10")
        
        return HikariDataSource(config)
    }
}

/**
 * Database connection properties
 */
data class DatabaseProperties(
    var url: String = "",
    var username: String = "",
    var password: String = "",
    var driverClassName: String = "org.postgresql.Driver",
    var hikari: HikariProperties = HikariProperties()
)

/**
 * HikariCP specific properties
 */
data class HikariProperties(
    var maximumPoolSize: Int = 20,
    var minimumIdle: Int = 5,
    var connectionTimeout: Long = 30000, // 30 seconds
    var idleTimeout: Long = 600000, // 10 minutes
    var maxLifetime: Long = 1800000, // 30 minutes
    var leakDetectionThreshold: Long = 60000 // 1 minute
)