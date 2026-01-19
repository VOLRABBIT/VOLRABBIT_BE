package com.example.volrabbit_be.todo.repository;

import com.example.volrabbit_be.todo.domain.TodoTimeLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface TodoTimeLogRepository extends JpaRepository<TodoTimeLog, Long> {

    @Query("""
        select coalesce(sum(t.durationSeconds), 0)
        from TodoTimeLog t
        where t.todo.id = :todoId
    """)
    long sumDurationSecondsByTodoId(@Param("todoId") Long todoId);

    // 날짜별 누적 (유저 기준)
    @Query("""
        select coalesce(sum(t.durationSeconds), 0)
        from TodoTimeLog t
        where t.todo.user.id = :userId
          and function('date', t.createdDate) = :date
    """)
    long sumDurationSecondsByUserIdAndDate(
            @Param("userId") Long userId,
            @Param("date") LocalDate date
    );

    // 기간(주차) 총 누적 (유저 기준)
    @Query("""
        select coalesce(sum(t.durationSeconds), 0)
        from TodoTimeLog t
        where t.todo.user.id = :userId
          and t.createdDate >= :startInclusive
          and t.createdDate < :endExclusive
    """)
    long sumDurationSecondsByUserIdAndCreatedDateRange(
            @Param("userId") Long userId,
            @Param("startInclusive") LocalDateTime startInclusive,
            @Param("endExclusive") LocalDateTime endExclusive
    );

    // 기간(주차) 시간대별 TOP (유저 기준) — hour로 group by
    interface HourSum {
        Integer getHour();
        Long getTotalSeconds();
    }

    @Query("""
        select function('hour', t.createdDate) as hour,
               coalesce(sum(t.durationSeconds), 0) as totalSeconds
        from TodoTimeLog t
        where t.todo.user.id = :userId
          and t.createdDate >= :startInclusive
          and t.createdDate < :endExclusive
        group by function('hour', t.createdDate)
        order by coalesce(sum(t.durationSeconds), 0) desc
    """)
    List<HourSum> findTopHoursByUserIdAndCreatedDateRange(
            @Param("userId") Long userId,
            @Param("startInclusive") LocalDateTime startInclusive,
            @Param("endExclusive") LocalDateTime endExclusive,
            Pageable pageable
    );
}
