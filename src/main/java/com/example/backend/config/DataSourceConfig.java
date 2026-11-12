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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

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

        // Conservative Hikari connection pool limits for cloud free tiers (Render/Neon/Railway)
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(2);
        dataSource.setConnectionTimeout(30000);
        dataSource.setIdleTimeout(600000);
        dataSource.setMaxLifetime(1800000);

        // 1. Check for cloud database URL (DATABASE_URL from Render/Railway, or SPRING_DATASOURCE_URL)
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            databaseUrl = System.getenv("SPRING_DATASOURCE_URL");
        }
        if (databaseUrl == null || databaseUrl.isBlank()) {
            databaseUrl = configuredUrl;
        }

        if (databaseUrl != null && !databaseUrl.isBlank()) {
            if (databaseUrl.startsWith("jdbc:")) {
                dataSource.setJdbcUrl(databaseUrl);
                if (configuredUsername != null && !configuredUsername.isBlank()) {
                    dataSource.setUsername(configuredUsername);
                }
                if (configuredPassword != null && !configuredPassword.isBlank()) {
                    dataSource.setPassword(configuredPassword);
                }
                log.info("[DataSourceConfig] Using jdbc: URL directly");
                return dataSource;
            }

            if (databaseUrl.startsWith("postgresql://") || databaseUrl.startsWith("postgres://")) {
                if (configureFromPostgresUrl(dataSource, databaseUrl)) {
                    return dataSource;
                }
            }
        }

        // 2. Check for Railway/Heroku PG* variables
        String pgHost = System.getenv("PGHOST");
        if (pgHost != null && !pgHost.isBlank()) {
            String pgPort = System.getenv("PGPORT");
            if (pgPort == null || pgPort.isBlank()) pgPort = "5432";
            String pgDb = System.getenv("PGDATABASE");
            if (pgDb == null || pgDb.isBlank()) pgDb = "devcollab";
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

        // 3. Fallback to application.properties
        dataSource.setJdbcUrl(configuredUrl);
        dataSource.setUsername(configuredUsername);
        dataSource.setPassword(configuredPassword);
        log.info("[DataSourceConfig] Using application.properties datasource URL: {}", configuredUrl);
        return dataSource;
    }

    private boolean configureFromPostgresUrl(HikariDataSource dataSource, String databaseUrl) {
        try {
            URI uri = new URI(databaseUrl);
            String host = uri.getHost();
            int port = uri.getPort() == -1 ? 5432 : uri.getPort();
            String path = uri.getPath();
            String dbName = (path != null && path.startsWith("/"))
                    ? path.substring(1)
                    : (path == null || path.isBlank() ? "devcollab" : path);

            String username = configuredUsername;
            String password = configuredPassword;
            String userInfo = uri.getUserInfo();
            if (userInfo != null && userInfo.contains(":")) {
                String[] creds = userInfo.split(":", 2);
                username = URLDecoder.decode(creds[0], StandardCharsets.UTF_8);
                password = URLDecoder.decode(creds[1], StandardCharsets.UTF_8);
            }

            String query = uri.getQuery();
            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
            if (query != null && !query.isBlank()) {
                jdbcUrl += "?" + query;
            }

            dataSource.setJdbcUrl(jdbcUrl);
            if (username != null && !username.isBlank()) dataSource.setUsername(username);
            if (password != null && !password.isBlank()) dataSource.setPassword(password);

            log.info("[DataSourceConfig] Configured DataSource from URI -> host: {}, port: {}, db: {}, user: {}", host, port, dbName, username);
            return true;
        } catch (Exception e) {
            log.warn("[DataSourceConfig] URI parsing failed, falling back to manual extraction: {}", e.getMessage());
            return parsePostgresUrlManually(dataSource, databaseUrl);
        }
    }

    private boolean parsePostgresUrlManually(HikariDataSource dataSource, String databaseUrl) {
        try {
            // Strip scheme
            String clean = databaseUrl.replaceFirst("^(postgres|postgresql)://", "");

            // Extract query if present
            String query = null;
            int queryIdx = clean.indexOf('?');
            if (queryIdx != -1) {
                query = clean.substring(queryIdx + 1);
                clean = clean.substring(0, queryIdx);
            }

            // Extract path/dbname
            String dbName = "devcollab";
            int slashIdx = clean.indexOf('/');
            if (slashIdx != -1) {
                dbName = clean.substring(slashIdx + 1);
                clean = clean.substring(0, slashIdx);
            }

            // Extract user:password if present
            String username = configuredUsername;
            String password = configuredPassword;
            int atIdx = clean.lastIndexOf('@');
            String hostPort = clean;
            if (atIdx != -1) {
                String userPass = clean.substring(0, atIdx);
                hostPort = clean.substring(atIdx + 1);
                if (userPass.contains(":")) {
                    String[] creds = userPass.split(":", 2);
                    username = URLDecoder.decode(creds[0], StandardCharsets.UTF_8);
                    password = URLDecoder.decode(creds[1], StandardCharsets.UTF_8);
                } else {
                    username = URLDecoder.decode(userPass, StandardCharsets.UTF_8);
                }
            }

            String host = hostPort;
            int port = 5432;
            if (hostPort.contains(":")) {
                String[] hp = hostPort.split(":", 2);
                host = hp[0];
                try {
                    port = Integer.parseInt(hp[1]);
                } catch (NumberFormatException ignored) {}
            }

            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
            if (query != null && !query.isBlank()) {
                jdbcUrl += "?" + query;
            }

            dataSource.setJdbcUrl(jdbcUrl);
            if (username != null && !username.isBlank()) dataSource.setUsername(username);
            if (password != null && !password.isBlank()) dataSource.setPassword(password);

            log.info("[DataSourceConfig] Configured DataSource manually -> host: {}, port: {}, db: {}, user: {}", host, port, dbName, username);
            return true;
        } catch (Exception ex) {
            log.error("[DataSourceConfig] Manual parsing also failed: {}", ex.getMessage());
            return false;
        }
    }
}
