package com.example.volrabbit_be.guild.domain;

import com.example.volrabbit_be.global.entity.BaseTimeEntity;
import com.example.volrabbit_be.todo.domain.Todo;
import com.example.volrabbit_be.todo.domain.enumType.DueType;
import com.example.volrabbit_be.todo.domain.enumType.RepeatType;
import com.example.volrabbit_be.todo.domain.enumType.VerificationType;
import com.example.volrabbit_be.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "guild_quests")
public class GuildQuest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guild_id", nullable = false)
    private Guild guild;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposer_id", nullable = false)
    private User proposer;

    @Column(nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DueType dueType;

    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationType verificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepeatType repeatType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestStatus status;

    @OneToMany
    @JoinTable(name = "guild_quest_todos", joinColumns = @JoinColumn(name = "guild_quest_id"), inverseJoinColumns = @JoinColumn(name = "todo_id"))
    private List<Todo> todos = new ArrayList<>();

    @Builder
    public GuildQuest(Guild guild, User proposer, String content, DueType dueType, LocalDate dueDate,
                      VerificationType verificationType, RepeatType repeatType) {
        this.guild = guild;
        this.proposer = proposer;
        this.content = content;
        this.dueType = dueType;
        this.dueDate = dueDate;
        this.verificationType = verificationType;
        this.repeatType = (repeatType != null) ? repeatType : RepeatType.NONE;
        this.status = QuestStatus.PENDING;
    }

    public void activate() {
        this.status = QuestStatus.ACTIVE;
    }

    public void complete() {
        this.status = QuestStatus.COMPLETED;
    }

    public void addTodo(Todo todo) {
        this.todos.add(todo);
    }

    public enum QuestStatus {
        PENDING, ACTIVE, COMPLETED
    }
}
