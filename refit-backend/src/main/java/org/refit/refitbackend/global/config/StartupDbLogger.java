package org.refit.refitbackend.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupDbLogger implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            log.info("DB url={}", meta.getURL());
            log.info("DB user={}", meta.getUserName());
        }

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "select current_database() as db, current_schema() as schema, " +
                        "inet_server_addr() as host, inet_server_port() as port"
        );
        log.info("DB current db={}, schema={}, host={}, port={}",
                row.get("db"), row.get("schema"), row.get("host"), row.get("port"));
    }
}
