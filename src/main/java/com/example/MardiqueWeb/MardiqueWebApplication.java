package com.example.MardiqueWeb;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;
import java.sql.Statement;

@SpringBootApplication
@EnableScheduling
public class MardiqueWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(MardiqueWebApplication.class, args);
	}

	@Bean
	CommandLineRunner fixDbColumns(DataSource ds) {
		return args -> {
			String[] migrations = {
				"ALTER TABLE solicitudes_hr_recipients ALTER COLUMN signature_url TYPE TEXT",
				"DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='solicitudes_hr_recipients' AND column_name='signed_document_url') THEN ALTER TABLE solicitudes_hr_recipients ADD COLUMN signed_document_url TEXT; END IF; END $$",
				"ALTER TABLE solicitudes_hr_recipients ALTER COLUMN signed_document_url TYPE TEXT"
			};
			for (String sql : migrations) {
				try (var conn = ds.getConnection(); Statement st = conn.createStatement()) {
					st.execute(sql);
					System.out.println("[DB-MIGRATION] OK: " + sql.substring(0, Math.min(sql.length(), 60)));
				} catch (Exception e) {
					System.out.println("[DB-MIGRATION] SKIP: " + e.getMessage());
				}
			}
		};
	}

}
