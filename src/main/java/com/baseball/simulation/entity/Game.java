package com.baseball.simulation.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "games")
@Getter
@Setter
@NoArgsConstructor
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_a_id")
    private Team teamA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_b_id")
    private Team teamB;

    private int scoreA;

    private int scoreB;

    /** IN_PROGRESS, FINISHED 등 간단한 상태 값 */
    private String status;

    /** 경기 날짜 (경기 생성 시 자동 설정) */
    private LocalDate gameDate;

    /**
     * 이닝별 득점 (쉼표 구분 문자열, A팀 공격 이닝 순서)
     * 예) 9이닝: "0,1,0,0,3,0,0,0,1"
     */
    private String inningScoresA;

    /**
     * 이닝별 득점 (쉼표 구분 문자열, B팀 공격 이닝 순서)
     * 말 이닝이 진행되지 않았을 때(홈팀 역전 마감) 마지막 값 없음 → 표시 시 "X" 처리
     */
    private String inningScoresB;
}

