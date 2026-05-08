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

    /**
     * 이 타석에서 발생한 득점 수.
     * <p>
     * - paEnd=true (타석 종료) 레코드: 0~4 실제 득점 값을 가집니다.
     *   (무득점=0, 최대 만루홈런=4)
     * - paEnd=false (중간 투구) 레코드: null (득점 발생 불가 시점)
     * <p>
     * 박스스코어 이닝별 득점 및 투수 실점 계산에 사용됩니다.
     */
    @Column(name = "runs_this_pa")
    private Integer runsThisPA;
}

