package com.himalaya.sabor;

import com.himalaya.sabor.model.*;
import com.himalaya.sabor.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SaboresDelHimalayaApplicationTests {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private DailyMenuRepository dailyMenuRepository;

	@Autowired
	private DailyMenuItemRepository dailyMenuItemRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	@DisplayName("El contexto de Spring Boot carga correctamente")
	void contextLoads() {
	}

	@Test
	@DisplayName("El Seeder inicializa usuarios administradores y chef")
	void seederInitializesUsers() {
		Optional<User> admin = userRepository.findByEmail("admin@himalaya.com");
		assertThat(admin).isPresent();
		assertThat(admin.get().getRole()).isEqualTo(Role.ADMIN);
		assertThat(admin.get().getStatus()).isEqualTo(UserStatus.ACTIVA);
		assertThat(passwordEncoder.matches("admin123", admin.get().getPassword())).isTrue();

		Optional<User> chef = userRepository.findByEmail("chef@himalaya.com");
		assertThat(chef).isPresent();
		assertThat(chef.get().getRole()).isEqualTo(Role.CHEF);
		assertThat(chef.get().getStatus()).isEqualTo(UserStatus.ACTIVA);
		assertThat(passwordEncoder.matches("chef123", chef.get().getPassword())).isTrue();
	}

	@Test
	@DisplayName("El Seeder inicializa las 6 categorías y 10 productos")
	void seederInitializesCategoriesAndProducts() {
		assertThat(categoryRepository.count()).isGreaterThanOrEqualTo(6);
		assertThat(productRepository.count()).isGreaterThanOrEqualTo(10);

		Optional<Category> entrantes = categoryRepository.findBySlug("entrantes");
		assertThat(entrantes).isPresent();

		var entrantesProducts = productRepository.findByCategoryAndStatus(entrantes.get(), ProductStatus.ACTIVO);
		assertThat(entrantesProducts).isNotEmpty();
	}

	@Test
	@DisplayName("Verificación de mapeo JPA para Menú Diario y sus Ítems")
	void testDailyMenuMappings() {
		LocalDate testDate = LocalDate.now().plusDays(1);
		DailyMenu dailyMenu = new DailyMenu(testDate, true);

		Product sampleProduct = productRepository.findAll().get(0);
		DailyMenuItem item1 = new DailyMenuItem(dailyMenu, sampleProduct, CourseType.ENTRANTE);
		DailyMenuItem item2 = new DailyMenuItem(dailyMenu, "Sopa Especial", "Sopa artesanal fuera de carta", CourseType.ENTRANTE);

		dailyMenu.addItem(item1);
		dailyMenu.addItem(item2);

		DailyMenu saved = dailyMenuRepository.save(dailyMenu);
		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getPrice()).isEqualByComparingTo(new BigDecimal("15.00"));
		assertThat(saved.getItems()).hasSize(2);
	}

	@Test
	@DisplayName("Verificación de mapeo JPA para Pedido (Order) y sus Ítems")
	void testOrderMappings() {
		User user = userRepository.findByEmail("admin@himalaya.com").orElseThrow();
		Product product = productRepository.findAll().get(0);

		Order order = new Order();
		order.setOrderNumber("PED-20261107-TEST");
		order.setUser(user);
		order.setDeliveryAddress("Calle Gran Vía 123");
		order.setPhone("699888777");
		order.setPaymentMethod(PaymentMethod.TARJETA);
		order.setTotalAmount(new BigDecimal("9.00"));
		order.setStatus(OrderStatus.RECIBIDO);
		order.setCreatedAt(LocalDateTime.now());

		OrderItem item = new OrderItem(order, product, product.getPrice(), 2, new BigDecimal("9.00"));
		order.addItem(item);

		Order saved = orderRepository.save(order);
		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getOrderNumber()).isEqualTo("PED-20261107-TEST");
		assertThat(saved.getItems()).hasSize(1);
		assertThat(saved.getStatus()).isEqualTo(OrderStatus.RECIBIDO);
	}
}
