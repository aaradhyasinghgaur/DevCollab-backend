package com.example.backend.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${spring.datasource.url:}")
    private String configuredUrl;

    @Value("${spring.datasource.username:}")
    private String configuredUsername;

    @Value("${spring.datasource.password:}")
    private String configuredPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");

        // 1. Check for cloud DATABASE_URL (Railway, Render, Heroku)
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl != null && !databaseUrl.isBlank()) {
            try {
                if (databaseUrl.startsWith("jdbc:")) {
                    dataSource.setJdbcUrl(databaseUrl);
                    if (configuredUsername != null && !configuredUsername.isBlank()) {
                        dataSource.setUsername(configuredUsername);
                    }
                    if (configuredPassword != null && !configuredPassword.isBlank()) {
                        dataSource.setPassword(configuredPassword);
                    }
                    log.info("[DataSourceConfig] Using jdbc: DATABASE_URL directly");
                    return dataSource;
                }

                if (databaseUrl.startsWith("postgresql://") || databaseUrl.startsWith("postgres://")) {
                    URI uri = new URI(databaseUrl);
                    String host = uri.getHost();
                    int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                    String path = uri.getPath();
                    String dbName = (path != null && path.startsWith("/"))
                            ? path.substring(1)
                            : (path == null || path.isBlank() ? "railway" : path);

                    String userInfo = uri.getUserInfo();
                    String username = configuredUsername;
                    String password = configuredPassword;
                    if (userInfo != null && userInfo.contains(":")) {
                        String[] creds = userInfo.split(":", 2);
                        username = creds[0];
                        password = creds[1];
                    }

                    String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
                    dataSource.setJdbcUrl(jdbcUrl);
                    if (username != null && !username.isBlank()) dataSource.setUsername(username);
                    if (password != null && !password.isBlank()) dataSource.setPassword(password);

                    log.info("[DataSourceConfig] Configured DataSource from DATABASE_URL -> host: {}, port: {}, db: {}, user: {}", host, port, dbName, username);
                    return dataSource;
                }
            } catch (Exception e) {
                log.error("[DataSourceConfig] Failed to parse DATABASE_URL: {}", e.getMessage());
            }
        }

        // 2. Check for Railway PG* variables
        String pgHost = System.getenv("PGHOST");
        if (pgHost != null && !pgHost.isBlank()) {
            String pgPort = System.getenv("PGPORT");
            if (pgPort == null || pgPort.isBlank()) pgPort = "5432";
            String pgDb = System.getenv("PGDATABASE");
            if (pgDb == null || pgDb.isBlank()) pgDb = "railway";
            String pgUser = System.getenv("PGUSER");
            if (pgUser == null || pgUser.isBlank()) pgUser = "postgres";
            String pgPass = System.getenv("PGPASSWORD");

            String jdbcUrl = "jdbc:postgresql://" + pgHost + ":" + pgPort + "/" + pgDb;
            dataSource.setJdbcUrl(jdbcUrl);
            dataSource.setUsername(pgUser);
            if (pgPass != null) dataSource.setPassword(pgPass);

            log.info("[DataSourceConfig] Configured DataSource from PG* variables -> host: {}, port: {}, db: {}, user: {}", pgHost, pgPort, pgDb, pgUser);
            return dataSource;
        }

        // 3. Fallback to properties from application.properties
        dataSource.setJdbcUrl(configuredUrl);
        dataSource.setUsername(configuredUsername);
        dataSource.setPassword(configuredPassword);
        log.info("[DataSourceConfig] Using application.properties datasource URL: {}", configuredUrl);
        return dataSource;
    }
}
