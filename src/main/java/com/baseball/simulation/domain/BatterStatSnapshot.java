package com.baseball.simulation.domain;

import com.baseball.simulation.entity.BatterRecord;

/**
 * 경기 진행 중 인메모리에서 타자 성적을 추적하는 뮤터블 도메인 객체입니다.
 * <p>
 * 경기 시작 시 DB에서 BatterRecord를 로드해 이 객체로 변환합니다.
 * 매 타석 결과가 나올 때마다 DB 저장 없이 즉시 수치를 업데이트하고,
 * 경기 종료 후 한 번의 saveAll로 DB에 일괄 반영합니다.
 * <p>
 * GameLogicService 계층에서 사용되며, 엔티티를 직접 보유하지 않습니다.
 */
public class BatterStatSnapshot {

    private final Long id;         // null이면 신규 생성 대상
    private final Long batterId;
    private final int  seasonYear;

    private int plateAppearances;
    private int atBats;
    private int hits;
    private int doubles;
    private int triples;
    private int homeRuns;
    private int runs;
    private int rbi;
    private int strikeouts;
    private int walks;

    /** DB에서 로드한 BatterRecord를 스냅샷으로 변환합니다. */
    public static BatterStatSnapshot from(BatterRecord record) {
        BatterStatSnapshot snap = new BatterStatSnapshot(
                record.getId(), record.getBatterId(), record.getSeasonYear());
        snap.plateAppearances = record.getPlateAppearances();
        snap.atBats           = record.getAtBats();
        snap.hits             = record.getHits();
        snap.doubles          = record.getDoubles();
        snap.triples          = record.getTriples();
        snap.homeRuns         = record.getHomeRuns();
        snap.runs             = record.getRuns();
        snap.rbi              = record.getRbi();
        snap.strikeouts       = record.getStrikeouts();
        snap.walks            = record.getWalks();
        return snap;
    }

    /** DB에 해당 선수 성적이 없을 때 빈 스냅샷을 생성합니다. */
    public static BatterStatSnapshot blank(Long batterId, int seasonYear) {
        return new BatterStatSnapshot(null, batterId, seasonYear);
    }

    private BatterStatSnapshot(Long id, Long batterId, int seasonYear) {
        this.id         = id;
        this.batterId   = batterId;
        this.seasonYear = seasonYear;
    }

    // -----------------------------------------------------------------------
    // 타석 결과 반영
    // -----------------------------------------------------------------------

    /**
     * 한 타석의 결과를 인메모리 수치에 즉시 반영합니다.
     *
     * @param paResultCode 타석 결과 코드 (STRIKEOUT / WALK / FIELD_OUT / SINGLE / DOUBLE / TRIPLE / HOMERUN)
     * @param runsInThisPA 이 타석에서 득점한 수 (타점 계산용)
     */
    public void applyPaResult(String paResultCode, int runsInThisPA) {
        plateAppearances++;
        rbi += runsInThisPA;

        switch (paResultCode) {
            case "STRIKEOUT" -> { atBats++; strikeouts++; }
            case "WALK"      -> walks++;
            case "FIELD_OUT" -> atBats++;
            case "SINGLE"    -> { atBats++; hits++; }
            case "DOUBLE"    -> { atBats++; hits++; doubles++; }
            case "TRIPLE"    -> { atBats++; hits++; triples++; }
            case "HOMERUN"   -> {
                atBats++; hits++; homeRuns++;
                // 홈런 타자 본인의 득점은 InningProcessor에서 scorerIds를 통해 addRun()으로 처리
            }
            default -> { /* 미지원 결과: 통계 반영 없음 */ }
        }
    }

    /**
     * 선수가 홈인(득점)했을 때 호출합니다.
     * scorerIds를 통해 InningProcessor에서 명시적으로 호출됩니다.
     */
    public void addRun() {
        runs++;
    }

    // -----------------------------------------------------------------------
    // 파생 지표 계산 (출력용)
    // -----------------------------------------------------------------------

    /** 타율 = hits / atBats. atBats == 0이면 0.000 */
    public double battingAvg() {
        return atBats == 0 ? 0.0 : (double) hits / atBats;
    }

    /** 출루율 = (hits + walks) / plateAppearances. PA == 0이면 0.000 */
    public double onBasePct() {
        return plateAppearances == 0 ? 0.0 : (double) (hits + walks) / plateAppearances;
        // TODO(part2): HBP(몸에 맞는 공), 희생플라이 고려
    }

    /** 장타율 = (1루타 + 2*2루타 + 3*3루타 + 4*홈런) / atBats */
    public double sluggingPct() {
        if (atBats == 0) return 0.0;
        int singles = hits - doubles - triples - homeRuns;
        return (double) (singles + 2 * doubles + 3 * triples + 4 * homeRuns) / atBats;
    }

    /** OPS = 출루율 + 장타율 */
    public double ops() {
        return onBasePct() + sluggingPct();
    }

    // -----------------------------------------------------------------------
    // Getters (불변 필드 포함)
    // -----------------------------------------------------------------------

    public Long   getId()               { return id; }
    public Long   getBatterId()         { return batterId; }
    public int    getSeasonYear()       { return seasonYear; }
    public int    getPlateAppearances() { return plateAppearances; }
    public int    getAtBats()           { return atBats; }
    public int    getHits()             { return hits; }
    public int    getDoubles()          { return doubles; }
    public int    getTriples()          { return triples; }
    public int    getHomeRuns()         { return homeRuns; }
    public int    getRuns()             { return runs; }
    public int    getRbi()              { return rbi; }
    public int    getStrikeouts()       { return strikeouts; }
    public int    getWalks()            { return walks; }
}
