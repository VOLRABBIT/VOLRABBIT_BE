package com.example.volrabbit_be.guild.repository;

import com.example.volrabbit_be.guild.domain.GuildQuestAcceptance;
import com.example.volrabbit_be.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuildQuestAcceptanceRepository extends JpaRepository<GuildQuestAcceptance, Long> {
    boolean existsByGuildQuestIdAndUser(Long guildQuestId, User user);

    long countByGuildQuestId(Long guildQuestId);
}
