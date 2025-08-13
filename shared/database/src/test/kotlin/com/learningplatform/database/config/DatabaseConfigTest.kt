package com.learningplatform.database.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class DatabaseConfigTest {

    private lateinit var databaseConfig: DatabaseConfig
    
    @BeforeEach
    fun setUp() {
        databaseConfig = DatabaseConfig()
    }
    
    @Test
    fun `should create primary database properties with correct values`() {
        // When
        val properties = databaseConfig.primaryDataSourceProperties()
        properties.url = "jdbc:postgresql://localhost:5432/test_db"
        properties.username = "test_user"
        properties.password = "test_password"
        properties.hikari.maximumPoolSize = 10
        properties.hikari.minimumIdle = 2
        
        // Then
        assertEquals("jdbc:postgresql://localhost:5432/test_db", properties.url)
        assertEquals("test_user", properties.username)
        assertEquals("test_password", properties.password)
        assertEquals("org.postgresql.Driver", properties.driverClassName)
        assertEquals(10, properties.hikari.maximumPoolSize)
        assertEquals(2, properties.hikari.minimumIdle)
    }
    
    @Test
    fun `should create read replica database properties with correct values`() {
        // When
        val properties = databaseConfig.readReplicaDataSourceProperties()
        properties.url = "jdbc:postgresql://localhost:5432/test_replica_db"
        properties.username = "test_replica_user"
        properties.password = "test_replica_password"
        
        // Then
        assertEquals("jdbc:postgresql://localhost:5432/test_replica_db", properties.url)
        assertEquals("test_replica_user", properties.username)
        assertEquals("test_replica_password", properties.password)
    }
    
    @Test
    fun `HikariProperties should have correct default values`() {
        // When
        val properties = HikariProperties()
        
        // Then
        assertEquals(20, properties.maximumPoolSize)
        assertEquals(5, properties.minimumIdle)
        assertEquals(30000L, properties.connectionTimeout)
        assertEquals(600000L, properties.idleTimeout)
        assertEquals(1800000L, properties.maxLifetime)
        assertEquals(60000L, properties.leakDetectionThreshold)
    }
    
    @Test
    fun `DatabaseProperties should have correct default values`() {
        // When
        val properties = DatabaseProperties()
        
        // Then
        assertEquals("", properties.url)
        assertEquals("", properties.username)
        assertEquals("", properties.password)
        assertEquals("org.postgresql.Driver", properties.driverClassName)
        assertNotNull(properties.hikari)
    }
    
    @Test
    fun `should validate connection pool configuration`() {
        // Given
        val properties = DatabaseProperties().apply {
            url = "jdbc:postgresql://localhost:5432/test_db"
            username = "test_user"
            password = "test_password"
            hikari = HikariProperties().apply {
                maximumPoolSize = 50
                minimumIdle = 10
                connectionTimeout = 20000
                idleTimeout = 300000
                maxLifetime = 1200000
            }
        }
        
        // When & Then - Should not throw exception
        assertDoesNotThrow {
            // Validate configuration constraints
            assertTrue(properties.hikari.maximumPoolSize >= properties.hikari.minimumIdle)
            assertTrue(properties.hikari.connectionTimeout > 0)
            assertTrue(properties.hikari.idleTimeout > 0)
            assertTrue(properties.hikari.maxLifetime > 0)
        }
    }
}