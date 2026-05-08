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
 * 팀별 시즌 성적 엔티티입니다.
 * <p>
 * JPA @ManyToOne 연관 관계를 사용하지 않고, teamId(Long)를 직접 저장합니다.
 * winRate(승률)는 필드로 관리하지 않으며 출력 시 계산합니다.
 * (teamId, seasonYear) 조합은 유니크 제약이 있어 중복 생성을 방지합니다.
 */
@Entity
@Table(
        name = "team_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_team_records_team_season",
                columnNames = {"team_id", "season_year"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class TeamRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long teamId;

    private int seasonYear;

    private int wins;

    private int losses;

    public static TeamRecord create(Long teamId, int seasonYear) {
        TeamRecord record = new TeamRecord();
        record.teamId     = teamId;
        record.seasonYear = seasonYear;
        record.wins       = 0;
        record.losses     = 0;
        return record;
    }

    public void addWin()  { this.wins++; }
    public void addLoss() { this.losses++; }

    /** 0-0 상황에서 Division by Zero를 방지합니다. */
    public double winRate() {
        int played = wins + losses;
        return played == 0 ? 0.0 : (double) wins / played;
    }
}
