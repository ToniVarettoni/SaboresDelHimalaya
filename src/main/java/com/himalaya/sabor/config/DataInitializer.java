package com.himalaya.sabor.config;

import com.himalaya.sabor.model.*;
import com.himalaya.sabor.repository.CategoryRepository;
import com.himalaya.sabor.repository.ProductRepository;
import com.himalaya.sabor.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           CategoryRepository categoryRepository,
                           ProductRepository productRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() == 0 && categoryRepository.count() == 0) {
            log.info("Base de datos vacía. Iniciando la carga de datos de prueba...");

            // 1. Usuarios del sistema
            User admin = new User(
                    "Administrador Himalaya",
                    "admin@himalaya.com",
                    passwordEncoder.encode("admin123"),
                    "Calle Mayor 10, Madrid",
                    "600111222",
                    Role.ADMIN,
                    UserStatus.ACTIVA
            );
            admin.setCreatedAt(LocalDateTime.now());
            userRepository.save(admin);

            User chef = new User(
                    "Chef Himalaya",
                    "chef@himalaya.com",
                    passwordEncoder.encode("chef123"),
                    "Calle Mayor 12, Madrid",
                    "600333444",
                    Role.CHEF,
                    UserStatus.ACTIVA
            );
            chef.setCreatedAt(LocalDateTime.now());
            userRepository.save(chef);

            log.info("Usuarios creados: admin@himalaya.com / chef@himalaya.com");

            // 2. Categorías
            Map<String, Category> categories = new HashMap<>();

            Category entrantes = new Category("Entrantes", "entrantes");
            Category ensaladas = new Category("Ensaladas", "ensaladas");
            Category tandoori = new Category("Tandoori", "tandoori");
            Category postres = new Category("Postres", "postres");
            Category bebidasNepal = new Category("Bebidas típicas nepalíes", "bebidas-tipicas-nepalies");
            Category bebidasTrad = new Category("Bebidas tradicionales", "bebidas-tradicionales");

            categories.put("entrantes", categoryRepository.save(entrantes));
            categories.put("ensaladas", categoryRepository.save(ensaladas));
            categories.put("tandoori", categoryRepository.save(tandoori));
            categories.put("postres", categoryRepository.save(postres));
            categories.put("bebidas-nepal", categoryRepository.save(bebidasNepal));
            categories.put("bebidas-trad", categoryRepository.save(bebidasTrad));

            log.info("6 categorías creadas exitosamente.");

            // 3. 10 Platos de prueba
            productRepository.save(new Product(
                    categories.get("entrantes"),
                    "Samosa Nepalí",
                    "Empanadillas triangulares crujientes rellenas de patatas especiadas, guisantes, comino y cilantro fresco.",
                    new BigDecimal("4.50"),
                    ProductStatus.ACTIVO
            ));

            productRepository.save(new Product(
                    categories.get("entrantes"),
                    "Tarkari Raas",
                    "Sopa tradicional nepalí elaborada con caldo de verduras de temporada, jengibre, cúrcuma y especias del Himalaya.",
                    new BigDecimal("5.00"),
                    ProductStatus.ACTIVO
            ));

            productRepository.save(new Product(
                    categories.get("entrantes"),
                    "Kukhura Ko Raas",
                    "Sopa reconfortante de pollo campero desmigado con caldo aromático, cilantro, jengibre y pimienta de Sichuan (Timur).",
                    new BigDecimal("5.50"),
                    ProductStatus.ACTIVO
            ));

            productRepository.save(new Product(
                    categories.get("ensaladas"),
                    "Ensalada Himalaya",
                    "Mezclum de brotes frescos, pepino, cebolla roja, tomate, rábano y queso fresco especiado con aliño de mostaza y limón.",
                    new BigDecimal("6.50"),
                    ProductStatus.ACTIVO
            ));

            productRepository.save(new Product(
                    categories.get("tandoori"),
                    "Pollo Tandoori",
                    "Muslos de pollo marinados durante 24 horas en yogur, garam masala, jengibre, ajo y asados al horno tandoor tradicional.",
                    new BigDecimal("11.50"),
                    ProductStatus.ACTIVO
            ));

            productRepository.save(new Product(
                    categories.get("tandoori"),
                    "Paneer Tikka Tandoori",
                    "Dados de queso fresco casero (paneer) marinados en especias suaves y asados con pimientos y cebollas al tandoor.",
                    new BigDecimal("10.00"),
                    ProductStatus.ACTIVO
            ));

            productRepository.save(new Product(
                    categories.get("postres"),
                    "Kheer Nepalí",
                    "Pudin tradicional de arroz basmati cocido a fuego lento con leche entera, cardamomo verde, azafrán, pasas y almendras.",
                    new BigDecimal("4.00"),
                    ProductStatus.ACTIVO
            ));

            productRepository.save(new Product(
                    categories.get("postres"),
                    "Sikarni",
                    "Postre celestial de yogur colado cremoso sazonado con azafrán, canela, pistachos picados y miel del Himalaya.",
                    new BigDecimal("4.50"),
                    ProductStatus.ACTIVO
            ));

            productRepository.save(new Product(
                    categories.get("bebidas-nepal"),
                    "Lassi de Mango",
                    "Bebida cremosa y refrescante a base de yogur natural batido con pulpa de mango Alphonso y toque de cardamomo.",
                    new BigDecimal("3.50"),
                    ProductStatus.ACTIVO
            ));

            productRepository.save(new Product(
                    categories.get("bebidas-trad"),
                    "Té Masala Chai",
                    "Té negro infusionado con leche entera, cardamomo, clavo, canela, jengibre fresco y azúcar de caña.",
                    new BigDecimal("2.50"),
                    ProductStatus.ACTIVO
            ));

            log.info("10 platos de prueba insertados con éxito.");
        } else {
            log.info("La base de datos ya contiene registros. Se omite el seeding inicial.");
        }
    }
}
