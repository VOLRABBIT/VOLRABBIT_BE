package com.example.volrabbit_be.guild.repository;

import com.example.volrabbit_be.guild.domain.Guild;
import com.example.volrabbit_be.guild.domain.GuildMember;
import com.example.volrabbit_be.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GuildMemberRepository extends JpaRepository<GuildMember, Long> {
    Optional<GuildMember> findByGuildAndUser(Guild guild, User user);

    List<GuildMember> findByGuild(Guild guild);

    boolean existsByGuildAndUser(Guild guild, User user);

    boolean existsByUser(User user);

    Optional<GuildMember> findByUser(User user);
}
