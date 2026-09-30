package br.com.armenguecar.repository;

import br.com.armenguecar.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    Optional<Client> findByEmailIgnoreCase(String email);
}
