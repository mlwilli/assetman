package com.github.mlwilli.assetman.tools

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories

/**
 * One-time baseline generator:
 * - Boots Spring with Hibernate DDL=create against a FILE-based H2 database
 * - Disables Flyway so Hibernate is the sole schema author
 * - Exports schema (no data) to src/main/resources/db/migration/V1__baseline.sql
 *
 * After running once, keep the migration file, but this test can remain (or be deleted).
 */
@SpringBootTest(
    properties = [
        // Create schema from entities
        "spring.jpa.hibernate.ddl-auto=create",

        // IMPORTANT: Flyway must be OFF for baseline generation
        "spring.flyway.enabled=false",

        // Use a FILE DB so the schema persists during export
        "spring.datasource.url=jdbc:h2:file:./build/h2/assetman-baseline;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.username=sa",
        "spring.datasource.password=",

        // Keep noise down
        "spring.jpa.show-sql=false",
        "logging.level.org.hibernate.SQL=OFF"
    ]
)
class FlywayBaselineGeneratorTest {

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun generateBaselineMigration() {
        val migrationsDir: Path = Paths.get("src/main/resources/db/migration")
        migrationsDir.createDirectories()

        val outFile: Path = migrationsDir.resolve("V1__baseline.sql").toAbsolutePath().normalize()
        Files.deleteIfExists(outFile)

        // H2 SCRIPT wants forward slashes
        val h2Path = outFile.toString().replace("\\", "/")

        // Export schema only (no inserts)
        jdbcTemplate.execute("SCRIPT NODATA TO '$h2Path'")

        require(Files.exists(outFile)) {
            "Baseline migration was not generated at: $outFile"
        }
    }
}
