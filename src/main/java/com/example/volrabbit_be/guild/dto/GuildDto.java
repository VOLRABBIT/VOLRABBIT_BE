package com.example.volrabbit_be.guild.dto;

import com.example.volrabbit_be.guild.domain.Guild;
import com.example.volrabbit_be.guild.domain.GuildMember;
import com.example.volrabbit_be.guild.domain.GuildQuest;
import com.example.volrabbit_be.todo.domain.enumType.DueType;
import com.example.volrabbit_be.todo.domain.enumType.RepeatType;
import com.example.volrabbit_be.todo.domain.enumType.VerificationType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class GuildDto {

    @Getter
    @NoArgsConstructor
    public static class CreateRequest {
        private String name;
        private String introduction;
        private Boolean isPrivate;
        private String password;
    }

    @Getter
    @NoArgsConstructor
    public static class UpdateRequest {
        private String name;
        private String introduction;
        private Boolean isPrivate;
        private String password;
    }

    @Getter
    @Builder
    public static class Response {
        private Long guildId;
        private String name;
        private String introduction;
        private boolean isPrivate;
        private int memberCount;

        public static Response from(Guild guild) {
            return Response.builder()
                    .guildId(guild.getId())
                    .name(guild.getName())
                    .introduction(guild.getIntroduction())
                    .isPrivate(guild.isPrivate())
                    .memberCount(guild.getMemberCount())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class MemberResponse {
        private Long userId;
        private String nickname;
        private String profileImageUrl;
        private String role;
        private LocalDateTime joinedAt;

        public static MemberResponse from(GuildMember member) {
            return MemberResponse.builder()
                    .userId(member.getUser().getId())
                    .nickname(member.getUser().getNickname())
                    .role(member.getRole().name())
                    .joinedAt(member.getCreatedDate())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    public static class QuestCreateRequest {
        private String content;
        private DueType dueType;
        private LocalDate dueDate;
        private VerificationType verificationType;
        private RepeatType repeatType;
    }

    @Getter
    @Builder
    public static class QuestResponse {
        private Long guildQuestId;
        private String status;
        private Long acceptedMemberCount;
        private int totalMemberCount;

        public static QuestResponse of(GuildQuest quest, long acceptedMemberCount, int totalMemberCount) {
            return QuestResponse.builder()
                    .guildQuestId(quest.getId())
                    .status(quest.getStatus().name())
                    .acceptedMemberCount(acceptedMemberCount)
                    .totalMemberCount(totalMemberCount)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class QuestCheckResponse {
        private Long guildQuestId;
        private String myStatus;
        private String guildStatus;
    }
}
