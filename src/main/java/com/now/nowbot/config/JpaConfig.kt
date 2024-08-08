package com.now.nowbot.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.PlatformTransactionManager
import java.util.*
import javax.sql.DataSource

@Configuration
@EnableJpaRepositories(
    basePackages = ["com.now.nowbot.mapper"],
    entityManagerFactoryRef = "botEntityManagerFactory",
    transactionManagerRef = "botTransactionManager"
)
// 只有 newbie 配置了才加载额外的 jpa 处理器
@ConditionalOnProperty(prefix = "spring.datasource.newbie", name = ["enable"], havingValue = "true")
class JpaConfig(
    private val env: Environment
) {
    @Bean
    @Primary
    fun botEntityManagerFactory(botDataSource: DataSource): LocalContainerEntityManagerFactoryBean {
        val factory = LocalContainerEntityManagerFactoryBean()
        factory.dataSource = botDataSource
        factory.setPackagesToScan("com.now.nowbot.entity")
        factory.persistenceUnitName = "bot"

        val jpa = HibernateJpaVendorAdapter()
        factory.jpaVendorAdapter = jpa
        val prop = Properties()
        prop["show-sql"] = env.getProperty("spring.jpa.show-sql")
        prop["database-platform"] = env.getProperty("spring.jpa.database-platform")
        prop["hibernate.ddl-auto"] = env.getProperty("spring.jpa.hibernate.ddl-auto", "none")
        prop["hibernate.physical_naming_strategy"] =
            "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"
        prop["hibernate.globally_quoted_identifiers"] = "true"
        factory.setJpaProperties(prop)
        return factory
    }

    @Bean
    @Primary
    fun botDataSource(): DataSource {
        val dataSource = DriverManagerDataSource()
        dataSource.setDriverClassName(env.getProperty("spring.datasource.driver-class-name", ""))
        dataSource.url = env.getProperty("spring.datasource.url")
        dataSource.username = env.getProperty("spring.datasource.username")
        dataSource.password = env.getProperty("spring.datasource.password")
        return dataSource
    }


    @Bean
    @Primary
    fun botTransactionManager(): PlatformTransactionManager {
        val manager = JpaTransactionManager()
        manager.entityManagerFactory = botEntityManagerFactory(botDataSource()).getObject()
        return manager
    }
}

@Configuration
@EnableJpaRepositories(
    basePackages = ["com.now.nowbot.newbie.mapper"],
    entityManagerFactoryRef = "newbieEntityManagerFactory",
    transactionManagerRef = "newbieTransactionManager"
)
@ConditionalOnProperty(prefix = "spring.datasource.newbie", name = ["enable"], havingValue = "true")
class NewbieJpa(
    private val env: Environment
) {

    @Bean("newbieEntityManagerFactory")
    fun newbieEntityManagerFactory(): LocalContainerEntityManagerFactoryBean {
        val factory = LocalContainerEntityManagerFactoryBean()
        factory.dataSource = newbieDataSource()
        factory.setPackagesToScan("com.now.nowbot.newbie.entity")
        factory.persistenceUnitName = "newbie"

        val jpa = HibernateJpaVendorAdapter()
        factory.jpaVendorAdapter = jpa
        val prop = Properties()
        prop["show-sql"] = env.getProperty("spring.jpa.show-sql")
        prop["database-platform"] = env.getProperty("spring.jpa.database-platform")
        prop["hibernate.ddl-auto"] = "none"
        prop["hibernate.globally_quoted_identifiers"] = "true"
        factory.setJpaProperties(prop)
        return factory
    }

    @Bean
    fun newbieDataSource(): DataSource {
        val dataSource = DriverManagerDataSource()
        dataSource.setDriverClassName(env.getProperty("spring.datasource.driver-class-name", ""))
        dataSource.url = env.getProperty("spring.datasource.newbie.url")
        dataSource.username = env.getProperty("spring.datasource.newbie.username")
        dataSource.password = env.getProperty("spring.datasource.newbie.password")
        return dataSource
    }

    @Bean("newbieTransactionManager")
    fun newbieTransactionManager(): PlatformTransactionManager {
        val manager = JpaTransactionManager()
        manager.entityManagerFactory = newbieEntityManagerFactory().getObject()
        return manager
    }
}