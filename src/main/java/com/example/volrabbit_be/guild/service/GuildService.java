package com.example.volrabbit_be.guild.service;

import com.example.volrabbit_be.guild.domain.*;
import com.example.volrabbit_be.guild.domain.Guild.GuildBuilder;
import com.example.volrabbit_be.guild.dto.GuildDto;
import com.example.volrabbit_be.guild.repository.*;
import com.example.volrabbit_be.todo.domain.Category;
import com.example.volrabbit_be.todo.domain.Todo;
import com.example.volrabbit_be.todo.domain.enumType.RepeatType;
import com.example.volrabbit_be.todo.domain.enumType.SegmentationType;
import com.example.volrabbit_be.todo.repository.CategoryRepository;
import com.example.volrabbit_be.todo.repository.TodoRepository;
import com.example.volrabbit_be.user.domain.User;
import com.example.volrabbit_be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuildService {

    private final GuildRepository guildRepository;
    private final GuildMemberRepository guildMemberRepository;
    private final GuildQuestRepository guildQuestRepository;
    private final GuildQuestAcceptanceRepository guildQuestAcceptanceRepository;
    private final TodoRepository todoRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public GuildDto.Response createGuild(User user, GuildDto.CreateRequest request) {
        if (guildRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("이미존재하는 길드명입니다.");
        }

        if (guildMemberRepository.existsByUser(user)) {
            throw new IllegalStateException("이미 가입된 길드가 있습니다.");
        }

        Guild guild = Guild.builder()
                .name(request.getName())
                .introduction(request.getIntroduction())
                .isPrivate(request.getIsPrivate() != null && request.getIsPrivate())
                .password(request.getPassword())
                .build();

        guildRepository.save(guild);

        GuildMember member = GuildMember.builder()
                .guild(guild)
                .user(user)
                .role(GuildMember.GuildRole.LEADER)
                .build();

        guildMemberRepository.save(member);

        return GuildDto.Response.from(guild);
    }

    public GuildDto.Response getGuild(Long guildId) {
        Guild guild = guildRepository.findById(guildId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 길드입니다."));
        return GuildDto.Response.from(guild);
    }

    @Transactional
    public GuildDto.Response updateGuild(User user, Long guildId, GuildDto.UpdateRequest request) {
        Guild guild = guildRepository.findById(guildId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 길드입니다."));

        validateLeader(guild, user);

        if (request.getName() != null && !request.getName().equals(guild.getName())) {
            if (guildRepository.existsByName(request.getName())) {
                throw new IllegalArgumentException("이미 사용 중인 길드명입니다.");
            }
        }

        guild.updateguild(request.getName(), request.getIntroduction(), request.getIsPrivate(), request.getPassword());
        return GuildDto.Response.from(guild);
    }

    @Transactional
    public void deleteGuild(User user, Long guildId) {
        Guild guild = guildRepository.findById(guildId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 길드입니다."));

        validateLeader(guild, user);
        guildRepository.delete(guild);
    }

    @Transactional
    public void joinGuild(User user, Long guildId, String password) {
        if (guildMemberRepository.existsByUser(user)) {
            throw new IllegalStateException("이미 가입된 길드가 있습니다.");
        }

        Guild guild = guildRepository.findById(guildId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 길드입니다."));

        if (guild.isPrivate()) {
            if (password == null || !password.equals(guild.getPassword())) {
                throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
            }
        }

        GuildMember member = GuildMember.builder()
                .guild(guild)
                .user(user)
                .role(GuildMember.GuildRole.MEMBER)
                .build();

        guildMemberRepository.save(member);
        guild.increaseMemberCount();
    }

    public List<GuildDto.MemberResponse> getGuildMembers(Long guildId) {
        Guild guild = guildRepository.findById(guildId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 길드입니다."));

        return guildMemberRepository.findByGuild(guild).stream()
                .map(GuildDto.MemberResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void leaveGuild(User user, Long guildId) {
        Guild guild = guildRepository.findById(guildId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 길드입니다."));

        GuildMember member = guildMemberRepository.findByGuildAndUser(guild, user)
                .orElseThrow(() -> new IllegalArgumentException("가입된 정보가 없습니다."));

        if (member.getRole() == GuildMember.GuildRole.LEADER) {
            throw new IllegalStateException("길드장은 탈퇴할 수 없습니다.");
        }

        guildMemberRepository.delete(member);
        guild.decreaseMemberCount();
    }

    @Transactional
    public GuildDto.QuestResponse proposeQuest(User user, Long guildId, GuildDto.QuestCreateRequest request) {
        Guild guild = guildRepository.findById(guildId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 길드입니다."));

        if (!guildMemberRepository.existsByGuildAndUser(guild, user)) {
            throw new IllegalArgumentException("길드원이 아닙니다.");
        }

        GuildQuest quest = GuildQuest.builder()
                .guild(guild)
                .proposer(user)
                .content(request.getContent())
                .dueType(request.getDueType())
                .dueDate(request.getDueDate())
                .verificationType(request.getVerificationType())
                .repeatType(request.getRepeatType())
                .build();

        guildQuestRepository.save(quest);

        return GuildDto.QuestResponse.of(quest, 0, guild.getMemberCount());
    }

    @Transactional
    public GuildDto.QuestResponse acceptQuest(User user, Long guildId, Long questId) {
        Guild guild = guildRepository.findById(guildId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 길드입니다."));

        GuildQuest quest = guildQuestRepository.findById(questId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 퀘스트입니다."));

        if (guildQuestAcceptanceRepository.existsByGuildQuestIdAndUser(questId, user)) {
            throw new IllegalStateException("이미 수락한 퀘스트입니다.");
        }

        GuildQuestAcceptance acceptance = GuildQuestAcceptance.builder()
                .guildQuest(quest)
                .user(user)
                .build();
        guildQuestAcceptanceRepository.save(acceptance);

        long acceptanceCount = guildQuestAcceptanceRepository.countByGuildQuestId(questId);
        int totalMembers = guild.getMemberCount();

        if (acceptanceCount >= totalMembers) {
            activateQuest(quest, guild);
        }

        return GuildDto.QuestResponse.of(quest, acceptanceCount, totalMembers);
    }

    private void activateQuest(GuildQuest quest, Guild guild) {
        quest.activate();

        List<GuildMember> members = guildMemberRepository.findByGuild(guild);

        for (GuildMember member : members) {
            User memberUser = member.getUser();
            Category category = getOrCreateGuildCategory(memberUser);

            Todo todo = Todo.builder()
                    .user(memberUser)
                    .category(category)
                    .content(quest.getContent())
                    .dueDate(quest.getDueDate() != null ? quest.getDueDate() : LocalDate.now())
                    .dueType(quest.getDueType())
                    .repeatType(quest.getRepeatType() != null ? quest.getRepeatType() : RepeatType.NONE)
                    .verificationType(quest.getVerificationType())
                    .segmentationType(SegmentationType.NONE)
                    .build();

            todoRepository.save(todo);
            quest.addTodo(todo);
        }
    }

    private Category getOrCreateGuildCategory(User user) {
        String categoryName = "길드 퀘스트";
        return categoryRepository.findAllByUserIdAndIsActiveTrueOrderByIdAsc(user.getId()).stream()
                .filter(c -> c.getName().equals(categoryName))
                .findFirst()
                .orElseGet(() -> {
                    Category newCat = Category.builder()
                            .user(user)
                            .name(categoryName)
                            .build();
                    return categoryRepository.save(newCat);
                });
    }

    @Transactional
    public GuildDto.QuestCheckResponse checkQuest(User user, Long guildId, Long questId) {
        return handleQuestVerification(user, guildId, questId);
    }

    @Transactional
    public GuildDto.QuestCheckResponse photoProofQuest(User user, Long guildId, Long questId, String photoUrl) {
        return handleQuestVerification(user, guildId, questId);
    }

    private GuildDto.QuestCheckResponse handleQuestVerification(User user, Long guildId, Long questId) {
        GuildQuest quest = guildQuestRepository.findById(questId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 퀘스트입니다."));

        Todo myTodo = quest.getTodos().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("해당 퀘스트에 대한 투두를 찾을 수 없습니다."));

        if (myTodo.getTodoStatus() != com.example.volrabbit_be.todo.domain.enumType.todoStatus.SUCCESS) {
            myTodo.complete();
        }

        boolean allComplete = quest.getTodos().stream()
                .allMatch(t -> t.getTodoStatus() == com.example.volrabbit_be.todo.domain.enumType.todoStatus.SUCCESS);

        if (allComplete) {
            quest.complete();
        }

        return GuildDto.QuestCheckResponse.builder()
                .guildQuestId(quest.getId())
                .myStatus(myTodo.getTodoStatus().name())
                .guildStatus(quest.getStatus().name())
                .build();
    }

    private void validateLeader(Guild guild, User user) {
        GuildMember member = guildMemberRepository.findByGuildAndUser(guild, user)
                .orElseThrow(() -> new IllegalArgumentException("길드 멤버가 아닙니다."));
        if (member.getRole() != GuildMember.GuildRole.LEADER) {
            throw new SecurityException("길드장 권한이 필요합니다.");
        }
    }
}
