package com.example.MardiqueWeb.Config;

import com.example.MardiqueWeb.Entity.*;
import com.example.MardiqueWeb.Repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class IntranetDataSeeder implements CommandLineRunner {

    private final CalendarEventRepository calendarRepo;
    private final ConversationRepository conversationRepo;
    private final IntranetAreaRepository areaRepo;

    public IntranetDataSeeder(CalendarEventRepository calendarRepo,
                              ConversationRepository conversationRepo,
                              IntranetAreaRepository areaRepo) {
        this.calendarRepo = calendarRepo;
        this.conversationRepo = conversationRepo;
        this.areaRepo = areaRepo;
    }

    @Override
    public void run(String... args) {
        seedCalendar();
        seedConversations();
        seedAreas();
    }

    private static final String F = "/images/Logo.png";

    private void seedCalendar() {
        if (calendarRepo.count() > 0) return;
        String[][] events = {
            {"2026-08-01", "Inicio mes fiscal", "general", "Apertura del ciclo fiscal mensual"},
            {"2026-08-05", "Reunión operativa", "reunion", "Coordinación de operaciones portuarias"},
            {"2026-08-07", "Cumpleaños Carlos M.", "cumple", "Felicitaciones al equipo"},
            {"2026-08-12", "Auditoría interna", "reunion", "Revisión de procesos y cumplimiento"},
            {"2026-08-15", "Capacitación Power BI", "capacitacion", "Taller práctico de dashboards"},
            {"2026-08-19", "Comité de convivencia", "reunion", "Reunión mensual de bienestar"},
            {"2026-08-22", "Cumpleaños Ana G.", "cumple", "Felicitaciones al equipo"},
            {"2026-08-28", "Cierre contable mensual", "general", "Reportes financieros del mes"},
            {"2026-09-01", "Inicio Q4", "general", "Inicio del cuarto trimestre"},
            {"2026-09-10", "Día de la Independencia", "feriado", "Feriado nacional"},
            {"2026-09-15", "Capacitación SST", "capacitacion", "Seguridad y salud en el trabajo"},
            {"2026-07-11", "Capacitación SAP", "capacitacion", "Módulo de logística"},
            {"2026-07-14", "Reunión de gerencia", "reunion", "Estrategia trimestral"},
            {"2026-07-18", "Cumpleaños corporativo", "cumple", "Celebración colectiva"},
            {"2026-07-20", "Día de la Independencia", "feriado", "Feriado nacional"},
            {"2026-07-22", "Taller de liderazgo", "capacitacion", "Desarrollo de habilidades gerenciales"},
            {"2026-07-25", "Revisión presupuesto Q3", "reunion", "Análisis financiero trimestral"},
            {"2026-07-28", "Entrega de reportes", "general", "Cierre de indicadores"}
        };
        for (String[] e : events) {
            CalendarEvent ev = new CalendarEvent();
            ev.setTitle(e[1]);
            ev.setDate(LocalDate.parse(e[0]));
            ev.setType(e[2]);
            ev.setDescription(e[3]);
            ev.setAuthorName("Sistema");
            calendarRepo.save(ev);
        }
    }

    private void seedConversations() {
        if (conversationRepo.count() > 0) return;
        String[][] convos = {
            {"Johnier Gomez", "Probando el nuevo foro de la intranet. ¡Está genial!", "Discusión"},
            {"Carlos Martinez", "Recordatorio: la capacitación de seguridad industrial es el viernes a las 3pm en el salón principal.", "Discusión"},
            {"Adriana Rios", "Felicitaciones a todo el equipo de operaciones por superar las metas del trimestre. ¡Excelente trabajo!", "Elogio"},
            {"Ramiro Rodelo", "¿Alguien tiene el manual actualizado de procedimientos portuarios? Necesito la versión de julio 2026.", "Pregunta"},
            {"Laura Mendez", "¿Qué horarios tienen disponibles para la reunión de planificación estratégica la próxima semana?", "Sondeo"}
        };
        for (String[] c : convos) {
            Conversation conv = new Conversation();
            conv.setAuthorName(c[0]);
            conv.setText(c[1]);
            conv.setType(c[2]);
            conv.setLikes((int)(Math.random() * 10));
            conv.setComments((int)(Math.random() * 5));
            conversationRepo.save(conv);
        }
    }

    private void seedAreas() {
        if (areaRepo.count() > 0) return;

        saveArea("Gerencia",
            "Dirección estratégica de la Sociedad Portuaria Mardique S.A. Equipo gerencial encargado de la toma de decisiones, planificación corporativa y liderazgo organizacional.",
            j(
                c("Alejandro Munera","Gerente Comercial","amunera@spmardique.com","316 3895254","/images/fotos/AlejandroMunera.png"),
                c("Landrus Rodriguez","Gerente de Operaciones","gerenteoperaciones@spmardique.com","317 4383785","/images/fotos/LandrusRodriguez.png"),
                c("Enrique Fernandez","Gerente Administrativo y Financiero","gerenteadministrativo@spmardique.com","317 2635747","/images/fotos/EnriqueFernandez.png")
            ),
            "/images/areas/Gerencia.png");

        saveArea("Sistemas",
            "Infraestructura tecnológica, soporte TI, desarrollo de soluciones digitales, seguridad informática y gestión de sistemas de información de la organización.",
            j(
                c("Ramiro Rodelo del Valle","Coordinador de Tecnología","coordinadortecnologia@spmardique.com","318 7243180","/images/fotos/RamiroRodelo.png"),
                c("Adelino Aragon Berrio","Analista TI","tecnicosistemas@spmardique.com","320 4214591","/images/fotos/AdelinoAragon.png"),
                c("Omar Caicedo","Analista TI","analistati@spmardique.com","315 1123402","/images/fotos/OmarCaicedo.png"),
                c("Johnnier Gomez Marrugo","Aprendiz TI","aprendizti@spmardique.com","","/images/fotos/JohnnierGomez.png")
            ),
            "/images/areas/Sistemas.png");

        saveArea("Talento Humano",
            "Administración del capital humano, reclutamiento, selección, nómina, bienestar laboral, capacitación y desarrollo organizacional.",
            j(
                c("Adriana Meola Patiño","Coordinadora de TH","coordinadortalento@spmardique.com","317 3660183","/images/fotos/AdrianaMeola.png"),
                c("Duvan Simancas","Analista de Talento Humano","analistatalento@spmardique.com","317 3660183","/images/fotos/DuvanSimancas.png"),
                c("Valentina Ospino","Aprendiz de Talento Humano","aprendiztalento@spmardique.com","317 3660183")
            ),
            "/images/areas/TalentoHumano.png");

        saveArea("Comercial",
            "Desarrollo de negocio portuario, gestión de clientes, cotizaciones, contratos comerciales y relaciones comerciales con navieras y operadores.",
            j(
                c("Alejandro Munera","Gerente Comercial","amunera@spmardique.com","316 3895254","/images/fotos/AlejandroMunera.png")
            ),
            "/images/areas/Comercial.png");

        saveArea("Compras",
            "Gestión de compras, abastecimiento, proveedores, licitaciones y adquisición de bienes y servicios para la operación portuaria.",
            j(
                c("Janice Hernandez Sierra","Coordinadora de Compras","coordinadorcompras@spmardique.com","318 0808361","/images/fotos/JaniceHernandez.png"),
                c("Estefany Simancas","Analista de Compras","auxiliardecompras@spmardique.com","301 2985964","/images/fotos/EstefanySimancas.png"),
                c("Alexa Mendez","Auxiliar de Compras","auxiliardecompras@spmardique.com","","/images/fotos/AlexaMendez.png")
            ),
            "/images/areas/Compras.png");

        saveArea("Jurídica",
            "Asesoría legal corporativa, contratos, cumplimiento normativo, gestión de riesgos legales y representación legal de la empresa.",
            j(
                c("Beisy Martinez","Representante Legal","bmartinez@spmardique.com","317 5025973","/images/fotos/BeisyMartinez.png")
            ),
            "/images/areas/Juridica.png");

        saveArea("HSEQ",
            "Gestión de la seguridad y salud en el trabajo, medio ambiente, calidad, prevención de riesgos y emergencias industriales.",
            j(
                c("Laura Clavijo Barboza","Coordinadora de HSEQ","coordinadorhseq@spmardique.com","300 6400024"),
                c("Danis Jimenez Altamar","Supervisor de Servicios Generales","supervisorsserviciosgenerales@spmardique.com","","/images/fotos/DannysJimenez.png"),
                c("Rosa Martinez Aleman","Supervisor de Calidad","supervisorcalidad@spmardique.com","","/images/fotos/RosaMartinez.png"),
                c("Ana Peñaranda","Supervisor HSEQ","supervisorhseq@spmardique.com","316 4177418","/images/fotos/AnaPeñaranda.png"),
                c("Luis David Gaces Julio","Auxiliar HSEQ","auxiliarhseq@spmardique.com","","")
            ),
            "/images/areas/HSEQ.png");

        saveArea("Operaciones",
            "Coordinación de operaciones portuarias de carga, descarga, logística, estiba y control de inventarios en muelles y bodegas.",
            j(
                c("Landrus Rodriguez","Gerente de Operaciones","gerenteoperaciones@spmardique.com","317 4383785","/images/fotos/LandrusRodriguez.png"),
                c("Jainer Ballestas","Coordinador de Operaciones","coordinadorcoque@spmardique.com","","/images/fotos/JainerBallestas.png"),
                c("Oscar Arrieta Maturana","Coordinador de Operaciones","coordinadorgranel@spmardique.com","","/images/fotos/OscarArrieta.png"),
                c("Efren Fernandez","Supervisor de Operaciones","","300 6193177"),
                c("Claudia Rivera","Analista de Operaciones","analistadeoperaciones4@spmardique.com","300 3634061"),
                c("Dayana Puello","Analista de Operaciones","analistagranel@spmardique.com","315 6152678","/images/fotos/DayanaPuello.png"),
                c("Hernan Lara del Valle","Analista de Datos","analistadatos@spmardique.com","324 5880693","/images/fotos/HernanLara.png")
            ),
            "/images/areas/Operaciones.png");

        saveArea("Documentación y Báscula",
            "Control de documentación portuaria, trámites aduaneros, pesaje de vehículos, gestión de guías y verificación de cargas.",
            j(
                c("Yarledis Perez","Supervisora de Documentación y Báscula","supervisoradocumentacion@spmardique.com","301 4706451","/images/fotos/YarledisPerez.png"),
                c("Oscar Pertuz","Coordinador de Operaciones Aduana","coordinadordocumentacion@spmardique.com","","/images/fotos/OscarPertuz.png")
            ),
            "/images/areas/Documentacion.png");

        saveArea("Contabilidad",
            "Gestión contable, reportes financieros, cuentas por pagar, cuentas por cobrar, conciliaciones y cumplimiento tributario.",
            j(
                c("Carlos Efrain Molina","Coordinador de Contabilidad","coordinadorcontable@spmardique.com","","/images/fotos/CarlosMolina.png"),
                c("Sebastian Vasquez","Analista Contable","svasquez@spmardique.com","350 3113607","/images/fotos/SebastianVasquez.png"),
                c("Yuliana Ramirez","Aprendiz de Contabilidad","auxiliarcontable@spmardique.com","350 3113607","/images/fotos/YulianaRamirez.png"),
                c("Yordanis Chavez","Analista Cuentas por Pagar","ychavez@spmardique.com","","/images/fotos/YordanisChavez.png")
            ),
            "/images/areas/Contabilidad.png");

        saveArea("Seguridad",
            "Control de acceso, vigilancia, seguridad perimetral, operación CCTV y protección de activos e instalaciones portuarias.",
            j(
                c("Juan Gabriel Laguado Peña","Coordinador de Seguridad","coordinadorseguridad@spmardique.com","315 4667140","/images/fotos/JunaLaguado.png"),
                c("Jose Yair Campo Orozco","Supervisor de Seguridad","superivisorseguridad@spmardique.com","","/images/fotos/JosejairCampo.png"),
                c("Yaelis Borja","Supervisor Control de Acceso","controldeacceso@spmardique.com","310 7639839")
            ),
            "/images/areas/Seguridad.png");

        saveArea("Almacén",
            "Gestión de almacén, control de inventarios, recepción y despacho de mercancía, almacenamiento y manejo de materiales.",
            j(
                c("Jorge Torres","Analista de Almacén","almacen@spmardique.com","315 4585285","/images/fotos/JorgeTorres.png"),
                c("Luis Alejandro Aleans","Auxiliar de Compras y Almacén","almacen@spmardique.com","",""),
                c("Emmanuel Cañate","Aprendiz de Almacén","almacen@spmardique.com","310 7947268","/images/fotos/EmmanuelCañate.png")
            ),
            "/images/areas/Almacen.png");

        saveArea("Protección",
            "Control de acceso de carga, protección de mercancías, custodia y control de ingreso de vehículos y contenedores al puerto.",
            j(
                c("Cindy Sabalza","Auxiliar de Control de Acceso Carga","controldeaccesocarga@spmardique.com","",""),
                c("Yerlys Gallo Vargas","Auxiliar de Control de Acceso Carga","controldeaccesocarga@spmardique.com","","")
            ),
            "/images/areas/Proteccion.png");

        saveArea("Mantenimiento",
            "Mantenimiento preventivo y correctivo de infraestructura, equipos portuarios, grúas, maquinaria pesada e instalaciones.",
            j(
                c("Daniel Barrios","Coordinador de Mantenimiento","coordinadormantenimiento@spmardique.com","310 3523238","/images/fotos/DanielBarrios.png"),
                c("Munir Jassir","Planner de Mantenimiento","planeadormantenimiento@spmardique.com","318 1313904","/images/fotos/MunirJassir.png")
            ),
            "/images/areas/Mantenimiento.png");

        saveArea("Administrativo y Financiero",
            "Dirección administrativa y financiera, planeación estratégica, presupuesto, tesorería y gestión de recursos financieros.",
            j(
                c("Enrique Fernandez","Gerente Administrativo y Financiero","gerenteadministrativo@spmardique.com","317 2635747","/images/fotos/EnriqueFernandez.png")
            ),
            "/images/areas/Administrativo.png");

        saveArea("Zona Franca",
            "Gestión de zona franca industrial, oficial de cumplimiento, operaciones logísticas en zona franca y relacionamiento con usuarios.",
            j(
                c("Francisco Milano","Oficial de Cumplimiento","oficialdecumplimiento@spmardique.com","350 3113600","/images/fotos/FranciscoMilano.png"),
                c("Cristian Franco","Zona Franca Bogotá","","")
            ),
            "/images/areas/ZonaFranca.png");

        saveArea("Oficina DIAN",
            "Atención y gestión de trámites ante la Dirección de Impuestos y Aduanas Nacionales, despacho aduanero y cumplimiento tributario.",
            j(
                c("Usuario DIAN","Sistemas","","")
            ),
            "/images/areas/DIAN.png");
    }

    private String c(String nombre, String cargo, String email, String telefono, String... fotoArr) {
        String foto = fotoArr.length > 0 && !fotoArr[0].isEmpty() ? fotoArr[0] : F;
        return "{\"nombre\":\"" + nombre + "\",\"cargo\":\"" + cargo + "\",\"email\":\"" + email + "\",\"telefono\":\"" + telefono + "\",\"foto\":\"" + foto + "\"}";
    }

    private String j(String... items) {
        return "[" + String.join(",", items) + "]";
    }

    private void saveArea(String nombre, String descripcion, String contactos, String cover) {
        IntranetArea area = new IntranetArea();
        area.setNombre(nombre);
        area.setDescripcion(descripcion);
        area.setContactos(contactos);
        area.setCover(cover);
        area.setInforme("");
        area.setSitio("");
        areaRepo.save(area);
    }
}
