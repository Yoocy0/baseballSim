package com.baseball.simulation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "game_records")
@Getter
@Setter
@NoArgsConstructor
public class GameRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    private Game game;

    private int inning; // 1,2,3,... (회)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pitcher_id")
    private Player pitcher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batter_id")
    private Player batter;

    /**
     * 해당 타석의 n구째
     */
    private int pitchSequence;

    /**
     * 투구 결과: STRIKE / BALL / HIT
     */
    private String pitchResult;

    /**
     * 타석 최종 결과: SINGLE, DOUBLE, TRIPLE, HOMERUN, STRIKEOUT, WALK 등
     * 타석 종료 시에만 값이 들어감.
     */
    private String paResult;

    /**
     * 타석 종료 여부 플래그
     */
    private boolean paEnd;

    /**
     * 당시의 볼/스트라이크/아웃 카운트 상황
     * 예: "B2-S1-O1"
     */
    @Column(name = "b_s_o_count")
    private String bsoCount;
}

