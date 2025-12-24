package yenha.foodstore.Auth.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yenha.foodstore.Auth.Entity.Role;
import yenha.foodstore.Auth.Entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByName(String name);

    boolean existsByName(String name);

    Optional<User> findByPhoneNumber(String phoneNumber);

    List<User> findAllByRoleIn(List<Role> roles);

}
