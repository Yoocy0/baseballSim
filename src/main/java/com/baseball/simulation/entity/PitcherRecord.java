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
 * 투수별 시즌 누적 성적 엔티티입니다.
 * <p>
 * JPA @ManyToOne 연관 관계를 사용하지 않고, pitcherId(Long)를 직접 저장합니다.
 * 파생 지표(ERA·피안타율·WHIP)는 필드로 관리하지 않으며 출력 시 계산합니다.
 * (pitcherId, seasonYear) 조합은 유니크 제약으로 중복 생성을 방지합니다.
 * <p>
 * [이닝 저장 방식 - inningsX3]
 * 이닝을 3배수로 저장합니다. 예) 6.2이닝 → 20, 9.0이닝 → 27
 * 출력 시: String.format("%d.%d", inningsX3/3, inningsX3%3)
 * 이유: DB에서 소수점 이닝(6.333...)을 정확히 저장하기 위함.
 * <p>
 * TODO(part2): 자책점(earnedRuns) 구분 추가 → ERA 정확도 향상
 */
@Entity
@Table(
        name = "pitcher_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_pitcher_records_pitcher_season",
                columnNames = {"pitcher_id", "season_year"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class PitcherRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long pitcherId;

    private int seasonYear;

    private int games;           // 출장 경기 수
    private int wins;            // 승
    private int losses;          // 패
    private int strikeouts;      // 탈삼진
    private int walks;           // 볼넷 허용

    /** 이닝 * 3. 6.2이닝 = 20, 9.0이닝 = 27 */
    private int inningsX3;

    private int hitsAllowed;     // 피안타
    private int homeRunsAllowed; // 피홈런
    private int battersFaced;    // 상대 타자 수
    private int pitchesThrown;   // 총 투구수
    private int runsAllowed;     // 실점 (※ 현재는 자책점 구분 없이 실점으로 ERA 계산)

    public static PitcherRecord create(Long pitcherId, int seasonYear) {
        PitcherRecord r = new PitcherRecord();
        r.pitcherId  = pitcherId;
        r.seasonYear = seasonYear;
        return r;
    }

    /** 출력용 이닝 문자열. 예) 20 → "6.2",  27 → "9.0" */
    public String inningsDisplay() {
        return String.format("%d.%d", inningsX3 / 3, inningsX3 % 3);
    }
}
