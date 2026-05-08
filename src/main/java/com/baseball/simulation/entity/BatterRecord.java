package com.baseball.simulation.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 타자별 시즌 누적 성적 엔티티입니다.
 * <p>
 * JPA @ManyToOne 연관 관계를 사용하지 않고, batterId(Long)를 직접 저장합니다.
 * 파생 지표(타율·출루율·장타율·OPS)는 필드로 관리하지 않으며 출력 시 계산합니다.
 * (batterId, seasonYear) 조합은 유니크 제약으로 중복 생성을 방지합니다.
 * <p>
 * TODO(part2): pitcherId 별 상대 성적, 좌우 타석 분리 통계 추가
 */
@Entity
@Table(
        name = "batter_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_batter_records_batter_season",
                columnNames = {"batter_id", "season_year"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class BatterRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long batterId;

    private int seasonYear;

    private int plateAppearances; // 타석
    private int atBats;           // 타수 (PA - 볼넷 등)
    private int hits;             // 안타 (1루+2루+3루+홈런 합계)
    private int doubles;          // 2루타
    private int triples;          // 3루타
    private int homeRuns;         // 홈런
    private int runs;             // 득점 (본인이 홈 터치)
    private int rbi;              // 타점
    private int strikeouts;       // 삼진
    private int walks;            // 볼넷

    public static BatterRecord create(Long batterId, int seasonYear) {
        BatterRecord r = new BatterRecord();
        r.batterId   = batterId;
        r.seasonYear = seasonYear;
        return r;
    }
}
