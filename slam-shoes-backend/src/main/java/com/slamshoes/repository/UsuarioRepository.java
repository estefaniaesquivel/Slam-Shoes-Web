package com.slamshoes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.slamshoes.model.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    // Agrega este método para que Spring Data JPA lo reconozca automáticamente:
    Usuario findByCorreo(String correo);

}