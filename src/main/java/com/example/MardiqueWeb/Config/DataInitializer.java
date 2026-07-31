package com.example.MardiqueWeb.Config;

import com.example.MardiqueWeb.Entity.*;
import com.example.MardiqueWeb.Repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PageContentRepository pageContentRepository;
    private final ContactRepository contactRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final FaqRepository faqRepository;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           PageContentRepository pageContentRepository,
                           ContactRepository contactRepository,
                           SystemConfigRepository systemConfigRepository,
                           FaqRepository faqRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.pageContentRepository = pageContentRepository;
        this.contactRepository = contactRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.faqRepository = faqRepository;
    }

    private void createUserIfNotExists(String username, String rawPassword, String email, String nombres, String apellidos, String role, String departamento) {
        if (userRepository.findByUsername(username).isEmpty()) {
            User u = new User(username, passwordEncoder.encode(rawPassword), email, nombres, apellidos, null, role);
            u.setDepartamento(departamento);
            userRepository.save(u);
        }
    }

    @Override
    public void run(String... args) {
        createUserIfNotExists("admin", "admin123", "admin@mardique.com", "Admin", "TI", "ROLE_ADMIN", null);
        createUserIfNotExists("admin_comercial", "admin123", "comercial@mardique.com", "Admin", "Comercial", "ROLE_ADMIN", "Comercial");
        createUserIfNotExists("admin_juridica", "admin123", "juridica@mardique.com", "Admin", "Jurídica", "ROLE_ADMIN", "Jurídica");
        createUserIfNotExists("admin_compras", "admin123", "compras@mardique.com", "Admin", "Compras", "ROLE_ADMIN", "Compras");
        createUserIfNotExists("admin_it", "admin123", "it@mardique.com", "Admin", "IT", "ROLE_ADMIN", "IT");
        createUserIfNotExists("editor", "editor123", "editor@mardique.com", "Edit", "Editor", "ROLE_EDITOR", null);
        createUserIfNotExists("user", "user123", "user@mardique.com", "Usua", "Rio", "ROLE_USER", null);

        if (pageContentRepository.count() == 0) {
            seedPageContent();
        }

        if (contactRepository.count() == 0) {
            seedContacts();
        }

        seedConfigIfAbsent("TARIFA_IFRAME_URL", "", "URL del iframe para la página de Tarifas (Google Sheets, SharePoint, PDF, etc.)");
        seedConfigIfAbsent("TARIFA_PHONE", "(57) (5) 669 0730", "Teléfono de contacto para información de tarifas");
        seedConfigIfAbsent("TARIFA_EMAIL", "info@spmardique.com", "Correo de contacto para información de tarifas");

        if (faqRepository.count() == 0) {
            seedFaqs();
        }
    }

    private void seedFaqs() {
        faqRepository.save(new Faq("¿Qué servicios ofrece Mardique?",
            "Mardique ofrece servicios portuarios multipropósito: **carga general, gráneles, contenedores, hidrocarburos y grúas móviles**, además de infraestructura y logística portuaria para el desarrollo del país."));
        faqRepository.save(new Faq("¿Dónde está ubicada la empresa?",
            "Estamos ubicados en **Km 7 Vía a Mamonal, Cartagena, Colombia**. Teléfono PBX: **(57) (5) 669 0730**."));
        faqRepository.save(new Faq("¿Cómo puedo contactar al Gerente Comercial?",
            "Para contactar al **Gerente Comercial**, agenda una cita o solicita información a través del formulario de contacto del chat y un representante te atenderá."));
        faqRepository.save(new Faq("¿Cómo radico una PQRS?",
            "Puedes radicar tu petición, queja, reclamo o solicitud en la sección **Contacto** de nuestra página web, llenando el formulario de PQRS."));
        faqRepository.save(new Faq("¿Cuáles son los horarios de atención?",
            "Nuestras operaciones portuarias se realizan las **24 horas, los 365 días del año**. Las oficinas administrativas atienden en horario hábil de lunes a viernes."));
        faqRepository.save(new Faq("¿Qué se necesita para inscribirse como usuario?",
            "Para inscribirte debes contactar al **Oficial de Cumplimiento** mediante el formulario de contacto del chat (área Inscripción de Usuarios), quien te indicará los requisitos documentales según tu perfil (cliente o proveedor)."));
        faqRepository.save(new Faq("¿Cómo solicito información de tarifas?",
            "Para información de tarifas, agenda una cita o solicita información a través del formulario de contacto del chat (área **Gerente Comercial**) y nuestro equipo te atenderá."));
        faqRepository.save(new Faq("¿Cómo contacto con Talento Humano?",
            "Para temas de empleo, pasantías y trámites de personal, agenda una cita o solicita información a través del formulario del chat (área **Talento Humano**)."));
        faqRepository.save(new Faq("¿Qué trámites puedo realizar en línea?",
            "Puedes descargar formatos de solicitudes de servicio, inscripciones y otros trámites en la sección **Trámites en Línea** de nuestra página web."));
        faqRepository.save(new Faq("¿Quién atiende la documentación aduanera?",
            "La **Documentación Aduanera** tiene un área especializada. Agenda una cita o solicita información a través del formulario de contacto del chat y un representante te atenderá."));
    }

    private void seedPageContent() {

        pageContentRepository.save(new PageContent("contacto", "hero-title", "Canales de <strong>Comunicaci&oacute;n</strong>", "T&iacute;tulo del hero"));
        pageContentRepository.save(new PageContent("contacto", "hero-sub", "Estamos listos para atenderle. Cont&aacute;ctenos a trav&eacute;s de cualquiera de nuestros canales.", "Subt&iacute;tulo del hero"));
        pageContentRepository.save(new PageContent("contacto", "section-title", "Estamos aqu&iacute; para <strong style=\"color:var(--accent-gold)\">ayudarle</strong>", "T&iacute;tulo secci&oacute;n contacto"));
        pageContentRepository.save(new PageContent("contacto", "section-text", "En Sociedad Portuaria Mardique nos comprometemos a dar respuesta oportuna a todas sus solicitudes. Nuestro equipo se encuentra disponible para orientarle en sus operaciones portuarias, comerciales y log&iacute;sticas.", "Texto secci&oacute;n contacto"));

        pageContentRepository.save(new PageContent("tramites", "hero-title", "Tr&aacute;mites <strong>en L&iacute;nea</strong>", "T&iacute;tulo del hero"));
        pageContentRepository.save(new PageContent("tramites", "hero-sub", "Descargue y diligencie los formatos requeridos para iniciar sus operaciones con nosotros de manera &aacute;gil y segura.", "Subt&iacute;tulo del hero"));
        pageContentRepository.save(new PageContent("tramites", "intro-title", "Formatos digitales para su gesti&oacute;n portuaria", "T&iacute;tulo introductorio"));
        pageContentRepository.save(new PageContent("tramites", "intro-text", "Con los tr&aacute;mites en l&iacute;nea, facilitamos la manera de realizar solicitudes de servicio, brindando respuesta oportuna y realizando la programaci&oacute;n de los mismos. Descargue los formatos seg&uacute;n su perfil.", "Texto introductorio"));

        pageContentRepository.save(new PageContent("procedimientos", "hero-title", "Servicio al <strong>Cliente</strong>", "T&iacute;tulo del hero"));
        pageContentRepository.save(new PageContent("procedimientos", "hero-sub", "Conozca los tr&aacute;mites, procedimientos e inscripciones disponibles para clientes y proveedores de Mardique.", "Subt&iacute;tulo del hero"));
        pageContentRepository.save(new PageContent("procedimientos", "section-title", "&iquest;C&oacute;mo podemos <strong>ayudarle</strong>?", "T&iacute;tulo secci&oacute;n"));

        pageContentRepository.save(new PageContent("tarifas", "hero-title", "Nuestras <strong>Tarifas</strong>", "T&iacute;tulo del hero"));
        pageContentRepository.save(new PageContent("tarifas", "hero-sub", "Consulte las tarifas de nuestros servicios portuarios, mar&iacute;timos y log&iacute;sticos. Para informaci&oacute;n detallada, nuestro equipo comercial est&aacute; disponible para atenderle.", "Subt&iacute;tulo del hero"));
        pageContentRepository.save(new PageContent("tarifas", "main-title", "Informaci&oacute;n de <span>Tarifas</span>", "T&iacute;tulo principal"));
        pageContentRepository.save(new PageContent("tarifas", "main-text", "Para conocer las tarifas actualizadas de nuestros servicios, por favor comun&iacute;quese directamente con nuestro equipo comercial.", "Texto principal"));

        pageContentRepository.save(new PageContent("galeria", "hero-title", "<strong>Galer&iacute;a</strong> de Im&aacute;genes", "T&iacute;tulo del hero"));
        pageContentRepository.save(new PageContent("galeria", "hero-sub", "Conozca nuestras instalaciones a trav&eacute;s de im&aacute;genes.", "Subt&iacute;tulo del hero"));
    }

    private void seedContacts() {
        Contact c;
        c = new Contact("Alejandro Munera", "Gerente Comercial", "316 389 5254", "amunera@spmardique.com", null, null, null);
        c.setIcono("fa-briefcase"); contactRepository.save(c);
        c = new Contact("Beisy Martinez", "Representante Legal", "317 502 5973", "bmartinez@spmardique.com", null, null, null);
        c.setIcono("fa-scale-balanced"); contactRepository.save(c);
        c = new Contact("Landrus Rodriguez", "Gerente de Operaciones", "317 438 3785", "lrodriguez@spmardique.com", null, null, null);
        c.setIcono("fa-ship"); contactRepository.save(c);
        c = new Contact("Enrique Fernandez", "Gerencia Administrativa", "317 263 5747", "efernandez@spmardique.com", null, null, null);
        c.setIcono("fa-building"); contactRepository.save(c);
        c = new Contact("Juan Laguado", "Seguridad", "312 366 1517", "seguridad@spmardique.com", null, null, null);
        c.setIcono("fa-shield-halved"); contactRepository.save(c);
        c = new Contact("Oscar Pertuz", "Documentaci\u00f3n Aduanera", "301 757 5518", "opertuz@spmardique.com", null, null, null);
        c.setIcono("fa-file-lines"); contactRepository.save(c);
        c = new Contact("Departamento RRHH", "Talento Humano", null, "talentohumano@spmardique.com", null, null, null);
        c.setIcono("fa-users"); contactRepository.save(c);
        c = new Contact("Departamento Contable", "Contabilidad", null, "contabilidad@spmardique.com", null, null, null);
        c.setIcono("fa-calculator"); contactRepository.save(c);
        c = new Contact("Coord. Operaciones", "Coordinaci\u00f3n de Operaciones", null, "operaciones@spmardique.com", "operaciones2@spmardique.com", null, null);
        c.setIcono("fa-ship"); contactRepository.save(c);
        c = new Contact("Zona Franca", "Supervisor Zona Franca", null, "zonafranca@spmardique.com", null, null, null);
        c.setIcono("fa-warehouse"); contactRepository.save(c);
        c = new Contact("Oficial de Cumplimiento", "Inscripci\u00f3n de Usuarios", null, "oficialcumplimiento@spmardique.com", null, null, null);
        c.setIcono("fa-user-check"); contactRepository.save(c);
        c = new Contact("Asistente Administrativa", "Asistente Adm. y Compras", null, "compras@spmardique.com", null, null, null);
        c.setIcono("fa-cart-shopping"); contactRepository.save(c);
    }

    private void seedConfigIfAbsent(String key, String value, String description) {
        if (systemConfigRepository.findByConfigKey(key).isEmpty()) {
            SystemConfig sc = new SystemConfig();
            sc.setConfigKey(key);
            sc.setConfigValue(value);
            sc.setDescription(description);
            systemConfigRepository.save(sc);
        }
    }
}
