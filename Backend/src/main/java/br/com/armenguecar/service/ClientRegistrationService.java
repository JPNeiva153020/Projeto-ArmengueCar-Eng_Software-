package br.com.armenguecar.service;
import br.com.armenguecar.dto.ClientDtos.CreateClientRequest;
import br.com.armenguecar.dto.UserDtos.CreateUserRequest;
import br.com.armenguecar.entity.Client;
import br.com.armenguecar.enums.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
@Service
public class ClientRegistrationService {
 private final ClientService clients; private final UserService users;
 public ClientRegistrationService(ClientService clients,UserService users){this.clients=clients;this.users=users;}
 @Transactional public Client create(String name,String phone,String email,String password,String plate,String vehicle,UUID actor) {
  var user=users.create(new CreateUserRequest(name,email,password,Role.CLIENTE),actor);
  Client client=clients.create(new CreateClientRequest(name,phone,email,plate,vehicle));
  return clients.linkUser(client.getId(),user.getId(),actor);
 }
}
