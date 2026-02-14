package com.example.volrabbit_be.guild.controller;

import com.example.volrabbit_be.global.util.SecurityUtil;
import com.example.volrabbit_be.guild.dto.GuildDto;
import com.example.volrabbit_be.guild.service.GuildService;
import com.example.volrabbit_be.todo.dto.TodoDto;
import com.example.volrabbit_be.user.domain.User;
import com.example.volrabbit_be.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/guilds")
@RequiredArgsConstructor
@Tag(name = "Guild", description = "길드 관련 API")
public class GuildController {

    private final GuildService guildService;
    private final UserRepository userRepository;

    private User getUser() {
        Long userId = SecurityUtil.getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
    }

    @Operation(summary = "길드 생성", description = "새로운 길드를 생성합니다.")
    @PostMapping
    public ResponseEntity<GuildDto.Response> createGuild(@RequestBody GuildDto.CreateRequest request) {
        return ResponseEntity.ok(guildService.createGuild(getUser(), request));
    }

    @Operation(summary = "길드 상세 조회", description = "특정 길드의 상세 정보를 조회합니다.")
    @GetMapping("/{guildId}")
    public ResponseEntity<GuildDto.Response> getGuild(@PathVariable Long guildId) {
        return ResponseEntity.ok(guildService.getGuild(guildId));
    }

    @Operation(summary = "길드 정보 수정", description = "길드 정보를 수정합니다. (길드장만 가능)")
    @PatchMapping("/{guildId}")
    public ResponseEntity<GuildDto.Response> updateGuild(
            @PathVariable Long guildId,
            @RequestBody GuildDto.UpdateRequest request) {
        return ResponseEntity.ok(guildService.updateGuild(getUser(), guildId, request));
    }

    @Operation(summary = "길드 삭제", description = "길드를 삭제합니다. (길드장만 가능)")
    @DeleteMapping("/{guildId}")
    public ResponseEntity<Void> deleteGuild(@PathVariable Long guildId) {
        guildService.deleteGuild(getUser(), guildId);
        return ResponseEntity.ok().build();
    }

    // --- Member ---

    @Operation(summary = "길드 가입", description = "길드에 가입합니다. 비공개 길드는 비밀번호가 필요합니다.")
    @PostMapping("/{guildId}/join")
    public ResponseEntity<Void> joinGuild(
            @PathVariable Long guildId,
            @RequestBody(required = false) String password) {
        guildService.joinGuild(getUser(), guildId, password);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "길드 멤버 목록 조회", description = "길드 멤버 목록을 조회합니다.")
    @GetMapping("/{guildId}/members")
    public ResponseEntity<List<GuildDto.MemberResponse>> getMembers(@PathVariable Long guildId) {
        return ResponseEntity.ok(guildService.getGuildMembers(guildId));
    }

    @Operation(summary = "길드 탈퇴", description = "현재 길드에서 탈퇴합니다. url path가 me인 점 주의")
    @DeleteMapping("/{guildId}/members/me")
    public ResponseEntity<Void> leaveGuild(@PathVariable Long guildId) {
        guildService.leaveGuild(getUser(), guildId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "길드 퀘스트 제안", description = "새로운 퀘스트를 제안합니다.")
    @PostMapping("/{guildId}/quests")
    public ResponseEntity<GuildDto.QuestResponse> proposeQuest(
            @PathVariable Long guildId,
            @RequestBody GuildDto.QuestCreateRequest request) {
        return ResponseEntity.ok(guildService.proposeQuest(getUser(), guildId, request));
    }

    @Operation(summary = "길드 퀘스트 수락", description = "제안된 퀘스트를 수락합니다.")
    @PostMapping("/{guildId}/quests/{questId}/accept")
    public ResponseEntity<GuildDto.QuestResponse> acceptQuest(
            @PathVariable Long guildId,
            @PathVariable Long questId) {
        return ResponseEntity.ok(guildService.acceptQuest(getUser(), guildId, questId));
    }

    @Operation(summary = "길드 퀘스트 인증 (체크)", description = "자가 체크 방식으로 인증합니다.")
    @PostMapping("/{guildId}/quests/{questId}/check")
    public ResponseEntity<GuildDto.QuestCheckResponse> checkQuest(
            @PathVariable Long guildId,
            @PathVariable Long questId) {
        return ResponseEntity.ok(guildService.checkQuest(getUser(), guildId, questId));
    }

    @Operation(summary = "길드 퀘스트 인증 (사진)", description = "사진 인증 방식으로 인증합니다.")
    @PostMapping("/{guildId}/quests/{questId}/photo-proof")
    public ResponseEntity<GuildDto.QuestCheckResponse> photoProofQuest(
            @PathVariable Long guildId,
            @PathVariable Long questId,
            @RequestBody TodoDto.PhotoProofRequest request) {
        return ResponseEntity.ok(guildService.photoProofQuest(getUser(), guildId, questId, request.getPhotoUrl()));
    }
}
