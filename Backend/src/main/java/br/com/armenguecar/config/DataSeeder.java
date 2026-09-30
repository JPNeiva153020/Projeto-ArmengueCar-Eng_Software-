package br.com.armenguecar.config;

import br.com.armenguecar.entity.Client;
import br.com.armenguecar.entity.ServiceOrder;
import br.com.armenguecar.entity.StockItem;
import br.com.armenguecar.entity.User;
import br.com.armenguecar.enums.OrderStage;
import br.com.armenguecar.enums.Role;
import br.com.armenguecar.repository.ClientRepository;
import br.com.armenguecar.repository.ServiceOrderRepository;
import br.com.armenguecar.repository.StockItemRepository;
import br.com.armenguecar.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")
public class DataSeeder {

    @Bean
    CommandLineRunner seed(
            UserRepository users,
            ClientRepository clients,
            StockItemRepository stockItems,
            ServiceOrderRepository orders,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (users.count() > 0) {
                return;
            }

            createUsers(users, passwordEncoder);
            createClients(clients);
            createStockItems(stockItems);
            createOrders(orders, clients);
        };
    }

    private void createUsers(UserRepository repository, PasswordEncoder encoder) {
        repository.save(new User(
                "Cristina Ferraz",
                "cristina@armenguecar.local",
                encoder.encode("1234"),
                Role.GERENTE
        ));

        repository.save(new User(
                "Marcos Andrade",
                "marcos@armenguecar.local",
                encoder.encode("1234"),
                Role.MECANICO
        ));

        repository.save(new User(
                "Eduardo Ramos",
                "eduardo@armenguecar.local",
                encoder.encode("1234"),
                Role.CLIENTE
        ));

        repository.save(new User(
                "Paula Menezes",
                "paula@armenguecar.local",
                encoder.encode("1234"),
                Role.ADMIN
        ));
    }

    private void createClients(ClientRepository repository) {
        repository.save(client(
                "Eduardo Ramos", "(79) 99911-2233", "eduardo@email.demo",
                "QWE-4A21", "Honda Civic 2021"
        ));
        repository.save(client(
                "Ana Beatriz", "(79) 99821-4470", "ana@email.demo",
                "ABC-1D23", "Toyota Corolla 2020"
        ));
        repository.save(client(
                "Rafael Souza", "(79) 99122-7831", "rafael@email.demo",
                "KLM-9P80", "VW T-Cross 2023"
        ));
        repository.save(client(
                "Juliana Costa", "(79) 99712-1190", "juliana@email.demo",
                "HJK-7B62", "Chevrolet Onix 2022"
        ));
    }

    private void createStockItems(StockItemRepository repository) {
        saveStockItem(repository, "Tinta PU Branco", "TIN-001", 18, 10, "L");
        saveStockItem(repository, "Primer PU", "TIN-014", 7, 8, "L");
        saveStockItem(repository, "Massa plástica", "MAT-022", 26, 12, "kg");
        saveStockItem(repository, "Lixa P800", "ABR-008", 32, 20, "un");
        saveStockItem(repository, "Lixa P1200", "ABR-011", 9, 15, "un");
        saveStockItem(repository, "Parachoque Civic 2020", "PC-104", 1, 2, "un");
        saveStockItem(repository, "Farol T-Cross D", "FR-221", 4, 2, "un");
        saveStockItem(repository, "Verniz PU Alto Sólidos", "TIN-031", 5, 6, "L");
        saveStockItem(repository, "Catalisador", "TIN-044", 14, 6, "L");
    }

    private void createOrders(ServiceOrderRepository repository, ClientRepository clients) {
        Client eduardo = findClient(clients, "eduardo@email.demo");
        Client ana = findClient(clients, "ana@email.demo");
        Client rafael = findClient(clients, "rafael@email.demo");
        Client juliana = findClient(clients, "juliana@email.demo");

        repository.save(order(
                "OS-1048", eduardo, "QWE-4A21", "Honda Civic 2021",
                "Colisão dianteira: para-choque, capô e paralama.",
                OrderStage.CONTROLE_QUALIDADE, 4820, true, true, 92, "MA", 5
        ));
        repository.save(order(
                "OS-1049", ana, "ABC-1D23", "Toyota Corolla 2020",
                "Risco profundo e amassado na porta traseira.",
                OrderStage.AGUARDANDO_APROVACAO, 1950, false, false, 30, "MA", 2
        ));
        repository.save(order(
                "OS-1050", rafael, "KLM-9P80", "VW T-Cross 2023",
                "Reparo estrutural lateral e pintura.",
                OrderStage.EXECUCAO, 7350, true, true, 54, "MA", 6
        ));
        repository.save(order(
                "OS-1051", juliana, "HJK-7B62", "Chevrolet Onix 2022",
                "Pintura do teto e correção de acabamento.",
                OrderStage.DIAGNOSTICO, 0, false, false, 10, "MA", 1
        ));
        repository.save(order(
                "OS-1044", ana, "ABC-1D23", "Toyota Corolla 2020",
                "Reparo de paralama e pintura localizada.",
                OrderStage.PRONTO_ENTREGA, 3250, true, true, 100, "MA", 8
        ));
        repository.save(order(
                "OS-1041", rafael, "KLM-9P80", "VW T-Cross 2023",
                "Funilaria de porta e pintura.",
                OrderStage.AGUARDANDO_INICIO, 4100, true, true, 46, "MA", 4
        ));
    }

    private Client findClient(ClientRepository repository, String email) {
        return repository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Cliente seed não encontrado: " + email));
    }

    private Client client(String name, String phone, String email, String plate, String vehicle) {
        Client client = new Client();
        client.setName(name);
        client.setPhone(phone);
        client.setEmail(email);
        client.setPlate(plate);
        client.setVehicle(vehicle);
        return client;
    }

    private void saveStockItem(
            StockItemRepository repository,
            String name,
            String code,
            double quantity,
            double reorderPoint,
            String unit
    ) {
        StockItem item = new StockItem();
        item.setName(name);
        item.setCode(code);
        item.setQuantity(quantity);
        item.setReorderPoint(reorderPoint);
        item.setUnit(unit);
        repository.save(item);
    }

    private ServiceOrder order(
            String number,
            Client client,
            String plate,
            String vehicle,
            String problem,
            OrderStage stage,
            double value,
            boolean approved,
            boolean reserved,
            int progress,
            String ownerInitials,
            int photos
    ) {
        ServiceOrder order = new ServiceOrder();
        order.setNumber(number);
        order.setClient(client);
        order.setPlate(plate);
        order.setVehicle(vehicle);
        order.setProblem(problem);
        order.setStage(stage);
        order.setValue(value);
        order.setApproved(approved);
        order.setReserved(reserved);
        order.setProgress(progress);
        order.setOwnerInitials(ownerInitials);
        order.setPhotos(photos);
        order.setDays(Math.max(0, stage.ordinal()));
        order.setChecklist(stage == OrderStage.PRONTO_ENTREGA
                ? "[true, true, true, true, true]"
                : "[false, false, false, false, false]");
        return order;
    }
}
