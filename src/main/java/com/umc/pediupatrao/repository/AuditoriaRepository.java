package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.RegistroAuditoria;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditoriaRepository extends MongoRepository<RegistroAuditoria, String> {
    List<RegistroAuditoria> findAllByOrderByDataHoraDesc();
}