package com.example.volrabbit_be.guild.repository;

import com.example.volrabbit_be.guild.domain.GuildQuest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GuildQuestRepository extends JpaRepository<GuildQuest, Long> {
    List<GuildQuest> findByGuildId(Long guildId);
}
