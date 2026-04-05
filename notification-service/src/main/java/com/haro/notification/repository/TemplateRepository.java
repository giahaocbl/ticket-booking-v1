package com.haro.notification.repository;

import com.haro.notification.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TemplateRepository extends JpaRepository<Template, UUID> {
    Optional<Template> findByChannelAndName(String channel, String name);
}
